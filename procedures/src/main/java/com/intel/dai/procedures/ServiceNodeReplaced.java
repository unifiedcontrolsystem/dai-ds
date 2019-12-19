// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltTable;

/**
 * Handle the database processing that is necessary when a node has been removed.
 *
 *  Input parameter:
 *      String  sNodeLctn           = String containing the Lctn of the node
 *      String  sNewSernum          = Serial number of the new node being put into this lctn
 *      String  sFruType            = FRU type field (e.g. a hardware type identifier or a model number)
 *      String  sParmState          = State of node (if null then use existing state from db)
 *      String  sInventoryInfo      = Additional inventory details provided by
 *      long    lTsInMicroSecs      = Time that this operation was reported
 *      String  sReqAdapterType     = Type of adapter that requested this stored procedure (PROVISIONER)
 *      long    lReqWorkItemId      = Work Item Id that the requesting adapter was performing when it requested this stored procedure (-1 is used when there is no work item yet associated with this change)
 *
 *  Return value:
 *      0L = Everything completed fine, and this record did occur in timestamp order.
 *      1L = Everything completed fine, but as an FYI this record did occur OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
 */

public class ServiceNodeReplaced extends ServiceNodeCommon {

    public final SQLStmt updateNode = new SQLStmt(
            "UPDATE ServiceNode " +
            "SET State=?, InventoryTimestamp=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? " +
            "WHERE Lctn=?;"
    );


    public final SQLStmt insertNodeHistory = new SQLStmt(
            "INSERT INTO ServiceNode_History(" +
            "Lctn, SequenceNumber, State, HostName, BootImageId, IpAddr, " +
            "MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp, LastChgTimestamp, " +
            "LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);"
    );


    public final SQLStmt selectNodeInventoryInfo = new SQLStmt("SELECT Sernum FROM NodeInventory_History WHERE (Lctn=? AND InventoryTimestamp=?);");

    public final SQLStmt insertInventoryHistory = new SQLStmt(
            "INSERT INTO NodeInventory_History(Lctn, DbUpdatedTimestamp, InventoryTimestamp, InventoryInfo, Sernum) " +
            "VALUES(?,?,?,?,?);"
    );


    public final SQLStmt insertReplacementHistory = new SQLStmt(
            "INSERT INTO Replacement_History(Lctn, FruType, OldSernum, NewSernum, OldState, NewState, DbUpdatedTimestamp, LastChgTimestamp) " +
            "VALUES(?,?,?,?,?,?,?,?)"
    );



    public long run(String sNodeLctn, String sNewSernum, String sFruType, String sParmState, String sInventoryInfo,
                    long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
                throws VoltAbortException
    {
        if (sNewSernum == null) {
            throw new VoltAbortException("ServiceNodeReplaced - no serial number was provided for the " +
                    "specified node lctn(" + sNodeLctn + ") - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" +
                    lReqWorkItemId + "!");
        }

        //----------------------------------------------------------------------
        // Grab the current record for this Lctn out of the "active" table (ServiceNode table).
        //      This information is used for determining whether the "new" record is indeed more recent than the record
        //      already in the table, which is necessary as "time is not a stream", and we can get records out of order
        //      (from a strict timestamp of occurrence point of view).
        //----------------------------------------------------------------------
        VoltTable[] aNodeData = getServiceNode(sNodeLctn);
        VoltTable nodeData = aNodeData[0];
        if (nodeData.getRowCount() == 0) {
            throw new VoltAbortException("ServiceNodeReplaced - there is no entry in the ServiceNode table for the " +
                    "specified node lctn(" + sNodeLctn + ") - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" +
                    lReqWorkItemId + "!");
        }
        // Get the current record in the "active" table's LastChgTimestamp (in micro-seconds since epoch).
        nodeData.advanceRow();
        long lCurRecordsTsInMicroSecs = nodeData.getTimestampAsTimestamp("LastChgTimestamp").getTime();
        // Fill in node's existing state if the caller did not specify an explicit value.
        String sState;
        if (sParmState == null)
            sState = nodeData.getString("State");
        else
            sState = sParmState;

        //----------------------------------------------------------------------
        // Ensure that we aren't inserting multiple records into history with the same timestamp.
        //      Said differently, check & see if there is already a record in the history table for this lctn and the
        //      exact same timestamp, if so bump this timestamp until it is unique.
        //----------------------------------------------------------------------
        lTsInMicroSecs = ensureHaveUniqueServiceNodeLastChgTimestamp(sNodeLctn, lTsInMicroSecs, lCurRecordsTsInMicroSecs);

        //----------------------------------------------------------------------
        // Check & see if this new record has a timestamp that is more recent than the current record for this Lctn in
        //      the "active" table.  This is determining whether the "new" record is occurring out of order in regards
        //      to the record already in the table, which is necessary as "time is not a stream", and we can get
        //      records out of order (from a strict timestamp of occurrence point of view).
        //----------------------------------------------------------------------
        boolean bUpdateCurrentlyActiveRow = true;
        if (lTsInMicroSecs < lCurRecordsTsInMicroSecs) {
            // this new record has a timestamp that is OLDER than the current record for this Lctn in the "active"
            // table (it has appeared OUT OF timestamp order).

            // indicate that we do NOT want to update the record in the currently active row (only want to insert into
            // the history table).
            bUpdateCurrentlyActiveRow = false;
            String sCurRecordsState = nodeData.getString("State");
            // Get the appropriate record out of the history table that we should use for "filling in" any record data
            // that we want copied from the preceding record.
            aNodeData = getServiceNodeFromHistoryWithPrecedingTime(sNodeLctn, lTsInMicroSecs);
            nodeData = aNodeData[0];
            nodeData.advanceRow();
            // Fill in node's existing state (from the history record) if the caller did not specify an explicit value.
            if (sParmState == null)
                sState = nodeData.getString("State");
//            System.out.println("ServiceNodeReplaced - " + sNodeLctn + " - OUT OF ORDER" +
//                    " - ThisRecsTsInMicroSecs="   + lTsInMicroSecs           + ", ThisRecsState="   + sState +
//                    " - CurRecordsTsInMicroSecs=" + lCurRecordsTsInMicroSecs + ", CurRecordsState=" +
//                    sCurRecordsState + "!");
            // Short-circuit if there are no rows in the history table (for this lctn) which are older than the time specified on this request
            // (since there are no entries we are unable to fill in any data in order to complete the row to be inserted).
            if (aNodeData[0].getRowCount() == 0) {
//                System.out.println("ServiceNodeReplaced - there is no row in the history table for this lctn (" + sNodeLctn + ") that is older than the time specified on this request, "
//                                  +"ignoring this request - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
                // this new record appeared OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
                return 1L;
            }
        }


        //----------------------------------------------------------------------
        // Get the "old" serial number out of the appropriate node inventory entry.
        //----------------------------------------------------------------------
        String sOldSernum = null;
        voltQueueSQL(selectNodeInventoryInfo, EXPECT_ZERO_OR_ONE_ROW, sNodeLctn, nodeData.getTimestampAsTimestamp("InventoryTimestamp"));
        VoltTable[] aNodeInvData = voltExecuteSQL();
        VoltTable nodeInvInfo = aNodeInvData[0];
        if (nodeInvInfo.getRowCount() != 0) {
            nodeInvInfo.advanceRow();
            sOldSernum = nodeInvInfo.getString("Sernum");
        }


        //----------------------------------------------------------------------
        // Update the record for this Lctn in the "active" table.
        //----------------------------------------------------------------------
        if (bUpdateCurrentlyActiveRow) {
            voltQueueSQL(updateNode
                        ,sState                     // State
                        ,lTsInMicroSecs             // InventoryTimestamp
                        ,this.getTransactionTime()  // DbUpdatedTimestamp
                        ,lTsInMicroSecs             // LastChgTimestamp
                        ,sReqAdapterType            // LastChgAdapterType
                        ,lReqWorkItemId             // LastChgWorkItemId
                        ,sNodeLctn                  // Lctn
                        );
        }

        //----------------------------------------------------------------------
        // Insert a "history" record for this node's inventory information.
        //----------------------------------------------------------------------
        voltQueueSQL(insertInventoryHistory
                    ,sNodeLctn                  // Lctn
                    ,this.getTransactionTime()  // DbUpdatedTimestamp
                    ,lTsInMicroSecs             // InventoryTimestamp
                    ,sInventoryInfo             // InventoryInfo
                    ,sNewSernum                 // Sernum
                    );

        //----------------------------------------------------------------------
        // Insert a "history" record for these updates into the history table
        // (this starts with pre-change values and then overlays them with the changes from this invocation).
        //----------------------------------------------------------------------
        voltQueueSQL(insertNodeHistory
                    ,nodeData.getString("Lctn")
                    ,nodeData.getLong("SequenceNumber")
                    ,sState                             // State
                    ,nodeData.getString("HostName")
                    ,nodeData.getString("BootImageId")
                    ,nodeData.getString("IpAddr")
                    ,nodeData.getString("MacAddr")
                    ,nodeData.getString("BmcIpAddr")
                    ,nodeData.getString("BmcMacAddr")
                    ,nodeData.getString("BmcHostName")
                    ,this.getTransactionTime()          // DbUpdatedTimestamp
                    ,lTsInMicroSecs                     // LastChgTimestamp
                    ,sReqAdapterType
                    ,lReqWorkItemId
                    ,nodeData.getString("Owner")
                    ,nodeData.getString("Aggregator")
                    ,lTsInMicroSecs                     // InventoryTimestamp
                    );

        //----------------------------------------------------------------------
        // Record this as a replacement (if the new FRU is different that the previous one)
        // - Otherwise just continue without logging this as a replacement.
        //----------------------------------------------------------------------
        if (nodeData.wasNull() || !sNewSernum.equals(sOldSernum)) {
            // Note: service operation ID is left null for now as this stored procedure is meant for handling the use
            // case where UCS monitors passively for inventory changes. However, this could be adapted to also work for
            // the service operations.
            voltQueueSQL(insertReplacementHistory
                        ,sNodeLctn
                        ,sFruType
                        ,sOldSernum                   // Old serial number
                        ,sNewSernum                   // New serial number
                        ,nodeData.getString("State")  // Old state
                        ,sState                       // State
                        ,this.getTransactionTime()    // DbUpdatedTimestamp
                        ,lTsInMicroSecs               // LastChgTimestamp
                        );
        }
        voltExecuteSQL(true);

        if (bUpdateCurrentlyActiveRow)
            // this new record appeared in timestamp order (in comparison to the other records for this lctn).
            return 0L;
        else
            // this new record appeared OUT OF timestamp order (at least 1 record has already appeared with a more
            // recent timestamp, i.e., newer than the timestamp for this record).
            return 1L;
    }
}
