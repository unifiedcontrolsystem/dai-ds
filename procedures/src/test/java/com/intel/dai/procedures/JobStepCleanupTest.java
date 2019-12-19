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

public class JobStepCleanupTest {
    class MockJobStepCleanup extends JobStepCleanup {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL() { return voltExecuteSQL(false); }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            if(firstTime) {
                VoltTable[] result = new VoltTable[1];
                result[0] = new VoltTable(
                        new VoltTable.ColumnInfo("JobId", VoltType.STRING),
                        new VoltTable.ColumnInfo("JobStepId", VoltType.STRING),
                        new VoltTable.ColumnInfo("NumNodes", VoltType.BIGINT),
                        new VoltTable.ColumnInfo("Nodes", VoltType.VARBINARY),
                        new VoltTable.ColumnInfo("NumProcessesPerNode", VoltType.BIGINT),
                        new VoltTable.ColumnInfo("Executable", VoltType.STRING),
                        new VoltTable.ColumnInfo("InitialWorkingDir", VoltType.STRING),
                        new VoltTable.ColumnInfo("Arguments", VoltType.STRING),
                        new VoltTable.ColumnInfo("EnvironmentVars", VoltType.STRING),
                        new VoltTable.ColumnInfo("MpiMapping", VoltType.STRING),
                        new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                        new VoltTable.ColumnInfo("WlmJobStepState", VoltType.STRING),
                        new VoltTable.ColumnInfo("LastChgTimestamp", VoltType.TIMESTAMP)
                );
                result[0].addRow("JobId", "JobStepId", 16, new byte[]{-1, -1}, 2L, "Executable",
                        "InitialWorkingDir", "Arguments", "EnvironmentVars", "MpiMapping",
                        new TimestampType(Date.from(Instant.now())), "WlmJobStepState",
                        new TimestampType(Date.from(Instant.now())));
                firstTime = false;
                return result;
            } else {
                VoltTable[] result = new VoltTable[1];
                result[0] = new VoltTable(
                        new VoltTable.ColumnInfo("JobId", VoltType.STRING)
                );
                return result;
            }
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
        private boolean firstTime = true;
    }

    @Test
    public void run() {
        MockJobStepCleanup proc = new MockJobStepCleanup();
        proc.run("JobId", 0L, "RAS", 9999L);
    }

    @Test
    public void run2() {
        MockJobStepCleanup proc = new MockJobStepCleanup();
        proc.firstTime = false;
        proc.run("JobId", 0L, "RAS", 9999L);
    }
}