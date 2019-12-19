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

    public final SQLStmt selectDiagSql = new SQLStmt(
            "SELECT * from Diag where DiagId = ?;"
    );

    public final SQLStmt updateSql = new SQLStmt(
                    "UPDATE Diag " +
                    "SET State=?, EndTimestamp=?, Results=?, LastChgAdapterType=?, LastChgWorkItemId=?, DbUpdatedTimestamp=?, LastChgTimestamp=? " +
                    "WHERE DiagId=?;"
    );

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

        //---------------------------------------------------------------------
        // Update this diagnostic's row with the results from the diagnostic.
        //---------------------------------------------------------------------
        Date now = this.getTransactionTime();

        voltQueueSQL(updateSql
                    ,sState
                    ,now  // EndTimestamp
                    ,sResults
                    ,sReqAdapterType
                    ,lReqWorkItemId
                    ,now // DbUpdatedTimestamp
                    ,now // LastChgTimestamp
                    ,lDiagId
                    );

        // Insert a history record for this update
        voltQueueSQL(insertDiagHistorySql
                    ,lDiagId
                    ,diagData.getString("Lctn")
                    ,diagData.getLong("ServiceOperationId")
                    ,diagData.getString("Diag")
                    ,sState
                    ,diagData.getTimestampAsTimestamp("StartTimestamp").getTime()
                    ,now
                    ,sResults
                    ,sReqAdapterType
                    ,lReqWorkItemId
                    ,now // DbUpdatedTimestamp
                    ,now // LastChgTimestamp
        );

        voltExecuteSQL(true);
        return 0;
    }
}