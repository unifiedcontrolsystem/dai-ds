// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.*;
import static java.lang.Math.toIntExact;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a diagnostic terminates.
 * NOTE:
 *
 *  Input parameter:
 *      long    lDiagId             - id that uniquely identifies this specific instance of a diagnostic run
 *      String  sLctn               - hardware location string of the hardware that this diagnostic was running on, a compute node, a whole rack, a cdu, a power distribution unit, a switch, etc.
 *      long    lServiceOperationId - the Service Operation ID that requested this diagnostic be run (-99999 indicates that this diagnostic was submitted outside of a Service Operation)
 *      String  sDiag               - identifies which diagnostic(s) was run, e.g., the CPU bucket, Memory bucket, Compute blade bucket, Power bucket, etc.
 *      String  sReqAdapterType     - Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId      - Work Item Id that the requesting adapter was performing when it requested this stored procedure
 */

public class DiagTerminated extends VoltProcedure {

    public final SQLStmt selectDiagSql = new SQLStmt("SELECT * FROM Diag WHERE DiagId=?;");

    public final SQLStmt deleteDiagSql = new SQLStmt("DELETE FROM Diag WHERE DiagId=?;");

    public final SQLStmt insertDiagHistorySql = new SQLStmt(
            "INSERT INTO Diag_History " +
            "(DiagId, Lctn, ServiceOperationId, Diag, State, StartTimestamp, EndTimestamp, Results, LastChgAdapterType, LastChgWorkItemId, DbUpdatedTimestamp, LastChgTimestamp) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );

    public long run(long lDiagId, String sState, String sResults, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        // Grab diagnostic record (identified by ID)
        voltQueueSQL(selectDiagSql, EXPECT_ZERO_OR_ONE_ROW, lDiagId);
        VoltTable[] aDiagData = voltExecuteSQL();
        VoltTable diagData = aDiagData[0];
        if (diagData.getRowCount() == 0) {
            throw new VoltAbortException("DiagTerminated - there is no entry in the Diag table for the specified " +
                                         "diagnostic ID (" + lDiagId + ") - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" +
                                         lReqWorkItemId + "!");
        }
        diagData.advanceRow();

        //--------------------------------------------------
        // Delete this diag out of the active diag table (we have its data in the diagData structure).
        //--------------------------------------------------
        voltQueueSQL(deleteDiagSql, EXPECT_ONE_ROW, lDiagId);

        //--------------------------------------------------
        // Insert a new row into the history table, indicating that this diag has terminated.
        //--------------------------------------------------
        Date now = this.getTransactionTime();
        voltQueueSQL(insertDiagHistorySql
                    ,lDiagId // DiagId
                    ,diagData.getString("Lctn")
                    ,diagData.getLong("ServiceOperationId")
                    ,diagData.getString("Diag")
                    ,sState // State
                    ,diagData.getTimestampAsTimestamp("StartTimestamp").getTime()
                    ,now // EndTimestamp
                    ,sResults
                    ,sReqAdapterType // LastChgAdapterType
                    ,lReqWorkItemId // LastChgWorkItemId
                    ,now // DbUpdatedTimestamp
                    ,now // LastChgTimestamp
        );

        voltExecuteSQL(true);
        return 0;
    }
}