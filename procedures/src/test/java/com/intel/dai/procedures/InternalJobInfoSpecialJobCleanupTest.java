// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.junit.Test;
import org.voltdb.Expectation;
import org.voltdb.SQLStmt;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.types.TimestampType;

import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

public class InternalJobInfoSpecialJobCleanupTest {
    class MockInternalJobInfoSpecialJobCleanup extends InternalJobInfoSpecialJobCleanup {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(new VoltTable.ColumnInfo("WlmJobStartTime", VoltType.TIMESTAMP));
            if(!doZeroRow[doZeroIndex++])
                result[0].addRow(new TimestampType(Date.from(Instant.now())));
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }

        private boolean[] doZeroRow = new boolean[] {false, false, false, false};
        private int doZeroIndex = 0;
    }

    @Test
    public void run() {
        MockInternalJobInfoSpecialJobCleanup proc = new MockInternalJobInfoSpecialJobCleanup();
        proc.run("Lctn", 5L, 0L);
    }

    @Test
    public void run2() {
        MockInternalJobInfoSpecialJobCleanup proc = new MockInternalJobInfoSpecialJobCleanup();
        proc.doZeroRow = new boolean[] {true, false, false, false};
        proc.run("Lctn", 5L, 0L);
    }

    @Test
    public void run3() {
        MockInternalJobInfoSpecialJobCleanup proc = new MockInternalJobInfoSpecialJobCleanup();
        proc.doZeroRow = new boolean[] {true, true, false, false};
        proc.run("Lctn", 5L, 0L);
    }
}
