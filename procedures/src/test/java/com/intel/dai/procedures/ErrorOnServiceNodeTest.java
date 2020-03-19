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

public class ErrorOnServiceNodeTest {
    class MockErrorOnServiceNode extends ErrorOnServiceNode {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                    new VoltTable.ColumnInfo("SequenceNumber", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("HostName", VoltType.STRING),
                    new VoltTable.ColumnInfo("BootImageId", VoltType.STRING),
                    new VoltTable.ColumnInfo("IpAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("MacAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("BmcIpAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("BmcMacAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("BmcHostName", VoltType.STRING),
                    new VoltTable.ColumnInfo("Owner", VoltType.STRING),
                    new VoltTable.ColumnInfo("Aggregator", VoltType.STRING),
                    new VoltTable.ColumnInfo("LastChgTimestamp", VoltType.TIMESTAMP),
                    new VoltTable.ColumnInfo("InventoryTimestamp", VoltType.TIMESTAMP)
            );
            if(!doZeroRows) {
                result[0].addRow("Lctn", 1L, "HostName", "BootImageId", "IpAddr", "MacAddr",
                        "BmcIpAddr", "BmcMacAddr", "BmcHostName", "Owner", "Agg01",
                        new TimestampType(Date.from(Instant.ofEpochMilli(40L))),
                        new TimestampType(Date.from(Instant.ofEpochMilli(40L))));
            }
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
        MockErrorOnServiceNode proc = new MockErrorOnServiceNode();
        proc.run("Lctn", "RAS", 9999L);
    }

    @Test(expected = VoltProcedure.VoltAbortException.class)
    public void run2() {
        MockErrorOnServiceNode proc = new MockErrorOnServiceNode();
        proc.doZeroRows = true;
        proc.run("Lctn", "RAS", 9999L);
    }
}