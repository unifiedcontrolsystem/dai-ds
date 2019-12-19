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

public class DiagListOfActiveDiagsAtTimeTest {
    class MockDiagListOfActiveDiagsAtTime extends DiagListOfActiveDiagsAtTime {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(new VoltTable.ColumnInfo("DiagToolId", VoltType.STRING),
                    new VoltTable.ColumnInfo("DiagListId", VoltType.STRING));
            result[0].addRow("tool_id", "list_id");
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
    }

    @Test
    public void run() {
        MockDiagListOfActiveDiagsAtTime proc = new MockDiagListOfActiveDiagsAtTime();
        proc.run(new TimestampType(Date.from(Instant.now())));
        proc.run(null);
    }
}