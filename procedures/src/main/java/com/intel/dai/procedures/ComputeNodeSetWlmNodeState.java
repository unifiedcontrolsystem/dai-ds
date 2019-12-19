// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when setting the WlmNodeState for this node.
 *
 *  Input parameter:
 *      String  sNodeLctn       = string containing the Lctn of the node
 *      String  sNodeNewWlmNodeState   = string containing the value to be put into WlmNodeState.
 *      long    lTsInMicroSecs  = time that this WlmNodeState changed
 *      String  sReqAdapterType = type of adapter that requested this stored procedure (PROVISIONER)
 *      long    lReqWorkItemId  = work Item Id that the requesting adapter was performing when it requested this stored procedure (-1 is used when there is no work item yet associated with this change)
 *
 *  Return value:
 *      0L = Everything completed fine, and this record did occur in timestamp order.
 *      1L = Everything completed fine, but as an FYI this record did occur OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
 */

public class ComputeNodeSetWlmNodeState extends ComputeNodeCommon {

    public final SQLStmt selectNode = new SQLStmt("SELECT * FROM ComputeNode WHERE Lctn=?;");
    public final SQLStmt selectNodeHistoryWithPreceedingTs = new SQLStmt("SELECT * FROM ComputeNode_History WHERE (Lctn=? AND LastChgTimestamp<?) ORDER BY LastChgTimestamp DESC LIMIT 1;");

    public final SQLStmt insertNodeHistory = new SQLStmt(
            "INSERT INTO ComputeNode_History " +
            "(Lctn, SequenceNumber, State, HostName, BootImageId, IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp, WlmNodeState) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );

    public final SQLStmt updateNode = new SQLStmt("UPDATE ComputeNode SET WlmNodeState=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE Lctn=?;");


    public long run(String sNodeLctn, String sNodeNewWlmNodeState, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //----------------------------------------------------------------------
        // Grab the current record for this Lctn out of the "active" table (ComputeNode table).
        //      This information is used for determining whether the "new" record is indeed more recent than the record already in the table,
        //      which is necessary as "time is not a stream", and we can get records out of order (from a strict timestamp of occurrence point of view).
        //----------------------------------------------------------------------
        voltQueueSQL(selectNode, EXPECT_ZERO_OR_ONE_ROW, sNodeLctn);
        VoltTable[] aNodeData = voltExecuteSQL();
        if (aNodeData[0].getRowCount() == 0) {
            throw new VoltAbortException("ComputeNodeSetWlmNodeState - there is no entry in the ComputeNode table for the specified " +
                                         "node lctn(" + sNodeLctn + ") - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }
        // Get the current record in the "active" table's LastChgTimestamp (in micro-seconds since epoch).
        aNodeData[0].advanceRow();
        long lCurRecordsTsInMicroSecs = aNodeData[0].getTimestampAsTimestamp("LastChgTimestamp").getTime();

        //----------------------------------------------------------------------
        // Ensure that we aren't inserting multiple records into history with the same timestamp.
        //      Said differently, check & see if there is already a record in the history table for this lctn and the exact same timestamp, if so bump this timestamp until it is unique.
        //----------------------------------------------------------------------
        lTsInMicroSecs = ensureHaveUniqueComputeNodeLastChgTimestamp(sNodeLctn, lTsInMicroSecs, lCurRecordsTsInMicroSecs);

        //----------------------------------------------------------------------
        // Check & see if this new record has a timestamp that is more recent than the current record for this Lctn in the "active" table.
        //      This is determining whether the "new" record is occurring out of order in regards to the record already in the table,
        //      which is necessary as "time is not a stream", and we can get records out of order (from a strict timestamp of occurrence point of view).
        //----------------------------------------------------------------------
        boolean bUpdateCurrentlyActiveRow = true;
        if (lTsInMicroSecs > lCurRecordsTsInMicroSecs) {
            // this new record has a timestamp that is more recent than the current record for this Lctn in the "active" table (it has appeared in timestamp order).
            bUpdateCurrentlyActiveRow = true;  // indicate that we do want to update the record in the currently active row (in addition to inserting into the history table).
        }
        else {
            // this new record has a timestamp that is OLDER than the current record for this Lctn in the "active" table (it has appeared OUT OF timestamp order).
            bUpdateCurrentlyActiveRow = false;  // indicate that we do NOT want to update the record in the currently active row (only want to insert into the history table).
            String sCurRecordsWlmNodeState = aNodeData[0].getString("WlmNodeState");
            // Get the appropriate record out of the history table that we should use for "filling in" any record data that we want copied from the preceding record.
            voltQueueSQL(selectNodeHistoryWithPreceedingTs, sNodeLctn, lTsInMicroSecs);
            aNodeData = voltExecuteSQL();
            aNodeData[0].advanceRow();
//            System.out.println("ComputeNodeSetWlmNodeState - " + sNodeLctn + " - OUT OF ORDER" +
//                               " - ThisRecsTsInMicroSecs="   + lTsInMicroSecs           + ", ThisRecsWlmNodeState=" + sNodeNewWlmNodeState +
//                               " - CurRecordsTsInMicroSecs=" + lCurRecordsTsInMicroSecs + ", CurRecsWlmNodeState="  + sCurRecordsWlmNodeState + "!");
            // Short-circuit if there are no rows in the history table (for this lctn) which are older than the time specified on this request
            // (since there are no entries we are unable to fill in any data in order to complete the row to be inserted).
            if (aNodeData[0].getRowCount() == 0) {
//                System.out.println("ComputeNodeSetWlmNodeState - there is no row in the history table for this lctn (" + sNodeLctn + ") that is older than the time specified on this request, "
//                                  +"ignoring this request - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
                // this new record appeared OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
                return 1L;
            }
        }

        //----------------------------------------------------------------------
        // Check and see if the record already has the "new" state that was specified by the caller.
        //----------------------------------------------------------------------
        if (!aNodeData[0].getString("WlmNodeState").equals(sNodeNewWlmNodeState)) {
            // this is a state transition, so we need to go ahead and update the appropriate data store rows (we do not want to update the data store if there was no transition).
            //----------------------------------------------------------------------
            // Update the record for this Lctn in the "active" table.
            //----------------------------------------------------------------------
            if (bUpdateCurrentlyActiveRow) {
                voltQueueSQL(updateNode, sNodeNewWlmNodeState, this.getTransactionTime(), lTsInMicroSecs, sReqAdapterType, lReqWorkItemId, sNodeLctn);
            }
            //----------------------------------------------------------------------
            // Insert a "history" record for these updates into the history table
            // (this starts with pre-change values and then overlays them with the changes from this invocation).
            //----------------------------------------------------------------------
            voltQueueSQL(insertNodeHistory
                        ,aNodeData[0].getString("Lctn")
                        ,aNodeData[0].getLong("SequenceNumber")
                        ,aNodeData[0].getString("State")
                        ,aNodeData[0].getString("HostName")
                        ,aNodeData[0].getString("BootImageId")
                        ,aNodeData[0].getString("IpAddr")
                        ,aNodeData[0].getString("MacAddr")
                        ,aNodeData[0].getString("BmcIpAddr")
                        ,aNodeData[0].getString("BmcMacAddr")
                        ,aNodeData[0].getString("BmcHostName")
                        ,this.getTransactionTime()                  // DbUpdatedTimestamp
                        ,lTsInMicroSecs                             // LastChgTimestamp
                        ,sReqAdapterType
                        ,lReqWorkItemId
                        ,aNodeData[0].getString("Owner")
                        ,aNodeData[0].getString("Aggregator")
                        ,aNodeData[0].getTimestampAsTimestamp("InventoryTimestamp")
                        ,sNodeNewWlmNodeState                       // New WlmNodeState
                        );
            voltExecuteSQL(true);
        }   // this is a state transition, so we need to go ahead and update the appropriate data store rows (we do not want to update the data store if there was no transition).
        else {
            // there was no state transition (the state did not change), so we do not want to do any data store updates.
//            System.out.println("ComputeNodeSetWlmNodeState - " + sNodeLctn + " - no state transition occurred, so no update to the data store is being done" +
//                               " - DsWlmNodeState=" + aNodeData[0].getString("WlmNodeState") + ", NewWlmNodeState="  + sNodeNewWlmNodeState);
        }


        //----------------------------------------------------------------------
        // Return to caller with indication of whether or not this record occurred in timestamp order or out of order.
        //----------------------------------------------------------------------
        if (bUpdateCurrentlyActiveRow)
            // this new record appeared in timestamp order (in comparison to the other records for this lctn).
            return 0L;
        else
            // this new record appeared OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
            return 1L;
    }
}
