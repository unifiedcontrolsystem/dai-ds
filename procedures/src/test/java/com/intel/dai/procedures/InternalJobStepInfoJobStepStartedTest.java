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

public class InternalJobStepInfoJobStepStartedTest {
    class MockInternalJobStepInfoJobStepStarted extends InternalJobStepInfoJobStepStarted {
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
        MockInternalJobStepInfoJobStepStarted proc = new MockInternalJobStepInfoJobStepStarted();
        proc.run("JobId", "Step");
    }

    @Test
    public void run2() {
        MockInternalJobStepInfoJobStepStarted proc = new MockInternalJobStepInfoJobStepStarted();
        proc.doZeroRow = true;
        proc.run("JobId", "Step");
    }
}