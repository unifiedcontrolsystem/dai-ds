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

public class AggregatedEnvDataStoreTest {
    class MockAggregatedEnvDataStore extends AggregatedEnvDataStore {
        boolean realResult = false;
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("NEXTVALUE", VoltType.BIGINT)
            );
            result[0].addRow(9999);
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
    }

    @Test
    public void run() {
        AggregatedEnvDataStore proc = new MockAggregatedEnvDataStore();
        proc.run("DATA_TYPE", "LOCATION", Instant.now().toEpochMilli() * 1000,
                1.0, 1.0, 1.0, "MONITORING", 9999L);
    }
}