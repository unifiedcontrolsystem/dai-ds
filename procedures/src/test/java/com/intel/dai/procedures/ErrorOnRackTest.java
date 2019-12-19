// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.junit.Test;
import org.voltdb.*;

import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

public class ErrorOnRackTest {
    class MockErrorOnRack extends ErrorOnRack {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("State", VoltType.STRING),
                    new VoltTable.ColumnInfo("Sernum", VoltType.STRING),
                    new VoltTable.ColumnInfo("Type", VoltType.STRING),
                    new VoltTable.ColumnInfo("Vpd", VoltType.STRING),
                    new VoltTable.ColumnInfo("Owner", VoltType.STRING)
            );
            if(!doZeroRows) {
                if (notA)
                    result[0].addRow("I", "Sernum", "Type", "Vpd", "Owner");
                else
                    result[0].addRow("A", "Sernum", "Type", "Vpd", "Owner");
            }
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
        private boolean doZeroRows = false;
        private boolean notA = false;
    }

    @Test
    public void run() {
        MockErrorOnRack proc = new MockErrorOnRack();
        proc.run("Lctn", "RAS", 9999L);
    }

    @Test(expected = VoltProcedure.VoltAbortException.class)
    public void run2() {
        MockErrorOnRack proc = new MockErrorOnRack();
        proc.doZeroRows = true;
        proc.run("Lctn", "RAS", 9999L);
    }

    @Test(expected = VoltProcedure.VoltAbortException.class)
    public void run3() {
        MockErrorOnRack proc = new MockErrorOnRack();
        proc.notA = true;
        proc.run("Lctn", "RAS", 9999L);
    }
}