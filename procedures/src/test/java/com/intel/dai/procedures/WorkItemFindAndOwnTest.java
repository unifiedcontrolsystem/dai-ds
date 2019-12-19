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

public class WorkItemFindAndOwnTest {
    class MockWorkItemFindAndOwn extends WorkItemFindAndOwn {
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

    private VoltTable[] buildTable() { return buildTable(false, "T", null); }

    private VoltTable[] buildTable(boolean noRows, String state, String workingResults) {
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
                    "Parameters", "Notify", 9998L, "RAS", 9997L, "Results", state, workingResults);
        return result;
    }

    @Test
    public void run() {
        MockWorkItemFindAndOwn proc = new MockWorkItemFindAndOwn();
        proc.tables_.add(buildTable());
        proc.tables_.add(buildTable());
        proc.run("RAS", 9998L, "T", 9999L, "");
    }

    @Test
    public void run2() {
        MockWorkItemFindAndOwn proc = new MockWorkItemFindAndOwn();
        proc.tables_.add(buildTable());
        proc.tables_.add(buildTable());
        proc.run("RAS", 9998L, "F", 9999L, "");
    }

    @Test(expected = VoltProcedure.VoltAbortException.class)
    public void run3() {
        MockWorkItemFindAndOwn proc = new MockWorkItemFindAndOwn();
        proc.tables_.add(buildTable());
        proc.tables_.add(buildTable());
        proc.run("RAS", 9998L, "N", 9999L, "");
    }

    @Test
    public void run4() {
        MockWorkItemFindAndOwn proc = new MockWorkItemFindAndOwn();
        proc.tables_.add(buildTable(false, "T", "Some results."));
        proc.run("RAS", 9998L, "T", 9999L, "");
    }
}
