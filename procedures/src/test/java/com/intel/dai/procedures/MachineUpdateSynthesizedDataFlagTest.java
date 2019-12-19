// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.junit.Test;
import org.voltdb.*;
import org.voltdb.types.TimestampType;

import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

public class MachineUpdateSynthesizedDataFlagTest {
    class MockMachineUpdateSynthesizedDataFlag extends MachineUpdateSynthesizedDataFlag {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("Sernum", VoltType.STRING),
                    new VoltTable.ColumnInfo("Description", VoltType.STRING),
                    new VoltTable.ColumnInfo("Type", VoltType.STRING),
                    new VoltTable.ColumnInfo("NumRows", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("NumColsInRow", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("NumChassisInRack", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("State", VoltType.STRING),
                    new VoltTable.ColumnInfo("ClockFreq", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("ManifestLctn", VoltType.STRING),
                    new VoltTable.ColumnInfo("ManifestContent", VoltType.STRING)
            );
            if(!doZeroRow)
                result[0].addRow("Sernum", "Description", "Type", 1L, 5L, 8L, "A", 1L, "ManifestLctn",
                        "ManifestContent");
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
        private boolean doZeroRow = false;
    }

    @Test
    public void run() {
        MockMachineUpdateSynthesizedDataFlag proc = new MockMachineUpdateSynthesizedDataFlag();
        proc.run(null, "T");
    }

    @Test(expected = VoltProcedure.VoltAbortException.class)
    public void run2() {
        MockMachineUpdateSynthesizedDataFlag proc = new MockMachineUpdateSynthesizedDataFlag();
        proc.doZeroRow = true;
        proc.run("3", "T");
    }
}