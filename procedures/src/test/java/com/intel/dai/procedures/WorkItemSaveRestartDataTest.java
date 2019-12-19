// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.junit.Test;
import org.voltdb.*;
import org.voltdb.types.TimestampType;

import java.time.Instant;
import java.util.Date;
import java.util.Deque;
import java.util.LinkedList;

import static org.junit.Assert.*;

public class WorkItemSaveRestartDataTest {
    class MockWorkItemSaveRestartData extends WorkItemSaveRestartData {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            if(tables_.size() == 0) return null;
            return tables_.poll();
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }

        private Deque<VoltTable[]> tables_ = new LinkedList<>();
    }

    private VoltTable[] buildTable() { return buildTable(false, "W", "F"); }

    private VoltTable[] buildTable(boolean noRows, String state, String notify) {
        VoltTable[] result = new VoltTable[1];
        result[0] = new VoltTable(
                new VoltTable.ColumnInfo("Id", VoltType.BIGINT),
                new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("Queue", VoltType.STRING),
                new VoltTable.ColumnInfo("WorkToBeDone", VoltType.STRING),
                new VoltTable.ColumnInfo("Parameters", VoltType.STRING),
                new VoltTable.ColumnInfo("NotifyWhenFinished", VoltType.STRING),
                new VoltTable.ColumnInfo("RequestingWorkItemId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("RequestingAdapterType", VoltType.STRING),
                new VoltTable.ColumnInfo("WorkingAdapterId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Results", VoltType.STRING),
                new VoltTable.ColumnInfo("State", VoltType.STRING),
                new VoltTable.ColumnInfo("WorkingResults", VoltType.STRING)
        );
        if(!noRows)
            result[0].addRow(9999L, new TimestampType(Date.from(Instant.now())), "Queue", "Work",
                    "Parameters", notify, 9998L, "RAS", 9997L, "Results", state, null);
        return result;
    }

    @Test
    public void run() {
        MockWorkItemSaveRestartData proc = new MockWorkItemSaveRestartData();
        proc.tables_.add(buildTable());
        proc.tables_.add(buildTable());
        proc.run("RAS", 10000L, "some results", (byte)0, 40000L);
    }

    @Test
    public void run2() {
        MockWorkItemSaveRestartData proc = new MockWorkItemSaveRestartData();
        proc.tables_.add(buildTable());
        proc.tables_.add(buildTable());
        proc.run("RAS", 10000L, "some results", (byte)1, 0L);
    }

    @Test(expected = VoltProcedure.VoltAbortException.class)
    public void run3() {
        MockWorkItemSaveRestartData proc = new MockWorkItemSaveRestartData();
        proc.tables_.add(buildTable(false, "E", "F"));
        proc.tables_.add(buildTable());
        proc.run("RAS", 10000L, "some results", (byte)1, 0L);
    }
}
