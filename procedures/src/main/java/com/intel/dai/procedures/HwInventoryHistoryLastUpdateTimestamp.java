// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class HwInventoryHistoryLastUpdateTimestamp extends VoltProcedure {
    private static final String SQL_TEXT = "SELECT MAX(DbUpdatedTimestamp) FROM HW_Inventory_History;";

    public static final SQLStmt sqlStmt = new SQLStmt(SQL_TEXT);

    public long run()
            throws VoltAbortException {
        voltQueueSQL(sqlStmt);
        VoltTable result = voltExecuteSQL()[0];
        if (result.getRowCount() < 1) {
            return -1;
        }
        return result.fetchRow(0).getLong(0);
    }
}
