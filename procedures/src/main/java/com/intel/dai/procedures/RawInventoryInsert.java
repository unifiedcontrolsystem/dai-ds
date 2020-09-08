// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

public class RawInventoryInsert extends VoltProcedure {
    private static final String upsertFruSqlCmd =
            "UPSERT INTO HW_Inventory_FRU (FRUID, FRUType, FRUSubType, FRUInfo, DbUpdatedTimestamp)" +
                    " VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP);";
    private static final String upsertLocSqlCmd =
            "UPSERT INTO HW_Inventory_Location (ID, Type, Ordinal, FRUID, Info, DbUpdatedTimestamp)" +
                    " VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP);";

    public static final long SUCCESSFUL = 0;

    public static final SQLStmt upsertIntoHWInvFruStmt = new SQLStmt(upsertFruSqlCmd);
    public static final SQLStmt upsertIntoHWInvLocStmt = new SQLStmt(upsertLocSqlCmd);

    public long run(String id, String type, int ordinal, String info,
                    String fruId, String fruType, String fruSubType, String fruInfo)
            throws VoltAbortException {

        // If fruid is null, only the loc can be captured.  However, this is NOT a
        // failure.  It most likely means the the loc is EMPTY.
        if (fruId != null) {
            voltQueueSQL(upsertIntoHWInvFruStmt, fruId, fruType, fruSubType, fruInfo);
        }
        voltQueueSQL(upsertIntoHWInvLocStmt, id, type, ordinal, fruId, info);

        voltExecuteSQL();
        return 0;  // voltdb stored procedures cannot return void
    }
}
