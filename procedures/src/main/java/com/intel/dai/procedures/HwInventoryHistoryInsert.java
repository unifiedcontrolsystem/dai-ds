// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class HwInventoryHistoryInsert extends VoltProcedure {
    private static final String SQL_TEXT =
            "INSERT INTO HW_Inventory_History (Action, ID, FRUID, DbUpdateTimestamp) VALUES (?, ?, ?, CURRENT_TIMESTAMP);";

    public static final long SUCCESSFUL = 0;
    public static final long FAILED = 1;

    public static final SQLStmt sqlStmt = new SQLStmt(SQL_TEXT);

    public long run(String action, String id, String fruId)
            throws VoltAbortException {

        if (action == null) {
            return FAILED;
        }
        if (id == null) {
            return FAILED;
        }
        if (fruId == null) {
            return FAILED;
        }

        switch (action.toUpperCase()) {
            case "INSERTED":
            case "DELETED":
                break;
            default:
                return FAILED;
        }

        voltQueueSQL(sqlStmt, action, id, fruId);
        VoltTable[] vt = voltExecuteSQL();
        return SUCCESSFUL;
    }
}
