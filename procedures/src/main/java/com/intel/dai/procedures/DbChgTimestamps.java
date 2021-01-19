// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;
import java.util.Arrays;
import java.util.ArrayList;


/**
 * This stored procedure is "Temporary" for use during the DAI Prototype, during the prototype we are using VoltDB to represent the entire data store not just Tier1.
 *
 */

public class DbChgTimestamps extends VoltProcedure {

    public final SQLStmt selectCountComputeNodeHistoryTimestampsSql    = new SQLStmt("SELECT COUNT(*) FROM ComputeNode_History;");
    public final SQLStmt selectCountRasTimestampSql                    = new SQLStmt("SELECT COUNT(*), MAX(DbUpdatedTimestamp) FROM RasEvent;");
    public final SQLStmt selectCountJobHistoryTimestampsSql            = new SQLStmt("SELECT COUNT(*) FROM Job_History;");
    public final SQLStmt selectCountWlmReservationHistoryTimestampsSql = new SQLStmt("SELECT COUNT(*) FROM WlmReservation_History;");
    public final SQLStmt selectCountServiceNodeHistoryTimestampsSql    = new SQLStmt("SELECT COUNT(*) FROM ServiceNode_History;");



    public VoltTable run() throws VoltAbortException {
        // Create the VoltTable that will be returned to the caller.
        VoltTable vtReturnToCaller = new VoltTable(new VoltTable.ColumnInfo("Key",   VoltType.STRING),
                                                   new VoltTable.ColumnInfo("Value", VoltType.STRING));

        // Queue up the sql stmts and execute them.
        voltQueueSQL(selectCountComputeNodeHistoryTimestampsSql, EXPECT_ONE_ROW);
        voltQueueSQL(selectCountRasTimestampSql, EXPECT_ONE_ROW);
        voltQueueSQL(selectCountJobHistoryTimestampsSql, EXPECT_ONE_ROW);
        voltQueueSQL(selectCountWlmReservationHistoryTimestampsSql, EXPECT_ONE_ROW);
        voltQueueSQL(selectCountServiceNodeHistoryTimestampsSql, EXPECT_ONE_ROW);
        VoltTable[] aVtInput = voltExecuteSQL(true);

        // Grab the info returned from the ComputeNode_History table.
        aVtInput[0].advanceRow();
        vtReturnToCaller.addRow("ComputeNodeHistory_Count", Long.toString(aVtInput[0].getLong(0)));

        // Grab the info returned from the RasEvent table.
        aVtInput[1].advanceRow();
        // Need to use max DbUpdatedTimestamp for RAS events because we actually sometimes do updates to ras event rows (when changing Done from N to Y), not just inserts
        // (so if we only used count(*) it would not change when updates occur so the ras event data being displayed in the GUI would be stale).
        vtReturnToCaller.addRow("Ras_Count", Long.toString(aVtInput[1].getLong(0)));
        if (aVtInput[1].getTimestampAsTimestamp(1) != null)
            vtReturnToCaller.addRow("Ras_Max_DbUpdatedTimestamp", aVtInput[1].getTimestampAsTimestamp(1).toString());
        else
            vtReturnToCaller.addRow("Ras_Max_DbUpdatedTimestamp", "0");

        // Grab the info returned from the Job_History table.
        aVtInput[2].advanceRow();
        vtReturnToCaller.addRow("JobHistory_Count", Long.toString(aVtInput[2].getLong(0)));

        // Grab the info returned from the WlmReservation_History table.
        aVtInput[3].advanceRow();
        vtReturnToCaller.addRow("Reservation_Count", Long.toString(aVtInput[3].getLong(0)));

        // Grab the info returned from the ServiceNode_History table.
        aVtInput[4].advanceRow();
        vtReturnToCaller.addRow("ServiceNodeHistory_Count", Long.toString(aVtInput[4].getLong(0)));

        // Returns the information to the caller.
        return vtReturnToCaller;
    }
}
