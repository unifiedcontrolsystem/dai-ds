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
import java.util.Deque;
import java.util.LinkedList;

import static org.junit.Assert.*;

public class RasEventStoreTest {
    class MockRasEventStore extends RasEventStore {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            if(tables_.size() == 0)
                return null;
            else
                return tables_.poll();
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }
        private Deque<VoltTable[]> tables_ = new LinkedList<>();
    }

    private VoltTable[] buildUniqueId(boolean noRows) {
        VoltTable[] result = new VoltTable[1];
        result[0] = new VoltTable(new VoltTable.ColumnInfo("Count", VoltType.BIGINT));
        if(!noRows)
            result[0].addRow(12L);
        return result;
    }

    private VoltTable[] buildNull() {
        return null;
    }

    private VoltTable[] buildControl(boolean noRows, boolean returnNull) {
        VoltTable[] result = new VoltTable[1];
        result[0] = new VoltTable(new VoltTable.ColumnInfo("ControlOperation", VoltType.STRING));
        if(!noRows)
            result[0].addRow(returnNull?null:"Do Something");
        return result;
    }

    @Test
    public void run() {
        MockRasEventStore proc = new MockRasEventStore();
        proc.tables_.add(buildUniqueId(false));
        proc.tables_.add(buildNull());
        proc.tables_.add(buildControl(false, false));
        proc.run("Type", "InstanceData", "Lctn", "JobId", 1000000L,
                "RAS", 9999L);
    }

    @Test
    public void run2() {
        MockRasEventStore proc = new MockRasEventStore();
        proc.tables_.add(buildUniqueId(true));
        proc.tables_.add(buildNull());
        proc.tables_.add(buildUniqueId(false));
        proc.tables_.add(buildNull());
        proc.tables_.add(buildControl(false, false));
        proc.run("Type", "InstanceData", "Lctn", "JobId", 1000000L,
                "RAS", 9999L);
    }

    @Test
    public void run3() {
        MockRasEventStore proc = new MockRasEventStore();
        proc.tables_.add(buildUniqueId(false));
        proc.tables_.add(buildNull());
        proc.tables_.add(buildControl(true, false));
        proc.run("Type", "InstanceData", "Lctn", "JobId", 1000000L,
                "RAS", 9999L);
    }

    @Test
    public void run4() {
        MockRasEventStore proc = new MockRasEventStore();
        proc.tables_.add(buildUniqueId(true));
        proc.tables_.add(buildNull());
        proc.tables_.add(buildUniqueId(false));
        proc.tables_.add(buildNull());
        proc.tables_.add(buildControl(false, true));
        proc.run("Type", "InstanceData", "Lctn", "JobId", 1000000L,
                "RAS", 9999L);
    }

    @Test
    public void run5() {
        MockRasEventStore proc = new MockRasEventStore();
        proc.tables_.add(buildUniqueId(false));
        proc.tables_.add(buildNull());
        proc.tables_.add(buildControl(false, false));
        proc.run("Type", "InstanceData", "", "JobId", 1000000L,
                "RAS", 9999L);
    }

    @Test
    public void run6() {
        MockRasEventStore proc = new MockRasEventStore();
        proc.tables_.add(buildUniqueId(false));
        proc.tables_.add(buildNull());
        proc.tables_.add(buildControl(false, false));
        proc.run("Type", "InstanceData", null, "JobId", 1000000L,
                "RAS", 9999L);
    }

    @Test
    public void run7() {
        MockRasEventStore proc = new MockRasEventStore();
        proc.tables_.add(buildUniqueId(false));
        proc.tables_.add(buildNull());
        proc.tables_.add(buildControl(false, false));
        proc.run("Type", "InstanceData", "Lctn", null, 1000000L,
                "RAS", 9999L);
    }

    @Test
    public void run8() {
        MockRasEventStore proc = new MockRasEventStore();
        proc.tables_.add(buildUniqueId(false));
        proc.tables_.add(buildNull());
        proc.tables_.add(buildControl(false, false));
        proc.run("Type", "InstanceData", "Lctn", "", 1000000L,
                "RAS", 9999L);
    }

    @Test
    public void run9() {
        MockRasEventStore proc = new MockRasEventStore();
        proc.tables_.add(buildUniqueId(false));
        proc.tables_.add(buildNull());
        proc.tables_.add(buildControl(false, false));
        proc.run("Type", "InstanceData", "Lctn", "null", 1000000L,
                "RAS", 9999L);
    }
}
