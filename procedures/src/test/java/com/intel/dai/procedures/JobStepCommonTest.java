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

public class JobStepCommonTest {
    class MockJobStepCommon extends JobStepCommon {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(new VoltTable.ColumnInfo("JobStepId", VoltType.STRING));
            if(!doZeroRow[doZeroRowIndex++])
                result[0].addRow("JobId");
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }

        private boolean[] doZeroRow = new boolean[] { false, true };
        private int doZeroRowIndex = 0;
    }

    @Test
    public void ensureHaveUniqueJobLastChgTimestamp() {
        MockJobStepCommon proc = new MockJobStepCommon();
        proc.ensureHaveUniqueJobStepLastChgTimestamp("JobId", "JobStepId", 10L, 10L);
    }
}
