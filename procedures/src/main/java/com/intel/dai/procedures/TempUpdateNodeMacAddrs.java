// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 *
 *  Input parameter:
 *      String  sNodeLctn        = string containing the Lctn of the node that now has IP address assigned to it
 *      String  sNodeMacAddr     = string containing the MAC address of the new node inserted at location sNodeLctn
 *      String  sBmcMacAddr      = string containing the BMC MAC address of the new node inserted at location sNodeLctn
 *
 *  Return value:
 *      0L = Everything completed fine, and this record did occur in timestamp order.
 *      1L = Everything completed fine, but as an FYI this record did occur OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
 */

public class TempUpdateNodeMacAddrs extends VoltProcedure {

    public final SQLStmt selectComputeNode = new SQLStmt("SELECT * FROM ComputeNode WHERE Lctn=?;");
    public final SQLStmt selectServiceNode = new SQLStmt("SELECT * FROM ServiceNode WHERE Lctn=?;");

    public final SQLStmt updateComputeNode = new SQLStmt("UPDATE ComputeNode SET MacAddr=?, BmcMacAddr=?, DbUpdatedTimestamp=?, LastChgTimestamp=? WHERE Lctn=?;");
    public final SQLStmt updateServiceNode = new SQLStmt("UPDATE ServiceNode SET MacAddr=?, BmcMacAddr=?, DbUpdatedTimestamp=?, LastChgTimestamp=? WHERE Lctn=?;");

    public final SQLStmt selectComputeNodeHistoryWithPreceedingTs = new SQLStmt("SELECT * FROM ComputeNode_History WHERE (Lctn=? AND LastChgTimestamp<?) ORDER BY LastChgTimestamp DESC LIMIT 1;");
    public final SQLStmt selectServiceNodeHistoryWithPreceedingTs = new SQLStmt("SELECT * FROM ServiceNode_History WHERE (Lctn=? AND LastChgTimestamp<?) ORDER BY LastChgTimestamp DESC LIMIT 1;");

    public final SQLStmt selectComputeNodeHistoryWithThisTimestamp = new SQLStmt("SELECT Lctn FROM ComputeNode_History WHERE Lctn=? AND LastChgTimestamp=?;");
    public final SQLStmt selectServiceNodeHistoryWithThisTimestamp = new SQLStmt("SELECT Lctn FROM ServiceNode_History WHERE Lctn=? AND LastChgTimestamp=?;");

    public final SQLStmt insertComputeNodeHistory = new SQLStmt(
            "INSERT INTO ComputeNode_History " +
            "(Lctn, SequenceNumber, State, HostName, BootImageId, IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp, WlmNodeState) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );
    public final SQLStmt insertServiceNodeHistory = new SQLStmt(
            "INSERT INTO ServiceNode_History " +
            "(Lctn, SequenceNumber, HostName, State, BootImageId, IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );

    public final SQLStmt deleteFromCacheMacAddrToLctnTable = new SQLStmt("DELETE FROM CacheMacAddrToLctn WHERE Lctn=?;");
    public final SQLStmt insertIntoCacheMacAddrToLctnTable = new SQLStmt("INSERT INTO CacheMacAddrToLctn (MacAddr, Lctn) VALUES (?, ?);");



    //----------------------------------------------------------------------
    // Ensure that the we have a unique timestamp that we can use for inserting a record into the ComputeNode_History table
    // (it is unique meaning that no other history record for this specified lctn is already using it in the LastChgTimestamp column).
    // - needed as we want the records to have unique timestamps for that field.
    //----------------------------------------------------------------------
    public long ensureHaveUniqueComputeNodeLastChgTimestamp(String sNodeLctn, long lNewRecordsTsInMicroSecs, long lCurRecordsTsInMicroSecs)
    {
        //----------------------------------------------------------------------
        // Ensure that we aren't inserting multiple records into history with the same timestamp (down to millisecond granularity).
        //----------------------------------------------------------------------
        ///System.out.println("ensureHaveUniqueComputeNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - On Entry");
        boolean bPhase2IsBad=true;  long lCntr=0L;
        while (bPhase2IsBad) {
//            if (lCntr > 0)
//                System.out.println("ensureHaveUniqueComputeNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - Doing a Recheck!");
            //--------
            // Phase 1 - make sure that this new timestamp is not the same as the current db value from the ComputeNode table.
            //--------
            // Check & see if this timestamp is the same as the timestamp on the current record (in the ComputeNode table).
            if ( lNewRecordsTsInMicroSecs == lCurRecordsTsInMicroSecs )
            {
                // these 2 records have the same timestamp - bump the number of microseconds for this new record.
                ++lNewRecordsTsInMicroSecs;  // bump by 1 microsecond.
//                System.out.println("ensureHaveUniqueComputeNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - Bumped in Phase1!");
            }
            //--------
            // Phase 2 - make sure that this new timestamp is not the same as an already existing record in the ComputeNode_History table.
            //--------
            // Query and see if this timestamp is already in the history table for this specific lctn.
            voltQueueSQL(selectComputeNodeHistoryWithThisTimestamp, EXPECT_ZERO_OR_ONE_ROW, sNodeLctn, lNewRecordsTsInMicroSecs);
            VoltTable[] aComputeNodeHistoryWithThisTimestamp = voltExecuteSQL();
            if (aComputeNodeHistoryWithThisTimestamp[0].getRowCount() > 0) {
                // there is already an existing record in the ComputeNode_History table for this lctn that has this timestamp.
                // Bump the number of microseconds for this new record.
                ++lNewRecordsTsInMicroSecs;  // bump by 1 microsecond.
//                System.out.println("ensureHaveUniqueComputeNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - Bumped in Phase2!");
            }
            else
                // there was not a match in the history table for this lctn with this timestamp - good to go.
                bPhase2IsBad = false;  // indicate that this timestamp is good.

            ++lCntr;
        }   // ensure no constraint violation due to multiple records with the same timestamp.
        ///System.out.println("ensureHaveUniqueComputeNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - After both Phases OK");

        return lNewRecordsTsInMicroSecs;  // return a timestamp that is unique.
    }   // End ensureHaveUniqueComputeNodeLastChgTimestamp(String sNodeLctn, long lNewRecordsTsInMicroSecs, long lCurRecordsTsInMicroSecs)


    //----------------------------------------------------------------------
    // Ensure that the we have a unique timestamp that we can use for inserting a record into the ServiceNode_History table
    // (it is unique meaning that no other history record for this specified lctn is already using it in the LastChgTimestamp column).
    // - needed as we want the records to have unique timestamps for that field.
    //----------------------------------------------------------------------
    public long ensureHaveUniqueServiceNodeLastChgTimestamp(String sNodeLctn, long lNewRecordsTsInMicroSecs, long lCurRecordsTsInMicroSecs)
    {
        //----------------------------------------------------------------------
        // Ensure that we aren't inserting multiple records into history with the same timestamp (down to millisecond granularity).
        //----------------------------------------------------------------------
        ///System.out.println("ensureHaveUniqueServiceNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - On Entry");
        boolean bPhase2IsBad=true;  long lCntr=0L;
        while (bPhase2IsBad) {
//            if (lCntr > 0)
//                System.out.println("ensureHaveUniqueServiceNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - Doing a Recheck!");
            //--------
            // Phase 1 - make sure that this new timestamp is not the same as the current db value from the ServiceNode table.
            //--------
            // Check & see if this timestamp is the same as the timestamp on the current record (in the ServiceNode table).
            if ( lNewRecordsTsInMicroSecs == lCurRecordsTsInMicroSecs )
            {
                // these 2 records have the same timestamp - bump the number of microseconds for this new record.
                ++lNewRecordsTsInMicroSecs;  // bump by 1 microsecond.
//                System.out.println("ensureHaveUniqueServiceNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - Bumped in Phase1!");
            }
            //--------
            // Phase 2 - make sure that this new timestamp is not the same as an already existing record in the ServiceNode_History table.
            //--------
            // Query and see if this timestamp is already in the history table for this specific lctn.
            voltQueueSQL(selectServiceNodeHistoryWithThisTimestamp, EXPECT_ZERO_OR_ONE_ROW, sNodeLctn, lNewRecordsTsInMicroSecs);
            VoltTable[] aServiceNodeHistoryWithThisTimestamp = voltExecuteSQL();
            if (aServiceNodeHistoryWithThisTimestamp[0].getRowCount() > 0) {
                // there is already an existing record in the ServiceNode_History table for this lctn that has this timestamp.
                // Bump the number of microseconds for this new record.
                ++lNewRecordsTsInMicroSecs;  // bump by 1 microsecond.
//                System.out.println("ensureHaveUniqueServiceNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - Bumped in Phase2!");
            }
            else
                // there was not a match in the history table for this lctn with this timestamp - good to go.
                bPhase2IsBad = false;  // indicate that this timestamp is good.

            ++lCntr;
        }   // ensure no constraint violation due to multiple records with the same timestamp.
        ///System.out.println("ensureHaveUniqueServiceNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - After both Phases OK");

        return lNewRecordsTsInMicroSecs;  // return a timestamp that is unique.
    }   // End ensureHaveUniqueServiceNodeLastChgTimestamp(String sNodeLctn, long lNewRecordsTsInMicroSecs, long lCurRecordsTsInMicroSecs)



    public long run(String sNodeLctn, String sNodeMacAddr, String sBmcMacAddr) throws VoltAbortException {

        long lTsInMicroSecs = this.getTransactionTime().getTime() * 1000L;  // get current time in micro-seconds since epoch

        // Determine if this is a ComputeNode or a ServiceNode lctn.
        voltQueueSQL(selectComputeNode, EXPECT_ZERO_OR_ONE_ROW, sNodeLctn);
        VoltTable[] aNodeData = voltExecuteSQL();
        if (aNodeData[0].getRowCount() > 0) {
            // this is a ComputeNode.
            // Get the current record in the "active" table's LastChgTimestamp (in micro-seconds since epoch).
            aNodeData[0].advanceRow();
            long lCurRecordsTsInMicroSecs = aNodeData[0].getTimestampAsTimestamp("LastChgTimestamp").getTime();
            // Ensure that we aren't inserting multiple records into history with the same timestamp.
            //      Said differently, check & see if there is already a record in the history table for this lctn and the exact same timestamp, if so bump this timestamp until it is unique.
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
                String sCurRecordsState = aNodeData[0].getString("State");
                // Get the appropriate record out of the history table that we should use for "filling in" any record data that we want copied from the preceding record.
                voltQueueSQL(selectComputeNodeHistoryWithPreceedingTs, sNodeLctn, lTsInMicroSecs);
                aNodeData = voltExecuteSQL();
                aNodeData[0].advanceRow();
//                System.out.println("TempUpdateNodeMacAddrs - " + sNodeLctn + " - OUT OF ORDER" +
//                                   " - ThisRecsTsInMicroSecs="   + lTsInMicroSecs           + ", ThisRecsState=I" +
//                                   " - CurRecordsTsInMicroSecs=" + lCurRecordsTsInMicroSecs + ", CurRecordsState=" + sCurRecordsState + "!");
            }

            //----------------------------------------------------------------------
            // Update the record for this Lctn in the "active" table.
            //----------------------------------------------------------------------
            if (bUpdateCurrentlyActiveRow) {
                voltQueueSQL(updateComputeNode, sNodeMacAddr, sBmcMacAddr, lTsInMicroSecs, lTsInMicroSecs, sNodeLctn);
            }

            //----------------------------------------------------------------------
            // Insert a "history" record for these updates into the history table
            // (this starts with pre-change values and then overlays them with the changes from this invocation).
            //----------------------------------------------------------------------
            voltQueueSQL(insertComputeNodeHistory
                        ,aNodeData[0].getString("Lctn")
                        ,aNodeData[0].getLong("SequenceNumber")
                        ,aNodeData[0].getString("State")
                        ,aNodeData[0].getString("HostName")
                        ,aNodeData[0].getString("BootImageId")
                        ,aNodeData[0].getString("IpAddr")
                        ,sNodeMacAddr                                   // MacAddr
                        ,aNodeData[0].getString("BmcIpAddr")
                        ,sBmcMacAddr                                    // BmcMacAddr
                        ,aNodeData[0].getString("BmcHostName")
                        ,lTsInMicroSecs                                 // DbUpdatedTimestamp
                        ,lTsInMicroSecs                                 // LastChgTimestamp
                        ,"TempUpdateNodeMacs"                           // LastChgAdapterType - not being done by an adapter, this is being done by a TEMPORARY tool!
                        ,-1                                             // LastChgWorkItemId
                        ,aNodeData[0].getString("Owner")
                        ,aNodeData[0].getString("Aggregator")
                        ,aNodeData[0].getTimestampAsTimestamp("InventoryTimestamp")
                        ,aNodeData[0].getString("WlmNodeState")
                        );
        }   // this is a ComputeNode.
        else {
            // this is a ServiceNode.
            voltQueueSQL(selectServiceNode, EXPECT_ZERO_OR_ONE_ROW, sNodeLctn);
            aNodeData = voltExecuteSQL();
            if (aNodeData[0].getRowCount() > 0) {
                //------------------------------------------------------------------
                // this is a ServiceNode.
                //------------------------------------------------------------------

                // Get the current record in the "active" table's LastChgTimestamp (in micro-seconds since epoch).
                aNodeData[0].advanceRow();
                long lCurRecordsTsInMicroSecs = aNodeData[0].getTimestampAsTimestamp("LastChgTimestamp").getTime();
                // Ensure that we aren't inserting multiple records into history with the same timestamp.
                //      Said differently, check & see if there is already a record in the history table for this lctn and the exact same timestamp, if so bump this timestamp until it is unique.
                lTsInMicroSecs = ensureHaveUniqueServiceNodeLastChgTimestamp(sNodeLctn, lTsInMicroSecs, lCurRecordsTsInMicroSecs);

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
                    String sCurRecordsState = aNodeData[0].getString("State");
                    // Get the appropriate record out of the history table that we should use for "filling in" any record data that we want copied from the preceding record.
                    voltQueueSQL(selectServiceNodeHistoryWithPreceedingTs, sNodeLctn, lTsInMicroSecs);
                    aNodeData = voltExecuteSQL();
                    aNodeData[0].advanceRow();
//                    System.out.println("TempUpdateNodeMacAddrs - " + sNodeLctn + " - OUT OF ORDER" +
//                                       " - ThisRecsTsInMicroSecs="   + lTsInMicroSecs           + ", ThisRecsState=I" +
//                                       " - CurRecordsTsInMicroSecs=" + lCurRecordsTsInMicroSecs + ", CurRecordsState=" + sCurRecordsState + "!");
                }

                //----------------------------------------------------------------------
                // Update the record for this Lctn in the "active" table.
                //----------------------------------------------------------------------
                if (bUpdateCurrentlyActiveRow) {
                    voltQueueSQL(updateServiceNode, sNodeMacAddr, sBmcMacAddr, lTsInMicroSecs, lTsInMicroSecs, sNodeLctn);
                }

                //----------------------------------------------------------------------
                // Insert a "history" record for these updates into the history table
                // (this starts with pre-change values and then overlays them with the changes from this invocation).
                //----------------------------------------------------------------------
                voltQueueSQL(insertServiceNodeHistory
                            ,aNodeData[0].getString("Lctn")
                            ,aNodeData[0].getLong("SequenceNumber")
                            ,aNodeData[0].getString("HostName")
                            ,aNodeData[0].getString("State")
                            ,aNodeData[0].getString("BootImageId")
                            ,aNodeData[0].getString("IpAddr")
                            ,sNodeMacAddr                                   // MacAddr
                            ,aNodeData[0].getString("BmcIpAddr")
                            ,sBmcMacAddr                                    // BmcMacAddr
                            ,aNodeData[0].getString("BmcHostName")
                            ,lTsInMicroSecs                                 // DbUpdatedTimestamp
                            ,lTsInMicroSecs                                 // LastChgTimestamp
                            ,"TempUpdateNodeMacs"                           // LastChgAdapterType - not being done by an adapter, this is being done by a TEMPORARY tool!
                            ,-1                                             // LastChgWorkItemId
                            ,aNodeData[0].getString("Owner")
                            ,aNodeData[0].getString("Aggregator")
                            ,aNodeData[0].getTimestampAsTimestamp("InventoryTimestamp")
                            );
            }
            else {
                throw new VoltAbortException("TempUpdateNodeMacAddrs - there is no entry for the specified node lctn(" + sNodeLctn + ") " +
                                             "in either the ComputeNode or ServiceNode table!");
            }
        }   // this is a ServiceNode.


        //----------------------------------------------------------------------
        // Also need to update the MacAddr in the CacheMacAddrToLctn table.
        //----------------------------------------------------------------------
        // Since we are updating the partitioning column we can't do an update, we have to do a delete, then an insert.
        voltQueueSQL(deleteFromCacheMacAddrToLctnTable
                    ,EXPECT_ONE_ROW
                    ,sNodeLctn
                    );
        voltQueueSQL(insertIntoCacheMacAddrToLctnTable
                    ,sNodeMacAddr
                    ,sNodeLctn
                    );

        voltExecuteSQL(true);
        return 0L;
    }
}
