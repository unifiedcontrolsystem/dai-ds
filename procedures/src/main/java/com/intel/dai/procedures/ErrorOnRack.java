// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a Rack transitions to "Error" state.
 * NOTE: We only change those that are currently Active into Error
 *
 *  Input parameter:
 *      String  sLctn           = string containing a fully qualified Rack location that should be set in error.
 *      String  sReqAdapterType = Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId  = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *
 *  Sample invocation:
 *      echo "Exec ErrorOnRack R00;" | sqlcmd
 *          Set an individual specified Rack into Error
 *
 */
public class ErrorOnRack extends VoltProcedure {

    final String selectRack = "SELECT * FROM Rack WHERE Lctn = ? Order By Lctn;";
    public final SQLStmt selectRackSql = new SQLStmt(selectRack);

    // final String selectChildRacksThatAreActive = "SELECT Lctn, State FROM Rack WHERE Lctn LIKE ? AND State = 'A' Order By Lctn;";
    // public final SQLStmt selectChildRacksThatAreActiveSql = new SQLStmt(selectChildRacksThatAreActive);

    final String insertRackHistory = "INSERT INTO Rack_History (Lctn, State, Sernum, Type, Vpd, DbUpdatedTimestamp, LastChgTimestamp, Owner) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
    public final SQLStmt insertRackHistorySql = new SQLStmt(insertRackHistory);

    final String updateRack = "UPDATE Rack SET State = ?, LastChgTimestamp = CURRENT_TIMESTAMP WHERE Lctn = ?;";
    public final SQLStmt updateRackSql = new SQLStmt(updateRack);



    long putSingleRackIntoError(String sLctn, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectRackSql, sLctn);
        VoltTable[] aRackData = voltExecuteSQL();
        if (aRackData[0].getRowCount() == 0) {
            throw new VoltAbortException("ErrorOnRack::putSingleRackIntoError - there is no entry in the Rack table for the specified Rack (" + sLctn + ") - " +
                                         "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }
        aRackData[0].advanceRow();

        // Ensure that this object is in Active state (can only transition into Error state from Active state).
        if (!aRackData[0].getString("State").equals("A")) {
            throw new VoltAbortException("ErrorOnRack::putSingleRackIntoError - unable to change to Error state due to incompatible state (" + aRackData[0].getString("State") + ") - " +
                                         "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }

        //----------------------------------------------------------------------
        // Insert a "history" record for these updates into the history table (start with pre-change values then overlay with these changes).
        //----------------------------------------------------------------------
        voltQueueSQL(insertRackHistorySql,
                     sLctn,
                     "E",  // State = Error
                     aRackData[0].getString("Sernum"),
                     aRackData[0].getString("Type"),
                     aRackData[0].getString("Vpd"),
                     this.getTransactionTime(),         // Get CURRENT_TIMESTAMP or NOW
                     this.getTransactionTime(),         // Get CURRENT_TIMESTAMP or NOW
                     aRackData[0].getString("Owner")
                    );

        //--------------------------------------------------
        // Change the object's state to Error.
        //--------------------------------------------------
        voltQueueSQL(updateRackSql, "E", sLctn);
        voltExecuteSQL();
        return 0;
    }


    public long run(String sLctn, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //
        // switch(sLctn.length()) {
        //     case ErrorOnComputeNode.LCTN_RACK_LENGTH :              // Rack            was specified (Rdd)
        //         putSingleRackIntoError(sLctn, sReqAdapterType, lReqWorkItemId);
        //         break;
        //
        //     default :
        //         throw new VoltAbortException("ErrorOnRack - an unsupported location string was specified (" + sLctn + ") - " +
        //                                      "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        // }

        putSingleRackIntoError(sLctn, sReqAdapterType, lReqWorkItemId);
        return 0;
    }
}