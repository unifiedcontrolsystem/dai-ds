// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class HwInventoryHistoryEventCount extends VoltProcedure {
    private static final String SQL_COUNT_TEXT =
            "SELECT COUNT(*) FROM HW_Inventory_History WHERE " +
                    "Action=? AND ID=? AND FRUID=? AND ForeignTimestamp=?";

    public static final SQLStmt sqlCountstmt = new SQLStmt(SQL_COUNT_TEXT);

    public long run(String action, String id, String fruId, String foreignServerTimestamp)
            throws VoltAbortException {

        voltQueueSQL(sqlCountstmt, action, id, fruId, foreignServerTimestamp);
        VoltTable result = voltExecuteSQL()[0];
        if (result.getRowCount() < 1) {
            return -1;
        }
        return result.fetchRow(0).getLong(0);
    }
}
