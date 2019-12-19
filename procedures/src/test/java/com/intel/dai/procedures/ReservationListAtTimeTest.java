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

public class ReservationListAtTimeTest {
    class MockReservationListAtTime extends ReservationListAtTime {
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
            result[0].addRow("Users", "Nodes", new TimestampType(Date.from(Instant.now())),
                    new TimestampType(Date.from(Instant.now())));
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
    }

    @Test
    public void run() {
        MockReservationListAtTime proc = new MockReservationListAtTime();
        proc.run(new TimestampType(Date.from(Instant.now())), new TimestampType(Date.from(Instant.now())));
    }

    @Test
    public void run2() {
        MockReservationListAtTime proc = new MockReservationListAtTime();
        proc.run(null, new TimestampType(Date.from(Instant.now())));
    }

    @Test
    public void run3() {
        MockReservationListAtTime proc = new MockReservationListAtTime();
        proc.run(new TimestampType(Date.from(Instant.now())), null);
    }

    @Test
    public void run4() {
        MockReservationListAtTime proc = new MockReservationListAtTime();
        proc.run(null, null);
    }
}
