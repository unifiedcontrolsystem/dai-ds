// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

public class DeleteAllLocationsAtIdFromHWInv extends VoltProcedure {
    private static final String SQL_TEXT = "DELETE FROM HW_Inventory_Location WHERE ID STARTS WITH ?;";

    public static final long SUCCESSFUL = 0;

    public static final SQLStmt sqlStmt = new SQLStmt(SQL_TEXT);

    public long run(String id)
            throws VoltAbortException {

        voltQueueSQL(sqlStmt, id);
        voltExecuteSQL();
        return SUCCESSFUL;
    }
}
