package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

public class UpsertFRUIntoHWInv extends VoltProcedure {

    private static final String SQL_TEXT =
            "UPSERT INTO HW_Location (ID, ParentID, Type, Ordinal, FRUID, FRUType, FRUSubType) VALUES (?, ?, ?, ?,  ?, ?, ?);";

    public static final long UPSERT_SUCCESSFUL = 0;
    public static final long UPSERT_FAILED = 1;

    public static final String emptyPrefix = "empty-";

    public static final SQLStmt upsertFRUIntoHWLocationStmt = new SQLStmt(SQL_TEXT);

    public long run(String id, String parentId, String type, int ordinal,
                    String fruId, String fruType, String fruSubType)
            throws VoltAbortException {

        if (fruId == null) {
            return UPSERT_FAILED;
        }
        if (fruId.indexOf(emptyPrefix) == 0) {
            if (fruType != null) {
                return UPSERT_FAILED;
            }
        }
        if (fruType == null) {
            if (fruSubType != null) {
                return UPSERT_FAILED;
            }
        }
        voltQueueSQL(upsertFRUIntoHWLocationStmt, id, parentId, type, ordinal,
                fruId, fruType, fruSubType);
        voltExecuteSQL();
        return UPSERT_SUCCESSFUL;
    }
}
