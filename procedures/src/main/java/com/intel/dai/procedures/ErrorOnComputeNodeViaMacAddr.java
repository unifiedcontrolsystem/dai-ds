// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a ComputeNode transitions to "Error" state.
 * NOTE: We only change those that are currently Active into Error
 *
 *  Input parameter:
 *      String  sMacAddr        = string containing the MacAddr for the ComputeNode that should be set in error.
 *      String  sReqAdapterType = Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId  = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *
 */

public class ErrorOnComputeNodeViaMacAddr extends VoltProcedure {

    public final SQLStmt selectComputeNodeSql = new SQLStmt("SELECT * FROM ComputeNode WHERE MacAddr=? Order By Lctn, MacAddr;");

    public final SQLStmt insertComputeNodeHistorySql = new SQLStmt(
            "INSERT INTO ComputeNode_History " +
            "(Lctn, SequenceNumber, State, HostName, BootImageId, IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp, WlmNodeState) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );

    public final SQLStmt updateComputeNodeSql = new SQLStmt("UPDATE ComputeNode SET State=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE MacAddr=?;");



    public long run(String sMacAddr, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectComputeNodeSql, sMacAddr);
        VoltTable[] aComputeNodeData = voltExecuteSQL();
        if (aComputeNodeData[0].getRowCount() == 0) {
            throw new VoltAbortException("ErrorOnComputeNodeViaMacAddr - there is no entry in the ComputeNode table for the specified MacAddr (" + sMacAddr + ") - " +
                                         "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }
        aComputeNodeData[0].advanceRow();

        //----------------------------------------------------------------------
        // Insert a "history" record for these updates into the history table (start with pre-change values then overlay with these changes).
        //----------------------------------------------------------------------
        voltQueueSQL(insertComputeNodeHistorySql
                    ,aComputeNodeData[0].getString("Lctn")
                    ,aComputeNodeData[0].getLong("SequenceNumber")
                    ,"E"                                            // State = Error
                    ,aComputeNodeData[0].getString("HostName")
                    ,aComputeNodeData[0].getString("BootImageId")
                    ,aComputeNodeData[0].getString("IpAddr")
                    ,aComputeNodeData[0].getString("MacAddr")
                    ,aComputeNodeData[0].getString("BmcIpAddr")
                    ,aComputeNodeData[0].getString("BmcMacAddr")
                    ,aComputeNodeData[0].getString("BmcHostName")
                    ,this.getTransactionTime()                      // Get CURRENT_TIMESTAMP or NOW
                    ,this.getTransactionTime()                      // Get CURRENT_TIMESTAMP or NOW
                    ,sReqAdapterType
                    ,lReqWorkItemId
                    ,aComputeNodeData[0].getString("Owner")
                    ,aComputeNodeData[0].getString("Aggregator")
                    ,aComputeNodeData[0].getTimestampAsTimestamp("InventoryTimestamp")
                    ,aComputeNodeData[0].getString("WlmNodeState")
                    );

        //--------------------------------------------------
        // Change the object's state to Error.
        //--------------------------------------------------
        voltQueueSQL(updateComputeNodeSql, "E", this.getTransactionTime(), this.getTransactionTime(), sReqAdapterType, lReqWorkItemId, sMacAddr);
        voltExecuteSQL(true);
        return 0;
    }
}