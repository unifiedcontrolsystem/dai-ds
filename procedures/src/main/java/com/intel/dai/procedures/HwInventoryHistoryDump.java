package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class HwInventoryHistoryDump extends VoltProcedure {
    private static final String SQL_TEXT =
            "SELECT * FROM HW_Inventory_History ORDER BY DbUpdateTimestamp;";

    public static final SQLStmt sqlStmt = new SQLStmt(SQL_TEXT);

    public VoltTable run(String id)
            throws VoltAbortException {
        voltQueueSQL(sqlStmt, id);
        VoltTable[] vt = voltExecuteSQL();
        return vt[0];
    }
}
