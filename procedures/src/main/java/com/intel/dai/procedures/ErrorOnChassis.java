// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a Chassis transitions to "Error" state.
 * NOTE: We only change those that are currently Active into Error
 *
 *  Input parameter:
 *      String  sLctn           = string containing a fully qualified Chassis location that should be set in error.
 *                                string containing a Rack location that has been set into error, this indicates that all of the active Chassis w/i that Rack should also be set into error.
 *      String  sReqAdapterType = Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId  = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *
 *  Sample invocation:
 *      echo "Exec ErrorOnChassis R00-CH3;" | sqlcmd
 *          Set an individual specified Chassis into Error
 *
 *      echo "Exec ErrorOnChassis R00;" | sqlcmd
 *          Set all Chassiss in the specified Rack into Error
 *
 */

public class ErrorOnChassis extends VoltProcedure {

    final String selectChassis = "SELECT * FROM Chassis WHERE Lctn = ? Order By Lctn;";
    public final SQLStmt selectChassisSql = new SQLStmt(selectChassis);

    // final String selectChildChassissThatAreActive = "SELECT Lctn, State FROM Chassis WHERE Lctn LIKE ? AND State = 'A' Order By Lctn;";
    // public final SQLStmt selectChildChassissThatAreActiveSql = new SQLStmt(selectChildChassissThatAreActive);

    final String insertChassisHistory = "INSERT INTO Chassis_History (Lctn, State, Sernum, Type, Vpd, DbUpdatedTimestamp, LastChgTimestamp, Owner) VALUES (?, ?, ?, ?, ?, ?, ?, ?);";
    public final SQLStmt insertChassisHistorySql = new SQLStmt(insertChassisHistory);

    final String updateChassis = "UPDATE Chassis SET State = ?, LastChgTimestamp = CURRENT_TIMESTAMP WHERE Lctn = ?;";
    public final SQLStmt updateChassisSql = new SQLStmt(updateChassis);



    long putSingleChassisIntoError(String sLctn, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectChassisSql, sLctn);
        VoltTable[] aChassisData = voltExecuteSQL();
        if (aChassisData[0].getRowCount() == 0) {
            throw new VoltAbortException("ErrorOnChassis::putSingleChassisIntoError - there is no entry in the Chassis table for the specified Chassis (" + sLctn + ") - " +
                                         "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }
        aChassisData[0].advanceRow();

        // Ensure that this object is in Active state (can only transition into Error state from Active state).
        if (!aChassisData[0].getString("State").equals("A")) {
            throw new VoltAbortException("ErrorOnChassis::putSingleChassisIntoError - unable to change to Error state due to incompatible state (" + aChassisData[0].getString("State") + ") - " +
                                         "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }

        //----------------------------------------------------------------------
        // Insert a "history" record for these updates into the history table (start with pre-change values then overlay with these changes).
        //----------------------------------------------------------------------
        voltQueueSQL(insertChassisHistorySql,
                     sLctn,
                     "E",  // State = Error
                     aChassisData[0].getString("Sernum"),
                     aChassisData[0].getString("Type"),
                     aChassisData[0].getString("Vpd"),
                     this.getTransactionTime(),             // Get CURRENT_TIMESTAMP or NOW
                     this.getTransactionTime(),             // Get CURRENT_TIMESTAMP or NOW
                     aChassisData[0].getString("Owner")
                    );

        //--------------------------------------------------
        // Change the object's state to Error.
        //--------------------------------------------------
        voltQueueSQL(updateChassisSql, "E", sLctn);
        voltExecuteSQL();
        return 0;
    }


    public long run(String sLctn, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //
        // switch(sLctn.length()) {
        //     case ErrorOnComputeNode.LCTN_CHASSIS_LENGTH :           // Chassis         was specified (Rdd-CH3)
        //         putSingleChassisIntoError(sLctn, sReqAdapterType, lReqWorkItemId);
        //         break;
        //
        //     case ErrorOnComputeNode.LCTN_RACK_LENGTH :              // Rack            was specified (Rdd)
        //         //System.out.println("\n\nErrorOnChassis - user specified an entire component rather than a single Chassis (" + sLctn + ")");
        //         // Get the list of "appropriate" Chassiss that exist w/i the specified component from the database.
        //         // (the appropriate node's are those with a state that it makes sense to transition from, to Error, e.g., transition from Active to Error.
        //         //  Those that are deemed inappropriate will simply not be changed.)
        //         voltQueueSQL(selectChildChassissThatAreActiveSql, sLctn+"%");
        //         VoltTable[] aChassisData = voltExecuteSQL();
        //
        //         // Loop through the returned list of Chassiss, marking them as being in Error.
        //         for (int cntr = 0; cntr < aChassisData[0].getRowCount(); ++cntr) {
        //             // Actually mark this Chassis into Error.
        //             putSingleChassisIntoError(aChassisData[0].fetchRow(cntr).getString("Lctn"), sReqAdapterType, lReqWorkItemId);
        //         }
        //         break;
        //
        //     default :
        //         throw new VoltAbortException("ErrorOnChassis - an unsupported location string was specified (" + sLctn + ") - " +
        //                                      "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        // }

        putSingleChassisIntoError(sLctn, sReqAdapterType, lReqWorkItemId);
        return 0;
    }
}