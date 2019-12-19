// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.voltdb.VoltProcedure;
import org.voltdb.SQLStmt;

/**
 * Set (update or insert) a UCS configuration value.
 *
 *  Input parameter:
 *      String  sKey                    = string containing the configuration key
 *      String  sValue                  = string containing the configuration value
 */

public class UcsConfigValueSet extends VoltProcedure {
    public final SQLStmt upsertUcsConfigValue =
            new SQLStmt("UPSERT INTO UcsConfigValue (Key, Value, DbUpdatedTimestamp) VALUES (?, ?, ?);");

    public long run(String key, String value) throws VoltAbortException {
        voltQueueSQL(upsertUcsConfigValue, key, value, this.getTransactionTime());
        voltExecuteSQL(true);

        return 0;
    }
}