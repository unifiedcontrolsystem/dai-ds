// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Arrays;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary to find the diagnostic tool ID corresponding to a diagnostic list ID.
 *
 *  Input parameter:
 *     String  sDiagListId  = The diagnostic list ID (used for finding the diagnostic tool corresponding to it)
 *
 *  Returns:
 *      VoltTable containing pertinent information about this work item.
 *          - DiagToolId   // Unique Diagnostic tool id that needs to be run for the DiagListId
 *
 *  Sample invocation:
 *      echo "Exec GetDiagToolId, RunInbandDiagnostics;" | sqlcmd
 *          Selects the DiagListId entry from Diag_List table and returns the corresponding DiagToolId
 *
 */

public class DiagGetDiagToolId extends VoltProcedure {


    final String selectDiagToolId = "SELECT " +
        "DiagListId, DiagToolId " +
        "FROM Diag_List WHERE DiagListId=? " +
        "Order By DiagListId ;";
    public final SQLStmt selectDiagToolIdsql = new SQLStmt(selectDiagToolId);


    public VoltTable run(String sDiagListId) throws VoltAbortException {

    VoltTable[] aDiagListEntry;
    voltQueueSQL(selectDiagToolIdsql, EXPECT_ONE_ROW, sDiagListId);
    aDiagListEntry = voltExecuteSQL();
    aDiagListEntry[0].advanceRow();
        
    //--------------------------------------------------
    // Get the DiagnosticToolId and return to the caller 
    //--------------------------------------------------
    String sDiagToolId = aDiagListEntry[0].getString("DiagToolId");
    VoltTable vt = new VoltTable(new VoltTable.ColumnInfo("DiagListId",   VoltType.STRING),
                                 new VoltTable.ColumnInfo("DiagToolId",   VoltType.STRING));
    vt.addRow(aDiagListEntry[0].getString("DiagListId"),
                  sDiagToolId);

    // Returns the Results field for this work item
    return vt;
    }

}
