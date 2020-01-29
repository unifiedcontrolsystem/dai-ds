// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a node's Boot Image information has been changed.
 *
 *  Input parameter:
 *      String  sNodeLctn               = string containing the Lctn of the node
 *      String  Environment             = environment
 *      long    lTsInMicroSecs          = Time that this occurred
 *      String  sReqAdapterType         = Type of adapter that requested this stored procedure (PROVISIONER)
 *      long    lReqWorkItemId          = Work Item Id that the requesting adapter was performing when it requested this stored procedure (-1 is used when there is no work item yet associated with this change)
 *
 *  Return value:
 *      0L = Everything completed fine, and this record did occur in timestamp order.
 *      1L = Everything completed fine, but as an FYI this record did occur OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
 */

public class ComputeNodeSaveEnvironmentInfo  extends ComputeNodeCommon {

    public final SQLStmt insertNodeHistory = new SQLStmt(
            "INSERT INTO ComputeNode_History " +
                    "(Lctn, SequenceNumber, State, HostName, BootImageId, Environment, IpAddr, MacAddr, " +
                    "BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, " +
                    "LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp, WlmNodeState) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );

    public final SQLStmt updateNode = new SQLStmt("UPDATE ComputeNode SET Environment=?, DbUpdatedTimestamp=?, " +
            "LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE Lctn=?;");

    public long run(String sNodeLctn, String environment, long lTsInMicroSecs, String sReqAdapterType,
                    long lReqWorkItemId) throws VoltAbortException {

        //----------------------------------------------------------------------
        // Grab the current record for this Lctn out of the "active" table (ComputeNode table).
        //      This information is used for determining whether the "new" record is indeed more recent than the record already in the table,
        //      which is necessary as "time is not a stream", and we can get records out of order (from a strict timestamp of occurrence point of view).
        //----------------------------------------------------------------------
        VoltTable[] aNodeData = getComputeNode(sNodeLctn);
        VoltTable nodeData = aNodeData[0];
        if (nodeData.getRowCount() == 0) {
            throw new VoltAbortException("ComputeNodeSaveBootImageInfo - there is no entry in the ComputeNode table for the specified " +
                    "node lctn(" + sNodeLctn + ") - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }
        // Get the current record in the "active" table's LastChgTimestamp (in micro-seconds since epoch).
        nodeData.advanceRow();
        long lCurRecordsTsInMicroSecs = nodeData.getTimestampAsTimestamp("LastChgTimestamp").getTime();

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
            // Get the appropriate record out of the history table that we should use for "filling in" any record data that we want copied from the preceding record.
            aNodeData = getComputeNodeFromHistoryWithPrecedingTime(sNodeLctn, lTsInMicroSecs);
            nodeData = aNodeData[0];
            nodeData.advanceRow();
//            System.out.println("ComputeNodeSaveEnvironmentInfo - " + sNodeLctn + " - OUT OF ORDER" +
//                    " - ThisRecsTsInMicroSecs="   + lTsInMicroSecs           + ", ThisRecsState="   + nodeData.getString("State") +
//                    " - CurRecordsTsInMicroSecs=" + lCurRecordsTsInMicroSecs + ", CurRecordsState=" + sCurRecordsState + "!");
            // Short-circuit if there are no rows in the history table (for this lctn) which are older than the time specified on this request
            // (since there are no entries we are unable to fill in any data in order to complete the row to be inserted).
            if (aNodeData[0].getRowCount() == 0) {
//                System.out.println("ComputeNodeSaveEnvironmentInfo - there is no row in the history table for this lctn (" + sNodeLctn + ") that is older than the time specified on this request, "
//                                  +"ignoring this request - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
                // this new record appeared OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
                return 1L;
            }
        }

        //----------------------------------------------------------------------
        // Update the record for this Lctn in the "active" table.
        //----------------------------------------------------------------------
        if (bUpdateCurrentlyActiveRow) {
            voltQueueSQL(updateNode, environment, this.getTransactionTime(), lTsInMicroSecs, sReqAdapterType, lReqWorkItemId, sNodeLctn);
        }

        //----------------------------------------------------------------------
        // Insert a "history" record for these updates into the history table
        // (this starts with pre-change values and then overlays them with the changes from this invocation).
        //----------------------------------------------------------------------
        voltQueueSQL(insertNodeHistory
                    ,nodeData.getString("Lctn")
                    ,nodeData.getLong("SequenceNumber")
                    ,nodeData.getString("State")
                    ,nodeData.getString("HostName")
                    ,nodeData.getString("BootImageId")
                    ,environment
                    ,nodeData.getString("IpAddr")
                    ,nodeData.getString("MacAddr")
                    ,nodeData.getString("BmcIpAddr")
                    ,nodeData.getString("BmcMacAddr")
                    ,nodeData.getString("BmcHostName")
                    ,this.getTransactionTime()              // DbUpdatedTimestamp
                    ,lTsInMicroSecs                         // LastChgTimestamp
                    ,sReqAdapterType                        // Type of the adapter that made this change
                    ,lReqWorkItemId                         // Work item id that adapter that made this change was working on
                    ,nodeData.getString("Owner")
                    ,nodeData.getString("Aggregator")
                    ,nodeData.getTimestampAsTimestamp("InventoryTimestamp")
                    ,nodeData.getString("WlmNodeState")
                    );

        voltExecuteSQL(true);

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