// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
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

public class ErrorOnServiceNode extends VoltProcedure {

    public final SQLStmt selectNode = new SQLStmt("SELECT * FROM ServiceNode WHERE Lctn=? Order By Lctn;");

    public final SQLStmt insertNodeHistory = new SQLStmt(
                    "INSERT INTO ServiceNode_History " +
                    "(Lctn, SequenceNumber, HostName, State, BootImageId, IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );

    public final SQLStmt updateNode = new SQLStmt("UPDATE ServiceNode SET State=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE Lctn=?;");



    public long run(String sLctn, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectNode, sLctn);
        VoltTable[] aNodeData = voltExecuteSQL();
        if (aNodeData[0].getRowCount() == 0) {
            throw new VoltAbortException("ErrorOnServiceNode - there is no entry in the ServiceNode table for the specified Node (" + sLctn + ") - " +
                                         "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }
        aNodeData[0].advanceRow();

        //----------------------------------------------------------------------
        // Insert a "history" record for these updates into the history table (start with pre-change values then overlay with these changes).
        //----------------------------------------------------------------------
        voltQueueSQL(insertNodeHistory
                    ,aNodeData[0].getString("Lctn")
                    ,aNodeData[0].getLong("SequenceNumber")
                    ,aNodeData[0].getString("HostName")
                    ,"E"                                            // State = Error
                    ,aNodeData[0].getString("BootImageId")
                    ,aNodeData[0].getString("IpAddr")
                    ,aNodeData[0].getString("MacAddr")
                    ,aNodeData[0].getString("BmcIpAddr")
                    ,aNodeData[0].getString("BmcMacAddr")
                    ,aNodeData[0].getString("BmcHostName")
                    ,this.getTransactionTime()                      // DbUpdatedTimestamp = CURRENT_TIMESTAMP or NOW
                    ,this.getTransactionTime()                      // LastChgTimestamp   = CURRENT_TIMESTAMP or NOW
                    ,sReqAdapterType
                    ,lReqWorkItemId
                    ,aNodeData[0].getString("Owner")
                    ,aNodeData[0].getString("Aggregator")
                    ,aNodeData[0].getTimestampAsTimestamp("InventoryTimestamp")
                    );

        //--------------------------------------------------
        // Update the object's values.
        //--------------------------------------------------
        voltQueueSQL(updateNode, "E", this.getTransactionTime(), this.getTransactionTime(), sReqAdapterType, lReqWorkItemId, sLctn);
        voltExecuteSQL();
        return 0;
    }

}