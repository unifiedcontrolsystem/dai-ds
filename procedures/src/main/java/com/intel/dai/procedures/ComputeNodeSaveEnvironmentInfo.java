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
 *      String  sNewEnvironment         = string containing the new environment value for this node
 *      long    lNewRecsLastChgTimestampTsInMicroSecs = Time that this occurred
 *      String  sReqAdapterType         = Type of adapter that requested this stored procedure (PROVISIONER)
 *      long    lReqWorkItemId          = Work Item Id that the requesting adapter was performing when it requested this stored procedure (-1 is used when there is no work item yet associated with this change)
 *
 *  Return value:
 *      0L = Everything completed fine, and this record did occur in timestamp order.
 *      1L = This record occurred OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
 *      2L = This record occurred OUT OF timestamp order but within a reasonable time range (this appears to have been a 'time is not a river' occurrence.
 */

public class ComputeNodeSaveEnvironmentInfo  extends ComputeNodeCommon {

    public final SQLStmt insertNodeHistory = new SQLStmt(
            "INSERT INTO ComputeNode_History " +
                    "(Lctn, SequenceNumber, State, HostName, BootImageId, Environment, IpAddr, MacAddr, " +
                    "BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, " +
                    "LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp, WlmNodeState, ConstraintId, ProofOfLifeTimestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );
    public final SQLStmt updateNode = new SQLStmt (
            "UPDATE ComputeNode SET Environment=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE Lctn=?;"
    );


    void insertRecordIntoHistoryTable(String sNodeLctn, VoltTable nodeData, String sNewEnvironment, long lNewRecsLastChgTimestampTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        voltQueueSQL(insertNodeHistory
                    ,nodeData.getString("Lctn")
                    ,nodeData.getLong("SequenceNumber")
                    ,nodeData.getString("State")
                    ,nodeData.getString("HostName")
                    ,nodeData.getString("BootImageId")
                    ,sNewEnvironment                        // Environment
                    ,nodeData.getString("IpAddr")
                    ,nodeData.getString("MacAddr")
                    ,nodeData.getString("BmcIpAddr")
                    ,nodeData.getString("BmcMacAddr")
                    ,nodeData.getString("BmcHostName")
                    ,this.getTransactionTime()              // DbUpdatedTimestamp
                    ,lNewRecsLastChgTimestampTsInMicroSecs  // LastChgTimestamp
                    ,sReqAdapterType                        // Type of the adapter that made this change
                    ,lReqWorkItemId                         // Work item id that adapter that made this change was working on
                    ,nodeData.getString("Owner")
                    ,nodeData.getString("Aggregator")
                    ,nodeData.getTimestampAsTimestamp("InventoryTimestamp")
                    ,nodeData.getString("WlmNodeState")
                    ,nodeData.getString("ConstraintId")
                    ,nodeData.getTimestampAsTimestamp("ProofOfLifeTimestamp")
                    );
    }   // End insertRecordIntoHistoryTable(String sNodeLctn, VoltTable nodeData, String sNewEnvironment, long lNewRecsLastChgTimestampTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)


    public long run(String sNodeLctn, String sNewEnvironment, long lNewRecsLastChgTimestampTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException
    {
        final long ReasonableAmtOfTimeInMicroSecs = (1 * 1000 * 1000) + (500 * 1000);  // 1.5 secs in microseconds.
        long lRc = 0;
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
        long lCurRecsLastChgTimestampTsInMicroSecs = nodeData.getTimestampAsTimestamp("LastChgTimestamp").getTime();

        //----------------------------------------------------------------------
        // Ensure that we don't use a LastChgTimestamp for this new record, that already exists for this Lctn w/i the database.
        //----------------------------------------------------------------------
        if (lNewRecsLastChgTimestampTsInMicroSecs <= lCurRecsLastChgTimestampTsInMicroSecs)
            lNewRecsLastChgTimestampTsInMicroSecs = ensureHaveUniqueComputeNodeLastChgTimestamp(sNodeLctn, lNewRecsLastChgTimestampTsInMicroSecs, lCurRecsLastChgTimestampTsInMicroSecs);

        //----------------------------------------------------------------------
        // Check & see if this new record has a timestamp that is more recent than the LastChgTimestamp for the current record for this Lctn in the "active" table.
        //      This is determining whether the "new" record is occurring out of order in regards to the record already in the table,
        //      this check is necessary as "time is not a river", and we may get records which are out of order (from a strict timestamp of occurrence point of view).
        //----------------------------------------------------------------------
        if (lNewRecsLastChgTimestampTsInMicroSecs > lCurRecsLastChgTimestampTsInMicroSecs) {
            // this new record is newer than the current db record's LastChgTimestamp (it has appeared in timestamp order).
            // Update the current db record's fields for this lctn.
            voltQueueSQL(updateNode, sNewEnvironment, this.getTransactionTime(), lNewRecsLastChgTimestampTsInMicroSecs, sReqAdapterType, lReqWorkItemId, sNodeLctn);
            // Insert this record into the history table (this starts with previous record's values and then overlays them with the changes from this record).
            insertRecordIntoHistoryTable(sNodeLctn, nodeData, sNewEnvironment, lNewRecsLastChgTimestampTsInMicroSecs, sReqAdapterType, lReqWorkItemId);
            lRc = 0L;  // Everything completed fine, and this record did occur in timestamp order.
        }
        else {
            // this new record has a timestamp that is OLDER than the current record for this Lctn in the "active" table (it has appeared OUT OF timestamp order).
            //------------------------------------------------------------------
            // Get the appropriate record out of the history table that we should use for "filling in" any record data that we want copied from the preceding record.
            //------------------------------------------------------------------
            aNodeData = getComputeNodeFromHistoryWithPrecedingTime(sNodeLctn, lNewRecsLastChgTimestampTsInMicroSecs);
            // Short-circuit if there are no rows in the history table (for this lctn) which are older than the time specified on this request
            // (since there are no entries we are unable to fill in any data in order to complete the row to be inserted).
            if (aNodeData[0].getRowCount() == 0) {
                System.out.println("ComputeNodeSaveEnvironmentInfo - there is no row in the history table for this lctn (" + sNodeLctn + ") that is older than the time specified on this request, "
                                  +"ignoring this request - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
                // Short-circuit as this new record appeared OUT OF timestamp order and there is no previous history record (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
                return 1L;
            }
            // Overlay the current record's db row data with the "preceding" history record's data so the history record can have the preceding data.
            nodeData = aNodeData[0];
            nodeData.advanceRow();
            //------------------------------------------------------------------
            // Record the appropriate data in the database.
            //------------------------------------------------------------------
            if ((lCurRecsLastChgTimestampTsInMicroSecs - lNewRecsLastChgTimestampTsInMicroSecs) <= ReasonableAmtOfTimeInMicroSecs) {
                // this new record's timestamp is older than but within a reasonable amt of time of the current db record's LastChgTimestamp (assume it was simply a 'time is not a river' occurrence).
                // Update certain fields in the current db record (don't change the LastChgTimestamp).
                voltQueueSQL(updateNode, sNewEnvironment, this.getTransactionTime(), lCurRecsLastChgTimestampTsInMicroSecs, sReqAdapterType, lReqWorkItemId, sNodeLctn);
                // Insert this record into the history table (this starts with previous record's values and then overlays them with the changes from this record).
                insertRecordIntoHistoryTable(sNodeLctn, nodeData, sNewEnvironment, lNewRecsLastChgTimestampTsInMicroSecs, sReqAdapterType, lReqWorkItemId);
                lRc = 2L;  // This record occurred OUT OF timestamp order but within a reasonable time range (this appears to have been a 'time is not a river' occurrence.
            }
            else {
                // this new records timestamp is more than a reasonable amount older than the current db record's LastChgTimestamp.
                // Do NOT change anything in the current db record!!
                // Insert this record into the history table (this starts with previous record's values and then overlays them with the changes from this record).
                insertRecordIntoHistoryTable(sNodeLctn, nodeData, sNewEnvironment, lNewRecsLastChgTimestampTsInMicroSecs, sReqAdapterType, lReqWorkItemId);
                lRc = 1L;  // This record occurred OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
            }
        }

        voltExecuteSQL(true);

        //----------------------------------------------------------------------
        // Return to caller with indication of whether or not this record occurred in timestamp order or out of order.
        //----------------------------------------------------------------------
        return lRc;
    }

}