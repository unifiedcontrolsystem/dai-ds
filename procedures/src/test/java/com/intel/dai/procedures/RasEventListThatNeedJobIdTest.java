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

public class RasEventListThatNeedJobIdTest {
    class MockRasEventListThatNeedJobId extends RasEventListThatNeedJobId {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("Something", VoltType.STRING)
            );
            result[0].addRow("Something");
            return result;
        }
    }

    @Test
    public void run() {
        MockRasEventListThatNeedJobId proc = new MockRasEventListThatNeedJobId();
        proc.run(0L);
    }
}
