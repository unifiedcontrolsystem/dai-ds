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

public class JobTerminatedTest {
    class MockJobTerminated extends JobTerminated {
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
                    new VoltTable.ColumnInfo("JobName", VoltType.STRING),
                    new VoltTable.ColumnInfo("Bsn", VoltType.STRING),
                    new VoltTable.ColumnInfo("NumNodes", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("Nodes", VoltType.VARBINARY),
                    new VoltTable.ColumnInfo("PowerCap", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("InitialWorkingDir", VoltType.STRING),
                    new VoltTable.ColumnInfo("UserName", VoltType.STRING),
                    new VoltTable.ColumnInfo("Arguments", VoltType.STRING),
                    new VoltTable.ColumnInfo("EnvironmentVars", VoltType.STRING),
                    new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP)
            );
            if(firstTime)
                result[0].addRow(new TimestampType(Date.from(Instant.now())), "Executable", "JobId",
                        "Name", "Bsn", 16, new byte[] {-1, -1}, 1200, "Dir", "UserName", "Args", "Vars",
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
        MockJobTerminated proc = new MockJobTerminated();
        proc.run("JobId", "Account", 0, "Dir", "Wlm", 0L,
                "RAS", 999L);
    }

    @Test
    public void run2() {
        MockJobTerminated proc = new MockJobTerminated();
        proc.firstTime = false;
        proc.run("JobId", "Account", 0, "Dir", "Wlm", 0L,
                "RAS", 999L);
    }

    @Test
    public void run3() {
        MockJobTerminated proc = new MockJobTerminated();
        proc.run("JobId", "Account", 0, null, "Wlm", 0L,
                "RAS", 999L);
    }
}