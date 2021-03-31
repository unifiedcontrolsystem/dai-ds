// Copyright (C) 2017-2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.dai.ras;

import com.intel.dai.AdapterSingletonFactory;
import com.intel.dai.IAdapter;
import com.intel.dai.dsapi.WorkQueue;
import com.intel.logging.*;
import com.intel.dai.control.*;
import com.intel.dai.exceptions.AdapterException;
import org.voltdb.client.*;
import org.voltdb.VoltTable;
import org.voltdb.VoltTableRow;
import com.intel.dai.Adapter;
import java.util.regex.Pattern;
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
 * SELECT RasEvent.DescriptiveName, RasEvent.LastChgTimestamp, RasMetaData.Severity, RasEvent.Lctn, RasEvent.ControlOperation, RasMetaData.Component, RasMetaData.Msg, RasEvent.InstanceData FROM RasEvent INNER JOIN RasMetaData on RasEvent.DescriptiveName=RasMetaData.DescriptiveName Order By LastChgTimestamp;
 * SELECT RasEvent.Id, RasEvent.DescriptiveName, RasEvent.LastChgTimestamp, RasEvent.ControlOperation, RasMetaData.Severity, RasMetaData.Category, RasMetaData.Component, RasMetaData.Msg, RasEvent.InstanceData FROM RasEvent INNER JOIN RasMetaData on RasEvent.DescriptiveName=RasMetaData.DescriptiveName WHERE RasEvent.DescriptiveName = 'RasWlmProvisionHookError';
 */
public class AdapterRas {

    // Constructor
    AdapterRas(IAdapter adapter, Logger logger) {
        log_ = logger;
        adapter_ = adapter;
        lastTimeInMsDidChkForExpiredJobs_ = System.currentTimeMillis();
        mCachedJobInfoMap = null;
        mNodesInServiceArraylist = new ArrayList<String>();
    }   // ctor

    // Member Data
    private IAdapter    adapter_;
    private WorkQueue   workQueue_;
    private ControlApi  controlHandler_;
    private Logger      log_;
    private long        lastTimeInMsDidChkForExpiredJobs_;  // the last time (in milliseconds) that we checked for expired jobs.
    private HashMap<String, ArrayList<VoltTableRow>> mCachedJobInfoMap = null;
    private ArrayList<String> mNodesInServiceArraylist;


    private boolean isNodeInResetRecursion(String sNodeLctn) throws InterruptedException, IOException, ProcCallException {
        long lNumNodeResetEvents = adapter_.client().callProcedure("RasEventCountNodeResetRecently"
                                                                  ,"RasCntrlNodeReset"
                                                                  ,sNodeLctn
                                                                  ,(System.currentTimeMillis() - (5 * 60 * 1000L)) * 1000L
                                                                  ).getResults()[0].asScalarLong();
        return lNumNodeResetEvents > 1;
    }   // End isNodeInResetRecursion(String sNodeLctn)


    private class RasEventData {
        // Constructor
        RasEventData(VoltTable vt) {
            sRasEventDescrName            = vt.getString("DescriptiveName");
            lRasEventId                   = vt.getLong("Id");
            sRasEventId                   = Long.toString(lRasEventId);
            sRasEventLctn                 = vt.getString("Lctn");
            sRasEventJobid                = vt.getString("JobId");
            sRasEventControlOperation     = vt.getString("ControlOperation");
            lRasEventLastChgTsInMicroSecs = vt.getTimestampAsLong("LastChgTimestamp");  // get a long representation of the ras event's timestamp, units of microseconds since epoch (GMT timezone).
        }   // ctor

        // Member Data
        String  sRasEventDescrName;
        long    lRasEventId;
        String  sRasEventId;
        String  sRasEventLctn;
        String  sRasEventJobid;
        String  sRasEventControlOperation;
        long    lRasEventLastChgTsInMicroSecs;

    }   // End class RasEventData


    //     // Perform an iterative binary search through the cached job info looking for first row with specified Lctn.
    //     // Returns:
    //     //      -1 = Lctn was not found
    //     //      Index of first row that has the specified Lctn (if found)
    //     int binarySearchCachedJobInfo(HashMap<String, VoltTableRow> mCachedJobInfoMap, String sLctn)
    //     {
    //         int iStartingIndex = 0;
    //         int iEndingIndex = vtCachedJobInfo.getRowCount() - 1;
    //         // Reset the VoltTable to be pointing at the beginning (first row).
    //         vtCachedJobInfo.resetRowPosition();
    //         vtCachedJobInfo.advanceRow();
    //         while (iStartingIndex <= iEndingIndex) {
    //             int iMiddleIndex = iStartingIndex + (iEndingIndex - iStartingIndex) / 2;
    //             log_.info("binarySearchCachedJobInfo - iStartingIndex=%d, iEndingIndex=%d -- iMiddleIndex=%d", iStartingIndex, iEndingIndex, iMiddleIndex);
    //             boolean bOk = vtCachedJobInfo.advanceToRow(iMiddleIndex);
    //             if (bOk == false) {
    //                 // advanceToRow failed.
    //                 String sError = "binarySearchCachedJobInfo failed - advanceToRow returned false - sLctn=" + sLctn + ", iMiddleIndex=" + iMiddleIndex + "!";
    //                 log_.error(sError);
    //                 throw new RuntimeException(sError);
    //             }
    //             // Check if this index is a match for the specified sLctn.
    //             int iComparison = vtCachedJobInfo.getString("NodeLctn").compareTo(sLctn);
    //             if (iComparison == 0) {
    //                 // Make sure that iMiddleIndex is the first occurrence of this lctn within the VoltTable.
    //                 while ((iMiddleIndex - 1) >= 0) {
    //                     // Go to the previous row.
    //                     vtCachedJobInfo.advanceToRow(iMiddleIndex - 1);
    //                     if (vtCachedJobInfo.getString("NodeLctn").equals(sLctn))
    //                         --iMiddleIndex;  // bump the index to be returned back by 1.
    //                     else
    //                         // this previous row has a different lctn, return iMiddleIndex.
    //                         break;
    //                 }
    //                 return iMiddleIndex;
    //             }
    //             // If the specified sLctn is greater than current value, ignore the first half of these results.
    //             else if (iComparison < 0)
    //                 iStartingIndex = iMiddleIndex + 1;
    //             // If sLctn is smaller than current value, ignore the second half of these results.
    //             else
    //                 iEndingIndex = iMiddleIndex - 1;
    //         }
    //
    //         // The specified Lctn was not found in the cached job info.
    //         return -1;
    //     }   // End binarySearchCachedJobInfo(HashMap<String, VoltTableRow> mCachedJobInfoMap, String sLctn)


    // Use a stored procedure to get this RAS events associated job id (if any).
    private String getThisEventsJobIdFromCachedJobInfo(String sRasEventDescrName, String sRasEventLctn, long lRasEventId, long lRasEventTsInMicroSecs, HashMap<String, ArrayList<VoltTableRow>> mCachedJobInfoMap) {
        String sFndJobId = null;
        // Get the ArrayList of volt table rows for this lctn's cached job information.
        ArrayList<VoltTableRow> alVoltTableRow = mCachedJobInfoMap.get(sRasEventLctn);
        if ((alVoltTableRow == null) || (alVoltTableRow.isEmpty())) {
            // there wasn't any cached job information for this lctn.
            log_.debug("Did NOT find %s in the cached job info, so there is no JobId for this event", sRasEventLctn);
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


    //--------------------------------------------------------------------------
    // Handle any RAS events that have a non-null control operation that has not yet been executed
    // (the above checks are ControlOperation is not null AND Done != 'Y' - respectively).
    //--------------------------------------------------------------------------
    private void handleControlOperationForThisEvent(RasEventData event)
                 throws InterruptedException, IOException, ProcCallException
    {
        // Do not run any ControlOperation if the RAS event does not have a lctn string.
        if (event.sRasEventLctn == null) {
            // the specified RAS event's lctn is null, so no don't try to run any control operation.
            log_.debug("Did NOT run this RAS event's ControlOperation because the event has no specified lctn, ignoring this ControlOperation - DescrName=%s, EventId=%s, Lctn=%s, JobId=%s, ControlOperation=%s!",
                       event.sRasEventDescrName, event.sRasEventId, event.sRasEventLctn, event.sRasEventJobid, event.sRasEventControlOperation);
        }   // the specified RAS event's lctn is null, so no don't try to run any control operation.

        // Do NOT run any ControlOperations if the RAS event's hardware lctn is currently being serviced.
        else if ((mNodesInServiceArraylist.contains(event.sRasEventLctn)) && (!event.sRasEventControlOperation.equals("NodeIsPoweredOff"))) {
            // this node is owned by the Service subsystem (i.e., this hardware is being serviced).
            log_.warn("Did NOT run this RAS event's ControlOperation (%s) because the hardware lctn is currently being Serviced, ignoring this ControlOperation - DescrName=%s, EventId=%s, Lctn=%s, JobId=%s!",
                      event.sRasEventControlOperation, event.sRasEventDescrName, event.sRasEventId, event.sRasEventLctn, event.sRasEventJobid);
            // Cut a RAS event to capture this occurrence.
            adapter_.logRasEventNoEffectedJob("RasSkippingControlOperationCuzHwBeingServiced"
                                             ,("DescrName=" + event.sRasEventDescrName + ", ID=" + event.sRasEventId + ", ControlOperation=" + event.sRasEventControlOperation + ", JobId=" + event.sRasEventJobid)
                                             ,event.sRasEventLctn                // Lctn associated with this ras event
                                             ,System.currentTimeMillis() * 1000L // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                             ,adapter_.adapterType()             // type of adapter_ that is generating this ras event
                                             ,workQueue_.baseWorkItemId()        // work item that is being worked on that resulted in the generation of this ras event
                                             );
        }   // this node is owned by the Service subsystem (i.e., this hardware is being serviced).

        // Run the specified ControlOperation.
        else {
            log_.info("Processing a RAS event's control operation - %s - DescrName=%s, EventId=%s, Lctn=%s, JobId=%s",
                      event.sRasEventControlOperation, event.sRasEventDescrName, event.sRasEventId, event.sRasEventLctn, event.sRasEventJobid);
            // Actually run the specified control operation.
            runTheEventsControlOperation(event);
        }   // run the specified ControlOperation.
    }   // End handleControlOperationForThisEvent(RasEventData event)


    //---------------------------------------------------------
    // Handles processing of filling in job ids, running control operations, or both while MINIMIZING DB updates (so fewer updates need to flow to Tier2)
    // Note: This work item is different than most in that this one work item will run for the length of time that the system is active.
    //       It does not start and stop, it starts and stays active.
    //---------------------------------------------------------
    public long handleFillingInJobIdsAndControlOps() throws InterruptedException, IOException, ProcCallException
    {
        log_.info("handleFillingInJobIdsAndControlOps - starting");
        SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS000");
        sqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // This line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
        long lNumIterationsWithoutWork = 0L;  // counter of number of iterations without having any work to do.
        while(adapter_.adapterShuttingDown() == false) {
            try {
                //--------------------------------------------------------------
                // Handle any RAS events that need one or more of the following done:
                // - a Job ID filled in
                // - a Control Operation run
                // - both a Job ID filled in and a Control Operation run
                //--------------------------------------------------------------
                long lWorkDoneThisIteration = fillInJobIdAndRunControlOps(sqlDateFormat);
                //-------------------------------------------------------------
                // Check & see if there was any work to do this iteration.
                //-------------------------------------------------------------
                if (lWorkDoneThisIteration > 0) {
                    // we did do work this iteration.
                    log_.info("Did work this iteration, so immediately checking for more work");
                    lNumIterationsWithoutWork = 0L;  // reset the counter of number of iterations without having any work to do.
                }
                else {
                    // we did NOT do any work this iteration.
                    ++lNumIterationsWithoutWork;  // bump the number of iterations without having any work to do.
                    Thread.sleep( Math.min(lNumIterationsWithoutWork, 5) * 100);
                }
            }   // End try
            catch (NoConnectionsException nce) {
                log_.error("NoConnectionsException exception occurred during handleFillingInJobIdsAndControlOps, RAS event can NOT be logged, pausing for 10 seconds!");
                try { Thread.sleep(10 * 1000L); } catch (Exception e) {}  // wait 10 seconds to give it a chance for reconnection to db.
            }
            catch (Exception e) {
                log_.error("Exception occurred during handleFillingInJobIdsAndControlOps - will catch and then continue processing!");
                log_.error("%s", Adapter.stackTraceToString(e));
                adapter_.logRasEventSyncNoEffectedJob("RasGenAdapterExceptionButContinue" // using synchronous version as we are in a flow where we want to ensure that this occurs in a timely manner.
                                                     ,("Exception=" + e)                  // instance data
                                                     ,null                                // Lctn
                                                     ,System.currentTimeMillis() * 1000L  // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                     ,adapter_.adapterType()              // type of adapter that is requesting this
                                                     ,workQueue_.baseWorkItemId()         // requesting work item
                                                     );
            }
        }   // End while loop
        return -99999;
    }   // End handleFillingInJobIdsAndControlOps()


    private HashMap<String, ArrayList<VoltTableRow>> createCachedJobInfoMap() throws IOException, ProcCallException {
        // Create an entry in the hash map for each compute node lctn defined in this machine.
        log_.debug("Creating HashMap of ArrayList<VoltTableRow> ");
        // Get the list of ComputeNodes defined in the system.
        ClientResponse response = adapter_.client().callProcedure("ComputeNodeListLctnAndSeqNum");
        VoltTable vt = response.getResults()[0];
        // Create the Map object (of the size equal to number of compute nodes).
        HashMap<String, ArrayList<VoltTableRow>> tempCachedJobInfoMap = new HashMap<String, ArrayList<VoltTableRow>>(vt.getRowCount());
        // Loop through each of the ComputeNodes defined in the ComputeNode table
        // (initializing the hashmap entries).
        for (int iCnCntr = 0; iCnCntr < vt.getRowCount(); ++iCnCntr) {
            vt.advanceRow();
            // Create the array list for this lctn w/ room for 3 entries.
            ArrayList<VoltTableRow> alVt = new ArrayList<VoltTableRow>(3);
            // Create the HashMap entry for this lctn and associating it with its ArrayList.
            tempCachedJobInfoMap.put(vt.getString("Lctn"), alVt);
        }
        log_.debug("Created  HashMap of ArrayList<VoltTableRow> - CachedJobInfoMap.size() = %d", tempCachedJobInfoMap.size());
        return tempCachedJobInfoMap;
    }   // End createCachedJobInfoMap()


    private int fillInCachedJobInfoMap(VoltTable vtListRasEventsThatNeedWorkDoneMaxTs, ClientResponse response) throws IOException, ProcCallException {
        // There are RAS events that need work done - fill in the cached job info (because we don't want to have to call volt for each individual RAS event).
        int tempNumEntriesInCachedJobInfoMap = 0;
        vtListRasEventsThatNeedWorkDoneMaxTs.advanceRow();  // advance to the VoltTable row that has the maximum LastChgTimestamp value within this list of RAS events.
        // Get the list of cached job info entries.
        response = adapter_.client().callProcedure("InternalCachedJobsGetListOfActiveInternalCachedJobsUsingTimestamp", vtListRasEventsThatNeedWorkDoneMaxTs.getTimestampAsTimestamp(0), vtListRasEventsThatNeedWorkDoneMaxTs.getTimestampAsTimestamp(0));
        VoltTable vtCachedJobInfo = response.getResults()[0];
        // Spin through putting this cached job info into our map.
        for (int i=0; i < vtCachedJobInfo.getRowCount(); ++i) {
            vtCachedJobInfo.advanceRow();
            // Get this node's array list object out of the map of cached job info.
            ArrayList<VoltTableRow> alVoltTableRow = mCachedJobInfoMap.get(vtCachedJobInfo.getString("NodeLctn"));
            if (alVoltTableRow != null) {
                // Add this cached job info entry into the array list.
                alVoltTableRow.add( vtCachedJobInfo.cloneRow() );
                ++tempNumEntriesInCachedJobInfoMap; // bump the number of entries in the cached map.
            }
            else {
                // Could not add this cached job lctn into the CachedJobInfo map because it is an unexpected lctn.
                log_.error("While filling in the CachedJobInfo map we found an unexpected lctn (%s), this lctn does not have an entry in the pre-built CachedJobInfoMap object - skipping this entry!",
                           vtCachedJobInfo.getString("NodeLctn"));
                adapter_.logRasEventNoEffectedJob("RasUnableToAddLctnToCachedjobinfomap"
                                                 ,null
                                                 ,vtCachedJobInfo.getString("NodeLctn") // Lctn associated with this ras event
                                                 ,System.currentTimeMillis() * 1000L    // Current time, in micro-seconds since epoch
                                                 ,adapter_.adapterType()                // type of adapter_ that is generating this ras event
                                                 ,workQueue_.workItemId()               // work item that is being worked on that resulted in the generation of this ras event
                                                 );
            }
        }
        log_.info("Put %d entries into the cached job information map", tempNumEntriesInCachedJobInfoMap);
        return tempNumEntriesInCachedJobInfoMap;
    }   // End fillInCachedJobInfoMap(VoltTable vtListRasEventsThatNeedWorkDoneMaxTs, ClientResponse response)


    //----------------------------------------------------------------------
    // Get the list of nodes that are currently being serviced.
    //----------------------------------------------------------------------
    private void fillInCachedNodesBeingServicedList(ClientResponse response) throws IOException, ProcCallException
    {
        response = adapter_.client().callProcedure("NodeListBeingServiced");
        VoltTable vtComputeNodesBeingServiced = response.getResults()[0];  // List of compute nodes currently in service.
        VoltTable vtServiceNodesBeingServiced = response.getResults()[1];  // list of service nodes currently being serviced.
        // Spin through putting the compute nodes into the array list.
        for (int i=0; i < vtComputeNodesBeingServiced.getRowCount(); ++i) {
            vtComputeNodesBeingServiced.advanceRow();
            mNodesInServiceArraylist.add( vtComputeNodesBeingServiced.getString("Lctn") );
        }
        // Spin through putting the service nodes into the array list.
        for (int i=0; i < vtServiceNodesBeingServiced.getRowCount(); ++i) {
            vtServiceNodesBeingServiced.advanceRow();
            mNodesInServiceArraylist.add( vtServiceNodesBeingServiced.getString("Lctn") );
        }
        log_.info("Put %d entries into the cached nodes being serviced list", (vtComputeNodesBeingServiced.getRowCount() + vtServiceNodesBeingServiced.getRowCount()));
    }   // End fillInCachedNodesBeingServicedList(ClientResponse response)


    //--------------------------------------------------------------------------
    // Actually run the specified control operation.
    //--------------------------------------------------------------------------
    private void runTheEventsControlOperation(RasEventData event)
                 throws InterruptedException, IOException, ProcCallException
    {
        switch(event.sRasEventControlOperation) {
            case "ErrorOnNode":
                // Mark this node as being in Error state.
                adapter_.markNodeInErrorState(event.sRasEventLctn, event.sRasEventDescrName, event.lRasEventId, event.sRasEventControlOperation,
                                              event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                break;
            case "ErrorAndKillJobOnNode":
                // Mark this node as being in Error state.
                adapter_.markNodeInErrorState(event.sRasEventLctn, event.sRasEventDescrName, event.lRasEventId, event.sRasEventControlOperation,
                                              event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                // Kill any job that is using this node.
                adapter_.killJob(event.sRasEventLctn, event.sRasEventDescrName, event.lRasEventId, event.sRasEventControlOperation,
                                 event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                break;

            case "ErrorAndPwrOffNode":
                // Mark this node as being in Error state.
                adapter_.markNodeInErrorState(event.sRasEventLctn, event.sRasEventDescrName, event.lRasEventId, event.sRasEventControlOperation,
                                              event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                // Power Off this node.
                controlHandler_.powerOffNode(adapter_.extractNodeLctnFromLctn(event.sRasEventLctn), adapter_.mapNodeLctnToAggregator(), event.sRasEventDescrName, event.lRasEventId,
                                             event.sRasEventControlOperation, event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                break;
            case "ErrorAndKillJobAndPwrOffNode":
                // Mark this node as being in Error state.
                adapter_.markNodeInErrorState(event.sRasEventLctn, event.sRasEventDescrName, event.lRasEventId, event.sRasEventControlOperation,
                                              event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                // Kill any job that is using this node.
                adapter_.killJob(event.sRasEventLctn, event.sRasEventDescrName, event.lRasEventId, event.sRasEventControlOperation,
                                 event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                // Power Off this node.
                controlHandler_.powerOffNode(adapter_.extractNodeLctnFromLctn(event.sRasEventLctn), adapter_.mapNodeLctnToAggregator(), event.sRasEventDescrName, event.lRasEventId,
                                             event.sRasEventControlOperation, event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                break;

            case "ErrorAndShutdownNode":
                // Mark this node as being in Error state.
                adapter_.markNodeInErrorState(event.sRasEventLctn, event.sRasEventDescrName, event.lRasEventId, event.sRasEventControlOperation,
                                              event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                // Shutdown this node.
                controlHandler_.shutdownNode(adapter_.extractNodeLctnFromLctn(event.sRasEventLctn), adapter_.mapNodeLctnToAggregator(), event.sRasEventDescrName, event.lRasEventId,
                                             event.sRasEventControlOperation, event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                break;
            case "ErrorAndKillJobAndShutdownNode":
                // Mark this node as being in Error state.
                adapter_.markNodeInErrorState(event.sRasEventLctn, event.sRasEventDescrName, event.lRasEventId, event.sRasEventControlOperation,
                                              event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                // Kill any job that is using this node.
                adapter_.killJob(event.sRasEventLctn, event.sRasEventDescrName, event.lRasEventId, event.sRasEventControlOperation,
                                 event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                // Shutdown this node.
                controlHandler_.shutdownNode(adapter_.extractNodeLctnFromLctn(event.sRasEventLctn), adapter_.mapNodeLctnToAggregator(), event.sRasEventDescrName, event.lRasEventId,
                                             event.sRasEventControlOperation, event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                break;

            case "IncreaseFanSpeed":
                // Increase this node's fan speed.
                controlHandler_.increaseNodeFanSpeed(adapter_.extractNodeLctnFromLctn(event.sRasEventLctn), adapter_.mapNodeLctnToAggregator(), event.sRasEventDescrName, event.lRasEventId,
                                                     event.sRasEventControlOperation, event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                break;

            case "KillJobOnNode":
                // This control action is intended for events that are job fatal but do not indicate that the node is fundamentally unhealthy.  E.g., running out of LWK memory.
                // Kill any job that is using this node.
                adapter_.killJob(event.sRasEventLctn, event.sRasEventDescrName, event.lRasEventId, event.sRasEventControlOperation,
                                 event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                break;

            case "NodeIsPoweredOff":
                // Mark this node as being powered off.
                adapter_.markNodePoweredOff(event.sRasEventLctn, true, event.lRasEventLastChgTsInMicroSecs, adapter_.adapterType(), workQueue_.baseWorkItemId());
                break;

            case "ErrorAndPowerCycleNode":
                // Mark this node as being in Error state.
                adapter_.markNodeInErrorState(event.sRasEventLctn, event.sRasEventDescrName, event.lRasEventId, event.sRasEventControlOperation,
                                              event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                // Power cycle this node.
                controlHandler_.powerCycleNode(adapter_.extractNodeLctnFromLctn(event.sRasEventLctn), adapter_.mapNodeLctnToAggregator(), event.sRasEventDescrName, event.lRasEventId,
                                               event.sRasEventControlOperation, event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                break;
            case "ErrorAndResetNode":
                // Mark this node as being in Error state.
                adapter_.markNodeInErrorState(event.sRasEventLctn, event.sRasEventDescrName, event.lRasEventId, event.sRasEventControlOperation,
                                              event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                // Ensure that this node is not in "reset recursion".
                if (!isNodeInResetRecursion(event.sRasEventLctn)) {
                    // this node is NOT in reset recursion - go ahead and reset the node.
                    // Reset this node (similar to powering off and back on the node).
                    String[] saLctnsToReset = { adapter_.extractNodeLctnFromLctn(event.sRasEventLctn) };
                    controlHandler_.resetNodes(saLctnsToReset, adapter_.mapNodeLctnToAggregator(), event.sRasEventDescrName, event.lRasEventId,
                                               event.sRasEventControlOperation, event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                }
                else {
                    // this node is indeed experiencing reset recursion - short-circuit
                    log_.warn("Detected 'reset recursion' for this node, another reset of this node was skipped - " +
                              "DescrName=%s, EventId=%s, ControlOperation=%s!!!", event.sRasEventDescrName, event.sRasEventId, event.sRasEventControlOperation);
                    // Cut RAS event so we know that reset recursion was detected and was short-circuited.
                    adapter_.logRasEventWithEffectedJob("RasResetRecursionDetected"
                                                       ,("DescrName=" + event.sRasEventDescrName + ", ID=" + event.sRasEventId + ", ControlOperation=" + event.sRasEventControlOperation)
                                                       ,event.sRasEventLctn                // Lctn associated with this ras event
                                                       ,event.sRasEventJobid               // JobId of a job that was effected by this ras event
                                                       ,System.currentTimeMillis() * 1000L // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                       ,adapter_.adapterType()             // type of adapter that is generating this ras event
                                                       ,workQueue_.baseWorkItemId()        // work item that is being worked on that resulted in the generation of this ras event
                                                       );
                }
                break;
            case "ResetNode":
                // Ensure that this node is not in "reset recursion".
                if (!isNodeInResetRecursion(event.sRasEventLctn)) {
                    // this node is NOT in reset recursion - go ahead and reset the node.
                    // Reset this node (similar to powering off and back on the node).
                    String[] saLctnsToReset = { adapter_.extractNodeLctnFromLctn(event.sRasEventLctn) };
                    controlHandler_.resetNodes(saLctnsToReset, adapter_.mapNodeLctnToAggregator(), event.sRasEventDescrName, event.lRasEventId,
                                               event.sRasEventControlOperation, event.sRasEventJobid, adapter_.adapterType(), workQueue_.baseWorkItemId());
                }
                else {
                    // this node is indeed experiencing reset recursion - short-circuit
                    log_.warn("Detected 'reset recursion' for this node, another reset of this node was skipped - " +
                              "DescrName=%s, EventId=%s, ControlOperation=%s!!!", event.sRasEventDescrName, event.sRasEventId, event.sRasEventControlOperation);
                    // Cut RAS event so we know that reset recursion was detected and was short-circuited.
                    adapter_.logRasEventWithEffectedJob("RasResetRecursionDetected"
                                                       ,("DescrName=" + event.sRasEventDescrName + ", ID=" + event.sRasEventId + ", ControlOperation=" + event.sRasEventControlOperation)
                                                       ,event.sRasEventLctn                // Lctn associated with this ras event
                                                       ,event.sRasEventJobid               // JobId of a job that was effected by this ras event
                                                       ,System.currentTimeMillis() * 1000L // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                       ,adapter_.adapterType()             // type of adapter that is generating this ras event
                                                       ,workQueue_.baseWorkItemId()        // work item that is being worked on that resulted in the generation of this ras event
                                                       );
                }
                break;
            case "ResetNodeConsoleConnection":
                // Attempt to reset specified node's connection for serial console msgs from bmc to conman.
                String sNodeName = adapter_.getNodeNameFromLctn(event.sRasEventLctn);
                try {
                    // Note: this reset invocation is currently fire and forget, there is no need to wait for the spun off process to terminate.
                    // Grab the aggregator for this node.
                    String sTempNodeAggregator = adapter_.mapNodeLctnToAggregator().get(event.sRasEventLctn);
                    log_.warn("Attempting to reset node's connection for console msgs from bmc - " +
                              "DescrName=%s, EventId=%s, ControlOperation=%s, Lctn=%s, Aggregator=%s, NodeName=%s!!!", event.sRasEventDescrName, event.sRasEventId, event.sRasEventControlOperation, event.sRasEventLctn, sTempNodeAggregator, sNodeName);
                    // Ensure we got the aggregator for this node's lctn.
                    if (sTempNodeAggregator == null) {
                        log_.error("During ResetNodeConsoleConnection control operation - we could not find the aggregator for the specified node lctn (%s)!", event.sRasEventLctn);
                        adapter_.logRasEventNoEffectedJob("RasUnableToFindNodesAggregator"
                                                         ,("DescrName=" + event.sRasEventDescrName + ", ID=" + event.sRasEventId + ", ControlOperation=" + event.sRasEventControlOperation)
                                                         ,event.sRasEventLctn                 // Lctn associated with this ras event
                                                         ,System.currentTimeMillis() * 1000L  // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,adapter_.adapterType()              // type of adapter_ that is generating this ras event
                                                         ,workQueue_.baseWorkItemId()         // work item that is being worked on that resulted in the generation of this ras event
                                                         );
                    }
                    else {
                        // Send a work item to the aggregator's Provisioner adapter/provider.
                        Map<String, String> parameters = new HashMap<>();
                        parameters.put("device", sNodeName);
                        parameters.put("node",   event.sRasEventLctn);
                        long lRnccWorkItemId = workQueue_.queueWorkItem("PROVISIONER"                // type of adapter that needs to handle this work
                                                                       ,sTempNodeAggregator          // queue this work should be put on
                                                                       ,"ResetNodeConsoleConnection" // work that needs to be done
                                                                       ,parameters                   // parameters for this work
                                                                       ,false                        // false indicates that we do NOT want to know when this work item finishes
                                                                       ,adapter_.adapterType()       // type of adapter that requested this work to be done
                                                                       ,workQueue_.baseWorkItemId()  // work item that the requesting adapter was working on when it requested this work be done
                                                                       );
                    }
                }
                catch (Exception e) {
                    log_.error("Exception occurred during a ResetNodeConsoleConnection control operation - will catch and then continue processing!");
                    log_.error("%s", Adapter.stackTraceToString(e));
                    adapter_.logRasEventSyncNoEffectedJob("RasResetNodeConsoleConnectionError" // using synchronous version as we are in a flow where we want to ensure that this occurs in a timely manner.
                                                         ,("Exception=" + e)                   // instance data
                                                         ,event.sRasEventLctn                  // Lctn
                                                         ,System.currentTimeMillis() * 1000L   // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,adapter_.adapterType()               // type of adapter that is requesting this
                                                         ,workQueue_.baseWorkItemId()          // requesting work item
                                                         );
                }
                break;
            case "TestControlOperation":
                adapter_.client().callProcedure(adapter_.createHouseKeepingCallbackNoRtrnValue(adapter_.adapterType(), adapter_.adapterName(), "ErrorOnComputeNode", event.sRasEventLctn, workQueue_.baseWorkItemId())
                                               ,"ErrorOnComputeNode"
                                               ,event.sRasEventLctn
                                               ,adapter_.adapterType()
                                               ,workQueue_.baseWorkItemId());
                break;
            default:
                log_.error("Detected an unexpected control operation for this RAS Event, this control operation has NOT YET BEEN IMPLEMENTED - " +
                           "DescrName=%s, EventId=%s, ControlOperation=%s!!!", event.sRasEventDescrName, event.sRasEventId, event.sRasEventControlOperation);
                // Indicate that an internal error occurred in this RAS adapter.
                adapter_.logRasEventNoEffectedJob("RasMissingControlOperationLogic"
                                                 ,("DescrName=" + event.sRasEventDescrName + ", ID=" + event.sRasEventId + ", ControlOperation=" + event.sRasEventControlOperation)
                                                 ,null                                // Lctn associated with this ras event
                                                 ,System.currentTimeMillis() * 1000L  // Time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                 ,adapter_.adapterType()              // type of adapter_ that is generating this ras event
                                                 ,workQueue_.baseWorkItemId()         // work item that is being worked on that resulted in the generation of this ras event
                                                 );
                break;
        }
    }   // End runTheEventsControlOperation(RasEventData event)


    //--------------------------------------------------------------------------
    // Find and return the JobId (if any) associated with this RAS event.
    //--------------------------------------------------------------------------
    private String findJobidForThisEvent(RasEventData event, int iNumEntriesInCachedJobInfoMap) throws IOException, ProcCallException {
        String  sFndJobId = null;  // default to "there is no job associated with this RAS event".
        // See if the CachedJobInfo map is empty (there are no entries in the map).
        if (iNumEntriesInCachedJobInfoMap == 0) {
            // short-circuit - there are no CachedJobInfo entries - since there are no entries this RAS event does not have an associated job id.
            ////log_.info("RAS event asked us to fill in an associated job id, but there is no cached job information, so this event can not have an associated job id - EventDescrName=%s, EventId=%d, Lctn=%s",
            ////          event.sRasEventDescrName, event.lRasEventId, event.sRasEventLctn);
        }

        // Ensure that there is a lctn in the RAS event, if not then there can't be any job id associated with this event.
        else if (event.sRasEventLctn == null) {
            // short-circuit - there is no lctn specified on this event, so it can not have an associated job id.
            log_.info("RAS event asked us to fill in an associated job id, but there isn't a lctn on this event, so it can not have a job id - EventDescrName=%s, EventId=%d, Lctn=%s",
                      event.sRasEventDescrName, event.lRasEventId, event.sRasEventLctn);
        }

        // See if this is a fully qualified ComputeNode location.
        else if (adapter_.isComputeNodeLctn(event.sRasEventLctn)) {
            // this is a fully qualified ComputeNode location - it IS capable of having a job associated with it.
            // Get the JobId (if any) that was running on this event's lctn at the specified time.
            sFndJobId = getThisEventsJobIdFromCachedJobInfo(event.sRasEventDescrName, event.sRasEventLctn, event.lRasEventId,
                                                            event.lRasEventLastChgTsInMicroSecs, mCachedJobInfoMap);
        }

        // See if this is a fully qualified ServiceNode location - service nodes can't have an associated job id.
        else if (adapter_.isServiceNodeLctn(event.sRasEventLctn)) {
            // short-circuit - this is a ServiceNode, it is not capable of having an associated job id.
            log_.info("RAS event asked us to fill in an associated job id, but this is a ServiceNode lctn, so it can not have a job id - EventDescrName=%s, EventId=%d, Lctn=%s",
                      event.sRasEventDescrName, event.lRasEventId, event.sRasEventLctn);
        }

        // See if this lctn is for an unexpected type of hardware - we haven't implemented a check to see if this kind of hardware has an associated job id.
        else {
            // this is an unexpected type of lctn - have not yet implemented how to check if this lctn has an associated job id.
            log_.error("RAS event asked us to fill in an associated job id, but the lctn is for an unexpected type of hardware, so it can not have a job id - EventDescrName=%s, EventId=%d, Lctn=%s",
                       event.sRasEventDescrName, event.lRasEventId, event.sRasEventLctn);
            adapter_.logRasEventNoEffectedJob("RasUnexpectedTypeOfLctnNonCn"
                                             ,("OrigEventDescrName=" + event.sRasEventDescrName + ", OrigEventId=" + event.lRasEventId + ", OrigEventLctn=" + event.sRasEventLctn)
                                             ,event.sRasEventLctn                // Lctn associated with this ras event
                                             ,System.currentTimeMillis() * 1000L // Current time, in micro-seconds since epoch
                                             ,adapter_.adapterType()             // type of adapter_ that is generating this ras event
                                             ,workQueue_.baseWorkItemId()        // work item that is being worked on that resulted in the generation of this ras event
                                             );
        }   // this is an unexpected type of lctn - have not yet implemented how to check if this lctn can be associated with a job.

        return sFndJobId;
    }   // End findJobidForThisEvent(RasEventData event, int iNumEntriesInCachedJobInfoMap)


    //-------------------------------------------------------------
    // Handle any RAS events that need one or more of the following done:
    // - a Job ID filled in
    // - a Control Operation run
    // - both a Job ID filled in and a Control Operation run
    // Returns:
    //      Number of RAS events that we finished.
    //-------------------------------------------------------------
    private long fillInJobIdAndRunControlOps(SimpleDateFormat sqlDateFormat) throws IOException, ProcCallException, InterruptedException, AdapterException
    {
        ClientResponse response = null;
        //----------------------------------------------------------------------
        // Ensure that the CachedJobInfo map (w/ entry for each possible ComputeNode Lctn) has already been setup.
        //----------------------------------------------------------------------
        if (mCachedJobInfoMap == null)
            mCachedJobInfoMap = createCachedJobInfoMap();

        //----------------------------------------------------------------------
        // Get a list of RAS events that still need work to be done (by the RAS adapter), for instance
        //      - that need to have their JobId filled in (check and see if there is a JobId that needs to be filled in)
        //      - that need a ControlOperation run
        //      NOTE1: that as a parameter to the stored procedure we specify a timestamp to use when processing the RAS events
        //          that need their JobId filled in.  We do NOT want to use the current time rather we want to process events
        //          that are at least 4 seconds old.  This delay is to give the wlm job info enough time to propagate from log files
        //          into the InternalCachedJobs table (just in case that RAS event shows up before the iob info percolates through).
        //      NOTE2: There is nothing scientific about the number 4 seconds, it was chosen simple because it seemed reasonable at the time.
        //----------------------------------------------------------------------
        long lTempSaveStartingTsInMs = System.currentTimeMillis();  // save the time that we began this iteration of filling in JobIds.
        long lTempDelayedTsInMs = lTempSaveStartingTsInMs - (4 * 1000L);  // calculate the delayed timestamp, current time minus 4 seconds (see note above).
        response = adapter_.client().callProcedure("RasEventListThatNeedToBeDone", (lTempDelayedTsInMs * 1000L));
        if (response.getStatus() != ClientResponse.SUCCESS) {
            // stored procedure failed.
            log_.error("Stored procedure RasEventListThatNeedToBeDone FAILED - Status=%s, StatusString=%s, AdapterType=%s, ThisAdapterId=%d!",
                       IAdapter.statusByteAsString(response.getStatus()), response.getStatusString(), adapter_.adapterType(), adapter_.adapterId());
            throw new RuntimeException(response.getStatusString());
        }
        VoltTable vtListRasEventsThatNeedWorkDone      = response.getResults()[0];  // List of RasEvents which need to be finished.
        VoltTable vtListRasEventsThatNeedWorkDoneMaxTs = response.getResults()[1];  // Maximum LastChgTimestamp column value within the list returned above.

        //----------------------------------------------------------------------
        // Check & see if there are any RAS events that need any work done for them.
        //----------------------------------------------------------------------
        if (vtListRasEventsThatNeedWorkDone.getRowCount() > 0)
        {   // at least 1 ras event needs work done.
            log_.info("Found %d RAS events that need to be finished", vtListRasEventsThatNeedWorkDone.getRowCount());
            // Fill in the CachedJobInfo map.
            int iNumEntriesInCachedJobInfoMap = fillInCachedJobInfoMap(vtListRasEventsThatNeedWorkDoneMaxTs, response);
            // Fill in the list of nodes that are currently being serviced.
            fillInCachedNodesBeingServicedList(response);

            //----------------------------------------------------------------------
            // Loop through each of the RAS events in the list and handle any work they need done.
            //----------------------------------------------------------------------
            for (int iRasEventCntr = 0; iRasEventCntr < vtListRasEventsThatNeedWorkDone.getRowCount(); ++iRasEventCntr) {
                vtListRasEventsThatNeedWorkDone.advanceRow();
                RasEventData event = new RasEventData(vtListRasEventsThatNeedWorkDone);

                //------------------------------------------------------------------
                // Find the appropriate job id for this RAS event.
                //------------------------------------------------------------------
                if ((event.sRasEventJobid != null) && event.sRasEventJobid.equals("?"))
                    event.sRasEventJobid = findJobidForThisEvent(event, iNumEntriesInCachedJobInfoMap);

                //------------------------------------------------------------------
                // Handle the specified ControlOperation for this RAS event.
                //------------------------------------------------------------------
                if (event.sRasEventControlOperation != null)
                    handleControlOperationForThisEvent(event);

                //------------------------------------------------------------------
                // Update the RAS event's JobId, Done, and DbUpdatedTimestamp columns.
                //------------------------------------------------------------------
                String sTempStoredProcedure = "RasEventUpdate";
                final String RasEventDone = "Y";  // indicate that we have finished everything for this ras event.
                String sPertinentInfo = "DescrName=" + event.sRasEventDescrName + ",EventId=" + event.sRasEventId + ",Lctn=" + event.sRasEventLctn + "," +
                                        "JobId=" + event.sRasEventJobid + ",ControlOperation=" + event.sRasEventControlOperation + ",Done=" + RasEventDone;
                adapter_.client().callProcedure(adapter_.createHouseKeepingCallbackNoRtrnValue(adapter_.adapterType(), adapter_.adapterName(), sTempStoredProcedure, sPertinentInfo, workQueue_.workItemId()) // asynchronously invoke the procedure
                                               ,sTempStoredProcedure     // stored procedure name
                                               ,event.sRasEventJobid     // this event's JobId
                                               ,RasEventDone             // flag indicating whether we are finished with this ras event
                                               ,event.sRasEventDescrName // this event's descriptive name
                                               ,event.lRasEventId        // this event's event id
                                               );
                log_.info("Called stored procedure %s - EventDescrName=%s, EventId=%d, JobId=%s, Done=%s",
                          sTempStoredProcedure, event.sRasEventDescrName, event.lRasEventId, event.sRasEventJobid, RasEventDone);
            }   // Loop through each of these RAS events and handle any work they need done.
            log_.info("Finished %d RAS events that needed work done", vtListRasEventsThatNeedWorkDone.getRowCount());

            //----------------------------------------------------------------------
            // Clean up the CachedJobInfo map (map of cached job info) since we are done processing this invocation's data.
            //----------------------------------------------------------------------
            if (iNumEntriesInCachedJobInfoMap > 0) {
                // we did put some entries into the map this iteration - so we need to clean up the map.
                log_.debug("Cleaning up the HashMap of ArrayList<VoltTableRow> ");
                // Remove any entries from each of this map's ArrayLists.
                mCachedJobInfoMap.forEach((key,value)->{
                    //log_.info("Clearing %s ArrayList in the Cached Job Info map", key);
                    value.clear();
                });
                log_.debug("Cleaned  up the HashMap of ArrayList<VoltTableRow> ");
            }

            //----------------------------------------------------------------------
            // Clean up the list of nodes that are currently being serviced since we are done with this iteration's data.
            //----------------------------------------------------------------------
            if (!mNodesInServiceArraylist.isEmpty()) {
                log_.debug("Cleaning up the list of the nodes that are in service");
                mNodesInServiceArraylist.clear();
                log_.debug("Cleaned  up the list of the nodes that are in service");
            }

            //----------------------------------------------------------------------
            // Periodically cleanup/delete expired entries in the InternalCachedJobs table once they are no longer needed
            // (we consider a job as "expired" if it has been more than "NumSecsBeforeJobIsExpired" seconds since the job was marked as terminated).
            // Note: Make sure and use the DbUpdatedTimestamp rather than EndTimestamp as we have sometimes seen delays before the job information appears for us to process!
            //----------------------------------------------------------------------
            final long NumSecsBeforeJobIsExpired = 30L;
            final long NumSecsBetweenChecksForExpiredJobs = 15L;
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
            }   // periodically cleanup/delete expired entries in the InternalCachedJobs table once they are no longer needed.

            //--------------------------------------------------------------
            // Save restart data indicating the current timestamp.
            //--------------------------------------------------------------
            String sRestartData = "Finished the processing for " + vtListRasEventsThatNeedWorkDone.getRowCount() + " RAS events, " + sqlDateFormat.format(new Date());
            workQueue_.saveWorkItemsRestartData(workQueue_.workItemId(), sRestartData, false);  // false means to update this workitem's history record rather than doing an insert of another history record - this is "unusual" (only used when a workitem is updating its working results fields very often)
        }   // at least 1 ras event needs work done.

        // Return an indication of whether or not there was any work done this invocation.
        return vtListRasEventsThatNeedWorkDone.getRowCount();
    }   // End fillInJobIdAndRunControlOps(SimpleDateFormat sqlDateFormat)


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

            controlHandler_ = new ControlApi(adapter_.adapterName(), adapter_.adapterType(), workQueue_, log_);

            //-----------------------------------------------------------------
            // Main processing loop
            //-----------------------------------------------------------------
            while(!adapter_.adapterShuttingDown()) {
                try {
                    // Handle any work items that have been queued for this type of adapter.
                    boolean bGotWorkItem = workQueue_.grabNextAvailWorkItem();
                    if (bGotWorkItem == true) {
                        // did get a work item
                        String[] aWiParms = workQueue_.getClientParameters(Pattern.quote("|"));
                        long rc = -99999;
                        switch(workQueue_.workToBeDone()) {
                            case "HandleFillingInJobIdsAndControlOps":
                                //---------------------------------------------------------
                                // Handles processing of filling in job ids, running control operations, or both while MINIMIZING DB updates (so fewer updates need to flow to Tier2)
                                // Note: This work item is different than most in that this one work item will run for the length of time that the system is active.
                                //       It does not start and stop, it starts and stays active.
                                //---------------------------------------------------------
                                rc = handleFillingInJobIdsAndControlOps();
                                break;
                            default:
                                log_.error("Detected an unexpected WorkToBeDone value of %s", workQueue_.workToBeDone());
                                workQueue_.handleProcessingWhenUnexpectedWorkItem();
                                Thread.sleep( 10 * 1000L );  // sleep 10 secs, so we are not spinning as fast as possible trying to process unknown work.
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
                    adapter_.logRasEventSyncNoEffectedJob("RasGenAdapterExceptionButContinue" // using synchronous version as we are in a flow where we want to ensure that this occurs in a timely manner.
                                                         ,("Exception=" + e)                  // instance data
                                                         ,null                                // Lctn
                                                         ,System.currentTimeMillis() * 1000L  // time that the event that triggered this ras event occurred, in micro-seconds since epoch
                                                         ,adapter_.adapterType()              // type of adapter that is requesting this
                                                         ,workQueue_.baseWorkItemId()         // requesting work item
                                                         );
                }
            }   // End while loop - handle any work items that have been queued for this type of adapter.

            //-----------------------------------------------------------------
            // Clean up adapter table, base work item, and close connections to db.
            //-----------------------------------------------------------------
            adapter_.handleMainlineAdapterCleanup(adapter_.adapterAbnormalShutdown());
        }   // End try (mainProcessingFlow)
        catch (Exception e) {
            adapter_.handleMainlineAdapterException(e);
        }
    }   // End mainProcessingFlow(String[] args)



    public static void main(String[] args) throws IOException {
        Logger logger = LoggerFactory.getInstance("RAS", AdapterRas.class.getName(), "log4j2");
        AdapterSingletonFactory.initializeFactory("RAS", AdapterRas.class.getName(), logger);
        final AdapterRas obj = new AdapterRas(AdapterSingletonFactory.getAdapter(), logger);
        // Start up the main processing flow for RAS adapters.
        obj.mainProcessingFlow(args);
    }   // End main(String[] args)

}   // End class AdapterRas
