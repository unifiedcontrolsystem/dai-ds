package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class FwVersionDump extends VoltProcedure {
    private static final String SQL_TEXT =
            "SELECT * FROM FW_Version WHERE ID STARTS WITH ? ORDER BY DbUpdatedTimestamp;";

    public static final SQLStmt sqlStmt = new SQLStmt(SQL_TEXT);

    public VoltTable run(String id)
            throws VoltProcedure.VoltAbortException {
        voltQueueSQL(sqlStmt, id);
        VoltTable[] vt = voltExecuteSQL();
        return vt[0];
    }
}
