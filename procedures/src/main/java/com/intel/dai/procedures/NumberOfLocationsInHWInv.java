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

    public long run()
            throws VoltProcedure.VoltAbortException {
        voltQueueSQL(sqlStmt);
        VoltTable[] vt =  voltExecuteSQL();
        VoltTable result = vt[0];
        return result.getRowCount();
    }
}
