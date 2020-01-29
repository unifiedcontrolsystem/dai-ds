// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.AdapterInformation;
import com.intel.dai.IAdapter;
import com.intel.dai.Adapter;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.RasEventLog;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.dai.dsapi.WorkQueue;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import javax.xml.crypto.Data;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.*;

public class VoltDbWorkQueue implements WorkQueue {
    // 256 * 1K - 4 bytes
    private static final int MAX_RESULT_LENGTH = 262140;

    // Compressor has a 32MB input limit, documentation does not state this but if you exceed this, it will throw an
    // IndexOutOfBoundsException.
    private static final int MAX_RESULT_COMPRESSOR_LENGTH = 33554432;

    // For byte buffers in IO operation this is the block size used.
    private static final int BYTE_BUFFER_BLOCK_SIZE = 65536;

    Client voltClient = null;
    Logger logger;
    private String adapterName;
    private String adapterType;
    private long adapterId = -99999L;
    private long baseWorkItemId = -99999L;
    private AdapterInformation information;
    RasEventLog logRasEvent;
    ClientResponse workItemResponse;
    private int timeToWaitIfNothingQueued = 1;
    private String workToBeDone = null;
    private long workItemId = -99999L;
    private boolean thisIsNewWorkItem = true;
    private String workingResults = null;
    private String[] servers;
    DataStoreFactory factory;

    public VoltDbWorkQueue(String[] servers, IAdapter adapter, Logger logger) throws DataStoreException {
        this.servers = servers;
        this.logger = logger;
        information = fromOldAdapter(adapter);
        factory = new DataStoreFactoryImpl(servers, logger);
        logRasEvent = factory.createRasEventLog(information);
    }

    public VoltDbWorkQueue(Client client, String[] servers, IAdapter adapter, Logger logger) throws DataStoreException {
        this.servers = servers;
        this.logger = logger;
        voltClient = client;
        information = fromOldAdapter(adapter);
        factory = new DataStoreFactoryImpl(servers, logger);
        logRasEvent = factory.createRasEventLog(information);
    }

    public VoltDbWorkQueue(String[] servers, AdapterInformation adapter, Logger logger) {
        this.servers = servers;
        this.logger = logger;
        information = adapter;
        factory = new DataStoreFactoryImpl(servers, logger);
        logRasEvent = factory.createRasEventLog(information);
    }

    VoltDbWorkQueue(String[] servers, IAdapter adapter, VoltDbRasEventLog eventLog, Logger logger) throws DataStoreException {
        this.logger = logger;
        information = fromOldAdapter(adapter);
        factory = new DataStoreFactoryImpl(servers, logger);
        logRasEvent = eventLog;
    }

    public void initialize() {
        initializeVoltClient(servers);
        initializeVoltDbWorkQueue(servers, information, logger);
    }

    static AdapterInformation fromOldAdapter(IAdapter adapter) throws DataStoreException {
        try {
            AdapterInformation result = new AdapterInformation(adapter.adapterType(), adapter.adapterName(),
                    adapter.snLctn(), adapter.mapServNodeLctnToHostName().get(adapter.snLctn()), adapter.adapterId());
            if (adapter.adapterAbnormalShutdown() || adapter.adapterShuttingDown())
                result.signalToShutdown();
            return result;
        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Failed to get the service node hostname from the service node location", e);
        }
    }

    private void initializeVoltClient(String[] servers) {
        if(voltClient == null) {
            VoltDbClient.initializeVoltDbClient(servers);
            voltClient = VoltDbClient.getVoltClientInstance();
        }
    }

    private void initializeVoltDbWorkQueue(String[] servers, AdapterInformation information, Logger logger) {
        assert voltClient != null : "voltClient must be set before initializing VoltDbWorkQueue";
        this.information = information;
        adapterName = information.getName();
        adapterType = information.getType();
        adapterId = information.getId();
        setupAdaptersBaseWorkItem();
    }

    Client getVoltClient() {
        return voltClient;
    }


    // Set up the work item that represents the BASE/DEFAULT work that this adapter is doing
    // - Queues the base work item for this particular adapter
    // - Grabs ownership of the base work item created above
    // (this will be done synchronously as we really can't do anything else before this is recorded anyway).
    @Override
    public void setupAdaptersBaseWorkItem()
    {
        ClientResponse response;
        try {
            // Queue this base work item for this particular adapter.
            response = getVoltClient().callProcedure("WorkItemQueue"
                    ,"BaseWorkItem"  // queue
                    ,adapterType     // type of adapter that should handle this work item
                    ,"BaseWork"      // work to be done
                    ,""              // parameters
                    ,"F"             // should requester be notified when this work item finishes or not?
                    ,adapterType     // requester's adapter type
                    ,-1              // requester's work item (the work item that requested this new work item),
                    // -1 means that this is special base work item, no work item requested it
            );
        }catch (ProcCallException | IOException ie) {
            logger.error("setupAdaptersBaseWorkItem - WorkItemQueue failed - %s", Adapter.stackTraceToString(ie));
            throw new RuntimeException("An exception occurred when the WorkItemQueue stored procedure was called");
        }

        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("setupAdaptersBaseWorkItem - WorkItemQueue of BaseWorkItem FAILED - Status=%s, StatusString=%s, RequestingAdapterType=%s!",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString(), adapterType);
            throw new RuntimeException(response.getStatusString());
        }
        long lThisAdaptersBaseWorkItemId = response.getResults()[0].asScalarLong();
        logger.info("setupAdaptersBaseWorkItem - successfully queued Base work item, WorkItem=%d", lThisAdaptersBaseWorkItemId);

        try {
            // Grab the base work item.
            response = getVoltClient().callProcedure("WorkItemFindAndOwn"
                    ,adapterType                   // this adapter's type
                    ,adapterId                     // this adapter's id
                    ,"T"                            // bGrabBaseWork, "F" = want "normal" work items, not Base work items.
                    // "T" = want only specified Base work item for this adapter.
                    ,lThisAdaptersBaseWorkItemId    // the specific base work item for this adapter
                    ,"BaseWorkItem"                 // the name of the queue that you want to grab a workitem from (within the specified adapter type) - null means take a work item from any queue
            );
        }catch (ProcCallException | IOException ie) {
            logger.error("%s - setupAdaptersBaseWorkItem - WorkItemFindAndOwn failed - %s", adapterName, Adapter.stackTraceToString(ie));
            throw new RuntimeException("An exception occurred when the WorkItemFindAndOwn stored procedure was called");
        }

        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("setupAdaptersBaseWorkItem - WorkItemFindAndOwn of BaseWorkItem FAILED - Status=%s, StatusString=%s, WorkItemId=%d!",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString(), lThisAdaptersBaseWorkItemId);
            throw new RuntimeException(response.getStatusString());
        }
        if (response.getResults()[0].fetchRow(0).getLong("Id") != lThisAdaptersBaseWorkItemId) {
            // we did not get back the correct Base work item for this adapter!
            logger.error("setupAdaptersBaseWorkItem - WorkItemFindAndOwn of BaseWorkItem returned an unexpected work item, Expected=%d, Returned=%d!",
                    lThisAdaptersBaseWorkItemId, response.getResults()[0].fetchRow(0).getLong("Id"));
            throw new RuntimeException(adapterName + " - WorkItemFindAndOwn of BaseWorkItem returned an unexpected work item!");
        }
        logger.info("setupAdaptersBaseWorkItem - successfully found and now own the Base work item, AdapterType=%s, WorkItem=%d",
                adapterType, lThisAdaptersBaseWorkItemId);

        // Set/save the value for this adapter's base work item id.
        baseWorkItemId = lThisAdaptersBaseWorkItemId;

    }

    // This method is used to grab the next available work item for this type of adapter.
    // Parms:
    //      String sQueueName - the name of the "queue" that you want to pull a work item from,
    //                          a value of null indicates that you will take a work item from any queue.
    // Returns - boolean indication whether or not we did grab a new work item to work on
    @Override
    public boolean grabNextAvailWorkItem() throws IOException  { return grabNextAvailWorkItem(null); }
    @Override
    public boolean grabNextAvailWorkItem(String sQueueName) throws IOException
    {
        try {
            long aId = adapterId;
            if(information != null)
                aId = information.getId();
            // Grab the next available work item to work on.
            workItemResponse = getVoltClient().callProcedure("WorkItemFindAndOwn"
                    ,adapterType      // this adapter's type
                    ,aId              // this adapter's id
                    ,"F"              // bGrabBaseWork, "F" = want "normal" work items, not Base work items.  "T" = want only specified Base work item for this adapter.
                    ,baseWorkItemId   // the specific base work item for this adapter
                    ,sQueueName       // the name of the queue that you want to grab a workitem from (within the specified adapter type)
            );
        }catch (ProcCallException ie) {
            logger.error("grabNextAvailWorkItem - WorkItemFindAndOwn failed - %s", Adapter.stackTraceToString(ie));
            throw new RuntimeException("An exception occurred when the WorkItemFindAndOwn stored procedure was called");
        }

        if (workItemResponse.getStatus() != ClientResponse.SUCCESS) {
            logger.error("grabNextAvailWorkItem - WorkItemFindAndOwn of a new WorkItem failed - Queue=%s - %s!", sQueueName, workItemResponse.getStatusString());
            logRasEvent.logRasEventNoEffectedJob(
                    logRasEvent.getRasEventType("RasGenWorkItemFindAndOwnFailed", baseWorkItemId)
                    ,("AdapterName=" + adapterName + ", StatusString=" + workItemResponse.getStatusString() + ", Queue=" + sQueueName)
                    ,null                               // lctn
                    ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                    ,adapterType                       // type of the adapter that is requesting/issuing this invocation
                    ,baseWorkItemId                    // work item id for the work item that is being processed/executing, that is requesting/issuing this invocation
            );
            throw new RuntimeException(workItemResponse.getStatusString());
        }
        // Check & see if we got a work item.
        if (workItemResponse.getResults()[0].getRowCount() == 0) {
            // No work items are queued for this adapter to work on.
            logger.debug("No work items available for AdapterType=%s", adapterType);
            ++timeToWaitIfNothingQueued;   // bump timeToWaitIfNothingQueued - since there weren't any work items for this adapter.
            workToBeDone = null;
            workItemId = -99999;
            thisIsNewWorkItem = true;
            workingResults = null;
            return false;  // did not grab a work item
        }
        else {
            // We did get a new work item.
            workItemResponse.getResults()[0].advanceRow();
            workItemId = workItemResponse.getResults()[0].getLong("Id");
            workToBeDone = workItemResponse.getResults()[0].getString("WorkToBeDone");
            String sState = workItemResponse.getResults()[0].getString("State");
            workingResults = workItemResponse.getResults()[0].getString("WorkingResults");
            thisIsNewWorkItem = true;  // tracks whether or not this is a "new" work item or one that has been requeued.
            if (sState.equals("Q")) {
                logger.info("grabNextAvailWorkItem - Grabbed a new work item WorkToBeDone=%s, WorkItem=%d", workToBeDone, workItemId);
            } else {
                thisIsNewWorkItem = false;  // this is a requeued work item.
                logger.info("grabNextAvailWorkItem - Grabbed a REQUEUED work item WorkToBeDone=%s, State=%s, LengthWorkingResults=%d, WorkItem=%d",
                        workToBeDone, sState, workingResults.length(), workItemId);
            }
            // Reset timeToWaitIfNothingQueued (since we did get a work item).
            timeToWaitIfNothingQueued = 0;
            return true;  // did grab a work item
        }
    }

    @Override
    public String workToBeDone()
    {
        return workToBeDone;
    }
    @Override
    public int amtTimeToWait()  { return timeToWaitIfNothingQueued; }

    //-----------------------------------------------------------------
    // Handles cleanup necessary in an Adapter's main() method, just before main() terminates.
    //-----------------------------------------------------------------
    @Override
    public void handleProcessingWhenUnexpectedWorkItem() throws IOException
    {
        logger.error("unable to handle this work item %s as we have not defined the switch case for it - need to " +
                "add a case statement!", workToBeDone);
        logRasEvent.logRasEventNoEffectedJob(logRasEvent.getRasEventType("RasGenAdapterMissingCaseStmt",
                workItemId)
                ,("WorkToBeDone=" + workToBeDone)   // instanceData
                ,null                               // lctn
                ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred,
                                                    // in micro-seconds since epoch
                ,adapterType                        // requesting adapter type
                ,workItemId                         // requesting workitem id
        );
        information.signalToShutdown();
    }

    @Override
    public long baseWorkItemId()
    {
        return baseWorkItemId;
    }

    @Override
    public boolean isThisNewWorkItem() { return thisIsNewWorkItem; }

    @Override
    public String[] getClientParameters(String separator) {
        String sTempParms = workItemResponse.getResults()[0].getString("Parameters");
        if (sTempParms == null)
            return null;
        else
            return sTempParms.split(separator);
    }   // End getClientParameters(String separator)

    @Override
    public Map<String, String> getClientParameters() {
        HashMap<String, String> result = new HashMap<>();
        /* '$' is used as regex to split between multiple parameters*/
        /* "(?<!\\\\)\\$") means split on all occurrences of $ that isn't precedded by "\" */
        String sTempParms = workItemResponse.getResults()[0].getString("Parameters");
        if (sTempParms == null)
            return result;
        String[] parameters = sTempParms.split("(?<!\\\\)\\$");
        for(String temp: parameters) {
            if((temp != null) && (!temp.isEmpty())){
                /* '#' is used as regex to split key and value of a parameter
                 * "(?<!\\\\)#" means split on all occurrences of # that isn't precedded by "\" */
                logger.info("parameter is %s", temp);
                String[] keyValue = temp.split("(?<!\\\\)#");
                logger.info("key is %s and value is %s", keyValue[0], keyValue[1]);
                result.put(keyValue[0], keyValue[1]);
            }
        }
        return result;
    }

    // Queue a work item for another adapter to work on.
    // Parms:
    //      String  sWrkAdapType                        - which type of adapter should this work item be queued to (which type of adapter should do the work item that is being queued)
    //      String  sQueue                              - which queue should this work item be put on
    //      String  sWorkWantDone                       - what work do we want this work item to do
    //      String  sWorkWantDoneParameters             - parameters (associated with sWorkWantDone) for the work item being queued
    //      boolean bNotifyCallerWhenWorkItemFinishes   - flag indicating whether or not the requester should be notified when this work item finishes or not
    //      String  sReqAdapterType                     - type of the adapter that invoked this method
    //      long    lReqWorkItemId                      - work item that the invoking adapter was working on when this method was invoked
    @Override
    public long queueWorkItem(String sWrkAdapType, String sQueue, String sWorkWantDone, String sWorkWantDoneParameters,
                       boolean bNotifyCallerWhenWorkItemFinishes,
                       String sReqAdapterType, long lReqWorkItemId)
            throws IOException
    {
        String sNotifyCallerWhenWorkItemFinishes;
        ClientResponse response;
        if (bNotifyCallerWhenWorkItemFinishes)
            sNotifyCallerWhenWorkItemFinishes = "T";
        else
            sNotifyCallerWhenWorkItemFinishes = "F";
        try {
            response = getVoltClient().callProcedure("WorkItemQueue"                     // stored procedure
                    ,sQueue                              // queue
                    ,sWrkAdapType                        // which type of adapter should do this work item (who are we
                                                         // queueing this work to)
                    ,sWorkWantDone                       // work to be done
                    ,sWorkWantDoneParameters             // this work item's parameters
                    ,sNotifyCallerWhenWorkItemFinishes   // should requester be notified when this work item finishes or not? -
                                                         // F = Don't need notification when this item has been completed.
                    ,sReqAdapterType                     // requester's adapter type
                    ,lReqWorkItemId                      // requester's work item (the work item that requested this new work item)
            );
        } catch (ProcCallException ie) {
            logger.error("%s - queueWorkItem - WorkItemQueue failed - %s", adapterName, Adapter.stackTraceToString(ie));
            throw new RuntimeException("An exception occurred when the WorkItemQueue stored procedure was called");
        }
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("queueWorkItem - WorkItemQueue of %s FAILED - Status=%s, StatusString=%s, RequestingAdapterId=%d, RequestingAdapterType=%s, RequestingWorkItemId=%d!",
                    sWorkWantDone, VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString(),
                    adapterId, sReqAdapterType, lReqWorkItemId);
            throw new RuntimeException(response.getStatusString());
        }
        return response.getResults()[0].asScalarLong();
    }

    @Override
    public long queueWorkItem(String sWrkAdapType, String sQueue, String sWorkWantDone,
                              Map<String, String> sWorkWantDoneParameters,
                              boolean bNotifyCallerWhenWorkItemFinishes,
                              String sReqAdapterType, long lReqWorkItemId)
            throws IOException
    {
        StringBuilder params = new StringBuilder();
        sWorkWantDoneParameters.forEach((k,v) -> {
            /*If there exists # or $ to value, add an escape character '\' to # or $ to value */
            if (v != null) {
                v = v.replaceAll("#", "\\\\#");
                v = v.replaceAll("\\$", "\\\\\\$");
            }
            logger.info("key is %s and value is %s", k, v);
            if (((k != null) && (!k.isEmpty())) && ((v != null) && (!v.isEmpty()))) {
                params.append(k);
                params.append("#");
                params.append(v);
                params.append("$");
            }
        });
        return  queueWorkItem(sWrkAdapType, sQueue, sWorkWantDone, params.toString(),
                bNotifyCallerWhenWorkItemFinishes, sReqAdapterType, lReqWorkItemId);
    }

    // Indicate that we have finished working on this work item (mark the work item as Finished).
    @Override
    public void finishedWorkItem(String sCmdForMsg, long lWorkItemId, String sWorkItemResults)
            throws IOException
    {
        ClientResponse response;
        try {
            response = getVoltClient().callProcedure("WorkItemFinished"
                    ,adapterType        // this particular adapter's type
                    ,lWorkItemId         // the specific base work item for this adapter
                    ,compressResult(sWorkItemResults)    // results for the adapter's base work item
            );

        }catch (ProcCallException ie) {
            logger.error("finishedWorkItem - WorkItemFinished failed - %s", Adapter.stackTraceToString(ie));
            throw new RuntimeException("An exception occurred when the WorkItemFinished stored procedure was called");
        }

        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("WorkItemFinished FAILED for work item %s - Status=%s, StatusString=%s, WorkItem=%d!",
                    sCmdForMsg, VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString(),
                    lWorkItemId);
            throw new RuntimeException(response.getStatusString());
        }
        logger.info("finished work item %s - AdapterType=%s, FinishedWorkItem=%d", sCmdForMsg, adapterType,
                lWorkItemId);

    }

    // Indicate that we have finished working on this work item (mark the work item as Finished),
    // but the workitem completed in Error, so update workitem status to 'E'.
    @Override
    public void finishedWorkItemDueToError(String sCmdForMsg, long lWorkItemId, String sWorkItemResults)
            throws IOException
    {
        ClientResponse response;
        try {
            response = getVoltClient().callProcedure("WorkItemFinishedDueToError"
                    ,adapterType        // this particular adapter's type
                    ,lWorkItemId         // the specific base work item for this adapter
                    ,compressResult(sWorkItemResults)    // results for the adapter's base work item
            );

        }catch (ProcCallException ie) {
            logger.error("finishedWorkItemDueToError - WorkItemFinishedDueToError failed - %s", Adapter.stackTraceToString(ie));
            throw new RuntimeException("An exception occurred when the WorkItemFinishedDueToError stored procedure was called");
        }

        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("WorkItemFinished FAILED for work item %s - Status=%s, StatusString=%s, WorkItem=%d!",
                    sCmdForMsg, VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString(), lWorkItemId);
            throw new RuntimeException(response.getStatusString());
        }
        logger.info("finished work item %s - AdapterType=%s, FinishedWorkItem=%d", sCmdForMsg, adapterType, lWorkItemId);

    }

    @Override
    public long workItemId()
    {
        return workItemId;
    }

    // Wait for the specified work item to "finish", meaning that the adapter working on the work item has done all it is going to do with it!
    //  Note: This could be done via a message being sent to this adapter to let it know the work item is finished,
    //        but for this prototype we are just going to look in the db table and see when it transitions to finished.
    // Parms:
    //      String  sCmdForMsg              - the work item that is being waited for (e.g., DetermineInitialNodeStates, HandleInputFromExternalComponent)
    //      String  sWaitingForAdapterType  - which type of adapter is working on this work item
    //      long    lWaitingForWorkItemId   - which work item id are we waiting to be finished
    //      String  sReqAdapterType         - type of the adapter that invoked this method
    //      long    lReqWorkItemId          - work item that the invoking adapter was working on when this method was invoked
    // Returns:
    //      String array - sa[0] = state of the work item (e.g., 'F' or 'E')   sa[1] = work item results (in string form)
    @Override
    public String[] waitForWorkItemToFinishAndMarkDone(String sCmdForMsg, String sWaitingForAdapterType,
                                                long lWaitingForWorkItemId, String sReqAdapterType, long lReqWorkItemId)
            throws IOException, InterruptedException
    {
        logger.info("waitForWorkItemToFinishAndMarkDone - waiting for work item %s to finish, WaitingForAdapterType=%s, WaitingForWorkItemId=%d",
                sCmdForMsg, sWaitingForAdapterType, lWaitingForWorkItemId);
        String sa[] = new String[2];
        ClientResponse response;
        while (true) {
            // Check & see if the work item has finished yet.
            try {
                response = getVoltClient().callProcedure("WorkItemFinishedResults"
                        ,sWaitingForAdapterType     // work item's adapter type (what type of adapter is working on this work item)
                        ,lWaitingForWorkItemId      // work item's id (the id of the work item that we are waiting to)
                );
            } catch (ProcCallException ie) {
                logger.error("waitForWorkItemToFinishAndMarkDone - WorkItemFinishedResults failed - %s", Adapter.stackTraceToString(ie));
                throw new RuntimeException("An exception occurred when the WorkItemFinishedResults stored procedure was called");
            }
            if (response.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                logger.error("waitForWorkItemToFinishAndMarkDone - WorkItemFinishedResults of %d FAILED - Status=%s, StatusString=%s, AdapterType=%s," +
                                " WorkItemId=%d!", lWaitingForWorkItemId, VoltDbClient.statusByteAsString(response.getStatus()),
                        response.getStatusString(), sWaitingForAdapterType, lWaitingForWorkItemId);
                throw new RuntimeException(response.getStatusString());
            }
            // Check & see if the work item has finished
            if (response.getResults()[0].getRowCount() > 0) {
                // Work item has finished
                response.getResults()[0].advanceRow();
                sa[0] = response.getResults()[0].getString("State");    // indicates whether the work item
                // finished successfully or finished due to error
                sa[1] = decompressResult(response.getResults()[0].getString("Results"));  // item's results
                // Log appropriate success or failure message.
                if (sa[0].equals("F")) {
                    // the work item's state is "F" (finished successfully, not finished due to an error)
                    logger.info("waitForWorkItemToFinishAndMarkDone - work item %s was successful, FinishedWorkItemId=%d", sCmdForMsg, lWaitingForWorkItemId);
                } else {
                    // the work item's state is not "F" (the work item did NOT finish successfully)
                    logger.error("waitForWorkItemToFinishAndMarkDone - work item %s FAILED - %s, FinishedWorkItemId=%d!", sCmdForMsg,
                            sa[1], lWaitingForWorkItemId);
                    logRasEvent.logRasEventNoEffectedJob(
                            logRasEvent.getRasEventType("RasGenAdapterWaitForWorkItemToFinishAndMarkDoneFailed",
                                    lReqWorkItemId)
                            ,("AdapterName=" + adapterName + ", WorkToBeDone=" + sCmdForMsg + ", " + "WorkItem=" +
                                    Long.toString(lWaitingForWorkItemId) + ", Results=" + sa[1])
                            ,null                               // lctn
                            ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                            ,sReqAdapterType                    // type of the adapter that invoked this method
                            ,lReqWorkItemId                     // work item that the adapter was working on when this method was invoked
                    );
                }
                // Also mark this work item as done.
                markWorkItemDone(sWaitingForAdapterType, lWaitingForWorkItemId);
                logger.info("waitForWorkItemToFinishAndMarkDone - marked work item %s as done, DoneWorkItemId=%d", sCmdForMsg, lWaitingForWorkItemId);
                return sa;
            }
            Thread.sleep(1);  // Give up the processor to let other work be done.
        }
    }

    static String compressResult(String data) {
        data = constrainInputForCompressor(data);
        try (ByteArrayOutputStream byteStream = new ByteArrayOutputStream()) {
            try (GZIPOutputStream compressorStream = new GZIPOutputStream(byteStream, true)) {
                while(true) {
                    String result = compressStringToStream(data, byteStream, compressorStream);
                    if(result.length() < MAX_RESULT_LENGTH)
                        return result;
                    else
                        data = reduceStringBy90Percent(data);
                }
            }
        } catch(IOException e) {
            throw new RuntimeException("Compressing result data into work item failed!", e); // Should NEVER happen!
        }
    }

    private static String compressStringToStream(String data, ByteArrayOutputStream byteStream,
                                                 GZIPOutputStream compressorStream) throws IOException {
        byte[] dataBytes = data.getBytes(StandardCharsets.UTF_8);
        compressorStream.write(dataBytes);
        compressorStream.flush();
        compressorStream.finish();
        byteStream.flush();
        byte[] compressedBytes = byteStream.toByteArray();
        return Base64.getEncoder().encodeToString(compressedBytes);
    }

    private static String constrainInputForCompressor(String data) {
        while(data.length() > MAX_RESULT_COMPRESSOR_LENGTH)
            data = reduceStringBy90Percent(data);
        return data;
    }

    // Returns 90% of the passed string, truncating at the end then adding a message.
    private static String reduceStringBy90Percent(String data) {
        return data.substring(0, data.length() * 9 / 10) + "\n\n*** Results have been truncated!";
    }

    static String decompressResult(String data) {
        if(data == null) {
            return "NULL response received";
        }
        byte[] dataBytes = Base64.getDecoder().decode(data);
        try (ByteArrayInputStream byteStream = new ByteArrayInputStream(dataBytes)) {
            try (GZIPInputStream decompressedStream = new GZIPInputStream(byteStream)) {
                try (ByteArrayOutputStream resultBytes = new ByteArrayOutputStream()) {
                    return readStringFromStream(decompressedStream, resultBytes);
                }
            }
        } catch(IOException e) {
            throw new RuntimeException("Decompressing result data from work item failed!", e); // Should NEVER happen!
        }
    }

    private static String readStringFromStream(GZIPInputStream decompressedStream,
                                               ByteArrayOutputStream resultBytes) throws IOException {
        byte[] chunk = new byte[BYTE_BUFFER_BLOCK_SIZE];
        int read = decompressedStream.read(chunk, 0, BYTE_BUFFER_BLOCK_SIZE);
        while (read != -1) {
            resultBytes.write(chunk, 0, read);
            read = decompressedStream.read(chunk, 0, BYTE_BUFFER_BLOCK_SIZE);
        }
        return new String(resultBytes.toByteArray(), StandardCharsets.UTF_8);
    }

    @Override
    public String workingResults()
    {
        return workingResults;
    }

    // Save "restart" data for the specified work item
    // (this data indicates how much processing has already happened, so in case the adapter fails and another instance picks up the work it will know where to continue)
    //  boolean bInsertRowIntoHistory = Flag indicating whether we should insert OR update a history record for the value of this work item's db row when updating the workitem for this particular invocation!
    //                                    - true  means to insert another workitem history record, rather than doing an updated of the existing workitem's history record - this is the "usual" flow
    //                                    - false means to update this workitem's history record, rather than doing an insert of another workitem history record - this is "UNusual" (only used when a workitem is updating its working results fields very often)
    //                                    Note: the only reason to not do an insert is when the updates are very numerous and very frequent
    //                                    (e.g., when updating work item entry for MonitorLogFile work item's - these save the timestamp of the last "handled" log message, no need for all the intermediary history of those timestamps)
    //  long lTsInMicroSecs = is the timestamp value (in units of microsecs since epoch) that should be used for the DbUpdatedTimestamp field
    //                           0L means that we should use the current timestamp at the time of the db operation - this is "usual" flow
    //                          !0L means use specified value during the db operation - this is "UNusual" (only used to prevent endless movement of DataMover/DataReceiver work items from Tier1 to Tier2)
    @Override
    public long saveWorkItemsRestartData(long lWorkItemId, String sRestartData, boolean bInsertRowIntoHistory, long lTsInMicroSecs)
            throws IOException {
        byte flagInsertOrUpdateRowInHistory = ((bInsertRowIntoHistory) ? (byte) 1 : (byte) 0);
        ClientResponse response;
        try {
            response = getVoltClient().callProcedure("WorkItemSaveRestartData"
                    , adapterType                    // this adapter's type
                    , lWorkItemId                     // the work item id that we want to save restart data for
                    , sRestartData                    // the restart data that should be saved for this work item
                    , flagInsertOrUpdateRowInHistory  // flag indicating whether we should insert OR update a history record
                    // for the value of this work item's db row when updating the workitem
                    , lTsInMicroSecs                  // timestamp value that should be used for DbUpdatedTimestamp field
                    // when updating this workitem's history - 0L means to use the current timestamp.
            );
        } catch (ProcCallException ie) {
            logger.error("saveWorkItemsRestartData - WorkItemSaveRestartData failed - %s", Adapter.stackTraceToString(ie));
            throw new RuntimeException("An exception occurred when the WorkItemSaveRestartData stored procedure was called");
        }

        if (response.getStatus() != ClientResponse.SUCCESS) {
            logger.error("saveWorkItemsRestartData - WorkItemSaveRestartData FAILED - Status=%s, StatusString=%s, AdapterType=%s, WorkItem=%d!",
                    VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString(), adapterType, lWorkItemId);
            logRasEvent.logRasEventNoEffectedJob(
                    logRasEvent.getRasEventType("RasGenAdapterWiSaveRestartDataFailed", lWorkItemId)
                    , ("AdapterName=" + adapterName + ", WorkItemId=" + Long.toString(lWorkItemId))
                    , null                               // lctn
                    , System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred,
                    // in micro-seconds since epoch
                    , adapterType                       // type of the adapter that is requesting/issuing this invocation
                    , lWorkItemId                       // work item id for the work item that is being processed/executing,
                    // that is requesting/issuing this invocation
            );
            throw new RuntimeException(response.getStatusString());
        }
        return 0;
    }

    // This is the default parameter version of the saveWorkItemsRestartData method.
    @Override
    public long saveWorkItemsRestartData(long lWorkItemId, String sRestartData)
            throws IOException
    {
        return saveWorkItemsRestartData(lWorkItemId, sRestartData, true, 0L);
    }

    // This is the second default parameter version of the saveWorkItemsRestartData method.
    @Override
    public long saveWorkItemsRestartData(long lWorkItemId, String sRestartData, boolean bInsertRowIntoHistory)
            throws IOException
    {
        return saveWorkItemsRestartData(lWorkItemId, sRestartData, bInsertRowIntoHistory, 0L);
    }

    // This method is used to check and see if any "work" was done within this adapter during this last "iteration".
    @Override
    public boolean wasWorkDone()
    {
        return (timeToWaitIfNothingQueued <= 0);
    }

    //--------------------------------------------------------------------------
    // Get the state and results for the specified work item.
    // Parms:
    //   sWaitingForAdapterType - work item's adapter type
    //   lWaitingForWorkItemId -
    //--------------------------------------------------------------------------
    @Override
    public String[] getWorkItemStatus(String sWaitingForAdapterType, long lWaitingForWorkItemId)
            throws IOException
    {
        String sa[] = new String[2];
        ClientResponse response;
        // Check & see if the work item has finished yet.
        try {
            response = getVoltClient().callProcedure("WorkItemStateAndResults"
                    ,sWaitingForAdapterType     // work item's adapter type (what type of adapter is working on this work item)
                    ,lWaitingForWorkItemId      // work item's id (the id of the work item that we are waiting to)
            );
        }catch (ProcCallException ie) {
            logger.error("getWorkItemStatus - WorkItemStateAndResults failed - %s", Adapter.stackTraceToString(ie));
            throw new RuntimeException("An exception occurred when the WorkItemStateAndResults stored procedure was called");
        }

        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("getWorkItemStatus - WorkItemStateAndResults of %d FAILED - Status=%s, StatusString=%s, AdapterType=%s, WorkItemId=%d!",
                    lWaitingForWorkItemId, VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString(), sWaitingForAdapterType, lWaitingForWorkItemId);
            throw new RuntimeException(response.getStatusString());
        }
        // Check & see if the work item has finished
        if (response.getResults()[0].getRowCount() > 0) {
            // Work item has finished
            response.getResults()[0].advanceRow();
            sa[0] = response.getResults()[0].getString("State");    // indicates whether the work item finished successfully or finished due to error
            sa[1] = decompressResult(response.getResults()[0].getString("Results"));  // work item's results
            // Log appropriate success or failure message.
            if (sa[0].equals("F")) {
                // the work item's state is "F" (finished successfully, not finished due to an error)
                logger.info("getWorkItemStatus - work item finished successfully - AdapterType=%s, WorkItemId=%d", sWaitingForAdapterType, lWaitingForWorkItemId);
                // Also mark this work item as done.
                markWorkItemDone(sWaitingForAdapterType, lWaitingForWorkItemId);
                logger.info("getWorkItemStatus - work item marked done - AdapterType=%s, WorkItemId=%d", sWaitingForAdapterType, lWaitingForWorkItemId);
            } else if (sa[0].equals("E")) {
                // the work item's state is not "F" (the work item did NOT finish successfully)
                logger.error("getWorkItemStatus - work item FAILED - %s, AdapterType=%s, WorkItemId=%d!", sa[1], sWaitingForAdapterType, lWaitingForWorkItemId);
                logRasEvent.logRasEventNoEffectedJob(
                        logRasEvent.getRasEventType("RasGenAdapterWaitForWorkItemToFinishAndMarkDoneFailed",
                                lWaitingForWorkItemId)
                        ,("AdapterName=" + adapterName + ", AdapterType=" + sWaitingForAdapterType + ", WorkItem=" + Long.toString(lWaitingForWorkItemId) + ", Results=" + sa[1])
                        ,null                               // lctn
                        ,System.currentTimeMillis() * 1000L // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                        ,sWaitingForAdapterType             // type of the adapter that invoked this method
                        ,lWaitingForWorkItemId              // work item that the adapter was working on when this method was invoked
                );
                markWorkItemDone(sWaitingForAdapterType, lWaitingForWorkItemId);
            }
        } else {
            sa[0] = "F";
            sa[1] = "Work Item Not Found!";
            logger.info("getWorkItemStatus - work item was not found - AdapterType=%s, WorkItemId=%d", sWaitingForAdapterType, lWaitingForWorkItemId);
        }
        return sa;
    }

    @Override
    public void markWorkItemDone(String workItemAdapterType, long workItemId)
            throws IOException
    {
        ClientResponse response;
        try {
            response = getVoltClient().callProcedure("WorkItemDone"
                    ,workItemAdapterType    // type of adapter associated with the work item
                    ,workItemId             // the work item id of the work item that we are now done with
            );
        } catch (ProcCallException ie) {
            logger.error("markWorkItemDone - WorkItemDone failed - %s", Adapter.stackTraceToString(ie));
            throw new RuntimeException("An exception occurred when the WorkItemDone stored procedure was called");
        }

        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            logger.error("markWorkItemDone - WorkItemDone of %d FAILED - Status=%s, StatusString=%s, AdapterType=%s, WorkItemId=%d!",
                    workItemId, VoltDbClient.statusByteAsString(response.getStatus()), response.getStatusString(),
                    workItemAdapterType, workItemId);
            throw new RuntimeException(response.getStatusString());
        }
    }   // End markWorkItemDone(String workItemAdapterType, long workItemId)


    //--------------------------------------------------------------------------
    // Get the timestamp string from a work item's working results field.
    //--------------------------------------------------------------------------
    @Override
    public String getTsFromWorkingResults(String sWrkResults) {
        final String WrkResultTimestampPrefix = "(Timestamp=";
        String sDataRcvrTs = null;
        int iIndexStartOfTsInfo = sWrkResults.indexOf(WrkResultTimestampPrefix);
        if (iIndexStartOfTsInfo != -1) {
            // found the start of the timestamp.
            int iIndexOfActualStartOfTs = iIndexStartOfTsInfo + WrkResultTimestampPrefix.length();
            int iIndexEndOfTsInfo = sWrkResults.indexOf(")", iIndexOfActualStartOfTs);
            if (iIndexEndOfTsInfo != -1) {
                sDataRcvrTs = sWrkResults.substring(iIndexOfActualStartOfTs, iIndexEndOfTsInfo);
            }
        }
        return sDataRcvrTs;
    }   // End getTsFromWorkingResults(String sWrkResults)


}
