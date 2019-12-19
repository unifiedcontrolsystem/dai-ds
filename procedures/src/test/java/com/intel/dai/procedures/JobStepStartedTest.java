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

import java.sql.Time;
import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

public class JobStepStartedTest {
    class MockJobStepStarted extends JobStepStarted {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("LastChgTimestamp", VoltType.TIMESTAMP),
                    new VoltTable.ColumnInfo("Executable", VoltType.STRING),
                    new VoltTable.ColumnInfo("JobId", VoltType.STRING),
                    new VoltTable.ColumnInfo("JobStepId", VoltType.STRING),
                    new VoltTable.ColumnInfo("NumNodes", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("Nodes", VoltType.VARBINARY),
                    new VoltTable.ColumnInfo("NumProcessesPerNode", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("InitialWorkingDir", VoltType.STRING),
                    new VoltTable.ColumnInfo("Arguments", VoltType.STRING),
                    new VoltTable.ColumnInfo("EnvironmentVars", VoltType.STRING),
                    new VoltTable.ColumnInfo("MpiMapping", VoltType.STRING),
                    new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP)
            );
            if(!doZeroRows)
                result[0].addRow(new TimestampType(Date.from(Instant.now())), "Executable", "JobId",
                        "JobStepId", 16, new byte[] {-1, -1}, 2, "Dir", "Args", "Env", "Mpi",
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
        MockJobStepStarted proc = new MockJobStepStarted();
        proc.run("JobId", "StepId", "Executable", 16, new byte[] {-1, -1},
                0L, "RAS", 9999L);
    }
}