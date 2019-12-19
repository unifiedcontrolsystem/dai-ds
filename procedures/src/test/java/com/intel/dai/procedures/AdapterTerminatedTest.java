// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.junit.Test;
import org.voltdb.Expectation;
import org.voltdb.SQLStmt;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;

import static org.junit.Assert.*;

public class AdapterTerminatedTest {
    class MockAdapterTerminated extends AdapterTerminated {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                    new VoltTable.ColumnInfo("Pid", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("SconRank", VoltType.BIGINT)
            );
            result[0].addRow("R0-CH0", 9999L, 12L);
            result[0].addRow("R0-CH0", 9999L, 12L);
            return result;
        }
    }

    @Test
    public void run() {
        AdapterTerminated proc = new MockAdapterTerminated();
        proc.run("RAS", 1234L, "WLM", 9999L);
    }
}
