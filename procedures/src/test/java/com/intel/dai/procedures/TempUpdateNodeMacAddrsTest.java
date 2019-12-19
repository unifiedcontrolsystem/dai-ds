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

public class TempUpdateNodeMacAddrsTest {
    class MockTempUpdateNodeMacAddrs extends TempUpdateNodeMacAddrs {
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

        @Override
        public long ensureHaveUniqueServiceNodeLastChgTimestamp(String lctn, long ts, long current) {
            if(callParent)
                return super.ensureHaveUniqueServiceNodeLastChgTimestamp(lctn, ts, current);
            return ts;
        }

        @Override
        public long ensureHaveUniqueComputeNodeLastChgTimestamp(String lctn, long ts, long current) {
            if(callParent)
                return super.ensureHaveUniqueComputeNodeLastChgTimestamp(lctn, ts, current);
            return ts;
        }

        private Deque<VoltTable[]> tables_ = new LinkedList<>();
        private boolean callParent = false;
    }

    private VoltTable[] buildTable() {
        return buildTable(false, 100000L);
    }

    private VoltTable[] buildTable(boolean noRows) {
        return buildTable(noRows, 100000L);
    }

    private VoltTable[] buildTable(boolean noRows, long microSec) {
        long milliSec = Instant.now().toEpochMilli() + (microSec / 1000L);
        VoltTable[] result = new VoltTable[1];
        result[0] = new VoltTable(
                new VoltTable.ColumnInfo("LastChgTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("SequenceNumber", VoltType.BIGINT),
                new VoltTable.ColumnInfo("HostName", VoltType.STRING),
                new VoltTable.ColumnInfo("IpAddr", VoltType.STRING),
                new VoltTable.ColumnInfo("MacAddr", VoltType.STRING),
                new VoltTable.ColumnInfo("BmcIpAddr", VoltType.STRING),
                new VoltTable.ColumnInfo("BmcMacAddr", VoltType.STRING),
                new VoltTable.ColumnInfo("BmcHostName", VoltType.STRING),
                new VoltTable.ColumnInfo("Owner", VoltType.STRING),
                new VoltTable.ColumnInfo("State", VoltType.STRING),
                new VoltTable.ColumnInfo("BootImageId", VoltType.STRING),
                new VoltTable.ColumnInfo("Aggregator", VoltType.STRING),
                new VoltTable.ColumnInfo("InventoryTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("WlmNodeState", VoltType.STRING)
        );
        if(!noRows)
            result[0].addRow(new TimestampType(Date.from(Instant.ofEpochMilli(milliSec))), "Lctn", 999L,
                    "HostName", "IpAddr", "MacAddr", "BmcIpAddr", "BmcMacAddr", "BmcHostName", "Owner",
                    "State", "BootImageId", "Agg01", new TimestampType(Date.from(Instant.ofEpochMilli(40L))), "A");
        return result;
    }

    @Test(expected = VoltProcedure.VoltAbortException.class)
    public void run() {
        MockTempUpdateNodeMacAddrs proc = new MockTempUpdateNodeMacAddrs();
        proc.tables_.add(buildTable(true));
        proc.tables_.add(buildTable(true));
        proc.run("Lctn", "00:11:22:33:44:55", "99:88:77:66:55:44");
    }

    @Test
    public void run2() {
        MockTempUpdateNodeMacAddrs proc = new MockTempUpdateNodeMacAddrs();
        proc.tables_.add(buildTable());
        proc.tables_.add(buildTable());
        proc.run("Lctn", "00:11:22:33:44:55", "99:88:77:66:55:44");
    }

    @Test
    public void run3() {
        MockTempUpdateNodeMacAddrs proc = new MockTempUpdateNodeMacAddrs();
        proc.tables_.add(buildTable(false, -100000L));
        proc.tables_.add(buildTable());
        proc.run("Lctn", "00:11:22:33:44:55", "99:88:77:66:55:44");
    }

    @Test
    public void run4() {
        MockTempUpdateNodeMacAddrs proc = new MockTempUpdateNodeMacAddrs();
        proc.tables_.add(buildTable(true));
        proc.tables_.add(buildTable());
        proc.tables_.add(buildTable());
        proc.run("Lctn", "00:11:22:33:44:55", "99:88:77:66:55:44");
    }

    @Test
    public void run5() {
        MockTempUpdateNodeMacAddrs proc = new MockTempUpdateNodeMacAddrs();
        proc.tables_.add(buildTable(true));
        proc.tables_.add(buildTable(false, -100000L));
        proc.tables_.add(buildTable());
        proc.run("Lctn", "00:11:22:33:44:55", "99:88:77:66:55:44");
    }

    @Test
    public void ensureHaveUniqueServiceNodeLastChgTimestamp() {
        long current = Instant.now().toEpochMilli() * 1000L;
        long old = current - 100000L;
        MockTempUpdateNodeMacAddrs proc = new MockTempUpdateNodeMacAddrs();
        proc.callParent = true;
        proc.tables_.add(buildTable(false));
        proc.tables_.add(buildTable(true));
        proc.ensureHaveUniqueServiceNodeLastChgTimestamp("Lctn", current, old);
    }

    @Test
    public void ensureHaveUniqueServiceNodeLastChgTimestamp2() {
        long current = Instant.now().toEpochMilli() * 1000L;
        long old = current - 100000L;
        MockTempUpdateNodeMacAddrs proc = new MockTempUpdateNodeMacAddrs();
        proc.callParent = true;
        proc.tables_.add(buildTable(false));
        proc.tables_.add(buildTable(true));
        proc.ensureHaveUniqueServiceNodeLastChgTimestamp("Lctn", current, current);
    }

    @Test
    public void ensureHaveUniqueComputeNodeLastChgTimestamp() {
        long current = Instant.now().toEpochMilli() * 1000L;
        long old = current - 100000L;
        MockTempUpdateNodeMacAddrs proc = new MockTempUpdateNodeMacAddrs();
        proc.callParent = true;
        proc.tables_.add(buildTable(false));
        proc.tables_.add(buildTable(true));
        proc.ensureHaveUniqueComputeNodeLastChgTimestamp("Lctn", current, old);
    }

    @Test
    public void ensureHaveUniqueComputeNodeLastChgTimestamp2() {
        long current = Instant.now().toEpochMilli() * 1000L;
        long old = current - 100000L;
        MockTempUpdateNodeMacAddrs proc = new MockTempUpdateNodeMacAddrs();
        proc.callParent = true;
        proc.tables_.add(buildTable(false));
        proc.tables_.add(buildTable(true));
        proc.ensureHaveUniqueComputeNodeLastChgTimestamp("Lctn", current, current);
    }
}
