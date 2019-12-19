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

public class InternalJobInfoJobRemoveTest {
    class MockInternalJobInfoJobRemove extends InternalJobInfoJobRemove {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            return null;
        }
    }

    @Test
    public void run() {
        MockInternalJobInfoJobRemove proc = new MockInternalJobInfoJobRemove();
        proc.run("JobId", 0L);
    }
}