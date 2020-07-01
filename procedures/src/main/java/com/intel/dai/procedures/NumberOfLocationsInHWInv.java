// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

/**
 * Determine the number of locations in the HW inventory db.
 */
public class NumberOfLocationsInHWInv extends VoltProcedure {
    private static final String SQL_TEXT = "SELECT COUNT(ID) FROM HW_Inventory_Location;";

    public static final SQLStmt sqlStmt = new SQLStmt(SQL_TEXT);

    public long run() throws VoltAbortException {
        voltQueueSQL(sqlStmt);
        VoltTable result = voltExecuteSQL()[0];
        if (result.getRowCount() < 1) {
            return -1;
        }
        return result.fetchRow(0).getLong(0);
    }
}
