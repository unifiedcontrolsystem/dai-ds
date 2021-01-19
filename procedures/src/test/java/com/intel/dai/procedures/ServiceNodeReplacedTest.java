// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;


import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;
import org.voltdb.SQLStmt;
import org.voltdb.Expectation;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.VoltProcedure;
import org.voltdb.types.TimestampType;

import java.time.Instant;
import java.util.Date;

public class ServiceNodeReplacedTest {
    class MockServiceNodeReplaced extends ServiceNodeReplaced {
        MockServiceNodeReplaced() {
            // Only one table is ever needed in all the results returned for the queries issued by this stored
            // procedure
            tables = new VoltTable[1];
            queryCount = 0;
        }

        @Override
        public long ensureHaveUniqueServiceNodeLastChgTimestamp(String sNodeLctn, long lNewRecordsTsInMicroSecs,
                                                                long lCurRecordsTsInMicroSecs) {
            return lNewRecordsTsInMicroSecs;
        }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) {
            queryCount++;
        }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) {
            queryCount++;
        }

        @Override
        public VoltTable[] voltExecuteSQL(boolean isFinalSQL) {
            return tables;
        }

        @Override
        public VoltTable[] voltExecuteSQL() {
            return tables;
        }

        @Override
        public Date getTransactionTime() {
            return new Date();
        }

        public void setTable(VoltTable table) {
            tables[0] = table;
        }

        public int getQueryCount() {
            return queryCount;
        }

        private VoltTable[] tables;
        private int queryCount;
    }

    @Test(expected = VoltProcedure.VoltAbortException.class)
    public void abortsWhenServiceNodeNotFound() {
        VoltTable emptyTable = new VoltTable(new VoltTable.ColumnInfo("DUMMY_FIELD", VoltType.BIGINT));
        MockServiceNodeReplaced proc = new MockServiceNodeReplaced();

        // Empty table (i.e. service node not found)
        proc.setTable(emptyTable);

        // Should throw an exception
        proc.run("R0-CH0-CN0", "ABCD", "X1", "A", "{}", "BIOS_STUFF", 1, "Test", 1);
    }

    @Test
    public void recordsReplacementForInOrderRecords() {
        MockServiceNodeReplaced proc = new MockServiceNodeReplaced();
        proc.setTable(NODE_DATA);

        final long time = LAST_CHG_TIMESTAMP.getTime() + 1; // After last change (i.e. in order)
        final String fruType = "X1";
        final String reqAdapterType = "Test";
        final String newSerNum = "WXYZ";
        final String newState = "A";
        final Long reqWorkItemId = 1L;

        long retVal = proc.run(LCTN, newSerNum, fruType, newState, "{}", "BIOS_STUFF", time, reqAdapterType, reqWorkItemId);

        // Should invoke 6 queries: retrieve service node data, update active service node record,
        // insert history record, insert replacement record
        Assert.assertEquals(6, proc.getQueryCount());
        // Should indicate change came in time order
        Assert.assertEquals(0L, retVal);
        // TODO: verify query arguments
    }

    @Test
    public void recordsReplacementForOutOfOrderRecords() {
        MockServiceNodeReplaced proc = new MockServiceNodeReplaced();
        proc.setTable(NODE_DATA);

        final long time = LAST_CHG_TIMESTAMP.getTime() - 1; // Before last change (i.e. out of order)
        final String fruType = "X1";
        final String reqAdapterType = "Test";
        final String newSerNum = "WXYZ";
        final String newState = "A";
        final Long reqWorkItemId = 1L;

        long retVal = proc.run(LCTN, newSerNum, fruType, newState, "{}", "BIOS_STUFF", time, reqAdapterType, reqWorkItemId);

        // Should invoke 7 queries: retrieve service node data, retrieve preceding service node history record,
        // insert history record, insert replacement record
        Assert.assertEquals(7, proc.getQueryCount());
        // Should indicate change came out of order
        Assert.assertEquals(2L, retVal);
        // TODO: verify query arguments
    }

    private static final VoltTable NODE_DATA;
    private static final TimestampType LAST_CHG_TIMESTAMP;
    private static final String STATE;
    private static final String LCTN;
    private static final long SEQUENCE_NUM;
    private static final String HOST_NAME;
    private static final String BOOT_IMAGE_ID;
    private static final String IP_ADDR;
    private static final String MAC_ADDR;
    private static final String TYPE;
    private static final String BMC_IP_ADDR;
    private static final String BMC_MAC_ADDR;
    private static final String BMC_HOST_NAME;
    private static final String OWNER;
    private static final String SER_NUM;
    private static final String AGGREGATOR;
    private static final String CONSTRAINTS;

    static {
        NODE_DATA = new VoltTable(
                new VoltTable.ColumnInfo("LastChgTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("State", VoltType.STRING),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("SequenceNumber", VoltType.BIGINT),
                new VoltTable.ColumnInfo("HostName", VoltType.STRING),
                new VoltTable.ColumnInfo("BootImageId", VoltType.STRING),
                new VoltTable.ColumnInfo("IpAddr", VoltType.STRING),
                new VoltTable.ColumnInfo("MacAddr", VoltType.STRING),
                new VoltTable.ColumnInfo("Type", VoltType.STRING),
                new VoltTable.ColumnInfo("BmcIpAddr", VoltType.STRING),
                new VoltTable.ColumnInfo("BmcMacAddr", VoltType.STRING),
                new VoltTable.ColumnInfo("BmcHostName", VoltType.STRING),
                new VoltTable.ColumnInfo("Owner", VoltType.STRING),
                new VoltTable.ColumnInfo("Sernum", VoltType.STRING),
                new VoltTable.ColumnInfo("Aggregator", VoltType.STRING),
                new VoltTable.ColumnInfo("InventoryTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("ConstraintId", VoltType.STRING),
                new VoltTable.ColumnInfo("ProofOfLifeTimestamp", VoltType.TIMESTAMP)
        );

        LAST_CHG_TIMESTAMP = new TimestampType(2);
        STATE = "M";
        LCTN = "R0-CH0-CN0";
        SEQUENCE_NUM = 9999L;
        HOST_NAME = "localhost";
        BOOT_IMAGE_ID = "IMG0";
        IP_ADDR = "127.0.0.1";
        MAC_ADDR = "AA:AA:AA:AA:AA:AA";
        TYPE = "AcmeNode";
        BMC_IP_ADDR = "127.0.0.2";
        BMC_MAC_ADDR = "AA:AA:AA:AA:AA:AB";
        BMC_HOST_NAME = "";
        OWNER = "S";
        SER_NUM = "ABCD";
        AGGREGATOR = "Agg01";
        CONSTRAINTS = "Constraint01";

        NODE_DATA.addRow(
                LAST_CHG_TIMESTAMP,
                STATE,
                LCTN,
                SEQUENCE_NUM,
                HOST_NAME,
                BOOT_IMAGE_ID,
                IP_ADDR,
                MAC_ADDR,
                TYPE,
                BMC_IP_ADDR,
                BMC_MAC_ADDR,
                BMC_HOST_NAME,
                OWNER,
                SER_NUM,
                AGGREGATOR,
                new TimestampType(Date.from(Instant.ofEpochMilli(40L))),
                CONSTRAINTS,
                new TimestampType(Date.from(Instant.ofEpochMilli(40L)))
        );
        NODE_DATA.addRow(
                LAST_CHG_TIMESTAMP,
                STATE,
                LCTN,
                SEQUENCE_NUM,
                HOST_NAME,
                BOOT_IMAGE_ID,
                IP_ADDR,
                MAC_ADDR,
                TYPE,
                BMC_IP_ADDR,
                BMC_MAC_ADDR,
                BMC_HOST_NAME,
                OWNER,
                SER_NUM,
                AGGREGATOR,
                new TimestampType(Date.from(Instant.ofEpochMilli(40L))),
                CONSTRAINTS,
                new TimestampType(Date.from(Instant.ofEpochMilli(40L)))
        );
    }
}