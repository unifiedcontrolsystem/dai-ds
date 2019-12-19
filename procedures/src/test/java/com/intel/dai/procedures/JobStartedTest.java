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

public class JobStartedTest {
    class MockJobStarted extends JobStarted {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL() { return voltExecuteSQL(false); }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            return null;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
    }

    @Test
    public void run() {
        MockJobStarted proc = new MockJobStarted();
        proc.run("JobId", "JobName", "Bsn", 16, new byte[] {-1, -1},
                "Username", 0L, "RAS", 9999L);
    }
}