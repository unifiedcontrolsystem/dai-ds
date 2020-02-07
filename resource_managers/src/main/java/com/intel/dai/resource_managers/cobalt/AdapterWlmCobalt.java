// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.resource_managers.cobalt;

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

import java.io.*;
import java.lang.*;
import java.text.SimpleDateFormat;
import java.util.*;
import static java.lang.Math.toIntExact;
import java.text.ParseException;

/**
 * AdapterWlmCobalt for the VoltDB database - specific instance of a WLM adapter that handles the Cobalt job scheduler.
 *
 * Parms:
 *  List of the db node names, so that this client connects to each of them (this is a comma separated list of hostnames or IP addresses.
 *  E.g., voltdbserver1,voltdbserver2,10.11.12.13
 *
 * Example invocation:
 *      java AdapterWlmCobalt voltdbserver1,voltdbserver2,10.11.12.13  (or java AdapterWlmCobalt  - this will default to using localhost)
 */
public class AdapterWlmCobalt implements WlmProvider {
    private AdapterInformation adapter;
    private final boolean DoAssociateJobIdAndNodeInCachedJobsTable      = true;         // constant that is used when we do want to associate the job id and node in the cached jobs table.
    private WorkQueue workQueue;
    private Logger log_;
    private long mNumMsgsHndld = 0;  // number of messages that have been handled within the handleInputFromExternalComponent method.
    private SimpleDateFormat oldDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    public Reservations reservations;
    public Jobs jobs;
    public RasEventLog raseventlog;
    public NodeInformation nodeinfo;

    // Constructor
    public AdapterWlmCobalt(Logger log, AdapterInformation iadapter, DataStoreFactory factory) {

        log_ = log;
        adapter = iadapter;

        jobs = factory.createJobApi();
        reservations = factory.createReservationApi();
        raseventlog = factory.createRasEventLog(adapter);
        workQueue = factory.createWorkQueue(adapter);
        nodeinfo = factory.createNodeInformation();

        sqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // This line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
    }

    //---------------------------------------------------------
    // Handle input coming in from the "attached" external component (e.g., PBS Pro, Slurm, Cobalt, LSF), looking for things that we are interested in / need to take action on.
    //      Since this is the adapter for Cobalt, this method monitors the input log file looking for things we need to take action on.
    //---------------------------------------------------------
    @Override
    public long handleInputFromExternalComponent(String[] aWiParms) throws ProviderException {

        long rc = 0;

        try {
            HashMap<String, String> args = new HashMap<String, String>();
            args.put("exchangeName","cobalt");
            args.put("subjects","InputFromLogstashForAdapterWlm");

            String rabbitMQ = "amqp://127.0.0.1";
            for(String nameValue: aWiParms) {
                if(nameValue.startsWith("RabbitMQHost="))
                    rabbitMQ = "amqp://" + nameValue.substring(nameValue.indexOf("=")+1).trim();
            }
            log_.info("rabbitMQ: " + rabbitMQ);
            args.put("uri", rabbitMQ);
            log_.info("Before create instance");
            NetworkDataSink sink = NetworkDataSinkFactory.createInstance(log_, "rabbitmq", args);
            log_.info("Before set logger");
            sink.setLogger(log_);
            log_.info("Before set callback delegate");
            sink.setCallbackDelegate(this::processSinkMessage);
            log_.info("Before start listening");
            sink.startListening();
            log_.info("Before processing messages");
            // Keep this "thread" active (processing messages off the queue) until we want the adapter to go away.
            waitUntilFinishedProcessingMessages();
            log_.info("handleInputFromExternalComponent - exiting");

        }
        catch (InterruptedException | NetworkDataSinkFactory.FactoryException e) {
            try {
                String eventtype = raseventlog.getRasEventType("RasGenAdapterUnableToConnectToAmqp", workQueue.workItemId());
                String instancedata = "AdapterName=" + adapter.getName() + ", QueueName=InputFromLogstashForAdapterWlm";
                raseventlog.logRasEventSyncNoEffectedJob(eventtype, instancedata, null, System.currentTimeMillis() * 1000L, adapter.getType(), workQueue.workItemId());
                log_.error("Unable to connect to network sink");
                rc = 1;
            }
            catch (IOException ex){
                log_.exception(ex, "Unable to log RAS EVENT");
                shutDown();
                rc = 1;
            }
        }
        finally {
            shutDown();
        }

        return rc;

    }   // End handleInputFromExternalComponent(String[] aWiParms)


    private void processSinkMessage(String subject, String message) {
        log_.info("Received message for subject: %s", subject);
        try {
            //--------------------------------------------------------------
            // Process the message that just came in.
            //--------------------------------------------------------------
            try {
                //----------------------------------------------------------
                // Determine what type of record this is.
                message = message.trim();

                String[] aLineCols = message.split(" ");

                // This record came in via bgsched log.
                if (aLineCols[0].contains("bgsched")) {
                    handleCobaltReservationMessages(message, aLineCols);
                }
                // This record came in via cqm log.
                else if (aLineCols[0].contains("cqm")) {
                    handleCobaltJobMessages(message, aLineCols);
                }
                else {
                    log_.error("Could not determine message origin: " + message);
                }

            }
            catch (Exception e) {
                // Log the exception, generate a RAS event and continue parsing the console and varlog messages
                log_.exception(e, "handleDelivery - Exception occurred while processing an individual message - '" + message + "'!");
                String eventtype = raseventlog.getRasEventType("RasProvException", workQueue.workItemId());
                String instancedata = "AdapterName=" + adapter.getName() + ", Exception=" + e.toString();
                raseventlog.logRasEventSyncNoEffectedJob(eventtype, instancedata, null, System.currentTimeMillis() * 1000L, adapter.getType(), workQueue.workItemId());
            }
        }
        catch (Exception e) {
            // Log the exception, generate a RAS event and continue parsing the console and varlog messages
            try {
                log_.exception(e, "handleDelivery - Exception occurred!");
                String eventtype = raseventlog.getRasEventType("RasProvException", workQueue.workItemId());
                String instancedata = "AdapterName=" + adapter.getName() + ", Exception=" + e.toString();
                raseventlog.logRasEventSyncNoEffectedJob(eventtype, instancedata, null, System.currentTimeMillis() * 1000L, adapter.getType(), workQueue.workItemId());
            }
            catch (Exception ex) {
                log_.exception(ex, "Unable to log RAS EVENT");
            }

        }

    }

    void waitUntilFinishedProcessingMessages() throws InterruptedException {
        do {
            Thread.sleep(SHUTDOWN_CHECK_INTERVAL_MS);
        } while (!adapter.isShuttingDown());
    }   // End waitUntilFinishedProcessingMessages()

    //--------------------------------------------------------------------------
    // Get a BitSet that contains this list of cobalt nodes (including doing any expansion of wild cards)
    //--------------------------------------------------------------------------
    private BitSet getBitSetOfCobaltNodes(String sNodeList, String sJobId, Date dTimestamp, boolean bAssociateJobIdAndNodeInCachedJobsTable)
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
                    log_.warn("getBitSetOfCobaltNodes - No sequence number found for this job - JobId=%s", sJobId);
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
                log_.error("getBitSetOfCobaltNodes - JobId=%s - unexpected Node (%s), could not find it in the map of compute node names to node lctns!", sJobId, sNode);
                // Cut RAS event indicating that the job has been killed - we do know which job was effected by this occurrence.
                String eventtype = raseventlog.getRasEventType("RasWlmInvalidHostname", workQueue.workItemId());
                String instancedata = "JobId=" + sJobId + ", Hostname=" + sNode + ",AdapterName=" + adapter.getName();
                raseventlog.logRasEventSyncNoEffectedJob(eventtype, instancedata, null, System.currentTimeMillis() * 1000L, adapter.getType(), workQueue.workItemId());
            }
        }
        // Also associate all of these nodes with this job in the InternalCachedJobs table - so information is available for others to use when checking which nodes are being used by which jobs, and visa versa.
        if (bAssociateJobIdAndNodeInCachedJobsTable) {
            String aTempNodeLctns[] = new String[alNodeLctns.size()];
            aTempNodeLctns = alNodeLctns.toArray(aTempNodeLctns);
            assocJobIdAndNodeInCachedJobsTable(sJobId, dTimestamp.getTime() * 1000L, aTempNodeLctns);
        }
        return bsNodes;
    }   // End getBitSetOfCobaltNodes(String sNodeList, String sJobId, boolean bAssociateJobIdAndNodeInCachedJobsTable)

    //---------------------------------------------------------
    // This method is used to associate a compute node location with a JobId in the InternalCachedJobs table.
    //---------------------------------------------------------
    private final void assocJobIdAndNodeInCachedJobsTable(String sJobId, long lStartTimeInMicrosecs, String[] aNodeLctns) throws IOException, InterruptedException, DataStoreException {

        jobs.addNodeEntryToInternalCachedJobs(aNodeLctns, sJobId, lStartTimeInMicrosecs);

    }   // End assocJobIdAndNodeInCachedJobsTable(String sJobId, long lStartTimeInMicrosecs, String[] aNodeLctns)


    private void handleJobStartedMsg(String[] aLineCols)
            throws IOException, InterruptedException, ParseException, DataStoreException
    {
        // 2019-04-03 00:32:17 04/03/2019 00:32:17;S;327220;Resource_List.ncpus=3000 Resource_List.nodect=3000 Resource_List.walltime=1:00:00 account=^P27857609013029^ args= ctime=1554251516.32 cwd=/lus/theta-fs0/projects/^P76243759028727^/^FILEPATH REMOVED^^U73007516364104^/^FILEPATH REMOVED^ etime=1554251516.32 exe=/home/^U73007516364104^/^FILEPATH REMOVED^ exec_host="0-4,6-7,9-11,20-29,52-75,90-109,148-259,270-279,290-299,310-319,340-349,360-395,404-409,411-417,419-439,451,454-458,480-515,517-523,532-559,600-639,659,662-665,667-671,678-699,740-759,770-779,788-815,820-843,852-893,895,900-907,916-939,960-979,1000-1025,1027-1059,1080-1087,1100-1109,1113-1115,1118-1159,1172-1179,1190-1200,1202-1207,1210-1227,1236-1245,1262,1264-1272,1276-1289,1300-1319,1330-1337,1342-1343,1370-1380,1382-1388,1390-1399,1440-1442,1444,1446-1507,1511,1521,1523-1527,1530,1532,1534-1536,1544-1545,1556-1559,1570-1591,1593,1595-1597,1599-1611,1620-1629,1653,1655,1684-1709,1718-1734,1736-1739,1750-1787,1791,1794-1795,1798,1800-1879,1897,1900-1935,1940-1961,1964-1979,1986-1987,1990,1993-1994,2020-2033,2035-2059,2080-2111,2113-2117,2136-2155,2157,2160-2175,2180-2191,2194-2239,2260-2319,2324,2337,2360-2379,2388-2402,2404-2409,2411-2443,2452-2469,2481,2500-2574,2576-2639,2669,2680-2703,2708-2763,2772-2779,2803,2805,2836,2840-2851,2853,2855-2857,2859-2873,2875-2899,2908,2912,2917-2919,2940-2957,2964-2977,2980-2989,2992-2999,3001-3021,3024-3025,3027-3031,3035,3038-3039,3042,3060-3071,3074-3075,3082-3083,3086-3087,3093-3097,3100-3106,3108,3110-3119,3124-3129,3141,3160-3167,3190-3194,3196,3198-3203,3205-3211,3223-3235,3239-3251,3254,3256-3259,3262,3264-3303,3306-3308,3310-3339,3341-3359,3361,3364,3369,3390-3393,3398,3400-3409,3430-3431,3433-3434,3436-3459,3464-3483,3485-3499,3510-3527,3529-3539,3549,3569,3577,3580-3594,3596-3599,3610-3611,3615-3617,3620,3626-3627,3640-3650,3652-3698,3705,3710,3720-3727,3729-3737,3740-3759,3765,3770-3782,3784-3792,3794-3796,3798,3800-3880,3885,3895,3900,3902-3925,3927-3942,3948-3957,3970,3972-3977,3982-3983,3988-3999,4012,4014,4020-4026,4028-4053,4055-4056,4059,4061-4062,4066,4070,4076,4080-4099,4120-4125,4127-4176,4178-4181,4183-4199,4210-4212,4214,4216-4223,4226,4229-4239,4249-4260,4268-4270,4272,4276-4280,4282-4284,4286-4299,4302-4303,4305-4321,4326-4327,4329-4362,4366-4368,4370-4375,4377-4389,4392,4400-4405,4407-4409,4413,4416,4418-4419,4422,4425,4427-4434,4437-4440,4446-4447,4454-4458,4460-4469,4490-4495,4497-4498,4500-4535,4537-4538,4540-4567,4569,4571-4573,4576,4578-4580,4584-4592,4594-4607" group=^U47439912589006^ jobname=nek mode=script qtime=1554251516.32 queue=R.pm2 session=^U47439912589006^ start=1554251537.81 user=^U73007516364104^
        final String NodeList_Prefix  = "exec_host=";
        final String User_Prefix  = "user=";
        final String Job_Name_Prefix  = "jobname=";
        final String Start_Time_Prefix  = "start=";
        final int Date_Pos = 1;
        final int Time_Pos = 2;
        final int Desc_Pos = 4;
        final int Job_ID_Pos = 2;
        final int Node_List_Pos = 13;
        final int Job_Name_Pos = 15;
        final int Job_Start_Pos = 20;
        final int Username_Pos = 21;

        // Create a sql compatible timestamp for this log message
        Date   dLineTimestamp    = oldDateFormat.parse(aLineCols[Date_Pos] + " " + aLineCols[Time_Pos]);
        String sLineSqlTimestamp = sqlDateFormat.format(dLineTimestamp);

        // 2019-04-03 00:32:17 04/03/2019 00:32:17;S;327220;Resource_List.ncpus=3000 Resource_List.nodect=3000 Resource_List.walltime=1:00:00 account=^P27857609013029^ args= ctime=1554251516.32 cwd=/lus/theta-fs0/projects/^P76243759028727^/^FILEPATH REMOVED^^U73007516364104^/^FILEPATH REMOVED^ etime=1554251516.32 exe=/home/^U73007516364104^/^FILEPATH REMOVED^ exec_host="0-4,6-7,9-11,20-29,52-75,90-109,148-259,270-279,290-299,310-319,340-349,360-395,404-409,411-417,419-439,451,454-458,480-515,517-523,532-559,600-639,659,662-665,667-671,678-699,740-759,770-779,788-815,820-843,852-893,895,900-907,916-939,960-979,1000-1025,1027-1059,1080-1087,1100-1109,1113-1115,1118-1159,1172-1179,1190-1200,1202-1207,1210-1227,1236-1245,1262,1264-1272,1276-1289,1300-1319,1330-1337,1342-1343,1370-1380,1382-1388,1390-1399,1440-1442,1444,1446-1507,1511,1521,1523-1527,1530,1532,1534-1536,1544-1545,1556-1559,1570-1591,1593,1595-1597,1599-1611,1620-1629,1653,1655,1684-1709,1718-1734,1736-1739,1750-1787,1791,1794-1795,1798,1800-1879,1897,1900-1935,1940-1961,1964-1979,1986-1987,1990,1993-1994,2020-2033,2035-2059,2080-2111,2113-2117,2136-2155,2157,2160-2175,2180-2191,2194-2239,2260-2319,2324,2337,2360-2379,2388-2402,2404-2409,2411-2443,2452-2469,2481,2500-2574,2576-2639,2669,2680-2703,2708-2763,2772-2779,2803,2805,2836,2840-2851,2853,2855-2857,2859-2873,2875-2899,2908,2912,2917-2919,2940-2957,2964-2977,2980-2989,2992-2999,3001-3021,3024-3025,3027-3031,3035,3038-3039,3042,3060-3071,3074-3075,3082-3083,3086-3087,3093-3097,3100-3106,3108,3110-3119,3124-3129,3141,3160-3167,3190-3194,3196,3198-3203,3205-3211,3223-3235,3239-3251,3254,3256-3259,3262,3264-3303,3306-3308,3310-3339,3341-3359,3361,3364,3369,3390-3393,3398,3400-3409,3430-3431,3433-3434,3436-3459,3464-3483,3485-3499,3510-3527,3529-3539,3549,3569,3577,3580-3594,3596-3599,3610-3611,3615-3617,3620,3626-3627,3640-3650,3652-3698,3705,3710,3720-3727,3729-3737,3740-3759,3765,3770-3782,3784-3792,3794-3796,3798,3800-3880,3885,3895,3900,3902-3925,3927-3942,3948-3957,3970,3972-3977,3982-3983,3988-3999,4012,4014,4020-4026,4028-4053,4055-4056,4059,4061-4062,4066,4070,4076,4080-4099,4120-4125,4127-4176,4178-4181,4183-4199,4210-4212,4214,4216-4223,4226,4229-4239,4249-4260,4268-4270,4272,4276-4280,4282-4284,4286-4299,4302-4303,4305-4321,4326-4327,4329-4362,4366-4368,4370-4375,4377-4389,4392,4400-4405,4407-4409,4413,4416,4418-4419,4422,4425,4427-4434,4437-4440,4446-4447,4454-4458,4460-4469,4490-4495,4497-4498,4500-4535,4537-4538,4540-4567,4569,4571-4573,4576,4578-4580,4584-4592,4594-4607" group=^U47439912589006^ jobname=nek mode=script qtime=1554251516.32 queue=R.pm2 session=^U47439912589006^ start=1554251537.81 user=^U73007516364104^
        // Grab the job's JobId.
        String sJobId = aLineCols[Desc_Pos].split(";")[Job_ID_Pos];
        // Grab the job's list of nodes from the node list.
        String sNodeList = aLineCols[Node_List_Pos].substring( aLineCols[Node_List_Pos].indexOf(NodeList_Prefix) + NodeList_Prefix.length());

        // Get a BitSet that contains this list of nodes (including doing any expansion of wild cards)
        // Note: the node list uses node hostnames.
        BitSet bsJobNodes = getBitSetOfCobaltNodes(sNodeList, sJobId, dLineTimestamp, DoAssociateJobIdAndNodeInCachedJobsTable);  // since this is a job start method, do associate job id in cached jobs table.

        String sJobName  = aLineCols[Job_Name_Pos].substring( aLineCols[Job_Name_Pos].indexOf(Job_Name_Prefix) + Job_Name_Prefix.length());
        String sUserName = aLineCols[Username_Pos].substring( aLineCols[Username_Pos].indexOf(User_Prefix) + User_Prefix.length());
        String sJobStartTs  = (aLineCols[Job_Start_Pos].substring( aLineCols[Job_Start_Pos].indexOf(Start_Time_Prefix) + Start_Time_Prefix.length())).split("\\.")[0];
        log_.info("aLineCols[Job_Start_Pos]" + aLineCols[Job_Start_Pos]);
        log_.info("aLineCols[Job_Start_Pos].substring( aLineCols[Job_Start_Pos].indexOf(Start_Time_Prefix) + Start_Time_Prefix.length())" + aLineCols[Job_Start_Pos].substring( aLineCols[Job_Start_Pos].indexOf(Start_Time_Prefix) + Start_Time_Prefix.length()));
        log_.info("sJobStartTs" + sJobStartTs);

        long lStartTsInMicroSecs = Long.parseLong(sJobStartTs) *1000L;
        //--------------------------------------------------------------
        // Update the JobInfo with the data from this log entry AND get all of the JobInfo for this job.
        //--------------------------------------------------------------
        HashMap<String, Object> jobinfo = jobs.startJobinternal(sJobId, lStartTsInMicroSecs);

        long lStartTsInMicroSecs = Long.parseLong(sJobStartTs) *1000L;
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

        chkAndUpdateWorkingResults(sLineSqlTimestamp);  // update this work item's working results field (to show progress) as appropriate.
    }   // End handleJobStartedMsg(String[] aLineCols, SimpleDateFormat oldDateFormat, SimpleDateFormat sqlDateFormat)


    private void handleJobCompletionMsg(String[] aLineCols)
            throws IOException, InterruptedException, ParseException, DataStoreException
    {
        // 2019-04-03 00:35:40 04/03/2019 00:35:40;E;327220;Exit_status=0 Resource_List.ncpus=3000 Resource_List.nodect=3000 Resource_List.walltime=1:00:00 account=^P27857609013029^ approx_total_etime=20 args= ctime=1554251516.32 cwd=/lus/theta-fs0/projects/^P76243759028727^/^FILEPATH REMOVED^^U73007516364104^/^FILEPATH REMOVED^ end=1554251740.88 etime=1554251516.32 exe=/home/^U73007516364104^/^FILEPATH REMOVED^ exec_host="0-4,6-7,9-11,20-29,52-75,90-109,148-259,270-279,290-299,310-319,340-349,360-395,404-409,411-417,419-439,451,454-458,480-515,517-523,532-559,600-639,659,662-665,667-671,678-699,740-759,770-779,788-815,820-843,852-893,895,900-907,916-939,960-979,1000-1025,1027-1059,1080-1087,1100-1109,1113-1115,1118-1159,1172-1179,1190-1200,1202-1207,1210-1227,1236-1245,1262,1264-1272,1276-1289,1300-1319,1330-1337,1342-1343,1370-1380,1382-1388,1390-1399,1440-1442,1444,1446-1507,1511,1521,1523-1527,1530,1532,1534-1536,1544-1545,1556-1559,1570-1591,1593,1595-1597,1599-1611,1620-1629,1653,1655,1684-1709,1718-1734,1736-1739,1750-1787,1791,1794-1795,1798,1800-1879,1897,1900-1935,1940-1961,1964-1979,1986-1987,1990,1993-1994,2020-2033,2035-2059,2080-2111,2113-2117,2136-2155,2157,2160-2175,2180-2191,2194-2239,2260-2319,2324,2337,2360-2379,2388-2402,2404-2409,2411-2443,2452-2469,2481,2500-2574,2576-2639,2669,2680-2703,2708-2763,2772-2779,2803,2805,2836,2840-2851,2853,2855-2857,2859-2873,2875-2899,2908,2912,2917-2919,2940-2957,2964-2977,2980-2989,2992-2999,3001-3021,3024-3025,3027-3031,3035,3038-3039,3042,3060-3071,3074-3075,3082-3083,3086-3087,3093-3097,3100-3106,3108,3110-3119,3124-3129,3141,3160-3167,3190-3194,3196,3198-3203,3205-3211,3223-3235,3239-3251,3254,3256-3259,3262,3264-3303,3306-3308,3310-3339,3341-3359,3361,3364,3369,3390-3393,3398,3400-3409,3430-3431,3433-3434,3436-3459,3464-3483,3485-3499,3510-3527,3529-3539,3549,3569,3577,3580-3594,3596-3599,3610-3611,3615-3617,3620,3626-3627,3640-3650,3652-3698,3705,3710,3720-3727,3729-3737,3740-3759,3765,3770-3782,3784-3792,3794-3796,3798,3800-3880,3885,3895,3900,3902-3925,3927-3942,3948-3957,3970,3972-3977,3982-3983,3988-3999,4012,4014,4020-4026,4028-4053,4055-4056,4059,4061-4062,4066,4070,4076,4080-4099,4120-4125,4127-4176,4178-4181,4183-4199,4210-4212,4214,4216-4223,4226,4229-4239,4249-4260,4268-4270,4272,4276-4280,4282-4284,4286-4299,4302-4303,4305-4321,4326-4327,4329-4362,4366-4368,4370-4375,4377-4389,4392,4400-4405,4407-4409,4413,4416,4418-4419,4422,4425,4427-4434,4437-4440,4446-4447,4454-4458,4460-4469,4490-4495,4497-4498,4500-4535,4537-4538,4540-4567,4569,4571-4573,4576,4578-4580,4584-4592,4594-4607" group=^U47439912589006^ jobname=nek mode=script priority_core_hours=2824063 qtime=1554251516.32 queue=R.pm2 resources_used.location="0-4,6-7,9-11,20-29,52-75,90-109,148-259,270-279,290-299,310-319,340-349,360-395,404-409,411-417,419-439,451,454-458,480-515,517-523,532-559,600-639,659,662-665,667-671,678-699,740-759,770-779,788-815,820-843,852-893,895,900-907,916-939,960-979,1000-1025,1027-1059,1080-1087,1100-1109,1113-1115,1118-1159,1172-1179,1190-1200,1202-1207,1210-1227,1236-1245,1262,1264-1272,1276-1289,1300-1319,1330-1337,1342-1343,1370-1380,1382-1388,1390-1399,1440-1442,1444,1446-1507,1511,1521,1523-1527,1530,1532,1534-1536,1544-1545,1556-1559,1570-1591,1593,1595-1597,1599-1611,1620-1629,1653,1655,1684-1709,1718-1734,1736-1739,1750-1787,1791,1794-1795,1798,1800-1879,1897,1900-1935,1940-1961,1964-1979,1986-1987,1990,1993-1994,2020-2033,2035-2059,2080-2111,2113-2117,2136-2155,2157,2160-2175,2180-2191,2194-2239,2260-2319,2324,2337,2360-2379,2388-2402,2404-2409,2411-2443,2452-2469,2481,2500-2574,2576-2639,2669,2680-2703,2708-2763,2772-2779,2803,2805,2836,2840-2851,2853,2855-2857,2859-2873,2875-2899,2908,2912,2917-2919,2940-2957,2964-2977,2980-2989,2992-2999,3001-3021,3024-3025,3027-3031,3035,3038-3039,3042,3060-3071,3074-3075,3082-3083,3086-3087,3093-3097,3100-3106,3108,3110-3119,3124-3129,3141,3160-3167,3190-3194,3196,3198-3203,3205-3211,3223-3235,3239-3251,3254,3256-3259,3262,3264-3303,3306-3308,3310-3339,3341-3359,3361,3364,3369,3390-3393,3398,3400-3409,3430-3431,3433-3434,3436-3459,3464-3483,3485-3499,3510-3527,3529-3539,3549,3569,3577,3580-3594,3596-3599,3610-3611,3615-3617,3620,3626-3627,3640-3650,3652-3698,3705,3710,3720-3727,3729-3737,3740-3759,3765,3770-3782,3784-3792,3794-3796,3798,3800-3880,3885,3895,3900,3902-3925,3927-3942,3948-3957,3970,3972-3977,3982-3983,3988-3999,4012,4014,4020-4026,4028-4053,4055-4056,4059,4061-4062,4066,4070,4076,4080-4099,4120-4125,4127-4176,4178-4181,4183-4199,4210-4212,4214,4216-4223,4226,4229-4239,4249-4260,4268-4270,4272,4276-4280,4282-4284,4286-4299,4302-4303,4305-4321,4326-4327,4329-4362,4366-4368,4370-4375,4377-4389,4392,4400-4405,4407-4409,4413,4416,4418-4419,4422,4425,4427-4434,4437-4440,4446-4447,4454-4458,4460-4469,4490-4495,4497-4498,4500-4535,4537-4538,4540-4567,4569,4571-4573,4576,4578-4580,4584-4592,4594-4607" resources_used.nodect=3000 resources_used.walltime=0:03:23 session=^U47439912589006^ start=1554251537.81 user=^U73007516364104^

        final String Work_Dir_Prefix  = "cwd=";
        final String Exit_Status_Prefix  = "Exit_status=";
        final String Start_Time_Prefix  = "start=";
        final String End_Time_Prefix  = "end=";
        final int Date_Pos = 1;
        final int Time_Pos = 2;
        final int Desc_Pos = 4;
        final int Job_ID_Pos = 2;
        final int Exit_Status_Pos = 3;
        final int Work_Dir_Pos = 12;
        final int Job_End_Pos = 13;
        final int Job_Start_Pos = 27;

        // Create a sql compatible timestamp for this log message
        Date   dLineTimestamp    = oldDateFormat.parse(aLineCols[Date_Pos] + " " + aLineCols[Time_Pos]);
        String sLineSqlTimestamp = sqlDateFormat.format(dLineTimestamp);

        //------------------------------------------------------------------
        // Get the pertinent data out of the record.
        //------------------------------------------------------------------
        String sJobId       = aLineCols[Desc_Pos].split(";")[Job_ID_Pos];
        String sWlmJobState = "T";
        String sExitStatus = aLineCols[Desc_Pos].split(";")[Exit_Status_Pos].substring(aLineCols[Desc_Pos].split(";")[Exit_Status_Pos].indexOf(Exit_Status_Prefix) + Exit_Status_Prefix.length());
        String sWorkDir = aLineCols[Work_Dir_Pos].substring( aLineCols[Work_Dir_Pos].indexOf(Work_Dir_Prefix) + Work_Dir_Prefix.length());
        String sJobStartTs  = (aLineCols[Job_Start_Pos].substring( aLineCols[Job_Start_Pos].indexOf(Start_Time_Prefix) + Start_Time_Prefix.length())).split("\\.")[0];
        String sJobEndTs  = (aLineCols[Job_End_Pos].substring( aLineCols[Job_End_Pos].indexOf(End_Time_Prefix) + End_Time_Prefix.length())).split("\\.")[0];

        log_.info("aLineCols[Job_Start_Pos]" + aLineCols[Job_Start_Pos]);
        log_.info("aLineCols[Job_Start_Pos].substring( aLineCols[Job_Start_Pos].indexOf(Start_Time_Prefix) + Start_Time_Prefix.length())" + aLineCols[Job_Start_Pos].substring( aLineCols[Job_Start_Pos].indexOf(Start_Time_Prefix) + Start_Time_Prefix.length()));
        log_.info("sJobStartTs" + sJobStartTs);

        log_.info("aLineCols[Job_End_Pos]" + aLineCols[Job_End_Pos]);
        log_.info("aLineCols[Job_End_Pos].substring( aLineCols[Job_End_Pos].indexOf(End_Time_Prefix) + End_Time_Prefix.length())" + aLineCols[Job_End_Pos].substring( aLineCols[Job_End_Pos].indexOf(End_Time_Prefix) + End_Time_Prefix.length()));
        log_.info("sJobEndTs" + sJobEndTs);
        //--------------------------------------------------------------
        // Update the JobInfo with the data from this log entry AND get all of the JobInfo for this job.
        //--------------------------------------------------------------
        long lStartTsInMicroSecs = Long.parseLong(sJobStartTs) *1000L;
        long lEndTsInMicroSecs = Long.parseLong(sJobEndTs) *1000L;
        HashMap<String, Object> jobinfo = jobs.completeJobInternal(sJobId, sWorkDir, sWlmJobState, lEndTsInMicroSecs, lStartTsInMicroSecs);

        //--------------------------------------------------------------
        // Check & see if we now have seen the JobStarted as well as this JobCompletion message - if so then the job is done-done, go ahead and handle that processing.
        //--------------------------------------------------------------
        if (jobinfo != null && ((String) jobinfo.get("WlmJobStarted")).equals("T")   &&
                ((String) jobinfo.get("WlmJobCompleted")).equals("T") )
        {   // all of the job state messages have now been seen - go ahead and handle the fact that this job is done-done.
            // Handle the stuff needed when a job ends.
            handleEndOfJobProcessing(sJobId, jobinfo, sExitStatus);
        }

        chkAndUpdateWorkingResults(sLineSqlTimestamp);  // update this work item's working results field (to show progress) as appropriate.
    }   // End handleJobCompletionMsg(String[] aLineCols, SimpleDateFormat oldDateFormat, SimpleDateFormat sqlDateFormat)


    //--------------------------------------------------------------------------
    // Handle all of the database updates, map cleanup, etc. that needs to happen when a job is totally finished.
    //--------------------------------------------------------------------------
    private void handleEndOfJobProcessing(String sJobId, HashMap<String, Object> jobInfo, String sExitStatus) throws InterruptedException, IOException, DataStoreException {

        //--------------------------------------------------------------
        // Put the 'Job Termination' record into the Job table.
        //--------------------------------------------------------------
        if (jobInfo != null) {
            jobs.terminateJob(sJobId, null, Integer.parseInt(sExitStatus), (String) jobInfo.get("WlmJobWorkDir"), (String) jobInfo.get("WlmJobState"), ((Date) jobInfo.get("WlmJobEndTime")).getTime(), adapter.getType(), workQueue.workItemId());

            //--------------------------------------------------------------
            // Put the appropriate 'Job Completion' information into the Internal Cached Jobs table (note this table is different from the InternalJobInfo table).
            //--------------------------------------------------------------
            // Update the Internal Cached Jobs table to fill in this job's termination time.
            jobs.terminateJobInternal(((Date) jobInfo.get("WlmJobEndTime")).getTime(), System.currentTimeMillis() * 1000L, sJobId);

            //----------------------------------------------------------
            // Remove this job's JobInfo entry.
            //----------------------------------------------------------
            jobs.removeJobInternal(sJobId, ((Date) jobInfo.get("WlmJobStartTime")).getTime());
        }

    }   // End handleEndOfJobProcessing(String sJobId, HashMap<String, Object> jobInfo)


    private void handleReservationCreatedMsg(String[] aLineCols)
            throws IOException, InterruptedException, ParseException, DataStoreException
    {
        final int Date_Pos = 1;
        final int Time_Pos = 2;
        final int Reservation_Name_Pos = 7;
        final int Users_Pos = 21;
        final int Nodes_Pos = 11;
        final int Start_Ts_Pos = 15;
        final int Duration_Pos = 17;

        // 2019-04-07 15:27:54 ^U94332985204799^ adding reservation: [{'name': 'benchmarking', 'block_passthrough': False, 'partitions': '24-47,52-75,84-139,148-286,288-395,404-459,468-523,532-779,788-815,820-843,852-907,916-1161,1163,1172-1227,1236-1291,1300-1521,1523-1547,1556-1611,1620-1675,1684-1935,1940-1995,2004-2059,2068-2319,2324-2351,2356-2379,2388-2443,2452-2703,2708-2763,2772-2827,2836-2983,2985-3087,3092-3119,3124-3147,3156-3211,3220-3547,3549,3560-3590,3594-3597,3612-3614,3619-3633,3640-3650,3652-3659,3662,3664-3665,3668-3689,3692-3694,3696-3699,3701,3710,3720-3764,3766-3779,3781-3782,3784,3787-3792,3794-3796,3798,3800-3814,3816-3823,3842,3846,3849,3860-3862,3864-3893,3895-3919,3930-3942,3944-3959,3961,3970-3971,3973,3976-3991,3993-3996,3998-4009,4014,4020-4053,4055-4056,4059-4064,4066,4068-4070,4076,4080-4090,4106-4140,4150-4181,4183-4191,4196-4199,4201,4210-4212,4214,4216-4223,4226,4229-4244,4246-4260,4262-4272,4276-4277,4280-4288,4290-4299,4302-4303,4306,4308-4327,4329-4335,4338,4340-4349,4351-4363,4366-4368,4370,4372-4376,4379-4400,4404-4406,4408-4409,4413,4416,4418-4419,4421,4424,4426,4428-4436,4438-4442,4445-4449,4454-4458,4460-4475,4477,4479,4487,4491,4495-4496,4498-4549,4551-4567,4569-4573,4576,4578-4579,4584-4607', 'project': ^P54486199121217^, 'start': 1554667200.0, 'duration': 12600, 'cycle': ^P54486199121217^, 'users': '^U92605928607567^:^U94332985204799^:^U76403053512984^:^U46918047287928^'}]
        // Create a sql compatible timestamp for this log message
        Date   dLineTimestamp    = oldDateFormat.parse(aLineCols[Date_Pos] + " " + aLineCols[Time_Pos]);
        String sLineSqlTimestamp = sqlDateFormat.format(dLineTimestamp);

        // Grab the Reservation's ReservationName.
        String sReservationName = aLineCols[Reservation_Name_Pos].split("'")[1];

        //------------------------------------------------------
        // Grab all the pertinent reservation data out of the msg.
        //------------------------------------------------------
        String sUsers = aLineCols[Users_Pos].split("'")[1];
        String sNodes = aLineCols[Nodes_Pos].split("'")[1];

        String sTempStartTs = aLineCols[Start_Ts_Pos].split("\\.")[0];
        long lReservationsStartTsInMicroSecs = Long.parseLong(sTempStartTs) * 1000L;

        String sDuration = aLineCols[Duration_Pos].split(",")[0];
        long lReservationsEndTsInMicroSecs = (Long.parseLong(sTempStartTs) + Long.parseLong(sDuration)) * 1000L;

        //------------------------------------------------------
        // Insert a record for this reservation into the table.
        //------------------------------------------------------
        reservations.createReservation(sReservationName, sUsers, sNodes, lReservationsStartTsInMicroSecs, lReservationsEndTsInMicroSecs, dLineTimestamp.getTime() * 1000L, adapter.getType(), workQueue.workItemId());

        chkAndUpdateWorkingResults(sLineSqlTimestamp);  // update this work item's working results field (to show progress) as appropriate.
    }   // End handleReservationCreatedMsg(String[] aLineCols, SimpleDateFormat oldDateFormat, SimpleDateFormat sqlDateFormat)


    private void handleReservationUpdatedMsg(String[] aLineCols)
            throws IOException, InterruptedException, ParseException, DataStoreException
    {
        // 2019-04-07 16:42:30 ^U94332985204799^ modifying reservation: [{'name': 'benchmarking'}] with updates {'users': '^U92605928607567^:^U94332985204799^:^U76403053512984^:^U46918047287928^:^U88988976361690^:^U833649142102^'}
        final String ReservationNamePrefix  = "'name'";
        final String UsersPrefix            = "'users'";
        final String NodesPrefix            = "'partitions'";
        final String StartTsPrefix          = "'start'";
        final int Date_Pos = 1;
        final int Time_Pos = 2;
        final int Reservation_Name_Pos = 7;

        // Create a sql compatible timestamp for this log message
        Date   dLineTimestamp    = oldDateFormat.parse(aLineCols[Date_Pos] + " " + aLineCols[Time_Pos]);
        String sLineSqlTimestamp = sqlDateFormat.format(dLineTimestamp);

        // Grab the Reservation's ReservationName.
        String sReservationName = aLineCols[Reservation_Name_Pos].split("'")[1];

        //------------------------------------------------------
        // Grab all the pertinent reservation data out of the msg.
        //------------------------------------------------------
        String sUsers=null, sNodes=null, sReservationsStartSqlTimestamp=null, sReservationsEndSqlTimestamp=null;
        long   lReservationsStartTsInMicroSecs=-99999;

        int i = 0;
        for (String sColInfo: aLineCols) {
            if (sColInfo.contains(UsersPrefix))
                sUsers = aLineCols[i+1].split("'")[1];
            if (sColInfo.contains(NodesPrefix))
                sNodes = aLineCols[i+1].split("'")[1];
            if (sColInfo.contains(StartTsPrefix)) {
                String sTempStartTs = aLineCols[i+1].split("'")[1].split("\\.")[0];
                lReservationsStartTsInMicroSecs = Long.parseLong(sTempStartTs) * 1000L;
            }
            i++;
        }

        //------------------------------------------------------
        // Update the reservation information for this reservation in the table.
        //------------------------------------------------------
        reservations.updateReservation(sReservationName, sUsers, sNodes, lReservationsStartTsInMicroSecs, dLineTimestamp.getTime() * 1000L, adapter.getType(), workQueue.workItemId());

        chkAndUpdateWorkingResults(sLineSqlTimestamp);  // update this work item's working results field (to show progress) as appropriate.
    }   // End handleReservationUpdatedMsg(String[] aLineCols, SimpleDateFormat oldDateFormat, SimpleDateFormat sqlDateFormat)


    private void handleReservationDeletedMsg(String[] aLineCols)
            throws IOException, InterruptedException, ParseException, DataStoreException
    {
        // 2019-04-07 21:40:35 ^U94332985204799^ releasing reservation: [{'name': 'benchmarking', 'partitions': '*'}]

        final int Date_Pos = 1;
        final int Time_Pos = 2;
        final int Reservation_Name_Pos = 7;

        // Create a sql compatible timestamp for this log message
        Date   dLineTimestamp    = oldDateFormat.parse(aLineCols[Date_Pos] + " " + aLineCols[Time_Pos]);
        String sLineSqlTimestamp = sqlDateFormat.format(dLineTimestamp);

        // Grab the Reservation's ReservationName.
        String   sReservationName = aLineCols[Reservation_Name_Pos].split("'")[1];

        //------------------------------------------------------
        // Update the reservation information for this reservation in the table so that the DeletedTimestamp is filled in with this message's timestamp.
        // NOTE: this procedure does NOT delete the entry from the WlmReservation_History table, it simply updates the reservation's DeletedTimestamp field!
        //------------------------------------------------------
        reservations.deleteReservation(sReservationName, dLineTimestamp.getTime() * 1000L, adapter.getType(), workQueue.workItemId());

        chkAndUpdateWorkingResults(sLineSqlTimestamp);  // update this work item's working results field (to show progress) as appropriate.
    }   // End handleReservationDeletedMsg(String[] aLineCols, SimpleDateFormat oldDateFormat, SimpleDateFormat sqlDateFormat)

    // Handle 'Cobalt Job' log messages.
    void handleCobaltJobMessages(String sLine, String[] aLineCols) throws InterruptedException, IOException, ParseException, DataStoreException
    {
        // 2019-04-03 00:32:17 04/03/2019 00:32:17;S;327220;Resource_List.ncpus=3000 Resource_List.nodect=3000 Resource_List.walltime=1:00:00 account=^P27857609013029^ args= ctime=1554251516.32 cwd=/lus/theta-fs0/projects/^P76243759028727^/^FILEPATH REMOVED^^U73007516364104^/^FILEPATH REMOVED^ etime=1554251516.32 exe=/home/^U73007516364104^/^FILEPATH REMOVED^ exec_host="0-4,6-7,9-11,20-29,52-75,90-109,148-259,270-279,290-299,310-319,340-349,360-395,404-409,411-417,419-439,451,454-458,480-515,517-523,532-559,600-639,659,662-665,667-671,678-699,740-759,770-779,788-815,820-843,852-893,895,900-907,916-939,960-979,1000-1025,1027-1059,1080-1087,1100-1109,1113-1115,1118-1159,1172-1179,1190-1200,1202-1207,1210-1227,1236-1245,1262,1264-1272,1276-1289,1300-1319,1330-1337,1342-1343,1370-1380,1382-1388,1390-1399,1440-1442,1444,1446-1507,1511,1521,1523-1527,1530,1532,1534-1536,1544-1545,1556-1559,1570-1591,1593,1595-1597,1599-1611,1620-1629,1653,1655,1684-1709,1718-1734,1736-1739,1750-1787,1791,1794-1795,1798,1800-1879,1897,1900-1935,1940-1961,1964-1979,1986-1987,1990,1993-1994,2020-2033,2035-2059,2080-2111,2113-2117,2136-2155,2157,2160-2175,2180-2191,2194-2239,2260-2319,2324,2337,2360-2379,2388-2402,2404-2409,2411-2443,2452-2469,2481,2500-2574,2576-2639,2669,2680-2703,2708-2763,2772-2779,2803,2805,2836,2840-2851,2853,2855-2857,2859-2873,2875-2899,2908,2912,2917-2919,2940-2957,2964-2977,2980-2989,2992-2999,3001-3021,3024-3025,3027-3031,3035,3038-3039,3042,3060-3071,3074-3075,3082-3083,3086-3087,3093-3097,3100-3106,3108,3110-3119,3124-3129,3141,3160-3167,3190-3194,3196,3198-3203,3205-3211,3223-3235,3239-3251,3254,3256-3259,3262,3264-3303,3306-3308,3310-3339,3341-3359,3361,3364,3369,3390-3393,3398,3400-3409,3430-3431,3433-3434,3436-3459,3464-3483,3485-3499,3510-3527,3529-3539,3549,3569,3577,3580-3594,3596-3599,3610-3611,3615-3617,3620,3626-3627,3640-3650,3652-3698,3705,3710,3720-3727,3729-3737,3740-3759,3765,3770-3782,3784-3792,3794-3796,3798,3800-3880,3885,3895,3900,3902-3925,3927-3942,3948-3957,3970,3972-3977,3982-3983,3988-3999,4012,4014,4020-4026,4028-4053,4055-4056,4059,4061-4062,4066,4070,4076,4080-4099,4120-4125,4127-4176,4178-4181,4183-4199,4210-4212,4214,4216-4223,4226,4229-4239,4249-4260,4268-4270,4272,4276-4280,4282-4284,4286-4299,4302-4303,4305-4321,4326-4327,4329-4362,4366-4368,4370-4375,4377-4389,4392,4400-4405,4407-4409,4413,4416,4418-4419,4422,4425,4427-4434,4437-4440,4446-4447,4454-4458,4460-4469,4490-4495,4497-4498,4500-4535,4537-4538,4540-4567,4569,4571-4573,4576,4578-4580,4584-4592,4594-4607" group=^U47439912589006^ jobname=nek mode=script qtime=1554251516.32 queue=R.pm2 session=^U47439912589006^ start=1554251537.81 user=^U73007516364104^
        // 2019-04-03 00:35:40 04/03/2019 00:35:40;E;327220;Exit_status=0 Resource_List.ncpus=3000 Resource_List.nodect=3000 Resource_List.walltime=1:00:00 account=^P27857609013029^ approx_total_etime=20 args= ctime=1554251516.32 cwd=/lus/theta-fs0/projects/^P76243759028727^/^FILEPATH REMOVED^^U73007516364104^/^FILEPATH REMOVED^ end=1554251740.88 etime=1554251516.32 exe=/home/^U73007516364104^/^FILEPATH REMOVED^ exec_host="0-4,6-7,9-11,20-29,52-75,90-109,148-259,270-279,290-299,310-319,340-349,360-395,404-409,411-417,419-439,451,454-458,480-515,517-523,532-559,600-639,659,662-665,667-671,678-699,740-759,770-779,788-815,820-843,852-893,895,900-907,916-939,960-979,1000-1025,1027-1059,1080-1087,1100-1109,1113-1115,1118-1159,1172-1179,1190-1200,1202-1207,1210-1227,1236-1245,1262,1264-1272,1276-1289,1300-1319,1330-1337,1342-1343,1370-1380,1382-1388,1390-1399,1440-1442,1444,1446-1507,1511,1521,1523-1527,1530,1532,1534-1536,1544-1545,1556-1559,1570-1591,1593,1595-1597,1599-1611,1620-1629,1653,1655,1684-1709,1718-1734,1736-1739,1750-1787,1791,1794-1795,1798,1800-1879,1897,1900-1935,1940-1961,1964-1979,1986-1987,1990,1993-1994,2020-2033,2035-2059,2080-2111,2113-2117,2136-2155,2157,2160-2175,2180-2191,2194-2239,2260-2319,2324,2337,2360-2379,2388-2402,2404-2409,2411-2443,2452-2469,2481,2500-2574,2576-2639,2669,2680-2703,2708-2763,2772-2779,2803,2805,2836,2840-2851,2853,2855-2857,2859-2873,2875-2899,2908,2912,2917-2919,2940-2957,2964-2977,2980-2989,2992-2999,3001-3021,3024-3025,3027-3031,3035,3038-3039,3042,3060-3071,3074-3075,3082-3083,3086-3087,3093-3097,3100-3106,3108,3110-3119,3124-3129,3141,3160-3167,3190-3194,3196,3198-3203,3205-3211,3223-3235,3239-3251,3254,3256-3259,3262,3264-3303,3306-3308,3310-3339,3341-3359,3361,3364,3369,3390-3393,3398,3400-3409,3430-3431,3433-3434,3436-3459,3464-3483,3485-3499,3510-3527,3529-3539,3549,3569,3577,3580-3594,3596-3599,3610-3611,3615-3617,3620,3626-3627,3640-3650,3652-3698,3705,3710,3720-3727,3729-3737,3740-3759,3765,3770-3782,3784-3792,3794-3796,3798,3800-3880,3885,3895,3900,3902-3925,3927-3942,3948-3957,3970,3972-3977,3982-3983,3988-3999,4012,4014,4020-4026,4028-4053,4055-4056,4059,4061-4062,4066,4070,4076,4080-4099,4120-4125,4127-4176,4178-4181,4183-4199,4210-4212,4214,4216-4223,4226,4229-4239,4249-4260,4268-4270,4272,4276-4280,4282-4284,4286-4299,4302-4303,4305-4321,4326-4327,4329-4362,4366-4368,4370-4375,4377-4389,4392,4400-4405,4407-4409,4413,4416,4418-4419,4422,4425,4427-4434,4437-4440,4446-4447,4454-4458,4460-4469,4490-4495,4497-4498,4500-4535,4537-4538,4540-4567,4569,4571-4573,4576,4578-4580,4584-4592,4594-4607" group=^U47439912589006^ jobname=nek mode=script priority_core_hours=2824063 qtime=1554251516.32 queue=R.pm2 resources_used.location="0-4,6-7,9-11,20-29,52-75,90-109,148-259,270-279,290-299,310-319,340-349,360-395,404-409,411-417,419-439,451,454-458,480-515,517-523,532-559,600-639,659,662-665,667-671,678-699,740-759,770-779,788-815,820-843,852-893,895,900-907,916-939,960-979,1000-1025,1027-1059,1080-1087,1100-1109,1113-1115,1118-1159,1172-1179,1190-1200,1202-1207,1210-1227,1236-1245,1262,1264-1272,1276-1289,1300-1319,1330-1337,1342-1343,1370-1380,1382-1388,1390-1399,1440-1442,1444,1446-1507,1511,1521,1523-1527,1530,1532,1534-1536,1544-1545,1556-1559,1570-1591,1593,1595-1597,1599-1611,1620-1629,1653,1655,1684-1709,1718-1734,1736-1739,1750-1787,1791,1794-1795,1798,1800-1879,1897,1900-1935,1940-1961,1964-1979,1986-1987,1990,1993-1994,2020-2033,2035-2059,2080-2111,2113-2117,2136-2155,2157,2160-2175,2180-2191,2194-2239,2260-2319,2324,2337,2360-2379,2388-2402,2404-2409,2411-2443,2452-2469,2481,2500-2574,2576-2639,2669,2680-2703,2708-2763,2772-2779,2803,2805,2836,2840-2851,2853,2855-2857,2859-2873,2875-2899,2908,2912,2917-2919,2940-2957,2964-2977,2980-2989,2992-2999,3001-3021,3024-3025,3027-3031,3035,3038-3039,3042,3060-3071,3074-3075,3082-3083,3086-3087,3093-3097,3100-3106,3108,3110-3119,3124-3129,3141,3160-3167,3190-3194,3196,3198-3203,3205-3211,3223-3235,3239-3251,3254,3256-3259,3262,3264-3303,3306-3308,3310-3339,3341-3359,3361,3364,3369,3390-3393,3398,3400-3409,3430-3431,3433-3434,3436-3459,3464-3483,3485-3499,3510-3527,3529-3539,3549,3569,3577,3580-3594,3596-3599,3610-3611,3615-3617,3620,3626-3627,3640-3650,3652-3698,3705,3710,3720-3727,3729-3737,3740-3759,3765,3770-3782,3784-3792,3794-3796,3798,3800-3880,3885,3895,3900,3902-3925,3927-3942,3948-3957,3970,3972-3977,3982-3983,3988-3999,4012,4014,4020-4026,4028-4053,4055-4056,4059,4061-4062,4066,4070,4076,4080-4099,4120-4125,4127-4176,4178-4181,4183-4199,4210-4212,4214,4216-4223,4226,4229-4239,4249-4260,4268-4270,4272,4276-4280,4282-4284,4286-4299,4302-4303,4305-4321,4326-4327,4329-4362,4366-4368,4370-4375,4377-4389,4392,4400-4405,4407-4409,4413,4416,4418-4419,4422,4425,4427-4434,4437-4440,4446-4447,4454-4458,4460-4469,4490-4495,4497-4498,4500-4535,4537-4538,4540-4567,4569,4571-4573,4576,4578-4580,4584-4592,4594-4607" resources_used.nodect=3000 resources_used.walltime=0:03:23 session=^U47439912589006^ start=1554251537.81 user=^U73007516364104^

        String start_or_end_indicator = aLineCols[4].split(";")[1];

        if (start_or_end_indicator.equals("S")){
            log_.info("FOUND a 'Job Started' log message - %s", sLine);
            handleJobStartedMsg(aLineCols);
        }
        else if (start_or_end_indicator.equals("E")){
            log_.info("FOUND a 'Job Completed' log message - %s", sLine);
            handleJobCompletionMsg(aLineCols);
        }

    }   // End handleCobaltJobMessages(String sLine, String[] aLineCols, SimpleDateFormat oldDateFormat, SimpleDateFormat sqlDateFormat)

    // Handle 'Cobalt Reservation' log messages.
    void handleCobaltReservationMessages(String sLine, String[] aLineCols) throws InterruptedException, IOException, ParseException, DataStoreException {
        // 2019-04-07 15:27:54 ^U94332985204799^ adding reservation: [{'name': 'benchmarking', 'block_passthrough': False, 'partitions': '24-47,52-75,84-139,148-286,288-395,404-459,468-523,532-779,788-815,820-843,852-907,916-1161,1163,1172-1227,1236-1291,1300-1521,1523-1547,1556-1611,1620-1675,1684-1935,1940-1995,2004-2059,2068-2319,2324-2351,2356-2379,2388-2443,2452-2703,2708-2763,2772-2827,2836-2983,2985-3087,3092-3119,3124-3147,3156-3211,3220-3547,3549,3560-3590,3594-3597,3612-3614,3619-3633,3640-3650,3652-3659,3662,3664-3665,3668-3689,3692-3694,3696-3699,3701,3710,3720-3764,3766-3779,3781-3782,3784,3787-3792,3794-3796,3798,3800-3814,3816-3823,3842,3846,3849,3860-3862,3864-3893,3895-3919,3930-3942,3944-3959,3961,3970-3971,3973,3976-3991,3993-3996,3998-4009,4014,4020-4053,4055-4056,4059-4064,4066,4068-4070,4076,4080-4090,4106-4140,4150-4181,4183-4191,4196-4199,4201,4210-4212,4214,4216-4223,4226,4229-4244,4246-4260,4262-4272,4276-4277,4280-4288,4290-4299,4302-4303,4306,4308-4327,4329-4335,4338,4340-4349,4351-4363,4366-4368,4370,4372-4376,4379-4400,4404-4406,4408-4409,4413,4416,4418-4419,4421,4424,4426,4428-4436,4438-4442,4445-4449,4454-4458,4460-4475,4477,4479,4487,4491,4495-4496,4498-4549,4551-4567,4569-4573,4576,4578-4579,4584-4607', 'project': ^P54486199121217^, 'start': 1554667200.0, 'duration': 12600, 'cycle': ^P54486199121217^, 'users': '^U92605928607567^:^U94332985204799^:^U76403053512984^:^U46918047287928^'}]
        // 2019-04-07 16:42:30 ^U94332985204799^ modifying reservation: [{'name': 'benchmarking'}] with updates {'users': '^U92605928607567^:^U94332985204799^:^U76403053512984^:^U46918047287928^:^U88988976361690^:^U833649142102^'}
        // 2019-04-07 21:40:35 ^U94332985204799^ releasing reservation: [{'name': 'benchmarking', 'partitions': '*'}]

        String reservation_indicator = aLineCols[4];

        if (reservation_indicator.equals("adding")){
            log_.info("FOUND a 'Job Started' log message - %s", sLine);
            handleReservationCreatedMsg(aLineCols);
        }
        else if (reservation_indicator.equals("modifying")){
            log_.info("FOUND a 'Job Completed' log message - %s", sLine);
            handleReservationUpdatedMsg(aLineCols);
        }
        else if (reservation_indicator.equals("releasing")){
            log_.info("FOUND a 'Job Completed' log message - %s", sLine);
            handleReservationDeletedMsg(aLineCols);
        }

    }   // End handleCobaltReservationMessages(String sLine, String[] aLineCols, SimpleDateFormat oldDateFormat, SimpleDateFormat sqlDateFormat)

    // Helper method that checks and sees if we want to update our working results (not done every msg to minimize overhead).
    private final void chkAndUpdateWorkingResults(String sLineSqlTimestamp) throws IOException, DataStoreException {
        if ((++mNumMsgsHndld % 10) == 0) {
            // Save restart data indicating where we are in the processing of the log.
            String sRestartData = "Processed through '" + sLineSqlTimestamp + "', NumMsgsHndld=" + mNumMsgsHndld;
            workQueue.saveWorkItemsRestartData(workQueue.workItemId(), sRestartData, false);  // false means to update this workitem's history record rather than doing an insert of another history record - this is "unusual" (only used when a workitem is updating its working results fields very often)
        }
    }   // End chkAndUpdateWorkingResults(String sLineSqlTimestamp)

    public void shutDown() {
        log_.info("Shutting down the adapter gracefully");
        adapter.signalToShutdown();
    }

    public static void main(String[] args) {
        if(args == null || args.length != 3)
            throw new RuntimeException(String.format("Wrong number of arguments for this adapter (%s), must " +
                            "use 3 arguments: voltdb_servers, location and hostname in that order",
                    AdapterWlmCobalt.class.getCanonicalName()));

        String SnLctn     = args[1];
        String SnHostname = args[2];


        AdapterInformation baseAdapter = new AdapterInformation("WLM", AdapterWlmCobalt.class.getCanonicalName(), SnLctn, SnHostname, -1L);
        Logger log = LoggerFactory.getInstance(baseAdapter.getType(), baseAdapter.getName(), "console");

        DataStoreFactory factory = new DataStoreFactoryImpl(args, log);

        AdapterWlmCobalt cobaltAdapter = new AdapterWlmCobalt(log, baseAdapter, factory);
        AdapterWlm wlmAdapter = new AdapterWlm(baseAdapter, cobaltAdapter, factory, log);
        Runtime.getRuntime().addShutdownHook(new Thread(cobaltAdapter::shutDown));

        System.exit(wlmAdapter.run());
    }

}   // End class AdapterWlmCobalt
