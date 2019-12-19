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

public class ServiceNodeCommonTest {
    class MockServiceNodeCommon extends ServiceNodeCommon {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(new VoltTable.ColumnInfo("Timestamp", VoltType.BIGINT));
            if(firstTime) {
                result[0].addRow(10L);
                firstTime = false;
            }
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }

        private boolean firstTime = true;
    }

    @Test
    public void ensureHaveUniqueServiceNodeLastChgTimestamp() {
        MockServiceNodeCommon proc = new MockServiceNodeCommon();
        proc.ensureHaveUniqueServiceNodeLastChgTimestamp("Lctn", 9L, 9L);
    }

    @Test
    public void ensureHaveUniqueServiceNodeLastChgTimestamp2() {
        MockServiceNodeCommon proc = new MockServiceNodeCommon();
        proc.ensureHaveUniqueServiceNodeLastChgTimestamp("Lctn", 0L, 10L);
    }

    @Test
    public void getServiceNode() {
        MockServiceNodeCommon proc = new MockServiceNodeCommon();
        proc.getServiceNode("Lctn");
    }

    @Test
    public void getServiceNodeFromHistoryWithPrecedingTime() {
        MockServiceNodeCommon proc = new MockServiceNodeCommon();
        proc.getServiceNodeFromHistoryWithPrecedingTime("Lctn", 20L);
    }
}
