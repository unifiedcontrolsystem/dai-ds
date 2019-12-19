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

public class JobHistoryListOfActiveJobsAtTimeTest {
    class MockJobHistoryListOfActiveJobsAtTime extends JobHistoryListOfActiveJobsAtTime {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL() { return voltExecuteSQL(false); }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            if(useOriginalMethod) {
                VoltTable[] result = new VoltTable[1];
                result[0] = new VoltTable(
                        new VoltTable.ColumnInfo("Lctn", VoltType.STRING)
                );
                if(!doZeroRows)
                    result[0].addRow("node");
                return result;
            } else {
                VoltTable[] result = new VoltTable[1];
                result[0] = new VoltTable(
                        new VoltTable.ColumnInfo("JobId", VoltType.STRING),
                        new VoltTable.ColumnInfo("State", VoltType.STRING),
                        new VoltTable.ColumnInfo("Nodes", VoltType.VARBINARY),
                        new VoltTable.ColumnInfo("JobName", VoltType.STRING),
                        new VoltTable.ColumnInfo("Bsn", VoltType.STRING),
                        new VoltTable.ColumnInfo("UserName", VoltType.STRING),
                        new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                        new VoltTable.ColumnInfo("NumNodes", VoltType.BIGINT)
                );
                result[0].addRow(id, state, new byte[128], "JobName", "Bsn", "UserName",
                        new TimestampType(Date.from(Instant.now())), 0L);
                return result;
            }
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }

        @Override
        public String getListNodeRanks(byte[] baThisJobsNodes) throws VoltAbortException {
            if(useOriginalMethod)
                return super.getListNodeRanks(baThisJobsNodes);
            else
                return "n1 n2";
        }

        private boolean doZeroRows = false;
        private String state = "S";
        private String id = "JobId";
        private boolean useOriginalMethod = false;
    }

    @Test
    public void run() {
        MockJobHistoryListOfActiveJobsAtTime proc = new MockJobHistoryListOfActiveJobsAtTime();
        proc.run(new TimestampType(Date.from(Instant.now())));
    }

    @Test
    public void run2() {
        MockJobHistoryListOfActiveJobsAtTime proc = new MockJobHistoryListOfActiveJobsAtTime();
        proc.state = "A";
        proc.run(null);
    }

    @Test
    public void run3() {
        MockJobHistoryListOfActiveJobsAtTime proc = new MockJobHistoryListOfActiveJobsAtTime();
        proc.id = "";
        proc.run(new TimestampType(Date.from(Instant.now())));
    }

    @Test
    public void getListNodeLctns() {
        MockJobHistoryListOfActiveJobsAtTime proc = new MockJobHistoryListOfActiveJobsAtTime();
        proc.useOriginalMethod = true;
        proc.getListNodeRanks(new byte[] { -1 });
    }
}