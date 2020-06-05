// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;

public class FwVersionHistoryInsert extends VoltProcedure {
    private static final String sqlCmd =
            "INSERT INTO FW_Version_History (ID, TargetID, Version, DbUpdatedTimestamp) VALUES (?, ?, ?, " +
                    "CURRENT_TIMESTAMP);";

    public static final long SUCCESSFUL = 0;

    public static final SQLStmt sqlStmt = new SQLStmt(sqlCmd);

    public long run(String id, String targetId, String version)
            throws VoltProcedure.VoltAbortException {

        voltQueueSQL(sqlStmt, id, targetId, version);
        voltExecuteSQL();
        return SUCCESSFUL;
    }
}
