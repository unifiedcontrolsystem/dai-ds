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

public class InternalJobStepInfoSpecialJobCleanupTest {
    class MockInternalJobStepInfoSpecialJobCleanup extends InternalJobStepInfoSpecialJobCleanup {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(new VoltTable.ColumnInfo("JobStepId", VoltType.STRING));
            if(!doZeroRow)
                result[0].addRow("JobId");
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
        private boolean doZeroRow = false;
    }

    @Test
    public void run() {
        MockInternalJobStepInfoSpecialJobCleanup proc = new MockInternalJobStepInfoSpecialJobCleanup();
        proc.run("JobId", 15L);
    }
}