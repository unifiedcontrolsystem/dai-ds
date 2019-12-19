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

public class ComputeNodeHistoryListOfStateAtTimeTest {
    class MockComputeNodeHistoryListOfStateAtTime extends ComputeNodeHistoryListOfStateAtTime {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        private boolean zeroRows = false;
        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                    new VoltTable.ColumnInfo("SequenceNumber", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("State", VoltType.STRING)
            );
            if(!zeroRows) {
                result[0].addRow("R0-CH0-CN0", 1234, "D");
                result[0].addRow("R0-CH0-CN1", 1235, "A");
            }
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
    }

    @Test
    public void run() {
        ComputeNodeHistoryListOfStateAtTime proc = new MockComputeNodeHistoryListOfStateAtTime();
        TimestampType ts = new TimestampType(Date.from(Instant.now()));
        proc.run(null, null);
    }

    @Test
    public void run2() {
        ComputeNodeHistoryListOfStateAtTime proc = new MockComputeNodeHistoryListOfStateAtTime();
        TimestampType ts = new TimestampType(Date.from(Instant.now()));
        proc.run(null, ts);
    }

    @Test
    public void run3() {
        ComputeNodeHistoryListOfStateAtTime proc = new MockComputeNodeHistoryListOfStateAtTime();
        TimestampType ts = new TimestampType(Date.from(Instant.now()));
        proc.run(ts, null);
    }

    @Test
    public void run4() {
        ComputeNodeHistoryListOfStateAtTime proc = new MockComputeNodeHistoryListOfStateAtTime();
        TimestampType ts = new TimestampType(Date.from(Instant.now()));
        proc.run(ts, ts);
    }
}
