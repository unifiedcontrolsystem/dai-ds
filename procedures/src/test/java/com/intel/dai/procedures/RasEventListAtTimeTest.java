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

public class RasEventListAtTimeTest {
    class MockRasEventListAtTime extends RasEventListAtTime {
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
            result[0].addRow("Sernum", "Description", "Type", 1L, 5L, 8L, "A", 1L, "ManifestLctn",
                    "ManifestContent");
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
    }

    @Test
    public void run() {
        MockRasEventListAtTime proc = new MockRasEventListAtTime();
        proc.run(new TimestampType(Date.from(Instant.now())), new TimestampType(Date.from(Instant.now())));
    }

    @Test
    public void run2() {
        MockRasEventListAtTime proc = new MockRasEventListAtTime();
        proc.run(new TimestampType(Date.from(Instant.now())), null);
    }

    @Test
    public void run3() {
        MockRasEventListAtTime proc = new MockRasEventListAtTime();
        proc.run(null, new TimestampType(Date.from(Instant.now())));
    }

    @Test
    public void run4() {
        MockRasEventListAtTime proc = new MockRasEventListAtTime();
        proc.run(null, null);
    }
}