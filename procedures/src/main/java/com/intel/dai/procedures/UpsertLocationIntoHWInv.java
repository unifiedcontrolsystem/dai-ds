// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

public class UpsertLocationIntoHWInv extends VoltProcedure {
    private static final String upsertFruSqlCmd =
            "UPSERT INTO HW_Inventory_FRU (FRUID, FRUType, FRUSubType, DbUpdatedTimestamp) VALUES (?, ?, ?, CURRENT_TIMESTAMP);";
    private static final String upsertLocSqlCmd =
            "UPSERT INTO HW_Inventory_Location (ID, Type, Ordinal, FRUID, DbUpdatedTimestamp) VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP);";

    public static final long SUCCESSFUL = 0;
    public static final long FAILED = 1;

    public static final String emptyPrefix = "empty-";

    public static final SQLStmt upsertIntoHWInvFruStmt = new SQLStmt(upsertFruSqlCmd);
    public static final SQLStmt upsertIntoHWInvLocStmt = new SQLStmt(upsertLocSqlCmd);

    public long run(String id, String type, int ordinal,
                    String fruId, String fruType, String fruSubType)
            throws VoltAbortException {

        if (fruId == null) {
            return FAILED;
        }
        if (fruId.indexOf(emptyPrefix) == 0) {
            if (fruType != null) {
                return FAILED;
            }
        }
        if (fruType == null) {
            if (fruSubType != null) {
                return FAILED;
            }
        }

        voltQueueSQL(upsertIntoHWInvFruStmt, fruId, fruType, fruSubType);
        voltQueueSQL(upsertIntoHWInvLocStmt, id, type, ordinal, fruId);

        voltExecuteSQL();
        return SUCCESSFUL;
    }
}
