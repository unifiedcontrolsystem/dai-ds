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

public class ComputeNodeDiscoveredTest {
    class MockComputeNodeDiscovered extends ComputeNodeDiscovered {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        boolean zeroRows = false;
        long offset_ = 0L;
        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("LastChgTimestamp", VoltType.TIMESTAMP),
                    new VoltTable.ColumnInfo("State", VoltType.STRING),
                    new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                    new VoltTable.ColumnInfo("SequenceNumber", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("HostName", VoltType.STRING),
                    new VoltTable.ColumnInfo("BootImageId", VoltType.STRING),
                    new VoltTable.ColumnInfo("Environment", VoltType.STRING),
                    new VoltTable.ColumnInfo("IpAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("MacAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("BmcIpAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("BmcMacAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("BmcHostName", VoltType.STRING),
                    new VoltTable.ColumnInfo("Owner", VoltType.STRING),
                    new VoltTable.ColumnInfo("Aggregator", VoltType.STRING),
                    new VoltTable.ColumnInfo("InventoryTimestamp", VoltType.TIMESTAMP),
                    new VoltTable.ColumnInfo("WlmNodeState", VoltType.STRING),
                    new VoltTable.ColumnInfo("ConstraintId", VoltType.STRING)
                    ,new VoltTable.ColumnInfo("ProofOfLifeTimestamp", VoltType.TIMESTAMP)
            );
            if(!zeroRows) {
                result[0].addRow(new TimestampType(Date.from(Instant.now())), "State", "Lctn", 0L, "HostName",
                        "BootImageId", "rich", "IpAddr", "MacAddr", "BmcIpAddr", "BmcMacAddr", "BmcHostName",
                        "Owner", "agg01", new TimestampType(Date.from(Instant.ofEpochMilli(40L))), "A", "Constraint1", new TimestampType(Date.from(Instant.ofEpochMilli(40L))));
                result[0].addRow(new TimestampType(Date.from(Instant.now())), "State", "Lctn", 0L, "HostName",
                        "BootImageId", "lean", "IpAddr", "MacAddr", "BmcIpAddr", "BmcMacAddr", "BmcHostName",
                        "Owner", "agg01", new TimestampType(Date.from(Instant.ofEpochMilli(40L))), "A", "Constraint1", new TimestampType(Date.from(Instant.ofEpochMilli(40L))));
            }
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }

        @Override
        public long ensureHaveUniqueComputeNodeLastChgTimestamp(String s, long a, long b) {
            return Instant.now().toEpochMilli() * 1000 - offset_;
        }
    }

    @Test
    public void run() {
        ComputeNodeDiscovered proc = new MockComputeNodeDiscovered();
        long micro = (Instant.now().toEpochMilli() + 60000) * 1000;
        proc.run("LOCATION", micro, "RAS",
                9999L);
    }

    @Test
    public void run2() {
        MockComputeNodeDiscovered proc = new MockComputeNodeDiscovered();
        long micro = (Instant.now().toEpochMilli()) * 1000;
        proc.offset_ = 2000L;
        proc.run("LOCATION", micro, "RAS",
                9999L);
    }

    @Test
    public void run3() {
        MockComputeNodeDiscovered proc = new MockComputeNodeDiscovered();
        long micro = (Instant.now().toEpochMilli()) * 1000;
        proc.zeroRows = true;
        try {
            proc.run("LOCATION", micro, "RAS",
                    9999L);
        } catch(VoltProcedure.VoltAbortException e) { /* PASS */ }
    }
}
