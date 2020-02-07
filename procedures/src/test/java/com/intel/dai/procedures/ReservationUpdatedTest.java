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

public class ReservationUpdatedTest {
    class MockReservationUpdated extends ReservationUpdated {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("Users", VoltType.STRING),
                    new VoltTable.ColumnInfo("Nodes", VoltType.STRING),
                    new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                    new VoltTable.ColumnInfo("EndTimestamp", VoltType.TIMESTAMP)
            );
            if(!doZeroRows)
                result[0].addRow("Users", "Nodes", new TimestampType(Date.from(Instant.now())),
                        new TimestampType(Date.from(Instant.now())));
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }

        private boolean doZeroRows = false;
    }

    @Test
    public void run() {
        MockReservationUpdated proc = new MockReservationUpdated();
        proc.run("Name", "Users", "Nodes", 0L, 10L,
                20L, "RAS", 9999L);
    }

    @Test(expected = VoltProcedure.VoltAbortException.class)
    public void run2() {
        MockReservationDeleted proc = new MockReservationDeleted();
        proc.doZeroRows = true;
        proc.run("Name", "Users", "Nodes", 0L, 10L,
                20L, "RAS", 9999L);
    }
}
