package com.intel.dai.procedures;

import org.junit.Test;
import org.voltdb.*;
import org.voltdb.types.TimestampType;

import java.time.Instant;
import java.util.Date;

import static org.junit.Assert.*;

public class ComputeNodeSaveEnvironmentInfoTest {
    class MockComputeNodeSaveEnvironmentInfo extends ComputeNodeSaveEnvironmentInfo {
        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) { }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) { }

        private boolean zeroRows = false;
        private long offset_ = 0L;
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
                    new VoltTable.ColumnInfo("IpAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("MacAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("BmcIpAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("BmcMacAddr", VoltType.STRING),
                    new VoltTable.ColumnInfo("BmcHostName", VoltType.STRING),
                    new VoltTable.ColumnInfo("Owner", VoltType.STRING),
                    new VoltTable.ColumnInfo("Aggregator", VoltType.STRING),
                    new VoltTable.ColumnInfo("InventoryTimestamp", VoltType.TIMESTAMP),
                    new VoltTable.ColumnInfo("WlmNodeState", VoltType.STRING)
            );
            if(!zeroRows) {
                result[0].addRow(new TimestampType(5000000L), "Good", "Lctn", 0L, "HostName",
                        "ID","IpAddr", "macAddr", "BmcIpAddr", "BmcMacAddr", "BmcHostName", "Owner",
                        "Agg01", new TimestampType(Date.from(Instant.ofEpochMilli(40L))), "A");
            }
            return result;
        }

        @Override
        public Date getTransactionTime() {
            return Date.from(Instant.now());
        }

        @Override
        public long ensureHaveUniqueComputeNodeLastChgTimestamp(String s, long a, long b) {
            return 5000000L + offset_;
        }
    }

    @Test
    public void run() {
        ComputeNodeSaveEnvironmentInfoTest.MockComputeNodeSaveEnvironmentInfo proc =
                new ComputeNodeSaveEnvironmentInfoTest.MockComputeNodeSaveEnvironmentInfo();
        long micro = Instant.now().toEpochMilli() * 1000;
        proc.run("LOCATION", "rich", micro, "RAS", 9999L);
    }

    @Test(expected = VoltProcedure.VoltAbortException.class)
    public void run2() {
        ComputeNodeSaveEnvironmentInfoTest.MockComputeNodeSaveEnvironmentInfo proc =
                new ComputeNodeSaveEnvironmentInfoTest.MockComputeNodeSaveEnvironmentInfo();
        long micro = Instant.now().toEpochMilli() * 1000;
        proc.zeroRows = true;
        proc.run("LOCATION", "rich", micro, "RAS", 9999L);
    }

    @Test
    public void run3() {
        ComputeNodeSaveEnvironmentInfoTest.MockComputeNodeSaveEnvironmentInfo proc =
                new ComputeNodeSaveEnvironmentInfoTest.MockComputeNodeSaveEnvironmentInfo();
        long micro = Instant.now().toEpochMilli() * 1000;
        proc.offset_ = 1000L;
        proc.run("LOCATION", "rich", micro, "RAS", 9999L);
    }
}