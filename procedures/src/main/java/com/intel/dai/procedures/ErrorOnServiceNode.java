// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.time.Instant;

import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a Node transitions to "Error" state.
 * NOTE: We only change those that are currently Active into Error
 *
 *  Input parameter:
 *      String  sLctn           = string containing a fully qualified ServiceNode location that should be set in error.
 *      String  sReqAdapterType = Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId  = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 */

public class ErrorOnServiceNode extends ServiceNodeCommon {

    public final SQLStmt selectServiceNode = new SQLStmt("SELECT * FROM ServiceNode WHERE Lctn=? Order By Lctn;");

    public final SQLStmt insertNodeHistory = new SQLStmt(
                    "INSERT INTO ServiceNode_History " +
                    "(Lctn, SequenceNumber, HostName, State, BootImageId, IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );

    public final SQLStmt updateNode = new SQLStmt("UPDATE ServiceNode SET State=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE Lctn=?;");

    long putSingleServiceNodeIntoError(String sLctn, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectServiceNode, EXPECT_ONE_ROW, sLctn);
        VoltTable[] aServiceNodeData = voltExecuteSQL();
        if (aServiceNodeData[0].getRowCount() == 0) {
            throw new VoltAbortException("ErrorOnServiceNode::putSingleServiceNodeIntoError - there is no entry in the ServiceNode table for the specified ServiceNode (" + sLctn + ") - " +
                    "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }
        // Get the current record in the "active" table's LastChgTimestamp (in micro-seconds since epoch).
        aServiceNodeData[0].advanceRow();
        long lCurRecsLastChgTimestampTsInMicroSecs = aServiceNodeData[0].getTimestampAsLong("LastChgTimestamp");

        // Get this transactions timestamp in microsecs.
        Instant ts = Instant.now();
        long lThisTransactionsTsInMicrosecs = (ts.getEpochSecond() * 1_000_000) + (ts.getNano() / 1_000);

        //----------------------------------------------------------------------
        // Ensure that we aren't updating this row with the exact same LastChgTimestamp as currently exists in the table.
        //----------------------------------------------------------------------
        // Check & see if this timestamp is the same as the LastChgTimestamp on the current record (in the ComputeNode table).
        if (lThisTransactionsTsInMicrosecs <= lCurRecsLastChgTimestampTsInMicroSecs)
        {
            lThisTransactionsTsInMicrosecs = lCurRecsLastChgTimestampTsInMicroSecs + 1;
            lThisTransactionsTsInMicrosecs = ensureHaveUniqueServiceNodeLastChgTimestamp(sLctn, lThisTransactionsTsInMicrosecs, lCurRecsLastChgTimestampTsInMicroSecs);
        }

        //----------------------------------------------------------------------
        // Insert a "history" record for these updates into the history table (start with pre-change values then overlay with these changes).
        //----------------------------------------------------------------------
        voltQueueSQL(insertNodeHistory
                ,aServiceNodeData[0].getString("Lctn")
                ,aServiceNodeData[0].getLong("SequenceNumber")
                ,aServiceNodeData[0].getString("HostName")
                ,"E"                                            // State = Error
                ,aServiceNodeData[0].getString("BootImageId")
                ,aServiceNodeData[0].getString("IpAddr")
                ,aServiceNodeData[0].getString("MacAddr")
                ,aServiceNodeData[0].getString("BmcIpAddr")
                ,aServiceNodeData[0].getString("BmcMacAddr")
                ,aServiceNodeData[0].getString("BmcHostName")
                ,lThisTransactionsTsInMicrosecs                 // Get CURRENT_TIMESTAMP or NOW
                ,lThisTransactionsTsInMicrosecs                 // LastChgTimestamp
                ,sReqAdapterType
                ,lReqWorkItemId
                ,aServiceNodeData[0].getString("Owner")
                ,aServiceNodeData[0].getString("Aggregator")
                ,aServiceNodeData[0].getTimestampAsTimestamp("InventoryTimestamp")
        );

        //--------------------------------------------------
        // Update the object's values.
        //--------------------------------------------------
        voltQueueSQL(updateNode, "E", lThisTransactionsTsInMicrosecs, lThisTransactionsTsInMicrosecs, sReqAdapterType, lReqWorkItemId, sLctn);
        voltExecuteSQL();
        return 0;
    }


    public long run(String sLctn, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
//        //--------------------------------------------------
//        // Grab the object's pre-change values so they are available for use creating the history record.
//        //--------------------------------------------------
//        voltQueueSQL(selectNode, sLctn);
//        VoltTable[] aNodeData = voltExecuteSQL();
//        if (aNodeData[0].getRowCount() == 0) {
//            throw new VoltAbortException("ErrorOnServiceNode - there is no entry in the ServiceNode table for the specified Node (" + sLctn + ") - " +
//                                         "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
//        }
//        aNodeData[0].advanceRow();
//
//        //----------------------------------------------------------------------
//        // Insert a "history" record for these updates into the history table (start with pre-change values then overlay with these changes).
//        //----------------------------------------------------------------------
//        voltQueueSQL(insertNodeHistory
//                    ,aNodeData[0].getString("Lctn")
//                    ,aNodeData[0].getLong("SequenceNumber")
//                    ,aNodeData[0].getString("HostName")
//                    ,"E"                                            // State = Error
//                    ,aNodeData[0].getString("BootImageId")
//                    ,aNodeData[0].getString("IpAddr")
//                    ,aNodeData[0].getString("MacAddr")
//                    ,aNodeData[0].getString("BmcIpAddr")
//                    ,aNodeData[0].getString("BmcMacAddr")
//                    ,aNodeData[0].getString("BmcHostName")
//                    ,this.getTransactionTime()                      // DbUpdatedTimestamp = CURRENT_TIMESTAMP or NOW
//                    ,this.getTransactionTime()                      // LastChgTimestamp   = CURRENT_TIMESTAMP or NOW
//                    ,sReqAdapterType
//                    ,lReqWorkItemId
//                    ,aNodeData[0].getString("Owner")
//                    ,aNodeData[0].getString("Aggregator")
//                    ,aNodeData[0].getTimestampAsTimestamp("InventoryTimestamp")
//                    );
//
//        //--------------------------------------------------
//        // Update the object's values.
//        //--------------------------------------------------
//        voltQueueSQL(updateNode, "E", this.getTransactionTime(), this.getTransactionTime(), sReqAdapterType, lReqWorkItemId, sLctn);
//        voltExecuteSQL();
        return putSingleServiceNodeIntoError(sLctn, sReqAdapterType, lReqWorkItemId);
    }

}