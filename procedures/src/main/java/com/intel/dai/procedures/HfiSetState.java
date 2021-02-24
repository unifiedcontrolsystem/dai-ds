// Copyright (C) 2020-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;


/**
 * Handle the database processing that is necessary when setting a node's Hfi's state.
 *
 *  Input parameter:
 *      String  sNodeLctn       = node's lctn (NOT the component's lctn)
 *      String  sComponentLctn  = component's lctn
 *      String  sNewState       = string containing the new state value for the above location
 *      long    lTsInMicroSecs  = Time that the event causing this state change occurred
 *      String  sReqAdapterType = Type of adapter that requested this stored procedure (PROVISIONER)
 *      long    lReqWorkItemId  = Work Item Id that the requesting adapter was performing when it requested this stored procedure (-1 is used when there is no work item yet associated with this change)
 *
 *  Return value:
 *      0L = Everything completed fine, and this record did occur in timestamp order.
 *      1L = Everything completed fine, but as an FYI this record did occur OUT OF timestamp order (at least 1 record has already appeared with a more recent timestamp, i.e., newer than the timestamp for this record).
 */

public class HfiSetState extends VoltProcedure {
    public final SQLStmt select = new SQLStmt("SELECT * FROM Hfi WHERE NodeLctn=? AND Lctn=?;");
    public final SQLStmt insertHistory = new SQLStmt(
                 "INSERT INTO Hfi_History " +
                 "(NodeLctn, Lctn, State, BusAddr, Slot, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );
    public final SQLStmt update = new SQLStmt("UPDATE Hfi SET State=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE NodeLctn=? AND Lctn=?;");



    public long run(String sNodeLctn, String sComponentLctn, String sNewState, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException
    {
        //----------------------------------------------------------------------
        // Grab the current record for this Lctn out of the "active" table (Hfi table).
        //      This information is used for determining whether the "new" record is indeed more recent than the record already in the table,
        //      which is necessary as "time is not a stream", and we can get records out of order (from a strict timestamp of occurrence point of view).
        //----------------------------------------------------------------------
        voltQueueSQL(select, EXPECT_ZERO_OR_ONE_ROW, sNodeLctn, sComponentLctn);
        VoltTable[] aData = voltExecuteSQL();
        if (aData[0].getRowCount() == 0) {
            throw new VoltAbortException("HfiSetState - there is no entry in the Hfi table for the specified " +
                                         "NodeLctn(" + sNodeLctn + "), ComponentLctn(" + sComponentLctn + ") - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }
        // Get the current record in the "active" table's LastChgTimestamp (in micro-seconds since epoch).
        aData[0].advanceRow();

        //----------------------------------------------------------------------
        // Update the record for this Lctn in the "active" table.
        //----------------------------------------------------------------------
        voltQueueSQL(update, sNewState, this.getTransactionTime(), lTsInMicroSecs, sReqAdapterType, lReqWorkItemId, sNodeLctn, sComponentLctn);

        //----------------------------------------------------------------------
        // Insert a "history" record for these updates into the history table
        // (this starts with pre-change values and then overlays them with the changes from this invocation).
        //----------------------------------------------------------------------
        voltQueueSQL(insertHistory
                    ,sNodeLctn                     // NodeLctn
                    ,sComponentLctn                // Lctn
                    ,sNewState                     // State
                    ,aData[0].getString("BusAddr")
                    ,aData[0].getString("Slot")
                    ,this.getTransactionTime()     // DbUpdatedTimestamp
                    ,lTsInMicroSecs                // LastChgTimestamp
                    ,sReqAdapterType               // LastChgAdapterType
                    ,lReqWorkItemId                // LastChgWorkItemId
                    );

        voltExecuteSQL(true);

        return 0L;
    }
}