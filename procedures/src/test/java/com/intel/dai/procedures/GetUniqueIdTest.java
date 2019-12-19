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

public class GetUniqueIdTest {
    class MockGetUniqueId extends GetUniqueId {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("UniqueId", VoltType.BIGINT)
            );
            if(!firstTime)
                result[0].addRow(23L);
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
        private boolean firstTime = true;
    }

    @Test(expected = IllegalStateException.class)
    public void run() {
        MockGetUniqueId proc = new MockGetUniqueId();
        proc.run("Entity");
    }

    @Test
    public void run2() {
        MockGetUniqueId proc = new MockGetUniqueId();
        proc.firstTime = false;
        proc.run("Entity");
    }
}