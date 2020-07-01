// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class HwInventoryHistoryLastUpdateTimestamp extends VoltProcedure {
    private static final String SQL_TEXT = "SELECT MAX(foreignServerTimestamp) FROM HW_Inventory_History;";

    public static final SQLStmt sqlStmt = new SQLStmt(SQL_TEXT);

    /**
     * Return the latest HW inventory history update timestamp string in RFC-3339 format.
     * @return string containing the latest update timestamp if it can be determined; otherwise null
     * @throws VoltAbortException volt abort exception
     */
    public String run()
            throws VoltAbortException {
        voltQueueSQL(sqlStmt);
        VoltTable result = voltExecuteSQL()[0];
        if (result.getRowCount() < 1) {
            return null;
        }
        return result.fetchRow(0).getString(0);
    }
}
