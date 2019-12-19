// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Date;

import org.voltdb.*;

/**
 * Store the results for per smallest unit of hardware after diagnostic completed
 * NOTE:
 *
 *  Input parameter:
 *      long    lDiagId             - id that uniquely identifies this specific instance of a diagnostic run
 *      String  sLctn               - hardware location string of the hardware that this diagnostic was running on, a compute node, a whole rack, a cdu, power distribution unit, a switch, etc.
 *      String  sState              - Actual state the diagnostic result for this hardware unit is in
 *      String  sResults            - Diagnostic result for this hardware unit
 *
 */

public class DiagResultSavePerUnit extends VoltProcedure {

    public final SQLStmt selectDiagSql = new SQLStmt(
            "SELECT * from Diag where DiagId = ?;"
    );


    public final SQLStmt insertDiagResultSql = new SQLStmt(
                    "INSERT INTO DiagResults " +
                    "(DiagId, Lctn, State, Results, DbUpdatedTimestamp) " +
                    "VALUES (?, ?, ?, ?, ?);"
    );

    public long run(long lDiagId, String sLctn, String cState, String sResults) throws VoltAbortException {
        //---------------------------------------------------------------------
        // Ensure the DiagId is in Diag Table
        //---------------------------------------------------------------------
        voltQueueSQL(selectDiagSql, EXPECT_ZERO_OR_ONE_ROW, lDiagId);
        VoltTable[] aDiagData = voltExecuteSQL();
        VoltTable diagData = aDiagData[0];
        if (diagData.getRowCount() == 0) {
           throw new VoltAbortException("DiagResultSavePerUnit - there is no entry in the Diag table for the specified " +
                "diagnostic ID (" + lDiagId );
        }
        Date now = this.getTransactionTime();

        voltQueueSQL(insertDiagResultSql
                ,lDiagId
                ,sLctn
                ,cState                     // State - completion status of diagnostics. P-Pass,F-Fail,E-Error didnt run/complete, U-completed but result not determined
                ,sResults                   // Results
                ,now                        // DbUpdatedTimestamp
        );

        voltExecuteSQL(true);
        return 0;
    }
}
