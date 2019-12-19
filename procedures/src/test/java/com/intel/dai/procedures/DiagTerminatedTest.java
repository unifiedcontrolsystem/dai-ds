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

public class DiagTerminatedTest {
    class MockDiagTerminated extends DiagTerminated {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] aTable = new VoltTable[1];
            aTable[0] = new VoltTable(
                    new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                    new VoltTable.ColumnInfo("ServiceOperationId", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("Diag", VoltType.STRING),
                    new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP)
            );
            aTable[0].addRow("R0-CH0", 1L, "Diag", Date.from(Instant.now()));

            return aTable;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
    }

    @Test
    public void run() {
        MockDiagTerminated proc = new MockDiagTerminated();
        proc.run(0L, "Done", "Results", "RAS", 0L);
    }
}