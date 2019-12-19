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

public class DbChgTimestampsTest {
    class MockDbChgTimestamps extends DbChgTimestamps {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[5];
            result[0] = new VoltTable(new VoltTable.ColumnInfo("DbUpdatedTimestamp", VoltType.TIMESTAMP),
                    new VoltTable.ColumnInfo("Count", VoltType.BIGINT));
            result[1] = new VoltTable(new VoltTable.ColumnInfo("Count", VoltType.BIGINT));
            result[2] = new VoltTable(new VoltTable.ColumnInfo("Default", VoltType.TIMESTAMP),
                    new VoltTable.ColumnInfo("Count", VoltType.BIGINT));
            result[3] = new VoltTable(new VoltTable.ColumnInfo("Default", VoltType.TIMESTAMP),
                    new VoltTable.ColumnInfo("Count", VoltType.BIGINT));
            result[4] = new VoltTable(new VoltTable.ColumnInfo("Default", VoltType.TIMESTAMP),
                    new VoltTable.ColumnInfo("Count", VoltType.BIGINT));
            if(doNull) {
                result[0].addRow(new TimestampType(Date.from(Instant.now())), 9999L);
                result[1].addRow( 9999L);
                result[2].addRow((Object)null, 9999L);
                result[3].addRow((Object)null, 9999L);
                result[4].addRow((Object)null, 9999L);

            } else {
                result[0].addRow(new TimestampType(Date.from(Instant.now())), 9999L);
                result[1].addRow( 9999L);
                result[2].addRow(new TimestampType(Date.from(Instant.now())), 9999L);
                result[3].addRow(new TimestampType(Date.from(Instant.now())), 9999L);
                result[4].addRow(new TimestampType(Date.from(Instant.now())), 9999L);
            }
            return result;
        }
        private boolean doNull = false;
    }

    @Test
    public void run() {
        MockDbChgTimestamps proc = new MockDbChgTimestamps();
        proc.run();
    }

    @Test
    public void run2() {
        MockDbChgTimestamps proc = new MockDbChgTimestamps();
        proc.doNull = true;
        proc.run();
    }
}
