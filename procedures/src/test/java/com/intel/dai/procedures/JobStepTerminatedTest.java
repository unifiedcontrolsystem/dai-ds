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

public class JobStepTerminatedTest {
    class MockJobStepTerminated extends JobStepTerminated {
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
            if(firstTime)
                result[0].addRow(new TimestampType(Date.from(Instant.now())), "Executable", "JobId",
                        "JobStepId", 16, new byte[] {-1, -1}, 2, "Dir", "Args", "Env", "Mpi",
                        new TimestampType(Date.from(Instant.now())));
            firstTime = false;
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }

        private boolean firstTime = true;
    }

    @Test
    public void run() {
        MockJobStepTerminated proc = new MockJobStepTerminated();
        proc.run("JobId", "StepId", 0, null, "Wlm", 0L,
                "RAS", 9999L);
    }

    @Test
    public void run2() {
        MockJobStepTerminated proc = new MockJobStepTerminated();
        proc.run("JobId", "StepId", 0, "Name", "Wlm", 0L,
                "RAS", 9999L);
    }

    @Test(expected = VoltProcedure.VoltAbortException.class)
    public void run3() {
        MockJobStepTerminated proc = new MockJobStepTerminated();
        proc.firstTime = false;
        proc.run("JobId", "StepId", 0, "Name", "Wlm", 0L,
                "RAS", 9999L);
    }
}