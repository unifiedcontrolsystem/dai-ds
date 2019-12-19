// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.voltdb.*;
import org.junit.Test;

import java.util.Date;

import static org.junit.Assert.*;

public class AdapterStartedTest {
    class MockAdapterStarted extends AdapterStarted {
        boolean realResult = false;
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            if(!realResult) {
                VoltTable[] result = new VoltTable[1];
                result[0] = new VoltTable(
                        new VoltTable.ColumnInfo("NEXTVALUE", VoltType.BIGINT)
                );
                realResult = true;
                return result;
            } else {
                VoltTable[] result = new VoltTable[1];
                result[0] = new VoltTable(
                        new VoltTable.ColumnInfo("NEXTVALUE", VoltType.BIGINT)
                );
                result[0].addRow(9999);
                return result;
            }
        }

        @Override
        public Date getTransactionTime() {
            return new Date();
        }
    }

    @Test
    public void run() {
        AdapterStarted proc = new MockAdapterStarted();
        proc.run("RAS", 9999, "R0-CH0", 112);
        proc.run("RAS", 9999,"R0-CH0", 112);
        try {
            proc.run("TEST", 9999L, "R0-CH0", 112);
            fail();
        } catch(VoltProcedure.VoltAbortException e) { /* PASs */ }
    }
}
