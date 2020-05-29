// Copyright (C) 2017-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.dai.ras;

import com.intel.dai.AdapterSingletonFactory;
import com.intel.dai.IAdapter;
import com.intel.dai.dsapi.WorkQueue;
import com.intel.logging.*;
import com.intel.perflogging.BenchmarkHelper;
import org.voltdb.client.*;
import org.voltdb.VoltTable;
import org.voltdb.VoltTableRow;
import com.intel.dai.Adapter;
import java.util.regex.Pattern;
import com.intel.dai.exceptions.AdapterException;


import java.io.*;
import java.lang.*;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * AdapterRas for the VoltDB database.
 *
 * Parms:
 *  List of the db node names, so that this client connects to each of them (this is a comma separated list of hostnames or IP addresses.
 *  E.g., voltdbserver1,voltdbserver2,10.11.12.13
 *
 * Example invocation:
 *      java AdapterRas voltdbserver1,voltdbserver2,10.11.12.13  (or java AdapterRas  - this will default to using localhost)
 *
 * SELECT RasEvent.EventType, RasEvent.LastChgTimestamp, RasMetaData.Severity, RasEvent.Lctn, RasEvent.ControlOperation, RasMetaData.Component, RasMetaData.Msg, RasEvent.InstanceData FROM RasEvent INNER JOIN RasMetaData on RasEvent.EventType=RasMetaData.EventType Order By LastChgTimestamp;
 * SELECT RasEvent.Id, RasEvent.EventType, RasEvent.LastChgTimestamp, RasEvent.ControlOperation, RasMetaData.Severity, RasMetaData.Category, RasMetaData.Component, RasMetaData.Msg, RasEvent.InstanceData FROM RasEvent INNER JOIN RasMetaData on RasEvent.EventType=RasMetaData.EventType WHERE RasEvent.EventType = 'RasWlmProvisionHookError';
 */
public class AdapterRasForeignBus {
    // Constructor
    AdapterRasForeignBus(IAdapter adapter, Logger logger) {
        log_ = logger;
        adapter_ = adapter;
        lastTimeInMsDidChkForExpiredJobs_ = System.currentTimeMillis();
        mCachedJobInfoMap = null;
    }   // ctor

    // Member Data
    private IAdapter    adapter_;
    private WorkQueue   workQueue_;
    private Logger      log_;
    private long        lastTimeInMsDidChkForExpiredJobs_;  // the last time (in milliseconds) that we checked for expired jobs.
    private HashMap<String, ArrayList<VoltTableRow>> mCachedJobInfoMap = null;
    private BenchmarkHelper benchmarking_;

    // Use a stored procedure to get this RAS events associated job id (if any).
    private String getThisEventsJobIdFromCachedJobInfo(String sRasEventDescrName, String sRasEventLctn,
                                                       long lRasEventId, long lRasEventTsInMicroSecs, HashMap<String,
                                                       ArrayList<VoltTableRow>> mCachedJobInfoMap) {
        String sFndJobId = null;
        // Get the ArrayList of volt table rows for this lctn's cached job information.
        ArrayList<VoltTableRow> alVoltTableRow = mCachedJobInfoMap.get(sRasEventLctn);
        if ((alVoltTableRow == null) || (alVoltTableRow.isEmpty())) {
            // there wasn't any cached job information for this lctn.
            log_.info("getThisEventsJobIdFromCachedJobInfo - did NOT find %s in the cached job info", sRasEventLctn);
        }
        else {
            // we have the array list of entries for this lctn - loop through checking for the associated job id.
            // Step through the entries for this lctn, in the cached job info, looking for a matching job id.
            for (VoltTableRow vtRow : alVoltTableRow) {
                // Check & see if this job was started before this ras event occurred.
                if (vtRow.getTimestampAsLong("StartTimestamp") <= lRasEventTsInMicroSecs) {
                    // this job's start timestamp was before this ras event occurred.
                    // Check & see if this job was still active when this ras event occurred.
                    long lCachedJobsEndTs = vtRow.getTimestampAsLong("EndTimestamp");
                    if ((vtRow.wasNull()) || (lCachedJobsEndTs >= lRasEventTsInMicroSecs)) {
                        // found the job - this job was still active when this ras event occurred.
                        if (sFndJobId != null) {
                            // we already found a job id for this RAS event
                            // Problem occurred there should not be 2 jobs active on the same node at same time.
                            String sInstanceData = "OrigRasEventDescrName=" + sRasEventDescrName + ", OrigRasEventId=" + lRasEventId + ", Lctn=" + sRasEventLctn +
                                                   ", 1stJobId=" + sFndJobId + ", 2ndJobId=" + vtRow.getString("JobId");
                            log_.error("Detected that there were multiple jobs using the same ComputeNode - " + sInstanceData);
                            adapter_.logRasEventNoEffectedJob("RasMultipleJobsUsingSameNode"
                                                             ,sInstanceData
                                                             ,sRasEventLctn
                                                             ,System.currentTimeMillis() * 1000L
                                                             ,adapter_.adapterType()
                                                             ,workQueue_.baseWorkItemId()
                                                             );
                        }
                        else {
                            // Found the job id, save it away.
                            sFndJobId = vtRow.getString("JobId");
                        }
                    }
                }
            }
        }
        return sFndJobId;
    }   // End getThisEventsJobIdFromCachedJobInfo(String sRasEventDescrName, String sRasEventLctn, long lRasEventId, long lRasEventTsInMicroSecs, HashMap<String, ArrayList<VoltTableRow>> mCachedJobInfoMap)


    // Use a stored procedure to get this RAS events associated job id (if any).
    private String getThisEventsJobIdFromVolt(String sRasEventDescrName, String sRasEventLctn, long lRasEventId, long lRasEventTsInMicroSecs) throws AdapterException {
        String sFndJobId = null;
        try {
            ClientResponse response = adapter_.client().callProcedure("InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp", sRasEventLctn, lRasEventTsInMicroSecs, lRasEventTsInMicroSecs);
            if (response.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                log_.error("Stored procedure InternalCachedJobsGetJobidForNodeLctnAndMatchingTimestamp FAILED - Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                           IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter_.adapterType(), adapter_.adapterId());
                throw new RuntimeException(response.getStatusString());
            }
            VoltTable vtLctnsListOfJobs = response.getResults()[0];
            // Ensure that there was <= 1 matching JobId.
            if (vtLctnsListOfJobs.getRowCount() == 1) {
                // exactly 1 job id was returned.
                // Save the matching JobId information.
                vtLctnsListOfJobs.advanceRow();
                sFndJobId = vtLctnsListOfJobs.getString("JobId");
            }
            else {
                // Ensure that we got at most 1 JobId returned - that we did not see 2 or more jobs using the same ComputeNode
                // (when this error is detected, log all relevant information, and then continue using the first job id).
                if (vtLctnsListOfJobs.getRowCount() > 1) {
                    StringBuilder sListOfJobIds = new StringBuilder();
                    for (int iJobCntr = 0; iJobCntr < vtLctnsListOfJobs.getRowCount(); ++iJobCntr) {
                        vtLctnsListOfJobs.advanceRow();
                        if (sFndJobId == null)
                            sFndJobId = vtLctnsListOfJobs.getString("JobId");  // use the first job id that was found.
                        // Get this row's data.
                        sListOfJobIds.append(vtLctnsListOfJobs.getString("JobId")).append(",");
                    }
                    sListOfJobIds.deleteCharAt((sListOfJobIds.length()-1));  // delete trailing comma.
                    log_.error("Detected that there were multiple jobs using the same ComputeNode - OrigRasEventDescrName=%s, OrigRasEventId=%d, Lctn=%s, Jobs=%s!",
                               sRasEventDescrName, lRasEventId, sRasEventLctn, sListOfJobIds.toString());
                    adapter_.logRasEventNoEffectedJob("RasMultipleJobsUsingSameNode"
                                                     ,("OrigRasEventDescrName=" + sRasEventDescrName + ", OrigRasEventId=" + lRasEventId + ", Lctn=" + sRasEventLctn + ", Jobs=" + sListOfJobIds.toString())
                                                     ,sRasEventLctn                      // Lctn associated with this ras event
                                                     ,System.currentTimeMillis() * 1000L // Current time, in micro-seconds since epoch
                                                     ,adapter_.adapterType()             // type of adapter_ that is generating this ras event
                                                     ,workQueue_.baseWorkItemId()        // work item that is being worked on that resulted in the generation of this ras event
                                                     );
                }
            }
        }
        catch (Exception ex) {
            throw new AdapterException("Unable to get this RAS events JobId from Volt", ex);
        }

        return sFndJobId;
    }   // End getThisEventsJobIdFromVolt(String sRasEventDescrName, String sRasEventLctn, long lRasEventId, long lRasEventTsInMicroSecs, VoltTable vtCachedJobInfo)



    private long handleRasEventControlOperations(SimpleDateFormat sqlDateFormat) throws IOException, ProcCallException, InterruptedException
    {
        //----------------------------------------------------------------------
        // Get a list of RAS events that need to have a Control Operation run.
        //----------------------------------------------------------------------
        ClientResponse response = adapter_.client().callProcedure("RasEventProcessNewControlOperations");
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.error("Stored procedure RasEventProcessNewControlOperations FAILED - Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                       IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter_.adapterType(), adapter_.adapterId());
            throw new RuntimeException(response.getStatusString());
        }
        VoltTable vtEvents = response.getResults()[0];  // List of RasEvents which need their control operations run.


        if (vtEvents.getRowCount() > 0)
            log_.info("Found %d RAS events with control operations to run", vtEvents.getRowCount());


        //----------------------------------------------------------------------
        // Loop through each of the pertinent RAS events invoking the appropriate Control Operation.
        //----------------------------------------------------------------------
        for (int iRasEventCntr = 0; iRasEventCntr < vtEvents.getRowCount(); ++iRasEventCntr) {
            vtEvents.advanceRow();
            // Determine the appropriate action to take for this RAS event, based on ControlOperation and any pertinent policy that may be in effect.
            String sTempControlOperation = vtEvents.getString("ControlOperation");
            String sTempLctn             = vtEvents.getString("Lctn");
            String sTempJobId            = vtEvents.getString("JobId");
            String sTempDescrName        = vtEvents.getString("EventType");
            long   lTempEventId          = vtEvents.getLong("Id");
            String sTempEventId          = Long.toString(lTempEventId);
            long   lLastChgTsInMicroSecs = vtEvents.getTimestampAsLong("LastChgTimestamp");  // last chg timestamp for this RAS event in microseconds.

            // Do not run any ControlOperation if the RAS event does not have a lctn string.
            if (sTempLctn == null) {
                // the specified RAS event's lctn is null, so no don't try to run any control operation.
                log_.warn("Did NOT run this RAS event's ControlOperation because the event has no specified lctn, " +
                                "ignoring this ControlOperation - DescrName=%s, EventId=%s, Lctn=null, JobId=%s, " +
                                "ControlOperation=%s!",
                          sTempDescrName, sTempEventId, sTempJobId, sTempControlOperation);
                // Explicitly set the boolean indicating that we are done with this event's control operation.
                // Note: yes I know that the boolean is already set to true, but I want it explicitly set here so it is never accidentally removed from this specific flow...
            }   // the specified RAS event's lctn is null, so no don't try to run any control operation.

            // Run the specified ControlOperation.
            else {
                log_.info("Processing a RAS event's control operation - %s - DescrName=%s, EventId=%s, Lctn=%s, JobId=%s",
                          sTempControlOperation, sTempDescrName, sTempEventId, sTempLctn, sTempJobId);
                switch(sTempControlOperation) {
                    case "ErrorOnNode":
                    case "ErrorAndKillJobOnNode":
                    case "ErrorAndPwrOffNode":
                    case "ErrorAndKillJobAndPwrOffNode":
                    case "ErrorAndShutdownNode":
                    case "ErrorAndKillJobAndShutdownNode":
                    case "NodeIsPoweredOff":
                    case "ErrorAndPowerCycleNode":
                    case "ErrorAndResetNode":
                        // Mark this node as being in Error state.
                        adapter_.markNodeInErrorState(sTempLctn, sTempDescrName, lTempEventId, sTempControlOperation, sTempJobId, adapter_.adapterType(), workQueue_.baseWorkItemId());
                        break;
                    case "IncreaseFanSpeed":
                    case "KillJobOnNode":
                        // IncreaseFanSpeed: Increase this node's fan speed.
                        // KillJobOnNode: This control action is intended for events that are job fatal but do not
                        //                indicate that the node is fundamentally unhealthy.  E.g., running out of
                        //                LWK memory.
                        break;
                    case "TestControlOperation":
                        log_.info("*** ========================================================================================================");
                        log_.info("*** TEST CONTROL OPERATION: Lctn='%s'; Event='%s'; JobId='%s'; LastChangeTime=%dus",
                                sTempLctn, sTempDescrName, sTempJobId, lLastChgTsInMicroSecs);
                        log_.info("*** ========================================================================================================");
                        break;
                    default:
                        log_.error("Detected an unexpected control operation for this RAS Event, this control operation has NOT YET BEEN IMPLEMENTED - " +
                                   "DescrName=%s, EventId=%s, ControlOperation=%s!!!", sTempDescrName, sTempEventId, sTempControlOperation);
                        // Indicate that an internal error occurred in this RAS adapter.
                        adapter_.logRasEventNoEffectedJob("RasMissingControlOperationLogic"
                                                         ,("DescrName=" + sTempDescrName + ", ID=" + sTempEventId + ", ControlOperation=" + sTempControlOperation)
                                                         ,null                                // Lctn associated with this ras event
                                                         ,System.currentTimeMillis() * 1000L  // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,adapter_.adapterType()              // type of adapter_ that is generating this ras event
                                                         ,workQueue_.baseWorkItemId()         // work item that is being worked on that resulted in the generation of this ras event
                                                         );
                        // Explicitly set the boolean indicating that we are done with this event's control operation.
                        // Note: yes I know that the boolean is already set to true, but I want it explicitly set here so it is never accidentally removed from this specific flow...
                        break;
                }
            }   // run the specified ControlOperation.

            // Record that we are done with this RAS event's control operation (so that we don't try and run its control event later).
            //------------------------------------------------------------------
            // Update this ras event to indicate that we have executed its control operation (also need to update the DbUpdatedTimestamp field).
            //------------------------------------------------------------------
            String sPertinentInfo = "DescrName=" + sTempDescrName + ",EventId=" + sTempEventId + ",ControlOperation=" + sTempControlOperation;
            String sTempStoredProcedure = "RasEventUpdateControlOperationDone";
            adapter_.client().callProcedure(adapter_.createHouseKeepingCallbackNoRtrnValue(adapter_.adapterType(), adapter_.adapterName(), sTempStoredProcedure, sPertinentInfo, workQueue_.baseWorkItemId()) // asynchronously invoke the procedure
                                           ,sTempStoredProcedure  // stored procedure name
                                           ,"Y"                   // store Y in the ControlOperationDone field (indicates we have executed the control operation)
                                           ,sTempDescrName        // ras event type
                                           ,sTempEventId          // ras event id
                                           );
            benchmarking_.addNamedValue("ControlOperation", 1);
        }   // Loop through each of the pertinent RAS events invoking the appropriate Control Operation.
        if (vtEvents.getRowCount() > 0)
            log_.info("Finished %d RAS events with control operations to run", vtEvents.getRowCount());


        //----------------------------------------------------------------------
        // Periodically cleanup/delete the cached job information once it is no longer needed (we consider a job as "expired" if it has been more than "NumSecsBeforeJobIsExpired" seconds since the job was marked as terminated).
        // Note: Make sure and use the DbUpdatedTimestamp rather than EndTimestamp as we have sometimes seen delays before the job information appears for us to process!
        //----------------------------------------------------------------------
        long NumSecsBeforeJobIsExpired = 30L;
        long NumSecsBetweenChecksForExpiredJobs = 15L;
        if (System.currentTimeMillis() > (lastTimeInMsDidChkForExpiredJobs_ + (NumSecsBetweenChecksForExpiredJobs * 1000L))) {
            // it has been at least "NumSecsBetweenChecksForExpiredJobs" seconds since the last time we checked for expired jobs.
            long   lExpirationTs = System.currentTimeMillis() - (NumSecsBeforeJobIsExpired * 1000L);  // calculate the time to use when removing entries from the InternalCachedJobs table (entries that were marked terminated 30 secs before we began this processing iteration).
            Date   dTempDate     = new Date(lExpirationTs);
            String sExpirationTs = sqlDateFormat.format(dTempDate);
            String sTempStoredProcedure = "InternalCachedJobsRemoveExpiredJobs";
            adapter_.client().callProcedure(adapter_.createHouseKeepingCallbackNoRtrnValue(adapter_.adapterType(), adapter_.adapterName(), sTempStoredProcedure, sExpirationTs, workQueue_.workItemId())  // asynchronously invoke the procedure
                                           ,sTempStoredProcedure  // stored procedure name
                                           ,lExpirationTs * 1000L // Expiration time in micro-seconds since epoch
                                           );
            log_.info("Called stored procedure %s - ExpirationTs=%s", sTempStoredProcedure, sExpirationTs);
            // Update the timestamp value since we just finished checking.
            lastTimeInMsDidChkForExpiredJobs_ = System.currentTimeMillis();
        }   // Periodically cleanup cached job information.


        //--------------------------------------------------------------
        // Save restart data indicating the current timestamp.
        //--------------------------------------------------------------
        if (vtEvents.getRowCount() > 0) {
            String sRestartData = "Handled " + vtEvents.getRowCount() + " ras events with Control Operations, " + sqlDateFormat.format(new Date());
            workQueue_.saveWorkItemsRestartData(workQueue_.workItemId(), sRestartData, false);  // false means to update this workitem's history record rather than doing an insert of another history record - this is "unusual" (only used when a workitem is updating its working results fields very often)
        }

        // Return an indication of whether or not there was any work to do this iteration.
        return vtEvents.getRowCount();
    }   // End handleRasEventControlOperations(SimpleDateFormat sqlDateFormat)


    //--------------------------------------------------------------------------
    // Fill in effected JobId for those RAS events that indicate that JobId should be checked for.
    // - Note: this method also deletes "expired" jobs from the InternalCachedJobs table.
    //--------------------------------------------------------------------------
    private long fillInRasEventsWithMissingJobId(SimpleDateFormat sqlDateFormat) throws IOException, ProcCallException, InterruptedException, AdapterException {
        ClientResponse response;
        //----------------------------------------------------------------------
        // Ensure that the CachedJobInfo map (w/ entry for each possible ComputeNode Lctn) is setup.
        //----------------------------------------------------------------------
        if (mCachedJobInfoMap == null) {
            // Create an entry in the hash map for each compute node lctn defined in this machine.
            //  log_.debug("Creating HashMap of ArrayList<VoltTableRow> ");
            // Get the list of ComputeNodes defined in the system.
            response = adapter_.client().callProcedure("ComputeNodeListLctnAndSeqNum");
            VoltTable vt = response.getResults()[0];
            // Create the Map object (of the size equal to number of compute nodes).
            mCachedJobInfoMap = new HashMap<String, ArrayList<VoltTableRow>>(vt.getRowCount());
            // Loop through each of the ComputeNodes defined in the ComputeNode table
            // (initializing the hashmap entries).
            for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
                vt.advanceRow();
                // Create the array list for this lctn w/ room for 3 entries.
                ArrayList<VoltTableRow> alVt = new ArrayList<VoltTableRow>(3);
                // Create the HashMap entry for this lctn and associating it with its ArrayList.
                mCachedJobInfoMap.put(vt.getString("Lctn"), alVt);
            }
            //  log_.debug("Created  HashMap of ArrayList<VoltTableRow> - mCachedJobInfoMap.size() = %d", mCachedJobInfoMap.size());
        }

        //----------------------------------------------------------------------
        // Get a list of RAS events that need to have a JobId filled in (or said differently to check and see if they want us to check and see if there is a JobId to fill in for it).
        //  Note: we don't want to use the current time for building this list of RAS events, rather we want to process events that are at least 4 seconds old.
        //        This is to give the job info time enough to propagate from log files into the InternalCachedJobs table (just in case that RAS event shows up before the Job info percolates through).
        //        NOTE2: There is nothing scientific about the number 4 seconds, it was chosen simple because it seemed reasonable at the time.
        //----------------------------------------------------------------------
        long lTempSaveStartingTsInMs = System.currentTimeMillis();  // save the time that we began this iteration of filling in JobIds.
        long lTempDelayedTsInMs = lTempSaveStartingTsInMs - (4 * 1000L);  // calculate the delayed timestamp, current time minus 4 seconds (see note above).
        response = adapter_.client().callProcedure("RasEventListThatNeedJobId", (lTempDelayedTsInMs * 1000L));
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.error("Stored procedure RasEventListThatNeedJobId FAILED - Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                       IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter_.adapterType(), adapter_.adapterId());
            throw new RuntimeException(response.getStatusString());
        }
        VoltTable vtRasEventList = response.getResults()[0];  // List of RasEvents which specified they want UCS to check for an associated job id.
        VoltTable vtRasEventListMaxTs = response.getResults()[1];  // Maximum value for LastChgTimestamp in the above list.

        //----------------------------------------------------------------------
        // Get the current "cached job info" out of the db (if appropriate).
        //----------------------------------------------------------------------
        boolean bUsingTheCachedJobInfoMap;  // flag indicating whether or not the map of cached job info is available to be used.
        int iNumEntriesInCachedJobInfoMap = 0;
        if (vtRasEventList.getRowCount() <= 3) {
            // only a couple of RAS events need job id filled in - just use direct call to volt stored procedure for each of them.
            bUsingTheCachedJobInfoMap = false;  // set flag to indicate that we are not setting up the map of cached job info.
            if (vtRasEventList.getRowCount() > 0)
                log_.info("Found %d RAS events that need job id filled in - will call individual stored proc for each of them", vtRasEventList.getRowCount());
        }
        else {
            // there are a bunch of RAS events that need their job id filled in - get the cached job info (because we don't want to call volt for each individual RAS event).
            // Get the cached job info.
            vtRasEventListMaxTs.advanceRow();  // get the maximum LastChgTimestamp value within the list of RAS events.
            response = adapter_.client().callProcedure("InternalCachedJobsGetListOfActiveInternalCachedJobsUsingTimestamp", vtRasEventListMaxTs.getTimestampAsTimestamp(0), vtRasEventListMaxTs.getTimestampAsTimestamp(0));
            VoltTable vtCachedJobInfo = response.getResults()[0];
            if (vtCachedJobInfo.getRowCount() > 0)
                log_.info("Found %d RAS events that need job id filled in - will use VoltTable with cached job info", vtRasEventList.getRowCount());
            else
                log_.info("Found %d RAS events that need job id filled in BUT there is currently no job info - so these events will NOT have any job ids filled in", vtRasEventList.getRowCount());
            // Spin through putting the cached job info into the map.
            for (int i=0; i < vtCachedJobInfo.getRowCount(); ++i) {
                vtCachedJobInfo.advanceRow();
                // Get the cached job info array list for this lctn out of the map.
                ArrayList<VoltTableRow> alVoltTableRow = mCachedJobInfoMap.get(vtCachedJobInfo.getString("NodeLctn"));
                if (alVoltTableRow != null) {
                    // Add this cached job info entry into the array list.
                    alVoltTableRow.add( vtCachedJobInfo.cloneRow() );
                    ++iNumEntriesInCachedJobInfoMap;  // bump the number of entries in the cached map.
                }
            }
            // Set flag indicating that the map of cached job info is available to be used.
            bUsingTheCachedJobInfoMap = true;
        }

        //----------------------------------------------------------------------
        // Loop through each of the RAS events in the list and determine whether or not they have an associated JobId.
        //----------------------------------------------------------------------
        for (int iRasEventCntr = 0; iRasEventCntr < vtRasEventList.getRowCount(); ++iRasEventCntr) {
            vtRasEventList.advanceRow();
            String  sRasEventDescrName = vtRasEventList.getString("EventType");
            long    lRasEventId   = vtRasEventList.getLong("Id");
            String  sRasEventLctn = vtRasEventList.getString("Lctn");
            long    lRasEventTsInMicroSecs = vtRasEventList.getTimestampAsLong("LastChgTimestamp");  // get a long representation of the ras event's timestamp, units of microseconds since epoch (GMT timezone).

            //------------------------------------------------------------------
            // Find the appropriate job id for this RAS event.
            //------------------------------------------------------------------
            String  sFndJobId = null;

            // See if we are using the map of cached job info AND there are no entries in the map.
            if ((bUsingTheCachedJobInfoMap) && (iNumEntriesInCachedJobInfoMap == 0)) {
                // short-circuit - we are using the cached job info map, but there are no entries in it - since there are no jobs there can not be a job id associated with this event.
                log_.debug("RAS event asked us to fill in an associated job id, but there is no pertinent job info, so this event can't have an associated job id - EventDescrName=%s, EventId=%d, Lctn=%s",
                           sRasEventDescrName, lRasEventId, sRasEventLctn);
            }

            // See if this is a fully qualified ComputeNode location.
            else if (adapter_.isComputeNodeLctn(sRasEventLctn)) {
                // this is a fully qualified ComputeNode location - it IS capable of having a job associated with it.
                //--------------------------------------------------------------
                // Get the JobId (if any) that was running on the specified lctn at the specified time.
                //--------------------------------------------------------------
                // Check & see if user gave us a map of the Cached Job Info entries OR if we need to call a stored procedure to get this information.
                if (bUsingTheCachedJobInfoMap) {
                    // there is cached job information - use it for determining the associated job id.
                    // Use the provided Cached Job Information to get this RAS event's associated job id (if any).
                    sFndJobId = getThisEventsJobIdFromCachedJobInfo(sRasEventDescrName, sRasEventLctn, lRasEventId, lRasEventTsInMicroSecs, mCachedJobInfoMap);
                }   // there is cached job information - use it for determining the associated job id.
                else {
                    // there is no cached job information - call the stored procedure to get this lctn's associated job id.
                    // Use a stored procedure to get this RAS event's associated job id (if any).
                    sFndJobId = getThisEventsJobIdFromVolt(sRasEventDescrName, sRasEventLctn, lRasEventId, lRasEventTsInMicroSecs);
                }   // there is no cached job information - call the stored procedure to get this lctn's associated job id.
            }   // this is a fully qualified ComputeNode location - it is capable of having a job associated with it.

            // See if this is a fully qualified ServiceNode location
            // (no reason to check if we already know that this lctn can not have an associated job id).
            else if (adapter_.isServiceNodeLctn(sRasEventLctn)) {
                // short-circuit - this is a ServiceNode, it is not capable of having an associated job id.
                log_.info("RAS event asked us to fill in an associated job id, but this is a ServiceNode lctn, it can't have a job id - EventDescrName=%s, EventId=%d, Lctn=%s",
                          sRasEventDescrName, lRasEventId, sRasEventLctn);
            }

            // See if this is an unexpected type of lctn
            // (we do not know how to tell if this lctn has an associated job id).
            else {
                // this is an unexpected type of lctn - have not yet implemented how to check if this lctn has an associated job id.
                log_.error("RAS event asked us to fill in an associated job id, but it is not a ComputeNode lctn, it can't have a job id - EventDescrName=%s, EventId=%d, Lctn=%s",
                           sRasEventDescrName, lRasEventId, sRasEventLctn);
                adapter_.logRasEventNoEffectedJob("RasUnexpectedTypeOfLctnNonCn"
                                                 ,("OrigEventDescrName=" + sRasEventDescrName + ", OrigEventId=" + lRasEventId + ", OrigEventLctn=" + sRasEventLctn)
                                                 ,sRasEventLctn                      // Lctn associated with this ras event
                                                 ,System.currentTimeMillis() * 1000L // Current time, in micro-seconds since epoch
                                                 ,adapter_.adapterType()             // type of adapter_ that is generating this ras event
                                                 ,workQueue_.baseWorkItemId()        // work item that is being worked on that resulted in the generation of this ras event
                                                 );
            }   // this is an unexpected type of lctn - have not yet implemented how to check if this lctn can be associated with a job.

            //------------------------------------------------------------------
            // Update the RAS event with the associated JobId information (also update the DbUpdatedTimestamp field).
            // Note: we need to include the update to the DbUpdatedTimestamp field to ensure that the code that handles ControlOperations will see this updated Ras event and process the control operations associated with it.
            //------------------------------------------------------------------
            String sPertinentInfo = "EventDescrName=" + sRasEventDescrName + ",EventId=" + lRasEventId + ",EventLctn=" + sRasEventLctn + ",JobId=" + sFndJobId;
            String sTempStoredProcedure = "RasEventUpdateJobId";
            adapter_.client().callProcedure(adapter_.createHouseKeepingCallbackNoRtrnValue(adapter_.adapterType(), adapter_.adapterName(), sTempStoredProcedure, sPertinentInfo, workQueue_.baseWorkItemId()) // asynchronously invoke the procedure
                                           ,sTempStoredProcedure // stored procedure name
                                           ,sFndJobId            // the new JobId we found for this ras event
                                           ,sRasEventDescrName   // ras event type
                                           ,lRasEventId          // ras event id
                                           );
            benchmarking_.addNamedValue("JobIdAssociation", 1);
            log_.info("Called stored procedure %s - EventDescrName=%s, EventId=%d, Lctn=%s, JobId=%s",
                      sTempStoredProcedure, sRasEventDescrName, lRasEventId, sRasEventLctn, sFndJobId);
        }   // Loop through each of these RAS events and determine whether or not they have an associated JobId.
        if (vtRasEventList.getRowCount() > 0)
            log_.info("Finished %d RAS events that needed job id filled in", vtRasEventList.getRowCount());


        //----------------------------------------------------------------------
        // Clean up the map of cached job info (since we are done with this iteration's data).
        //----------------------------------------------------------------------
        if (bUsingTheCachedJobInfoMap) {
            // we did populate the map so we need to clean it up.
            log_.debug("Cleaning up the HashMap of ArrayList<VoltTableRow> ");
            // Remove any entries from all of this map's ArrayLists.
            mCachedJobInfoMap.forEach((key,value)->{
                //log_.info("Clearing %s ArrayList in the Cached Job Info map", key);
                value.clear();
            });
            log_.debug("Cleaned  up the HashMap of ArrayList<VoltTableRow> ");
        }


        //--------------------------------------------------------------
        // Save restart data indicating the current timestamp.
        //--------------------------------------------------------------
        if (vtRasEventList.getRowCount() > 0) {
            String sRestartData = "Filled job ids into " + vtRasEventList.getRowCount() + " RAS events, " + sqlDateFormat.format(new Date());
            workQueue_.saveWorkItemsRestartData(workQueue_.workItemId(), sRestartData, false);  // false means to update this workitem's history record rather than doing an insert of another history record - this is "unusual" (only used when a workitem is updating its working results fields very often)
        }

        // Return an indication of whether or not there was any work done this iteration.
        return vtRasEventList.getRowCount();
    }   // End fillInRasEventsWithMissingJobId(SimpleDateFormat sqlDateFormat)


    //---------------------------------------------------------
    // Handles all the processing that needs to be done in order to fill in job ids AND handle RAS event control operations.
    // Note: This work item is different than most in that this one work item will run for the length of time that the system is active.
    //       It does not start and stop, it starts and stays active.
    //---------------------------------------------------------
    public long handleFillingInJobIdsAndControlOps() throws InterruptedException, IOException, ProcCallException
    {
        log_.info("handleFillingInJobIdsAndControlOps - starting");
        SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS000");
        sqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // This line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
        long lNumIterationsWithoutWork = 0L;  // counter of number of iterations without having any work to do.
        while(!adapter_.adapterShuttingDown()) {
            try {
                //-------------------------------------------------------------
                // Fill in effected JobId for those RAS events that indicate that JobId should be checked for.
                //-------------------------------------------------------------
                long lWorkToDoThisIteration = fillInRasEventsWithMissingJobId(sqlDateFormat);

                //-------------------------------------------------------------
                // Handle any RAS events that have a non-null control operation that has not yet been executed
                // (the above checks are ControlOperation is not null AND ControlOperationDone = 'N' - respectively).
                // - Note: this method also deletes "expired" jobs from the InternalCachedJobs table.
                //-------------------------------------------------------------
                lWorkToDoThisIteration += handleRasEventControlOperations(sqlDateFormat);

                //-------------------------------------------------------------
                // Check & see if there was any work to do this iteration.
                //-------------------------------------------------------------
                // ToDo: Wait for indicator to come in over scon waking us up (the wake up indicates that either a work item was queued for this adapter or that there have been ras events added in the RasEvent table).
                //       Want to have a timeout on that scon wait, as we definitely want to overtly check for work every so often regardless (in case the scon "wake up" had somehow been lost)!
                if (lWorkToDoThisIteration > 0) {
                    // we did do work this iteration.
                    log_.info("Did work this iteration, so immediately checking for more work");
                    lNumIterationsWithoutWork = 0L;  // reset the counter of number of iterations without having any work to do.
                }
                else {
                    // we did NOT do any work this iteration.
                    ++lNumIterationsWithoutWork;  // bump the number of iterations without having any work to do.
                    Thread.sleep( Math.min(lNumIterationsWithoutWork, 5) * 100);
                    benchmarking_.tick();
                }
            }   // End try
            catch (NoConnectionsException nce) {
                log_.error("NoConnectionsException exception occurred during handleFillingInJobIdsAndControlOps, RAS event can NOT be logged, pausing for 10 seconds!");
                try { Thread.sleep(10 * 1000L); } catch (Exception e) {/* */}  // wait 10 seconds to give it a chance for reconnection to db.
            }
            catch (Exception e) {
                log_.error("Exception occurred during handleFillingInJobIdsAndControlOps - will catch and then continue processing!");
                log_.error("%s", Adapter.stackTraceToString(e));
                try {
                    adapter_.logRasEventSyncNoEffectedJob("RasGenAdapterExceptionButContinue" // using synchronous version as we are in a flow where we want to ensure that this occurs in a timely manner.
                                                         ,("Exception=" + e)                  // instance data
                                                         ,null                                // Lctn
                                                         ,System.currentTimeMillis() * 1000L  // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,adapter_.adapterType()              // type of adapter that is requesting this
                                                         ,workQueue_.baseWorkItemId()         // requesting work item
                                                         );
                }
                catch (Exception e2) {/* */}
            }
        }   // End while loop
        return -99999;
    }   // End handleFillingInJobIdsAndControlOps()



    //---------------------------------------------------------
    // Handles all the processing that needs to be done in order to ONLY fill in job ids.
    // Note: This work item is different than most in that this one work item will run for the length of time that the system is active.
    //       It does not start and stop, it starts and stays active.
    //---------------------------------------------------------
    public long handleFillingInJobIdOnly() throws InterruptedException, IOException, ProcCallException
    {
        log_.info("handleFillingInJobIdOnly - starting");
        SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS000");
        sqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // This line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
        long lNumIterationsWithoutWork = 0L;  // counter of number of iterations without having any work to do.
        while(!adapter_.adapterShuttingDown()) {
            try {
                //-------------------------------------------------------------
                // Fill in effected JobId for those RAS events that indicate that JobId should be checked for.
                // - Note: this method also deletes "expired" jobs from the InternalCachedJobs table.
                //-------------------------------------------------------------
                long lWorkToDoThisIteration = fillInRasEventsWithMissingJobId(sqlDateFormat);
                //-------------------------------------------------------------
                // Check & see if there was any work to do this iteration.
                //-------------------------------------------------------------
                // ToDo: Wait for indicator to come in over scon waking us up (the wake up indicates that either a work item was queued for this adapter or that there have been ras events added in the RasEvent table).
                //       Want to have a timeout on that scon wait, as we definitely want to overtly check for work every so often regardless (in case the scon "wake up" had somehow been lost)!
                if (lWorkToDoThisIteration > 0) {
                    // we did do work this iteration.
                    log_.info("Did work this iteration, so immediately checking for more work");
                    lNumIterationsWithoutWork = 0L;  // reset the counter of number of iterations without having any work to do.
                }
                else {
                    // we did NOT do any work this iteration.
                    ++lNumIterationsWithoutWork;  // bump the number of iterations without having any work to do.
                    Thread.sleep( Math.min(lNumIterationsWithoutWork, 5) * 100);
                    benchmarking_.tick();
                }
            }   // End try
            catch (NoConnectionsException nce) {
                log_.error("NoConnectionsException exception occurred during handleFillingInJobIdOnly, RAS event can NOT be logged, pausing for 10 seconds!");
                try { Thread.sleep(10 * 1000L); } catch (Exception e) {}  // wait 10 seconds to give it a chance for reconnection to db.
            }
            catch (Exception e) {
                log_.error("Exception occurred during handleFillingInJobIdOnly - will catch and then continue processing!");
                log_.error("%s", Adapter.stackTraceToString(e));
                try {
                    adapter_.logRasEventSyncNoEffectedJob("RasGenAdapterExceptionButContinue" // using synchronous version as we are in a flow where we want to ensure that this occurs in a timely manner.
                                                         ,("Exception=" + e)                  // instance data
                                                         ,null                                // Lctn
                                                         ,System.currentTimeMillis() * 1000L  // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,adapter_.adapterType()              // type of adapter that is requesting this
                                                         ,workQueue_.baseWorkItemId()         // requesting work item
                                                         );
                }
                catch (Exception e2) {}
            }
        }   // End while loop
        return -99999;
    }   // End handleFillingInJobIdOnly()



    //---------------------------------------------------------
    // Handles all the processing that needs to be done in order to ONLY handle RAS event control operations.
    // Note: This work item is different than most in that this one work item will run for the length of time that the system is active.
    //       It does not start and stop, it starts and stays active.
    //---------------------------------------------------------
    public long handleControlOpsOnly() throws InterruptedException, IOException, ProcCallException
    {
        log_.info("handleControlOpsOnly - starting");
        SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS000");
        sqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // This line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
        long lNumIterationsWithoutWork = 0L;  // counter of number of iterations without having any work to do.
        while(!adapter_.adapterShuttingDown()) {
            try {
                //-------------------------------------------------------------
                // Handle any RAS events that have a non-null control operation that has not yet been executed
                // (the above checks are ControlOperation is not null AND ControlOperationDone = 'N' - respectively).
                //-------------------------------------------------------------
                long lWorkToDoThisIteration = handleRasEventControlOperations(sqlDateFormat);
                //-------------------------------------------------------------
                // Check & see if there was any work to do this iteration.
                //-------------------------------------------------------------
                // ToDo: Wait for indicator to come in over scon waking us up (the wake up indicates that either a work item was queued for this adapter or that there have been ras events added in the RasEvent table).
                //       Want to have a timeout on that scon wait, as we definitely want to overtly check for work every so often regardless (in case the scon "wake up" had somehow been lost)!
                if (lWorkToDoThisIteration > 0) {
                    // we did do work this iteration.
                    log_.info("Did work this iteration, so immediately checking for more work");
                    lNumIterationsWithoutWork = 0L;  // reset the counter of number of iterations without having any work to do.
                }
                else {
                    // we did NOT do any work this iteration.
                    ++lNumIterationsWithoutWork;  // bump the number of iterations without having any work to do.
                    Thread.sleep( Math.min(lNumIterationsWithoutWork, 5) * 100);
                    benchmarking_.tick();
                }
            }   // End try
            catch (NoConnectionsException nce) {
                log_.error("NoConnectionsException exception occurred during handleControlOpsOnly, RAS event can NOT be logged, pausing for 10 seconds!");
                try { Thread.sleep(10 * 1000L); } catch (Exception e) {}  // wait 10 seconds to give it a chance for reconnection to db.
            }
            catch (Exception e) {
                log_.error("Exception occurred during handleControlOpsOnly - will catch and then continue processing!");
                log_.error("%s", Adapter.stackTraceToString(e));
                try {
                    adapter_.logRasEventSyncNoEffectedJob("RasGenAdapterExceptionButContinue" // using synchronous version as we are in a flow where we want to ensure that this occurs in a timely manner.
                                                         ,("Exception=" + e)                  // instance data
                                                         ,null                                // Lctn
                                                         ,System.currentTimeMillis() * 1000L  // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,adapter_.adapterType()              // type of adapter that is requesting this
                                                         ,workQueue_.baseWorkItemId()         // requesting work item
                                                         );
                }
                catch (Exception e2) {}
            }
        }   // End while loop
        return -99999;
    }   // End handleControlOpsOnly()



    //--------------------------------------------------------------------------
    // This method handles the general processing flow for RAS adapters (regardless of specific implementation).
    //--------------------------------------------------------------------------
    private void mainProcessingFlow(String[] args) {
        try {
            log_.info("Starting");

            // Get list of VoltDb servers, location of service node this adapter is running on, and service node's hostname.
            final String DbServers  = (args.length >= 1) ? args[0] : "localhost";  // e.g., voltdbserver1,voltdbserver2,10.11.12.13
            final String SnLctn     = (args.length >= 2) ? args[1] : "UnknownLctn";
            final String SnHostname = (args.length >= 3) ? args[2] : "UnknownHostName";
            log_.info("This adapter instance is running on lctn=%s, hostname=%s, pid=%d", SnLctn, SnHostname, adapter_.pid());
            // Set up the adapter instance.

            workQueue_ = adapter_.setUpAdapter(DbServers, SnLctn);

            //-----------------------------------------------------------------
            // Main processing loop
            //-----------------------------------------------------------------
            while(!adapter_.adapterShuttingDown()) {
                try {
                    // Handle any work items that have been queued for this type of adapter.
                    boolean bGotWorkItem = workQueue_.grabNextAvailWorkItem();
                    if (bGotWorkItem) {
                        // did get a work item
                        String[] aWiParms = workQueue_.getClientParameters(Pattern.quote("|"));
                        long rc = -99999;
                        switch(workQueue_.workToBeDone()) {

                            case "HandleFillingInJobIdsAndControlOps":
                                //---------------------------------------------------------
                                // Handles all the processing that needs to be done in order to fill in job ids AND handle RAS event control operations.
                                // Note: This work item is different than most in that this one work item will run for the length of time that the system is active.
                                //       It does not start and stop, it starts and stays active.
                                //---------------------------------------------------------
                                benchmarking_ = new BenchmarkHelper("HandleFillingInJobIdsAndControlOps",
                                    "/opt/ucs/log/Ras-HandleFillingInJobIdsAndControlOps-Benchmarking.json", 15);
                                rc = handleFillingInJobIdsAndControlOps();
                                break;

                            case "HandleFillingInJobIdOnly":
                                //---------------------------------------------------------
                                // Handles all the processing that needs to be done in order to ONLY fill in job ids.
                                // Note: This work item is different than most in that this one work item will run for the length of time that the system is active.
                                //       It does not start and stop, it starts and stays active.
                                //---------------------------------------------------------
                                benchmarking_ = new BenchmarkHelper("HandleFillingInJobIdOnly",
                                        "/opt/ucs/log/Ras-HandleFillingInJobIdOnly-Benchmarking.json", 15);
                                rc = handleFillingInJobIdOnly();
                                break;

                            case "HandleControlOpsOnly":
                                //---------------------------------------------------------
                                // Handles all the processing that needs to be done in order to ONLY handle RAS event control operations.
                                // Note: This work item is different than most in that this one work item will run for the length of time that the system is active.
                                //       It does not start and stop, it starts and stays active.
                                //---------------------------------------------------------
                                benchmarking_ = new BenchmarkHelper("HandleControlOpsOnly",
                                        "/opt/ucs/log/Ras-HandleControlOpsOnly-Benchmarking.json", 15);
                                rc = handleControlOpsOnly();
                                break;

                            default:
                                workQueue_.handleProcessingWhenUnexpectedWorkItem();
                                break;

                        }   // end of switch - workToBeDone()
                    }   // did get a work item

                    // Sleep for a little bit if this adapter type doesn't have any work.
                    if (!workQueue_.wasWorkDone())
                        Thread.sleep( 8 * 1000L );  // 8 secs


                }   // End try (while loop)
                catch (NoConnectionsException nce) {
                    log_.error("NoConnectionsException exception occurred during main processing loop, RAS event can NOT be logged, pausing for 10 seconds!");
                    try { Thread.sleep(10 * 1000L); } catch (Exception e) {}  // wait 10 seconds to give it a chance for reconnection to db.
                }
                catch (Exception e) {
                    log_.error("Exception occurred during main processing loop - will catch and then continue processing!");
                    log_.error("%s", Adapter.stackTraceToString(e));
                    try {
                        adapter_.logRasEventSyncNoEffectedJob("RasGenAdapterExceptionButContinue" // using synchronous version as we are in a flow where we want to ensure that this occurs in a timely manner.
                                                             ,("Exception=" + e)                  // instance data
                                                             ,null                                // Lctn
                                                             ,System.currentTimeMillis() * 1000L  // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                             ,adapter_.adapterType()              // type of adapter that is requesting this
                                                             ,workQueue_.baseWorkItemId()         // requesting work item
                                                             );
                    }
                    catch (Exception e2) {}
                }
            }   // End while loop - handle any work items that have been queued for this type of adapter.

            //-----------------------------------------------------------------
            // Clean up adapter table, base work item, and close connections to db.
            //-----------------------------------------------------------------
            adapter_.handleMainlineAdapterCleanup(adapter_.adapterAbnormalShutdown());
        }   // End try (mainProcessingFlow)
        catch (IOException | AdapterException | InterruptedException e) {
            adapter_.handleMainlineAdapterException(e);
        }
    }   // End mainProcessingFlow(String[] args)

    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getInstance("RAS", AdapterRasForeignBus.class.getName(), "console");
        AdapterSingletonFactory.initializeFactory("RAS", AdapterRasForeignBus.class.getName(), logger);
        final AdapterRasForeignBus obj = new AdapterRasForeignBus(AdapterSingletonFactory.getAdapter(), logger);
        // Start up the main processing flow for RAS adapters.
        obj.mainProcessingFlow(args);
    }   // End main(String[] args)

}   // End class AdapterRas
