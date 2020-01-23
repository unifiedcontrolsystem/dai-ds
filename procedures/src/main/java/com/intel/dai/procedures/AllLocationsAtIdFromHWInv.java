// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

/**
 * Select the children of the given location.
 */
public class AllLocationsAtIdFromHWInv extends VoltProcedure {
    private static final String SQL_TEXT =
            "SELECT * FROM " +
                    "(SELECT * FROM HW_Inventory_Location I, HW_Inventory_FRU F WHERE I.FRUID = F.FRUID) AS " +
                    "HW_Inventory WHERE HW_Inventory.ID STARTS WITH ? ORDER BY HW_Inventory.ID;";

    public static final SQLStmt sqlStmt = new SQLStmt(SQL_TEXT);

    public VoltTable run(String id)
            throws VoltAbortException {
        voltQueueSQL(sqlStmt, id);
        VoltTable[] vt = voltExecuteSQL();
        return vt[0];
    }
}
