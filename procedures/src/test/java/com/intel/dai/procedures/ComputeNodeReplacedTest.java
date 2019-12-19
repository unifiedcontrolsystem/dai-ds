// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.junit.Assert;
import org.junit.Test;
import org.junit.Ignore;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;
import org.voltdb.SQLStmt;
import org.voltdb.Expectation;
import org.voltdb.VoltType;
import org.voltdb.types.TimestampType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ComputeNodeReplacedTest {

    class MockComputeNodeReplaced extends ComputeNodeReplaced {
        MockComputeNodeReplaced() {
            // Only one table is ever needed in all the results returned for the queries issued by this stored
            // procedure
            tables = new VoltTable[1];
            queryInvocations = new ArrayList<>();
            queries = new ArrayList<>();
            queryCount = 0;
        }

        @Override
        public long ensureHaveUniqueComputeNodeLastChgTimestamp(String sNodeLctn, long lNewRecordsTsInMicroSecs,
                                                                long lCurRecordsTsInMicroSecs) {
            return lNewRecordsTsInMicroSecs;
        }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Expectation expectation, Object... args) {
            recordQueryInvocation(stmt.getText(), args);
        }

        @Override
        public void voltQueueSQL(final SQLStmt stmt, Object... args) {
            recordQueryInvocation(stmt.getText(), args);
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

        public List<Object[]> getQueryInvocations() {
            return queryInvocations;
        }

        public List<String> getQueries() {
            return queries;
        }

        public int getQueryCount() {
            return queryCount;
        }

        private void recordQueryInvocation(String query, Object... args) {
            queries.add(query);
            queryInvocations.add(args);
            queryCount++;
        }

        private VoltTable[] tables;
        private List<Object[]> queryInvocations;
        private List<String> queries;
        private int queryCount;
    }

    @Test(expected = VoltProcedure.VoltAbortException.class)
    public void abortsWhenComputeNodeNotFound() {
        VoltTable emptyTable = new VoltTable(new VoltTable.ColumnInfo("DUMMY_FIELD", VoltType.BIGINT));
        MockComputeNodeReplaced proc = new MockComputeNodeReplaced();

        // Empty table (i.e. compute node not found)
        proc.setTable(emptyTable);

        // Should throw an exception
        proc.run("R0-CH0-CN0", "ABCD", "X1", "A", "{}", 1, "Test", 1);
    }

    @Test
    public void recordsReplacementForInOrderRecords() {
        MockComputeNodeReplaced proc = new MockComputeNodeReplaced();
        proc.setTable(NODE_DATA);

        final long time = LAST_CHG_TIMESTAMP.getTime() + 1; // After last change (i.e. in order)
        final String fruType = "X1";
        final String reqAdapterType = "Test";
        final String newSerNum = "WXYZ";
        final String newState = "A";
        final Long reqWorkItemId = 1L;

        long retVal = proc.run(LCTN, newSerNum, fruType, newState, "{}", time, reqAdapterType, reqWorkItemId);

        // Should invoke 6 queries: retrieve compute node data, update active compute node record,
        // insert history record, insert replacement record
        Assert.assertEquals(6, proc.getQueryCount());
        // Should indicate change came in time order
        Assert.assertEquals(0L, retVal);
        // TODO: verify query arguments
    }

    @Test
    public void recordsReplacementForOutOfOrderRecords() {
        MockComputeNodeReplaced proc = new MockComputeNodeReplaced();
        proc.setTable(NODE_DATA);

        final long time = LAST_CHG_TIMESTAMP.getTime() - 1; // Before last change (i.e. out of order)
        final String fruType = "X1";
        final String reqAdapterType = "Test";
        final String newSerNum = "WXYZ";
        final String newState = "A";
        final Long reqWorkItemId = 1L;

        long retVal = proc.run(LCTN, newSerNum, fruType, newState, "{}", time, reqAdapterType, reqWorkItemId);

        // Should invoke 6 queries: retrieve compute node data, retrieve preceding compute node history record,
        // insert history record, insert replacement record
        Assert.assertEquals(6, proc.getQueryCount());
        // Should indicate change came out of order
        Assert.assertEquals(1L, retVal);
        // TODO: verify query arguments
    }

    private void verifyInvocation(String query, String expectedTable, int numArgs, Object[] actualArgs,
                                  Object... expectedArgs) {
        Assert.assertTrue(query.contains(expectedTable));
        Assert.assertEquals(numArgs, actualArgs.length);
        for (int i = 0; i < expectedArgs.length; i++) {
            Object expectedArg = expectedArgs[i];
            boolean containsArg = false;
            for (int j = 0; j < actualArgs.length && !containsArg; j++) {
                containsArg = actualArgs[j].equals(expectedArg);
            }
            Assert.assertTrue(containsArg);
        }
    }

    private static final VoltTable NODE_DATA;
    private static final TimestampType LAST_CHG_TIMESTAMP;
    private static final String STATE;
    private static final String LCTN;
    private static final Integer SEQUENCE_NUMBER;
    private static final String HOST_NAME;
    private static final String BOOT_IMAGE_ID;
    private static final String ENVIRONMENT;
    private static final String IP_ADDR;
    private static final String MAC_ADDR;
    private static final String TYPE;
    private static final String BMC_IP_ADDR;
    private static final String BMC_MAC_ADDR;
    private static final String BMC_HOST_NAME;
    private static final String OWNER;
    private static final String SER_NUM;
    private static final String AGGREGATOR;

    static {
        NODE_DATA = new VoltTable(
                new VoltTable.ColumnInfo("LastChgTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("State", VoltType.STRING),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("SequenceNumber", VoltType.INTEGER),
                new VoltTable.ColumnInfo("HostName", VoltType.STRING),
                new VoltTable.ColumnInfo("BootImageId", VoltType.STRING),
                new VoltTable.ColumnInfo("Environment", VoltType.STRING),
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
                new VoltTable.ColumnInfo("WlmNodeState", VoltType.STRING)
        );

        LAST_CHG_TIMESTAMP = new TimestampType(2);
        STATE = "M";
        LCTN = "R0-CH0-CN0";
        SEQUENCE_NUMBER = 1;
        HOST_NAME = "localhost";
        BOOT_IMAGE_ID = "IMG0";
        ENVIRONMENT = "rich";
        IP_ADDR = "127.0.0.1";
        MAC_ADDR = "AA:AA:AA:AA:AA:AA";
        TYPE = "AcmeNode";
        BMC_IP_ADDR = "127.0.0.2";
        BMC_MAC_ADDR = "AA:AA:AA:AA:AA:AB";
        BMC_HOST_NAME = "";
        OWNER = "S";
        SER_NUM = "ABCD";
        AGGREGATOR = "Agg01";

        NODE_DATA.addRow(
                LAST_CHG_TIMESTAMP,
                STATE,
                LCTN,
                SEQUENCE_NUMBER,
                HOST_NAME,
                BOOT_IMAGE_ID,
                ENVIRONMENT,
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
                "A"
        );
        NODE_DATA.addRow(
                LAST_CHG_TIMESTAMP,
                STATE,
                LCTN,
                SEQUENCE_NUMBER,
                HOST_NAME,
                BOOT_IMAGE_ID,
                ENVIRONMENT,
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
                "A"
        );
    }
}