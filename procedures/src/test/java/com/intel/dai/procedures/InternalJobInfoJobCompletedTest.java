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

public class InternalJobInfoJobCompletedTest {
    class MockInternalJobInfoJobCompleted extends InternalJobInfoJobCompleted {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(new VoltTable.ColumnInfo("WlmJobStartTime", VoltType.TIMESTAMP));
            if(!doZeroRow)
                result[0].addRow(new TimestampType(Date.from(Instant.now())));
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
        MockInternalJobInfoJobCompleted proc = new MockInternalJobInfoJobCompleted();
        proc.run("JobId", "WorkDir", "A", 1L, 0L);
    }

    @Test
    public void run2() {
        MockInternalJobInfoJobCompleted proc = new MockInternalJobInfoJobCompleted();
        proc.doZeroRow = true;
        proc.run("JobId", "WorkDir", "A", 1L, 0L);
    }
}