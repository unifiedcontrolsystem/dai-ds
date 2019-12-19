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

public class DiagStartedTest {
    class MockDiagStarted extends DiagStarted {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

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
        MockDiagStarted proc = new MockDiagStarted();
        proc.run(0L, "Lctn", 0L, "sDiag", "RAS", 0L);
        proc.run(0L, "Lctn", -99999L, "sDiag", "RAS", 0L);
    }
}