// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.util.Date;

import org.voltdb.SQLStmt;
import org.voltdb.Expectation;
import org.voltdb.VoltTable;

import org.junit.Test;
import org.junit.Assert;

public class UcsConfigValueSetTest {
    class MockUcsConfigValueSet extends UcsConfigValueSet {
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) {
            sqlArgs = args;
        }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) {
            sqlArgs = args;
        }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            sqlExecuted = true;
            return null;
        }

        @Override
        public Date getTransactionTime() {
            return new Date();
        }

        Object[] sqlArgs = null;
        boolean sqlExecuted = false;
    }

    @Test
    public void setsConfigValue() {
        MockUcsConfigValueSet proc = new MockUcsConfigValueSet();
        final String KEY = "IpAddr";
        final String VALUE = "127.0.0.1";

        proc.run(KEY, VALUE);

        // Must have called SQL command for setting the config value
        Assert.assertNotNull(proc.sqlArgs);
        Assert.assertTrue(proc.sqlExecuted);
        // Must have passed three parameters: key, value and timestamp
        Assert.assertEquals(3, proc.sqlArgs.length);
        Assert.assertEquals(KEY, proc.sqlArgs[0]);
        Assert.assertEquals(VALUE, proc.sqlArgs[1]);
    }
}
