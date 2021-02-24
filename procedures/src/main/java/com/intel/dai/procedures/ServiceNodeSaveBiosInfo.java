// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.dai.procedures;
import org.voltdb.SQLStmt;
import org.voltdb.VoltTable;

/**
 * Handle the database processing that is necessary when a node has been removed.
 *
 *  Input parameter:
 *      String  sParmNodeLctn                         = String containing the Lctn of the node
 *      String  sParmBiosInfo                         = New bios information
 *      long    lNewRecsLastChgTimestampTsInMicroSecs = Time that this operation was reported
 *      String  sReqAdapterType                       = Type of adapter that requested this stored procedure (PROVISIONER)
 *      long    lReqWorkItemId                        = Work Item Id that the requesting adapter was performing when it requested this stored procedure (-1 is used when there is no work item yet associated with this change)
 *
 *  Return value:
 *      0L = Everything completed fine, and this record did occur in timestamp order.
 *      1L = This record occurred OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
 *      2L = This record occurred OUT OF timestamp order but within a reasonable time range (this appears to have been a 'time is not a river' occurrence.
 */

public class ServiceNodeSaveBiosInfo extends ServiceNodeCommon {

    public final SQLStmt updateNode = new SQLStmt(
            "UPDATE ServiceNode " +
            "SET InventoryTimestamp=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? " +
            "WHERE Lctn=?;"
    );

    public final SQLStmt selectNodeInventoryInfo = new SQLStmt("SELECT * FROM NodeInventory_History WHERE (Lctn=? AND InventoryTimestamp=?);");

    public final SQLStmt insertNodeInventoryHistory = new SQLStmt(
            "INSERT INTO NodeInventory_History(Lctn, DbUpdatedTimestamp, InventoryTimestamp, InventoryInfo, Sernum, BiosInfo) " +
            "VALUES(?,?,?,?,?,?);"
    );

    public final SQLStmt insertNodeHistory = new SQLStmt(
            "INSERT INTO ServiceNode_History(" +
            "Lctn, SequenceNumber, State, HostName, BootImageId, IpAddr, " +
            "MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp, LastChgTimestamp, " +
            "LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp, ConstraintId, ProofOfLifeTimestamp) " +
            "VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?);"
    );


    // Update the record for this Lctn in the "active" table.
    void updateRecordInActiveNodeTable(String sNodeLctn, long lNewInvTsInMicroSecs, long lNewLastChgTsTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        voltQueueSQL(updateNode
                    ,lNewInvTsInMicroSecs       // InventoryTimestamp
                    ,this.getTransactionTime()  // DbUpdatedTimestamp
                    ,lNewLastChgTsTsInMicroSecs // LastChgTimestamp
                    ,sReqAdapterType            // LastChgAdapterType
                    ,lReqWorkItemId             // LastChgWorkItemId
                    ,sNodeLctn                  // Lctn
                    );
    }   // End updateRecordInActiveNodeTable(String sNodeLctn, long lNewInvTsInMicroSecs, long lNewLastChgTsTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)


    // Insert a "history" record for this node's inventory information.
    void insertRecordIntoInventoryHistoryTable(String sNodeLctn, long lNewRecsLastChgTimestampTsInMicroSecs, String sInventoryInfo, String sNewSernum, String sBiosInfo)
    {
        voltQueueSQL(insertNodeInventoryHistory
                    ,sNodeLctn                              // Lctn
                    ,this.getTransactionTime()              // DbUpdatedTimestamp
                    ,lNewRecsLastChgTimestampTsInMicroSecs  // InventoryTimestamp
                    ,sInventoryInfo                         // InventoryInfo
                    ,sNewSernum                             // Sernum
                    ,sBiosInfo                              // BiosInfo
                    );
    }   // End insertRecordIntoInventoryHistoryTable(String sNodeLctn, long lNewRecsLastChgTimestampTsInMicroSecs, String sInventoryInfo, String sNewSernum, String sBiosInfo)


    // Insert a "history" record for these updates into the history table
    // (this starts with pre-change values and then overlays them with the changes from this invocation).
    void insertRecordIntoNodeHistoryTable(VoltTable nodeData, long lNewRecsLastChgTimestampTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
    {
        voltQueueSQL(insertNodeHistory
                    ,nodeData.getString("Lctn")
                    ,nodeData.getLong("SequenceNumber")
                    ,nodeData.getString("State")
                    ,nodeData.getString("HostName")
                    ,nodeData.getString("BootImageId")
                    ,nodeData.getString("IpAddr")
                    ,nodeData.getString("MacAddr")
                    ,nodeData.getString("BmcIpAddr")
                    ,nodeData.getString("BmcMacAddr")
                    ,nodeData.getString("BmcHostName")
                    ,this.getTransactionTime()                                  // DbUpdatedTimestamp
                    ,lNewRecsLastChgTimestampTsInMicroSecs                      // LastChgTimestamp
                    ,sReqAdapterType
                    ,lReqWorkItemId
                    ,nodeData.getString("Owner")
                    ,nodeData.getString("Aggregator")
                    ,lNewRecsLastChgTimestampTsInMicroSecs                      // InventoryTimestamp
                    ,nodeData.getString("ConstraintId")
                    ,nodeData.getTimestampAsTimestamp("ProofOfLifeTimestamp")
                    );
    }   // End insertRecordIntoNodeHistoryTable(VoltTable nodeData, long lNewRecsLastChgTimestampTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)



    public long run(String sParmNodeLctn, String sParmBiosInfo, long lNewRecsLastChgTimestampTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
                throws VoltAbortException
    {
        final long ReasonableAmtOfTimeInMicroSecs = (1 * 1000 * 1000) + (500 * 1000);  // 1.5 secs in microseconds.
        long lRc = -99999L;

        //----------------------------------------------------------------------
        // Grab the current record for this Lctn out of the "active" table (ServiceNode table).
        //      This information is used for determining whether the "new" record is indeed more recent than the record
        //      already in the table, which is necessary as "time is not a stream", and we can get records out of order
        //      (from a strict timestamp of occurrence point of view).
        //----------------------------------------------------------------------
        VoltTable[] aNodeData = getServiceNode(sParmNodeLctn);
        VoltTable nodeData = aNodeData[0];
        if (nodeData.getRowCount() == 0) {
            throw new VoltAbortException("ServiceNodeSaveBiosInfo - there is no entry in the ServiceNode table for the " +
                    "specified node lctn(" + sParmNodeLctn + ") - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" +
                    lReqWorkItemId + "!");
        }
        // Get the current record in the "active" table's LastChgTimestamp (in micro-seconds since epoch).
        nodeData.advanceRow();
        long lCurRecsLastChgTimestampTsInMicroSecs = nodeData.getTimestampAsTimestamp("LastChgTimestamp").getTime();

        //----------------------------------------------------------------------
        // Ensure that we don't use a LastChgTimestamp for this new record, that already exists for this Lctn w/i the database.
        //----------------------------------------------------------------------
        if (lNewRecsLastChgTimestampTsInMicroSecs <= lCurRecsLastChgTimestampTsInMicroSecs)
            lNewRecsLastChgTimestampTsInMicroSecs = ensureHaveUniqueServiceNodeLastChgTimestamp(sParmNodeLctn, lNewRecsLastChgTimestampTsInMicroSecs, lCurRecsLastChgTimestampTsInMicroSecs);

        //----------------------------------------------------------------------
        // Check & see if this new record has a timestamp that is more recent than the LastChgTimestamp for the current record for this Lctn in the "active" table.
        //      This is determining whether the "new" record is occurring out of order in regards to the record already in the table,
        //      this check is necessary as "time is not a river", and we may get records which are out of order (from a strict timestamp of occurrence point of view).
        //----------------------------------------------------------------------
        if (lNewRecsLastChgTimestampTsInMicroSecs > lCurRecsLastChgTimestampTsInMicroSecs) {
            // this new record is newer than the current db record's LastChgTimestamp (it has appeared in timestamp order).
            // Get the pertinent "old" node inventory information.
            String sOldInventoryInfo = null;
            String sOldSernum = null;
            voltQueueSQL(selectNodeInventoryInfo, EXPECT_ZERO_OR_ONE_ROW, sParmNodeLctn, nodeData.getTimestampAsTimestamp("InventoryTimestamp"));
            VoltTable[] aNodeInvData = voltExecuteSQL();
            VoltTable nodeInvInfo = aNodeInvData[0];
            if (nodeInvInfo.getRowCount() != 0) {
                nodeInvInfo.advanceRow();
                sOldInventoryInfo = nodeInvInfo.getString("InventoryInfo");
                sOldSernum = nodeInvInfo.getString("Sernum");
            }
            // Update the current db record's fields for this lctn.
            updateRecordInActiveNodeTable(sParmNodeLctn, lNewRecsLastChgTimestampTsInMicroSecs, lNewRecsLastChgTimestampTsInMicroSecs, sReqAdapterType, lReqWorkItemId);
            // Insert a "history" record for this node's inventory information.
            insertRecordIntoInventoryHistoryTable(sParmNodeLctn, lNewRecsLastChgTimestampTsInMicroSecs, sOldInventoryInfo, sOldSernum, sParmBiosInfo);
            // Insert this record into the node's history table (this starts with previous record's values and then overlays them with the changes from this record).
            insertRecordIntoNodeHistoryTable(nodeData, lNewRecsLastChgTimestampTsInMicroSecs, sReqAdapterType, lReqWorkItemId);
            // Everything completed fine, and this record did occur in timestamp order.
            lRc = 0L;
        }
        else {
            // this new record has a timestamp that is OLDER than the current record for this Lctn in the "active" table (it has appeared OUT OF timestamp order).
            //------------------------------------------------------------------
            // Get the appropriate record out of the history table that we should use for "filling in" any record data that we want copied from the preceding record.
            //------------------------------------------------------------------
            aNodeData = getServiceNodeFromHistoryWithPrecedingTime(sParmNodeLctn, lNewRecsLastChgTimestampTsInMicroSecs);
            // Short-circuit if there are no rows in the history table (for this lctn) which are older than the time specified on this request
            // (since there are no entries we are unable to fill in any data in order to complete the row to be inserted).
            if (aNodeData[0].getRowCount() == 0) {
                System.out.println("ServiceNodeSaveBiosInfo - there is no row in the node history table for this lctn (" + sParmNodeLctn + ") that is older than the time specified on this request, "
                                  +"ignoring this request - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
                // Short-circuit as this new record appeared OUT OF timestamp order and there is no previous history record (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
                return 1L;
            }
            // Overlay the current record's db row data with the "preceding" history record's data so the history record can have the preceding data.
            nodeData = aNodeData[0];
            nodeData.advanceRow();
            // Get the pertinent "old" node inventory information.
            String sOldInventoryInfo = null;
            String sOldSernum = null;
            voltQueueSQL(selectNodeInventoryInfo, EXPECT_ZERO_OR_ONE_ROW, sParmNodeLctn, nodeData.getTimestampAsTimestamp("InventoryTimestamp"));
            VoltTable[] aNodeInvData = voltExecuteSQL();
            VoltTable nodeInvInfo = aNodeInvData[0];
            if (nodeInvInfo.getRowCount() != 0) {
                nodeInvInfo.advanceRow();
                sOldInventoryInfo = nodeInvInfo.getString("InventoryInfo");
                sOldSernum = nodeInvInfo.getString("Sernum");
            }
            //------------------------------------------------------------------
            // Record the appropriate data in the database.
            //------------------------------------------------------------------
            if ((lCurRecsLastChgTimestampTsInMicroSecs - lNewRecsLastChgTimestampTsInMicroSecs) <= ReasonableAmtOfTimeInMicroSecs) {
                // this new record's timestamp is older than but within a reasonable amt of time of the current db record's LastChgTimestamp (assume it was simply a 'time is not a river' occurrence).
                // Update certain fields in the current db record (don't change the LastChgTimestamp nor the State).
                updateRecordInActiveNodeTable(sParmNodeLctn, lNewRecsLastChgTimestampTsInMicroSecs, lCurRecsLastChgTimestampTsInMicroSecs, sReqAdapterType, lReqWorkItemId);
                // Insert a "history" record for this node's inventory information.
                insertRecordIntoInventoryHistoryTable(sParmNodeLctn, lNewRecsLastChgTimestampTsInMicroSecs, sOldInventoryInfo, sOldSernum, sParmBiosInfo);
                // Insert this record into the node's history table (this starts with previous record's values and then overlays them with the changes from this record).
                insertRecordIntoNodeHistoryTable(nodeData, lNewRecsLastChgTimestampTsInMicroSecs, sReqAdapterType, lReqWorkItemId);
                // This record occurred OUT OF timestamp order but within a reasonable time range (this appears to have been a 'time is not a river' occurrence.
                lRc = 2L;
            }
            else {
                // this new records timestamp is more than a reasonable amount older than the current db record's LastChgTimestamp.
                // Do NOT change anything in the current db record!!
                // Insert a "history" record for this node's inventory information.
                insertRecordIntoInventoryHistoryTable(sParmNodeLctn, lNewRecsLastChgTimestampTsInMicroSecs, sOldInventoryInfo, sOldSernum, sParmBiosInfo);
                // Insert this record into the node's history table (this starts with previous record's values and then overlays them with the changes from this record).
                insertRecordIntoNodeHistoryTable(nodeData, lNewRecsLastChgTimestampTsInMicroSecs, sReqAdapterType, lReqWorkItemId);
                // This record occurred OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
                lRc = 1L;
            }
        }

        voltExecuteSQL(true);

        //----------------------------------------------------------------------
        // Return to caller with indication of whether or not this record occurred in timestamp order or out of order.
        //----------------------------------------------------------------------
        return lRc;
    }
}
