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

import static org.junit.Assert.*;

public class ComputeNodesFromListWithoutThisBootImageIdTest {
    class MockComputeNodesFromListWithoutThisBootImageId extends ComputeNodesFromListWithoutThisBootImageId {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        @Override
        public VoltTable[] voltExecuteSQL(boolean value) {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("LastChgTimestamp", VoltType.TIMESTAMP),
                    new VoltTable.ColumnInfo("State", VoltType.STRING),
                    new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                    new VoltTable.ColumnInfo("SequenceNumber", VoltType.BIGINT),
                    new VoltTable.ColumnInfo("HostName", VoltType.STRING),
                    new VoltTable.ColumnInfo("Sernum", VoltType.STRING),
                    new VoltTable.ColumnInfo("IpAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("MacAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("Type", VoltType.STRING),
                    new VoltTable.ColumnInfo("BmcIpAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("BmcMacAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("BmcHostName", VoltType.STRING),
                    new VoltTable.ColumnInfo("Owner", VoltType.STRING),
                    new VoltTable.ColumnInfo("BootImageId", VoltType.STRING)
            );
            result[0].addRow(new TimestampType(5000000L), "Good", "Lctn", 0L, "HostName",
                    "sernum", "IpAddr", "macAddr", "Type", "BmcIpAddr", "BmcMacAddr", "BmcHostName", "Owner", "ID");
            return result;
        }
    }

    @Test
    public void run() {
        ComputeNodesFromListWithoutThisBootImageId proc = new MockComputeNodesFromListWithoutThisBootImageId();
        proc.run(new String[] {"Lctn"}, "ID");
    }
}