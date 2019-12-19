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

public class ServiceNodeHistoryListOfStateAtTimeTest {
    class MockServiceNodeHistoryListOfStateAtTime extends ServiceNodeHistoryListOfStateAtTime {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                    new VoltTable.ColumnInfo("SequenceNumber", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("State", VoltType.STRING)
            );
            result[0].addRow("Lctn", 9999L, "State");
            result[0].addRow("Lctn", 9997L, "State");
            result[0].addRow("Lctn2", 9998L, "State");
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
    }

    @Test
    public void run() {
        MockServiceNodeHistoryListOfStateAtTime proc = new MockServiceNodeHistoryListOfStateAtTime();
        proc.run(new TimestampType(Date.from(Instant.now())), new TimestampType(Date.from(Instant.now())));
    }

    @Test
    public void run2() {
        MockServiceNodeHistoryListOfStateAtTime proc = new MockServiceNodeHistoryListOfStateAtTime();
        proc.run(null, new TimestampType(Date.from(Instant.now())));
    }

    @Test
    public void run3() {
        MockServiceNodeHistoryListOfStateAtTime proc = new MockServiceNodeHistoryListOfStateAtTime();
        proc.run(new TimestampType(Date.from(Instant.now())), null);
    }

    @Test
    public void run4() {
        MockServiceNodeHistoryListOfStateAtTime proc = new MockServiceNodeHistoryListOfStateAtTime();
        proc.run(null, null);
    }
}
