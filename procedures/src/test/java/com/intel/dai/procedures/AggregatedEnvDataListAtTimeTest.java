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

public class AggregatedEnvDataListAtTimeTest {
    class MockAggregatedEnvDataListAtTime extends AggregatedEnvDataListAtTime {
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
        AggregatedEnvDataListAtTime proc = new MockAggregatedEnvDataListAtTime();
        proc.run(null, null);
        proc.run(new TimestampType(Date.from(Instant.now())), null);
        proc.run(null, new TimestampType(Date.from(Instant.now())));
        proc.run(new TimestampType(Date.from(Instant.now())), new TimestampType(Date.from(Instant.now())));
    }
}
