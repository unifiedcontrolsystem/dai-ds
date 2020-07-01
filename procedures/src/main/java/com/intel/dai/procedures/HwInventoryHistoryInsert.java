// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

public class HwInventoryHistoryInsert extends VoltProcedure {
    private static final String SQL_INSERT_TEXT =
            "INSERT INTO HW_Inventory_History (Action, ID, FRUID, ForeignServerTimestamp, DbUpdatedTimestamp) VALUES " +
                    "(?, ?, ?, ?, CURRENT_TIMESTAMP);";

    public static final long SUCCESSFUL = 0;
    public static final long FAILED = 1;

    public static final SQLStmt sqlInsertStmt = new SQLStmt(SQL_INSERT_TEXT);

    public long run(String action, String id, String fruId, String foreignServerTimestamp)
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
        if (foreignServerTimestamp == null) {
            return FAILED;
        }

        switch (action.toUpperCase()) {
            case "ADDED":
            case "REMOVED":
                break;
            default:
                return FAILED;
        }

        voltQueueSQL(sqlInsertStmt, action, id, fruId, foreignServerTimestamp);
        voltExecuteSQL();
        return SUCCESSFUL;
    }
}
