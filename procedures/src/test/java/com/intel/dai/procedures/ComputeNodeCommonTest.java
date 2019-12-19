// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.junit.Test;
import org.voltdb.Expectation;
import org.voltdb.SQLStmt;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;

import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

public class ComputeNodeCommonTest {
    class MockComputeNodeCommon extends ComputeNodeCommon {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        private boolean firstTime = true;
        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("NEXTVALUE", VoltType.BIGINT)
            );
            if(firstTime) {
                result[0].addRow(9999);
                firstTime = false;
            }
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
    }

    @Test
    public void ensureHaveUniqueComputeNodeLastChgTimestamp() {
        ComputeNodeCommon proc = new MockComputeNodeCommon();
        long micro = Instant.now().toEpochMilli() * 1000;
        proc.ensureHaveUniqueComputeNodeLastChgTimestamp("LOCATION", micro, micro);
    }
}
