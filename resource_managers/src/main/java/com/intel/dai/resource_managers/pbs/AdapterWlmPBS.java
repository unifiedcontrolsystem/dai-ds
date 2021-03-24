// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.resource_managers.pbs;

import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.exceptions.ProviderException;
import com.intel.dai.resource_managers.WlmProvider;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.dsapi.*;
import com.intel.dai.resource_managers.AdapterWlm;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.AdapterInformation;
import com.intel.networking.sink.NetworkDataSink;
import com.intel.networking.sink.NetworkDataSinkFactory;
import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.intel.xdg.XdgConfigFile;

import java.io.*;
import java.lang.*;
import java.util.*;
import static java.lang.Math.toIntExact;
import java.text.ParseException;

/**
 * AdapterWlmPBS for the VoltDB database - specific instance of a WLM adapter that handles the PBS job scheduler.
 *
 * Parms:
 *  List of the db node names, so that this client connects to each of them (this is a comma separated list of hostnames or IP addresses.
 *  E.g., voltdbserver1,voltdbserver2,10.11.12.13
 *
 * Example invocation:
 *      java AdapterWlmPBS voltdbserver1,voltdbserver2,10.11.12.13  (or java AdapterWlmPBS  - this will default to using localhost)
 */
public class AdapterWlmPBS implements WlmProvider {
    private AdapterInformation adapter;
    private final boolean DoAssociateJobIdAndNodeInCachedJobsTable      = true;         // constant that is used when we do want to associate the job id and node in the cached jobs table.
    private WorkQueue workQueue;
    private Logger log_;
    private long mNumMsgsHndld = 0;  // number of messages that have been handled within the handleInputFromExternalComponent method.
    public Jobs jobs;
    public RasEventLog raseventlog;
    public NodeInformation nodeinfo;
    public ConfigIO jsonParser;
    public XdgConfigFile xdg;

    // Constructor
    public AdapterWlmPBS(Logger log, AdapterInformation iadapter, DataStoreFactory factory) {

        log_ = log;
        adapter = iadapter;

        jobs = factory.createJobApi();
        raseventlog = factory.createRasEventLog(adapter);
        workQueue = factory.createWorkQueue(adapter);
        nodeinfo = factory.createNodeInformation();
        jsonParser = ConfigIOFactory.getInstance("json");
        assert jsonParser != null: "Failed to get a JSON parser!";
    }

    //---------------------------------------------------------
    // Handle input coming in from the "attached" external component (e.g., PBS Pro, Slurm, Cobalt, LSF), looking for things that we are interested in / need to take action on.
    //      Since this is the adapter for PBS, this method monitors the kafka bus looking for things we need to take action on.
    //---------------------------------------------------------
    @Override
    public long handleInputFromExternalComponent(String[] aWiParms) throws ProviderException {

        long rc = 0;

        try {
            HashMap<String, String> args = new HashMap<String, String>();

            String configName = AdapterWlmPBS.class.getSimpleName() + ".json";
            xdg = new XdgConfigFile("ucs");
            InputStream result = xdg.Open(configName);
            if(result == null)
                throw new FileNotFoundException("Failed to locate or open '" + configName + "'");

            PropertyMap configJson = jsonParser.readConfig(result).getAsMap();
            args.put("bootstrap.servers", configJson.getString("bootstrap.servers"));
            args.put("group.id", configJson.getString("group.id"));
            args.put("schema.registry.url", configJson.getString("schema.registry.url"));
            args.put("topics", configJson.getString("topics"));
            args.put("auto.commit.enable", configJson.getString("auto.commit.enable"));
            args.put("specific.avro.reader", configJson.getString("specific.avro.reader"));
            args.put("auto.offset.reset", configJson.getString("auto.offset.reset"));

            NetworkDataSink sink = NetworkDataSinkFactory.createInstance(log_, "kafka", args);
            sink.setLogger(log_);
            sink.setCallbackDelegate(this::processSinkMessage);
            sink.startListening();
            // Keep this "thread" active (processing messages off the queue) until we want the adapter to go away.
            waitUntilFinishedProcessingMessages();
            log_.info("handleInputFromExternalComponent - exiting");

        }
        catch (Exception e) {
            String eventtype = "RasGenAdapterUnableToConnectToAmqp";
            String instancedata = "AdapterName=" + adapter.getName();
            raseventlog.logRasEventSyncNoEffectedJob(eventtype, instancedata, null, System.currentTimeMillis() * 1000L, adapter.getType(), workQueue.workItemId());
            log_.exception(e, "Unable to connect to network sink");
            rc = 1;
        }
        finally {
            shutDown();
        }

        return rc;

    }


    private void processSinkMessage(String subject, String message) {

        log_.info("Received message for subject: %s", subject);
        try {
            if (subject.equals("pbs_runjobs")) {
                PropertyMap jsonMessage = jsonParser.fromString(message).getAsMap();
                handlePBSJobMessages(message, jsonMessage);
            }
            else {
                log_.error("Could not determine message origin: " + message);
            }
        }
        catch (Exception e) {
            // Log the exception, generate a RAS event and continue parsing the console and varlog messages
            e.printStackTrace();
            log_.exception(e, "handleDelivery - Exception occurred while processing an individual message - '" + message + "'!");
            String eventtype = "RasProvException";
            String instancedata = "AdapterName=" + adapter.getName() + ", Exception=" + e.toString();
            raseventlog.logRasEventNoEffectedJob(eventtype, instancedata, null, System.currentTimeMillis() * 1000L, adapter.getType(), workQueue.workItemId());
        }
    }

    void waitUntilFinishedProcessingMessages() throws InterruptedException {
        do {
            Thread.sleep(SHUTDOWN_CHECK_INTERVAL_MS);
        } while (!adapter.isShuttingDown());
    }

    //--------------------------------------------------------------------------
    // Get a BitSet that contains this list of pbs nodes (including doing any expansion of wild cards)
    //--------------------------------------------------------------------------
    private BitSet getBitSetOfPBSNodes(String sNodeList, String sJobId, long lStartTimeInMicrosecs, boolean bAssociateJobIdAndNodeInCachedJobsTable)
            throws InterruptedException, IOException, DataStoreException
    {
        // Extract the array list of nodes from the node list (including doing any expansion of wild cards).
        ArrayList<String> alNodes = new ArrayList<String>(Arrays.asList(sNodeList.replaceAll("\"","").split(",")));
        // Loop through the list of node's and adding them to the JobStep's bitset of nodes
        // (note: we need to calculate the node's lctn from the node's hostname).
        BitSet bsNodes = new BitSet(12800);  // BitSet to represent the compute nodes that are allocated to this session.
        ArrayList<String> alNodeLctns = new ArrayList<String>(alNodes.size());

        for (String sNode: alNodes) {
            // Determine the node's lctn string (used when we put this node into the job's bitset of nodes).
            String sTempNodeLctn = nodeinfo.getComputeNodeLocationFromHostnameMap().get(sNode);
            // Ensure that we got a valid lctn for this node.
            if (sTempNodeLctn != null) {
                // do have a valid node lctn.
                // Add this node into the job's bitset of nodes.
                Long seqNum = nodeinfo.getComputeNodeSequenceNumberFromLocationMap().get(sTempNodeLctn);
                if (seqNum == null) {
                    log_.warn("getBitSetOfPBSNodes - No sequence number found for this job - JobId=%s", sJobId);
                    return bsNodes;
                }
                bsNodes.set( toIntExact(seqNum) );
                // Save this node's lctn in the list of nodes being used by this job.
                if (bAssociateJobIdAndNodeInCachedJobsTable) {
                    alNodeLctns.add(sTempNodeLctn);
                }
            }
            else {
                // unable to find the node's lctn.
                log_.error("getBitSetOfPBSNodes - JobId=%s - unexpected Node (%s), could not find it in the map of compute node names to node lctns!", sJobId, sNode);
                // Cut RAS event indicating that the job has been killed - we do know which job was effected by this occurrence.
                String eventtype = "RasWlmInvalidHostname";
                String instancedata = "JobId=" + sJobId + ", Hostname=" + sNode + ",AdapterName=" + adapter.getName();
                raseventlog.logRasEventNoEffectedJob(eventtype, instancedata, null, System.currentTimeMillis() * 1000L, adapter.getType(), workQueue.workItemId());
            }
        }
        // Also associate all of these nodes with this job in the InternalCachedJobs table - so information is available for others to use when checking which nodes are being used by which jobs, and visa versa.
        if (bAssociateJobIdAndNodeInCachedJobsTable) {
            String aTempNodeLctns[] = new String[alNodeLctns.size()];
            aTempNodeLctns = alNodeLctns.toArray(aTempNodeLctns);
            assocJobIdAndNodeInCachedJobsTable(sJobId, lStartTimeInMicrosecs, aTempNodeLctns);
        }
        return bsNodes;
    }

    //---------------------------------------------------------
    // This method is used to associate a compute node location with a JobId in the InternalCachedJobs table.
    //---------------------------------------------------------
    private final void assocJobIdAndNodeInCachedJobsTable(String sJobId, long lStartTimeInMicrosecs, String[] aNodeLctns) throws IOException, InterruptedException, DataStoreException {

        jobs.addNodeEntryToInternalCachedJobs(aNodeLctns, sJobId, lStartTimeInMicrosecs);

    }


    private void handleJobStartedMsg(PropertyMap jsonMessage) throws PropertyNotExpectedType, InterruptedException, IOException, DataStoreException
    {

        long lStartTsInMicroSecs = jsonMessage.getLong("timestamp") *1000000L;

        String sJobId = jsonMessage.getString("job_id");
        String sNodeList = jsonMessage.getString("host");

        BitSet bsJobNodes = getBitSetOfPBSNodes(sNodeList, sJobId, lStartTsInMicroSecs, DoAssociateJobIdAndNodeInCachedJobsTable);  // since this is a job start method, do associate job id in cached jobs table.

        String sJobName  = jsonMessage.getString("job_name");
        String sUserName = jsonMessage.getString("user");

        //--------------------------------------------------------------
        // Update the JobInfo with the data from this log entry AND get all of the JobInfo for this job.
        //--------------------------------------------------------------
        HashMap<String, Object> jobinfo = jobs.startJobinternal(sJobId, lStartTsInMicroSecs);

        //--------------------------------------------------------------
        // Update the JobInfo with the data from this log entry AND get all of the JobInfo for this job.
        //--------------------------------------------------------------

        jobs.startJob(sJobId, sJobName, adapter.getHostname(), bsJobNodes.cardinality(), bsJobNodes.toByteArray(), sUserName, lStartTsInMicroSecs, adapter.getType(), workQueue.workItemId());

        //--------------------------------------------------------------
        // Check & see if we now have seen the JobCompletion as well as this JobStarted message - if so then the job is done-done, go ahead and handle that processing.
        //--------------------------------------------------------------
        if (jobinfo != null && ((String)jobinfo.get("WlmJobStarted")).equals("T")    &&
                ((String)jobinfo.get("WlmJobCompleted")).equals("T")  )
        {   // all of the job state messages have now been seen - go ahead and handle the fact that this job is done-done.
            // Handle the stuff needed when a job ends.
            handleEndOfJobProcessing(sJobId, jobinfo, (String) jobinfo.get("ExitStatus"));
        }
    }


    private void handleJobCompletionMsg(PropertyMap jsonMessage) throws PropertyNotExpectedType, DataStoreException
    {

        //------------------------------------------------------------------
        // Get the pertinent data out of the record.
        //------------------------------------------------------------------
        String sJobId       = jsonMessage.getString("job_id");
        String sWlmJobState = "T";
        String sExitStatus = jsonMessage.getString("exit_status");
        String workDir = jsonMessage.getString("work_directory");

        //--------------------------------------------------------------
        // Update the JobInfo with the data from this log entry AND get all of the JobInfo for this job.
        //--------------------------------------------------------------
        long lEndTsInMicroSecs = jsonMessage.getLong("timestamp") *1000000L;
        HashMap<String, Object> jobinfo = jobs.completeJobInternal(sJobId, workDir, sWlmJobState, lEndTsInMicroSecs, -1L);

        //--------------------------------------------------------------
        // Check & see if we now have seen the JobStarted as well as this JobCompletion message - if so then the job is done-done, go ahead and handle that processing.
        //--------------------------------------------------------------
        if (jobinfo != null && ((String) jobinfo.get("WlmJobStarted")).equals("T")   &&
                ((String) jobinfo.get("WlmJobCompleted")).equals("T") )
        {   // all of the job state messages have now been seen - go ahead and handle the fact that this job is done-done.
            // Handle the stuff needed when a job ends.
            handleEndOfJobProcessing(sJobId, jobinfo, sExitStatus);
        }

    }

    //--------------------------------------------------------------------------
    // Handle all of the database updates, map cleanup, etc. that needs to happen when a job is totally finished.
    //--------------------------------------------------------------------------
    private void handleEndOfJobProcessing(String sJobId, HashMap<String, Object> jobInfo, String sExitStatus) throws DataStoreException  {

        //--------------------------------------------------------------
        // Put the 'Job Termination' record into the Job table.
        //--------------------------------------------------------------
        if (jobInfo != null) {
            jobs.terminateJob(sJobId, null, Integer.parseInt(sExitStatus), (String) jobInfo.get("WlmJobWorkDir"), (String) jobInfo.get("WlmJobState"), ((Long) jobInfo.get("WlmJobEndTime")).longValue(), adapter.getType(), workQueue.workItemId());

            //--------------------------------------------------------------
            // Put the appropriate 'Job Completion' information into the Internal Cached Jobs table (note this table is different from the InternalJobInfo table).
            //--------------------------------------------------------------
            // Update the Internal Cached Jobs table to fill in this job's termination time.
            jobs.terminateJobInternal(((Long) jobInfo.get("WlmJobEndTime")).longValue(), System.currentTimeMillis() * 1000L, sJobId);

            //----------------------------------------------------------
            // Remove this job's JobInfo entry.
            //----------------------------------------------------------
            jobs.removeJobInternal(sJobId, ((Long) jobInfo.get("WlmJobStartTime")).longValue());
        }

    }

    void handlePBSJobMessages(String sLine, PropertyMap jsonMessage) throws PropertyNotExpectedType, DataStoreException, InterruptedException, IOException
    {
        String start_or_end_indicator = jsonMessage.getString("job_state");

        if (start_or_end_indicator.equals("Job Running")){
            log_.info("FOUND a 'Job Running' log message - %s", sLine);
            handleJobStartedMsg(jsonMessage);
        }
        else if (start_or_end_indicator.equals("Job Completed")){
            log_.info("FOUND a 'Job Completed' log message - %s", sLine);
            handleJobCompletionMsg(jsonMessage);
        }

    }

    public void shutDown() {
        log_.info("Shutting down the adapter gracefully");
        adapter.signalToShutdown();
    }

    public static void main(String[] args) {
        if(args == null || args.length != 3)
            throw new RuntimeException(String.format("Wrong number of arguments for this adapter (%s), must " +
                            "use 3 arguments: voltdb_servers, location and hostname in that order",
                    AdapterWlmPBS.class.getCanonicalName()));

        String SnLctn     = args[1];
        String SnHostname = args[2];


        AdapterInformation baseAdapter = new AdapterInformation("WLM", AdapterWlmPBS.class.getCanonicalName(), SnLctn, SnHostname, -1L);
        Logger log = LoggerFactory.getInstance(baseAdapter.getType(), baseAdapter.getName(), "console");

        DataStoreFactory factory = new DataStoreFactoryImpl(args, log);

        AdapterWlmPBS pbsAdapter = new AdapterWlmPBS(log, baseAdapter, factory);
        AdapterWlm wlmAdapter = new AdapterWlm(baseAdapter, pbsAdapter, factory, log);
        Runtime.getRuntime().addShutdownHook(new Thread(pbsAdapter::shutDown));

        System.exit(wlmAdapter.run());
    }

}
