// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.junit.Test;
import org.voltdb.Expectation;
import org.voltdb.SQLStmt;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;

import java.sql.Date;
import java.time.Instant;

import static org.junit.Assert.*;

public class RasEventProcessNewControlOperationsTest {
    class MockRasEventProcessNewControlOperations extends RasEventProcessNewControlOperations {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("Count", VoltType.BIGINT)
            );
            result[0].addRow(10L);
            return result;
        }

        @Override
        public java.util.Date getTransactionTime() {
            return Date.from(Instant.now());
        }
    }

    @Test
    public void run() throws NullPointerException {
        MockRasEventProcessNewControlOperations proc = new MockRasEventProcessNewControlOperations();
        proc.run();
    }
}
