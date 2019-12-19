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

    public final SQLStmt selectMaxComputeNodeHistoryTimestampsSql       = new SQLStmt("SELECT MAX(DbUpdatedTimestamp), COUNT(*) FROM ComputeNode_History;");
    public final SQLStmt selectMaxRasTimestampSql                       = new SQLStmt("SELECT COUNT(*) FROM RasEvent;");
    public final SQLStmt selectMaxJobHistoryTimestampsSql               = new SQLStmt("SELECT MAX(DbUpdatedTimestamp), COUNT(*) FROM Job_History;");
    public final SQLStmt selectMaxWlmReservationHistoryTimestampsSql    = new SQLStmt("SELECT MAX(DbUpdatedTimestamp), COUNT(*) FROM WlmReservation_History;");
    public final SQLStmt selectMaxServiceNodeHistoryTimestampsSql       = new SQLStmt("SELECT MAX(DbUpdatedTimestamp), COUNT(*) FROM ServiceNode_History;");



    public VoltTable run() throws VoltAbortException {

        // Create the VoltTable that will be returned to the caller.
        VoltTable vtReturnToCaller = new VoltTable(new VoltTable.ColumnInfo("Key",   VoltType.STRING),
                                                   new VoltTable.ColumnInfo("Value", VoltType.STRING));

        // Queue up the sql stmts and execute them.
        voltQueueSQL(selectMaxComputeNodeHistoryTimestampsSql, EXPECT_ONE_ROW);
        voltQueueSQL(selectMaxRasTimestampSql, EXPECT_ONE_ROW);
        voltQueueSQL(selectMaxJobHistoryTimestampsSql, EXPECT_ONE_ROW);
        voltQueueSQL(selectMaxWlmReservationHistoryTimestampsSql, EXPECT_ONE_ROW);
        voltQueueSQL(selectMaxServiceNodeHistoryTimestampsSql, EXPECT_ONE_ROW);
        VoltTable[] aVtInput = voltExecuteSQL(true);

        // Grab the info returned from the ComputeNode_History table.
        aVtInput[0].advanceRow();
        if (aVtInput[0].getTimestampAsTimestamp(0) != null)
            vtReturnToCaller.addRow("ComputeNodeHistory_Max_DbUpdatedTimestamp", aVtInput[0].getTimestampAsTimestamp(0).toString());
        else
            vtReturnToCaller.addRow("ComputeNodeHistory_Max_DbUpdatedTimestamp", "0");
        vtReturnToCaller.addRow("ComputeNodeHistory_Count", Long.toString(aVtInput[0].getLong(1)));

        // Grab the info returned from the RasEvent table.
        aVtInput[1].advanceRow();
        vtReturnToCaller.addRow("Ras_Count", Long.toString(aVtInput[1].getLong(0)));

        // Grab the info returned from the Job_History table.
        aVtInput[2].advanceRow();
        if (aVtInput[2].getTimestampAsTimestamp(0) != null)
            vtReturnToCaller.addRow("JobHistory_Max_DbUpdatedTimestamp", aVtInput[2].getTimestampAsTimestamp(0).toString());
        else
            vtReturnToCaller.addRow("JobHistory_Max_DbUpdatedTimestamp", "0");
        vtReturnToCaller.addRow("JobHistory_Count", Long.toString(aVtInput[2].getLong(1)));

        // Grab the info returned from the WlmReservation_History table.
        aVtInput[3].advanceRow();
        if (aVtInput[3].getTimestampAsTimestamp(0) != null)
            vtReturnToCaller.addRow("Reservation_Max_DbUpdatedTimestamp", aVtInput[3].getTimestampAsTimestamp(0).toString());
        else
            vtReturnToCaller.addRow("Reservation_Max_DbUpdatedTimestamp", "0");
        vtReturnToCaller.addRow("Reservation_Count", Long.toString(aVtInput[3].getLong(1)));

        // Grab the info returned from the ServiceNode_History table.
        aVtInput[4].advanceRow();
        if (aVtInput[4].getTimestampAsTimestamp(0) != null)
            vtReturnToCaller.addRow("ServiceNodeHistory_Max_DbUpdatedTimestamp", aVtInput[4].getTimestampAsTimestamp(0).toString());
        else
            vtReturnToCaller.addRow("ServiceNodeHistory_Max_DbUpdatedTimestamp", "0");
        vtReturnToCaller.addRow("ServiceNodeHistory_Count", Long.toString(aVtInput[4].getLong(1)));

        // Returns the information to the caller.
        return vtReturnToCaller;
    }
}
