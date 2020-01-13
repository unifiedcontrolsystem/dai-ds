// Copyright (C) 2017-2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ras;

import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.*;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.exceptions.AdapterException;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.*;
import org.voltdb.client.*;
import org.voltdb.VoltTable;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.text.SimpleDateFormat;


/**
 * AdapterRas for the VoltDB database.
 *
 * Parms:
 *  List of the db node names, so that this client connects to each of them (this is a comma separated list of
 *  hostnames or IP addresses. E.g., voltdbserver1,voltdbserver2,10.11.12.13
 *
 * Example invocation:
 *      java AdapterRasForeignBus voltdbserver1,voltdbserver2,10.11.12.13
 *      or
 *      java AdapterRasForeignBus  - this will default to using localhost
 *
 * SELECT RasEvent.EventType, RasEvent.Timestamp,  RasMetaData.Severity, RasEvent.Lctn, RasEvent.ControlOperation,
 *      RasMetaData.Component, RasMetaData.Msg, RasEvent.InstanceData FROM RasEvent INNER JOIN RasMetaData on
 *      RasEvent.EventType=RasMetaData.EventType Order By Timestamp;
 * SELECT RasEvent.Id, RasEvent.EventType, RasEvent.Timestamp, RasEvent.ControlOperation, RasMetaData.Severity,
 *      RasMetaData.Category, RasMetaData.Component, RasMetaData.Msg, RasEvent.InstanceData FROM RasEvent INNER JOIN
 *      RasMetaData on RasEvent.EventType=RasMetaData.EventType WHERE RasEvent.EventType = '0005000005';
 */
public class AdapterRasForeignBus {
    private AdapterInformation adapter_;
    private AdapterOperations      operations_;
    private NodeInformation        nodeInfo_;
    private RasEventLog            rasEvents_;
    private Logger                 log_;
    private DataStoreFactory       factory_;
    private Client                 client_;
    private long                   lastTimeInMsDidChkForExpiredJobs_;

    // Constructor
    AdapterRasForeignBus(AdapterInformation adapter, Logger logger, DataStoreFactory factory) {
        log_ = logger;
        adapter_ = adapter;
        factory_ = factory;
        client_ = factory_.createVoltDbLegacyAccess().getVoltDbClient();
        nodeInfo_ = factory_.createNodeInformation();
        rasEvents_ = factory_.createRasEventLog(adapter_);
        operations_ = factory_.createAdapterOperations(adapter_);
        lastTimeInMsDidChkForExpiredJobs_ = System.currentTimeMillis();
    }   // ctor
    // Member Data

    private void fetchSystemName() {
        try {
            ClientResponse response = client_.callProcedure("MachineDescription");
            if (response.getStatus() == ClientResponse.SUCCESS) {
                response.getResults()[0].advanceRow();
                machineName_ = response.getResults()[0].getString("Description");
            }
        } catch(IOException | ProcCallException e) {
            log_.exception(e, "Failed to get the correct machine name!");
        }
    }

    private long handleRasEventControlOperations() throws IOException, ProcCallException, InterruptedException {
        // Actually get the list of pertinent ras events.
        ClientResponse response = getClientResponseforRasEventProcessNewControlOperations(client_.callProcedure("RasEventProcessNewControlOperations"), "Stored procedure RasEventProcessNewControlOperations FAILED - Status=%s, StatusString=%s, ");
        // Loop through each of the pertinent RAS events invoking the appropriate Control Operation.
        VoltTable vt = response.getResults()[0];
        long workDone = vt.getRowCount();
        for (int iRasEventCntr = 0; iRasEventCntr < vt.getRowCount(); ++iRasEventCntr) {
            vt.advanceRow();
            // Determine the appropriate action to take for this RAS event, based on ControlOperation and any pertinent
            // policy that may be in effect.
            String sTempEventType        = vt.getString("EventType");
            long   lTempEventId          = vt.getLong("Id");
            String sTempEventId          = Long.toString(lTempEventId);
            String sTempControlOperation = vt.getString("ControlOperation");
            String sTempLctn             = vt.getString("Lctn");
            String sTempJobId            = vt.getString("JobId");
            String sTempDescrName        = vt.getString("DescriptiveName");
            boolean bDoneWithThisEventsControlOperation = false;

            if (sTempLctn == null) {
                // the specified RAS event's lctn is null, so no don't try to run any control operation.
                log_.warn("Did NOT run this RAS event's ControlOperation because the event has no specified lctn, ignoring this ControlOperation - DescrName=%s, EventId=%s, Lctn=%s, JobId=%s, ControlOperation=%s!",
                        sTempDescrName, sTempEventId, sTempLctn, sTempJobId, sTempControlOperation);
                // Explicitly set the boolean indicating that we are done with this event's control operation.
                // Note: yes I know that the boolean is already set to true, but I want it explicitly set here so it is never accidentally removed from this specific flow...
                bDoneWithThisEventsControlOperation = true;
            }   // the specified RAS event's lctn is null, so no don't try to run any control operation.

            // Is the node in error?
            if(!bDoneWithThisEventsControlOperation && errorOperations_.contains(sTempControlOperation))
                operations_.markNodeInErrorState(sTempLctn, false);

            // Record that we are done with this RAS event's control operation (so that we don't try and run its
            // control event later).
            //------------------------------------------------------------------
            // Update this ras event to indicate that we have executed its control operation (also need to update the
            // DbUpdatedTimestamp field).
            //------------------------------------------------------------------
            String sTempStoredProcedure = "RasEventUpdateControlOperationDone";
            ClientResponse response1 = client_.callProcedure(sTempStoredProcedure, "Y", sTempEventType, sTempEventId);
            if(response1.getStatus() != ClientResponse.SUCCESS)
                logRasEventOnFailure(sTempStoredProcedure, response1.getStatus());
        }   // Loop through each of the pertinent RAS events invoking the appropriate Control Operation.
        return workDone;
    }   // End handleRasEventControlOperations()

    private ClientResponse getClientResponseforRasEventProcessNewControlOperations(ClientResponse rasEventProcessNewControlOperations, String s) {
        ClientResponse response = rasEventProcessNewControlOperations;
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.error(s +
                            "AdapterType=%s, ThisAdapterId=%d!",
                    statusByteAsString(response.getStatus()), response.getStatusString(),
                    adapter_.getType(), adapter_.getId());
            throw new RuntimeException(response.getStatusString());
        }
        return response;
    }

    //--------------------------------------------------------------------------
    // Fill in effected JobId for those RAS events that indicate that JobId should be checked for.
    // - Note: this method also deletes "expired" jobs from the InternalCachedJobs table.
    //--------------------------------------------------------------------------
    long fillInRasEventsWithMissingJobId(SimpleDateFormat sqlDateFormat)
            throws IOException, ProcCallException, DataStoreException {
        long lTempSaveStartingTsInMs = System.currentTimeMillis();  // save the time that we began this iteration of
        // filling in JobIds.
        //----------------------------------------------------------------------
        // Get a list of RAS events that need to have a JobId filled in (or said differently to check and see if they
        // want us to check and see if there is a JobId to fill in for it).
        // Note: we don't want to use the current time for building this list of RAS events, rather we want to
        //       process events that are at least 4 seconds old.
        //        This is to give the job info time enough to propagate from log files into the InternalCachedJobs
        //          table (just in case that RAS event shows up before the Job info percolates through).
        //        NOTE2: There is nothing scientific about the number 4 seconds, it was chosen simple because it
        //          seemed reasonable at the time.
        //----------------------------------------------------------------------
        long lTempDelayedTsInMs = lTempSaveStartingTsInMs - (4 * 1000L);  // calculate the delayed timestamp, current
        // time minus 4 seconds (see note above).
        ClientResponse response = getClientResponseForRasEventListThatNeedJobId(
                client_.callProcedure("RasEventListThatNeedJobId", (lTempDelayedTsInMs * 1000L)),
                "Stored procedure RasEventListThatNeedJobId FAILED - Status=%s, StatusString=%s, ");
        //----------------------------------------------------------------------
        // Loop through each of these RAS events and determine whether or not they have an associated JobId.
        //----------------------------------------------------------------------
        VoltTable vtRasEventList = response.getResults()[0];
        long workDone = vtRasEventList.getRowCount();
        for (int iRasEventCntr = 0; iRasEventCntr < vtRasEventList.getRowCount(); ++iRasEventCntr) {
            vtRasEventList.advanceRow();
            String  sRasEventType = vtRasEventList.getString("EventType");
            long    lRasEventId   = vtRasEventList.getLong("Id");
            String  sRasEventLctn = vtRasEventList.getString("Lctn");
            // get a long representation of the ras event's timestamp, units of microseconds since epoch (GMT timezone).
            long    lRasEventTsInMicroSecs = vtRasEventList.getTimestampAsLong("LastChgTimestamp");
            //------------------------------------------------------------------
            // Check and see if the specified lctn is one that could have a job associated with it.
            //------------------------------------------------------------------
            String sFndJobId = null;  // defaults to "there is no job associated with this RAS event".
            // Check & see if this is a fully qualified ComputeNode location.
            if (nodeInfo_.isComputeNodeLocation(sRasEventLctn)) {
                // this is a fully qualified ComputeNode location - it is capable of having a job associated with it.
                // Get the JobId (if any) that was running on the specified ComputeNode lctn at the specified time.
                response = client_.callProcedure("InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp",
                        sRasEventLctn, lRasEventTsInMicroSecs, lRasEventTsInMicroSecs);
                if (response.getStatus() != ClientResponse.SUCCESS) {
                    // stored procedure failed.
                    log_.error("Stored procedure InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp FAILED - " +
                                    "Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                            statusByteAsString(response.getStatus()), response.getStatusString(),
                            adapter_.getType(), adapter_.getId());
                    throw new RuntimeException(response.getStatusString());
                }
                VoltTable vtInternalCachedJobsList = response.getResults()[0];
                // Ensure that there was <= 1 matching JobId.
                if (vtInternalCachedJobsList.getRowCount() == 1) {
                    // exactly 1 job id was returned.
                    // Save the matching JobId information.
                    vtInternalCachedJobsList.advanceRow();
                    sFndJobId = vtInternalCachedJobsList.getString("JobId");
                }
                else {
                    // Ensure that we got at most 1 JobId returned - that we did not see 2 or more jobs using the
                    // same ComputeNode (when this error is detected, log all relevant information, and then continue
                    // using simply the first job id).
                    if (vtInternalCachedJobsList.getRowCount() > 1) {
                        StringBuilder sListOfJobIds = new StringBuilder();
                        for (int iJobCntr = 0; iJobCntr < vtInternalCachedJobsList.getRowCount(); ++iJobCntr) {
                            vtInternalCachedJobsList.advanceRow();
                            if (sFndJobId == null)
                                sFndJobId = vtInternalCachedJobsList.getString("JobId");  // use the first job id that
                            // was found.
                            // Get this row's data.
                            sListOfJobIds.append(vtInternalCachedJobsList.getString("JobId")).append(",");
                        }
                        sListOfJobIds.deleteCharAt((sListOfJobIds.length()-1));  // delete trailing comma.
                        log_.error("Detected that there were multiple jobs using the same ComputeNode - " +
                                        "OrigRasEventType=%s, OrigRasEventId=%d, Lctn=%s, Jobs=%s!",
                                sRasEventType, lRasEventId, sRasEventLctn, sListOfJobIds.toString());
                        String instanceData = "OrigRasEventType=" + sRasEventType + ", OrigRasEventId=" + lRasEventId +
                                ", Lctn=" + sRasEventLctn + ", Jobs=" + sListOfJobIds.toString();
                        rasEvents_.logRasEventNoEffectedJob(rasEvents_.getRasEventType("RasMultipleJobsUsingSameNode",
                                0L), instanceData, sRasEventLctn, System.currentTimeMillis() * 1000L,
                                adapter_.getType(), -1L);
                    }
                }
            }   // this is a fully qualified ComputeNode location - it is capable of having a job associated with it.
            // Check & see if this is a fully qualified ServiceNode location.
            else if (nodeInfo_.isServiceNodeLocation(sRasEventLctn)) {
                // this is a ServiceNode lctn - it is not capable of having a job associated with it.
                log_.info("This RAS event indicates that an effected JobID should be filled in, but it is a " +
                        "ServiceNode lctn, it can't have a wlm job running on it - EventType=%s, EventId=%d, " +
                        "Lctn=%s", sRasEventType, lRasEventId, sRasEventLctn);
            } // this is a ServiceNode lctn - it is not capable of having a job associated with it.
            // This is an unexpected type of lctn.
            else {
                // this is an unexpected type of lctn - have not yet implemented how to check if this lctn can be
                // associated with a job.
                log_.error("This RAS event indicates that an effected JobID should be filled in, but it is not a " +
                                "ComputeNode lctn - EventType=%s, EventId=%d, Lctn=%s",
                        sRasEventType, lRasEventId, sRasEventLctn);
                rasEvents_.logRasEventNoEffectedJob(rasEvents_.getRasEventType("RasUnexpectedTypeOfLctnNonCn", 0L)
                        ,("OrigEventType=" + sRasEventType + ", OrigEventId=" + lRasEventId +
                                ", OrigEventLctn=" + sRasEventLctn), sRasEventLctn,
                        System.currentTimeMillis() * 1000L, adapter_.getType(), -1L);
            }   // this is an unexpected type of lctn - have not yet implemented how to check if this lctn can be
            // associated with a job.

            //------------------------------------------------------------------
            // Update the RAS event with the associated JobId information (and also update the DbUpdatedTimestamp
            // field).
            // Note: we need to include the update to the DbUpdatedTimestamp field to ensure that the code that handles
            //       ControlOperations will see this updated Ras event and process the control operations associated
            //       with it.
            //------------------------------------------------------------------
            String sTempStoredProcedure = "RasEventUpdateJobId";
            ClientResponse response1 = client_.callProcedure(sTempStoredProcedure, sFndJobId, sRasEventType,
                    lRasEventId);
            if(response1.getStatus() != ClientResponse.SUCCESS)
                logRasEventOnFailure(sTempStoredProcedure, response1.getStatus());
            // Log a message indicating whether we found a JobId associated with this Ras Event.
            if (sFndJobId != null) {
                // there was a JobId associated with this RasEvent.
                log_.info("Called stored procedure %s - EventType=%s, EventId=%d, Lctn=%s, JobId=%s",
                        sTempStoredProcedure, sRasEventType, lRasEventId, sRasEventLctn, sFndJobId);
            }
            else {
                // there wasn't a JobId associated with this RasEvent.
                log_.info("Called stored procedure %s - EventType=%s, EventId=%d, Lctn=%s, JobId=NULL",
                        sTempStoredProcedure, sRasEventType, lRasEventId, sRasEventLctn);
                log_.info("Note: did not find a JobId associated with this RAS event - EventType=%s, EventId=%d, " +
                        "Lctn=%s", sRasEventType, lRasEventId, sRasEventLctn);
            }
        }   // Loop through each of these RAS events and determine whether or not they have an associated JobId.

        //----------------------------------------------------------------------
        // Periodically cleanup/delete the cached job information once it is no longer needed (we consider a job as
        // "expired" if it has been more than "NumSecsBeforeJobIsExpired" seconds since the job was marked as
        // terminated).
        // Note: Make sure and use the DbUpdatedTimestamp rather than EndTimestamp as we have sometimes seen delays
        //       before the job information appears for us to process!
        //----------------------------------------------------------------------
        final long NumSecsBeforeJobIsExpired = 30L;
        final long NumSecsBetweenChecksForExpiredJobs = 15L;
        if (System.currentTimeMillis() > (lastTimeInMsDidChkForExpiredJobs_ + (NumSecsBetweenChecksForExpiredJobs *
                1000L))) {
            // it has been at least "NumSecsBetweenChecksForExpiredJobs" seconds since the last time we checked for
            // expired jobs.
            // calculate the time to use when removing entries from the InternalCachedJobs table (entries that were
            // marked terminated 30 secs before we began this processing iteration).
            long   lExpirationTs = lTempSaveStartingTsInMs - (NumSecsBeforeJobIsExpired * 1000L);
            Date   dTempDate     = new Date(lExpirationTs);
            String sExpirationTs = sqlDateFormat.format(dTempDate);
            String sTempStoredProcedure = "InternalCachedJobsRemoveExpiredJobs";
            ClientResponse response1 = client_.callProcedure(sTempStoredProcedure, lExpirationTs * 1000L);
            if(response1.getStatus() != ClientResponse.SUCCESS)
                logRasEventOnFailure(sTempStoredProcedure, response1.getStatus());
            log_.info("Called stored procedure %s - ExpirationTs=%s", sTempStoredProcedure, sExpirationTs);
            // Update the timestamp value since we just finished checking.
            lastTimeInMsDidChkForExpiredJobs_ = System.currentTimeMillis();
        }   // Periodically cleanup cached job information.

        return workDone;
    }   // End fillInRasEventsWithMissingJobId(SimpleDateFormat sqlDateFormat)

    private ClientResponse getClientResponseForRasEventListThatNeedJobId(ClientResponse rasEventListThatNeedJobId, String s) {
        ClientResponse response = rasEventListThatNeedJobId;
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.error(s +
                            "AdapterType=%s, ThisAdapterId=%d!",
                    statusByteAsString(response.getStatus()), response.getStatusString(),
                    adapter_.getType(), adapter_.getId());
            throw new RuntimeException(response.getStatusString());
        }
        return response;
    }

    private void logRasEventOnFailure(String procName, byte status) throws IOException {
        rasEvents_.logRasEventNoEffectedJob(
                rasEvents_.getRasEventType("RasGenAdapterMyCallbackForHouseKeepingNoRtrnValueFailed", 0L),
                ("AdapterName=" + adapter_.getName() + ", SpThisIsCallbackFor=" + procName + ", " + "StatusString="
                        + statusByteAsString(status)), null, System.currentTimeMillis() * 1000L,
                adapter_.getType(), -1L);
    }

    //--------------------------------------------------------------------------
    // This method handles the general processing flow for RAS adapters (regardless of specific implementation).
    //--------------------------------------------------------------------------
    void mainProcessingFlow(String[] args) throws AdapterException {
        try {
            log_.info("Starting");

            // Get list of VoltDb servers, location of service node this adapter is running on, and service node's
            // hostname.
            final String SnHostname = adapter_.getHostname();
            final String SnLctn     = adapter_.getLocation();
            log_.info("This adapter instance is running on lctn=%s, hostname=%s, pid=%d", SnLctn, SnHostname,
                    adapter_.getPid());

            // Prefetch system name from voltdb.
            fetchSystemName();

            //-----------------------------------------------------------------
            // Main processing loop
            //-----------------------------------------------------------------
            while(!adapter_.isShuttingDown()) {
                try {
                    //-------------------------------------------------------------
                    // Fill in effected JobId for those RAS events that indicate that JobId should be checked for.
                    // - Note: this method also deletes "expired" jobs from the InternalCachedJobs table.
                    //-------------------------------------------------------------
                    SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS000");
                    sqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // This line cause timestamps formatted
                    // by this SimpleDateFormat to be converted
                    // into UTC time zone
                    long workDone = fillInRasEventsWithMissingJobId(sqlDateFormat);

                    //-------------------------------------------------------------
                    // Handle any RAS events that "have showed up in RasEvent table since last check" AND "have a
                    // non-null ControlOperation".
                    //-------------------------------------------------------------
                    workDone += handleRasEventControlOperations();

                    //-------------------------------------------------------------
                    // Check & see if we should wait for additional "work" before proceeding.
                    //-------------------------------------------------------------
                    // See if we did work during this iteration, if so don't wait for wake up - immediately continue
                    // and check for additional work.
                    if (workDone == 0L)
                        Thread.sleep(100); // 1 tenth second max
                }
                catch (Exception e) {
                    log_.exception(e, "Exception occurred during main processing flow - %s!", e.getMessage());
                    rasEvents_.logRasEventSyncNoEffectedJob(
                            rasEvents_.getRasEventType("RasGenAdapterExceptionButContinue", 0L),
                            "Exception=" + e, null, System.currentTimeMillis() * 1000L, adapter_.getType(),
                            -1L);
                }
            }   // End while loop - handle any work items that have been queued for this type of adapter.

            //-----------------------------------------------------------------
            // Clean up adapter table, base work item, and close connections to db.
            //-----------------------------------------------------------------
            operations_.shutdownAdapter();
        }   // End try
        catch (Exception e) {
            operations_.shutdownAdapter(e);
        }
    }   // End mainProcessingFlow(String[] args)

    private static String statusByteAsString(byte status) {
        return statusMap_.getOrDefault(status, Byte.toString(status));
    }

    public static void main(String[] args) throws AdapterException {
        Logger logger = LoggerFactory.getInstance("RAS", AdapterRasForeignBus.class.getName(), "console");
        if(logger == null) throw new RuntimeException("Failed to create the required logger");

        final String servers  = (args.length >= 1) ? args[0] : "localhost";
        final String hostname = (args.length >= 3) ? args[2] : "UnknownHostName";
        final String location = (args.length >= 2) ? args[1] : "UnknownLctn";
        final AdapterInformation adapter = new AdapterInformation("RAS", AdapterRasForeignBus.class.getName(), location, hostname);
        final AdapterRasForeignBus obj = new AdapterRasForeignBus(adapter, logger, new DataStoreFactoryImpl(servers, logger));
        // Start up the main processing flow for RAS adapters.
        obj.mainProcessingFlow(args);
    }   // End main(String[] args)

    private String machineName_ = "Undefined Machine Name";

    private static final Map<Byte,String> statusMap_ = new HashMap<>() {{
        put(ClientResponse.USER_ABORT, "USER_ABORT");
        put(ClientResponse.CONNECTION_LOST, "CONNECTION_LOST");
        put(ClientResponse.CONNECTION_TIMEOUT, "CONNECTION_TIMEOUT");
        put(ClientResponse.GRACEFUL_FAILURE, "GRACEFUL_FAILURE");
        put(ClientResponse.RESPONSE_UNKNOWN, "RESPONSE_UNKNOWN");
        put(ClientResponse.UNEXPECTED_FAILURE, "UNEXPECTED_FAILURE");
        put(ClientResponse.SUCCESS, "SUCCESS");
    }};

    private static final List<String> errorOperations_ = new ArrayList<>() {{
        add("ErrorOnNode");
        add("ErrorAndKillJobOnNode");
        add("ErrorAndPwrOffNode");
        add("ErrorAndKillJobAndPwrOffNode");
        add("ErrorAndShutdownNode");
        add("ErrorAndKillJobAndShutdownNode");
        add("ErrorAndPowerCycleNode");
        add("ErrorAndResetNode");
    }};
}   // End class AdapterRas
