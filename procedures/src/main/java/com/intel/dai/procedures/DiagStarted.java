// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Date;

import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a diagnostic is started.
 * NOTE:
 *
 *  Input parameter:
 *      long    lDiagId             - id that uniquely identifies this specific instance of a diagnostic run
 *      String  sLctn               - hardware location string of the hardware that this diagnostic was running on, a compute node, a whole rack, a cdu, power distribution unit, a switch, etc.
 *      long    lServiceOperationId - the Service Operation ID that requested this diagnostic be run (-99999 indicates that this diagnostic was submitted outside of a Service Operation)
 *      String  sDiag               - identifies which diagnostic(s) was run, e.g., the CPU bucket, Memory bucket, Compute blade bucket, Power bucket, etc.
 *      String  sReqAdapterType     - Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId      - Work Item Id that the requesting adapter was performing when it requested this stored procedure
 */

public class DiagStarted extends VoltProcedure {

    public final SQLStmt insertDiagSql = new SQLStmt(
                    "INSERT INTO Diag " +
                    "(DiagId, Lctn, ServiceOperationId, Diag, State, StartTimestamp, EndTimestamp, Results, LastChgAdapterType, LastChgWorkItemId, DbUpdatedTimestamp, LastChgTimestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?);"
    );

    public final SQLStmt insertDiagHistorySql = new SQLStmt(
                    "INSERT INTO Diag_History " +
                    "(DiagId, Lctn, ServiceOperationId, Diag, State, StartTimestamp, EndTimestamp, Results, LastChgAdapterType, LastChgWorkItemId, DbUpdatedTimestamp, LastChgTimestamp) " +
                    "VALUES (?, ?, ?, ?, ?, ?, NULL, NULL, ?, ?, ?, ?);"
    );

    public long run(long lDiagId, String sLctn, long lServiceOperationId, String sDiag, String sReqAdapterType,
                    long lReqWorkItemId) throws VoltAbortException {
        //---------------------------------------------------------------------
        // Create a row in the table for this entry.
        //---------------------------------------------------------------------
        Long serviceOperationId = null;
        if (lServiceOperationId != -99999L) {
            serviceOperationId = lServiceOperationId;
        }

        Date now = this.getTransactionTime();

        voltQueueSQL(insertDiagSql
                ,lDiagId
                ,sLctn
                ,serviceOperationId        // ServiceOperationId
                ,sDiag
                ,"W"                        // State - W = Working/running
                ,now                        // StartTimestamp
                ,sReqAdapterType
                ,lReqWorkItemId
                ,now                        // DbUpdatedTimestamp
                ,now                        // LastChgTimestamp
        );

        voltQueueSQL(insertDiagHistorySql
                ,lDiagId
                ,sLctn
                ,serviceOperationId        // ServiceOperationId
                ,sDiag
                ,"W"                        // State - W = Working/running
                ,now                        // StartTimestamp
                ,sReqAdapterType
                ,lReqWorkItemId
                ,now                        // DbUpdatedTimestamp
                ,now                        // LastChgTimestamp
        );

        voltExecuteSQL(true);
        return 0;
    }
}