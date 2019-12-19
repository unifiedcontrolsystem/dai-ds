// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Types;
import java.sql.SQLException;
import java.util.Map;
import java.util.HashMap;
import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;
import org.mockito.Mockito;
import org.mockito.AdditionalMatchers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.intel.logging.Logger;
import com.intel.dai.exceptions.DataStoreException;

public class DataLoaderApiJdbcTest {
    @Before
    public void setUp() {
        mockNearlineConn = Mockito.mock(Connection.class);
        mockOnlineConn = Mockito.mock(Connection.class);
        mockStmt = Mockito.mock(PreparedStatement.class);
        mockResultSet = Mockito.mock(ResultSet.class);
        mockLog = Mockito.mock(Logger.class);

        dataLoaderApi = new DataLoaderApiJdbc(mockOnlineConn, mockNearlineConn, mockLog);
    }

    @Test
    public void isNearlineTierValidQueriesAndReturnsCorrectStatus() throws Exception {
        Mockito.when(mockNearlineConn.prepareStatement(Mockito.anyString())).thenReturn(mockStmt);
        Mockito.when(mockStmt.executeQuery()).thenReturn(mockResultSet);
        Mockito.when(mockResultSet.next()).thenReturn(true);

        Mockito.when(mockResultSet.getString(Mockito.anyString())).thenReturn("true");
        boolean isValid = dataLoaderApi.isNearlineTierValid();

        // Must query the "tier2_config" table for the right configuration parameter
        Mockito.verify(mockNearlineConn, Mockito.times(1))
                .prepareStatement("select value from tier2_config where key = 'tier2_valid'");
        // Must execute the query (once)
        Mockito.verify(mockStmt, Mockito.times(1)).executeQuery();
        // Must retrieve the result: advance result set and get the "value" column
        Mockito.verify(mockResultSet, Mockito.times(1)).next();
        Mockito.verify(mockResultSet, Mockito.times(1)).getString("value");
        // Should return true based on result ("true")
        Assert.assertTrue(isValid);

        Mockito.when(mockResultSet.getString(Mockito.anyString())).thenReturn("True");
        isValid = dataLoaderApi.isNearlineTierValid();
        // Should return true based on result ("True")
        Assert.assertTrue(isValid);

        Mockito.when(mockResultSet.getString(Mockito.anyString())).thenReturn("TRUE");
        isValid = dataLoaderApi.isNearlineTierValid();
        // Should return true based on result ("TRUE")
        Assert.assertTrue(isValid);

        Mockito.when(mockResultSet.getString(Mockito.anyString())).thenReturn("false");
        isValid = dataLoaderApi.isNearlineTierValid();
        // Should return false based on result ("false")
        Assert.assertFalse(isValid);

        Mockito.when(mockResultSet.getString(Mockito.anyString())).thenReturn("random text");
        isValid = dataLoaderApi.isNearlineTierValid();
        // Anything other than true should be considered false
        Assert.assertFalse(isValid);

        Mockito.when(mockResultSet.getString(Mockito.anyString())).thenReturn(null);
        isValid = dataLoaderApi.isNearlineTierValid();
        // Null should be treated as false
        Assert.assertFalse(isValid);
    }

    @Test(expected = DataStoreException.class)
    public void isNearlineTierValidHandlesEmptyResultSet() throws Exception {
        Mockito.when(mockNearlineConn.prepareStatement(Mockito.anyString())).thenReturn(mockStmt);
        Mockito.when(mockStmt.executeQuery()).thenReturn(mockResultSet);
        Mockito.when(mockResultSet.next()).thenReturn(false); // Empty result set

        dataLoaderApi.isNearlineTierValid(); // Should throw DataStoreException
    }

    @Test(expected = DataStoreException.class)
    public void isNearlineTierValidHandlesDbException() throws Exception {
        Mockito.when(mockNearlineConn.prepareStatement(Mockito.anyString())).thenReturn(mockStmt);
        Mockito.when(mockStmt.executeQuery()).thenThrow(new SQLException("Connection closed by server"));

        dataLoaderApi.isNearlineTierValid(); // Should handle exception and throw a DataStoreException
    }

    @Test
    public void setsNearlineTierValidConfigValue() throws Exception {
        Mockito.when(mockNearlineConn.prepareStatement(Mockito.anyString())).thenReturn(mockStmt);

        boolean isValid = true; // "true" case
        dataLoaderApi.setNearlineTierValid(isValid);

        // Must set the right parameter in the "tier2_config" table
        Mockito.verify(mockNearlineConn, Mockito.times(1))
                .prepareStatement("update tier2_config set value = ? where key = 'tier2_valid'");
        // Must set value correctly
        Mockito.verify(mockStmt, Mockito.times(1)).setString(1, Boolean.toString(isValid));
        // Must execute the statement
        Mockito.verify(mockStmt, Mockito.times(1)).execute();
        // Must commit the transaction
        Mockito.verify(mockNearlineConn, Mockito.times(1)).commit();

        isValid = false; // "false" case
        dataLoaderApi.setNearlineTierValid(isValid);
        // Must set value correctly
        Mockito.verify(mockStmt, Mockito.times(1)).setString(1, Boolean.toString(isValid));
    }

    @Test(expected = DataStoreException.class)
    public void setNearlineTierValidHandlesDbException() throws Exception {
        Mockito.when(mockNearlineConn.prepareStatement(Mockito.anyString())).thenReturn(mockStmt);
        Mockito.when(mockStmt.execute()).thenThrow(new SQLException("Connection closed by server"));

        dataLoaderApi.setNearlineTierValid(true); // Should handle exception and throw a DataStoreException
    }

    @Test
    public void populatesOnlineTierFromNearlineTier() throws Exception {
        CallableStatement mockLoadStmt = Mockito.mock(CallableStatement.class);
        PreparedStatement mockActiveStoreStmt = Mockito.mock(PreparedStatement.class);
        PreparedStatement mockHistoryStoreStmt = Mockito.mock(PreparedStatement.class);
        ResultSetMetaData mockMetaData = Mockito.mock(ResultSetMetaData.class);
        Object mockValue = Mockito.mock(Object.class);
        ResultSet[] mockResultSets = new ResultSet[TABLES.length];

        // Prepare the populateOnlineTierFromNearlineTier mock objects for full execution
        preparePopulateOnlineTierFromNearlineTier(mockLoadStmt, mockActiveStoreStmt, mockHistoryStoreStmt, mockValue,
                mockResultSets);

        dataLoaderApi.populateOnlineTierFromNearlineTier();

        // Must have loaded data from Nearline DB for each table in TABLES
        Mockito.verify(mockNearlineConn, Mockito.times(TABLES.length)).prepareCall(Mockito.anyString());
        Mockito.verify(mockLoadStmt, Mockito.times(TABLES.length)).executeQuery();
        // Must have stored data in the Online DB in each active (TABLES) and history table (HISTORY_TABLES)
        Mockito.verify(mockOnlineConn, Mockito.times(TABLES.length + HISTORY_TABLES.size()))
                .prepareStatement(Mockito.anyString());
        Mockito.verify(mockActiveStoreStmt, Mockito.times(TABLES.length)).execute(); // One record per table
        Mockito.verify(mockHistoryStoreStmt, Mockito.times(HISTORY_TABLES.size())).execute(); // One record per table
        // All the Nearline tier data must have been retrieved from result sets
        for (int i = 0; i < mockResultSets.length; i++) {
            // All the data from the result sets (data from Nearline tables) must have been retrieved (note: result
            // sets have one row and all dummy tables have the same number of fields
            if (HISTORY_TABLES.containsKey(TABLES[i])) { // Has a history table?
                // Data is retrieved twice: once for active table and once for history table
                Mockito.verify(
                        mockResultSets[i],
                        Mockito.times(DUMMY_TABLE_FIELDS.length * 2))
                        .getObject(Mockito.anyString());
            } else {
                // Data is retrieved once
                Mockito.verify(
                        mockResultSets[i],
                        Mockito.times(DUMMY_TABLE_FIELDS.length))
                        .getObject(Mockito.anyString());
            }
        }
        // All the data must have been stored in the respective active and history tables
        for (int i = 1; i <= DUMMY_TABLE_FIELDS.length; i++) {
            // All dummy tables have the same number of fields and column type

            // Should have been called once per active table
            Mockito.verify(mockActiveStoreStmt, Mockito.times(TABLES.length)).setObject(i, mockValue, SQL_DATA_TYPE);
            // Should have been called once per history table
            Mockito.verify(mockHistoryStoreStmt, Mockito.times(HISTORY_TABLES.size()))
                    .setObject(i, mockValue, SQL_DATA_TYPE);
        }
    }

    @Test
    public void populateOnlineTierFromNearlineTierHandlesNullFields() throws Exception {
        CallableStatement mockLoadStmt = Mockito.mock(CallableStatement.class);
        PreparedStatement mockActiveStoreStmt = Mockito.mock(PreparedStatement.class);
        PreparedStatement mockHistoryStoreStmt = Mockito.mock(PreparedStatement.class);
        ResultSetMetaData mockMetaData = Mockito.mock(ResultSetMetaData.class);
        ResultSet[] mockResultSets = new ResultSet[TABLES.length];

        // Prepare the populateOnlineTierFromNearlineTier mock objects for full execution.  Return null as the mock
        // value to test that it can handle null values from DB
        preparePopulateOnlineTierFromNearlineTier(mockLoadStmt, mockActiveStoreStmt, mockHistoryStoreStmt, null,
                mockResultSets);

        dataLoaderApi.populateOnlineTierFromNearlineTier();

        for (int i = 1; i <= DUMMY_TABLE_FIELDS.length; i++) {
            // All dummy tables have the same number of fields and column type.  All data is null.

            // Should have been called once per active table
            Mockito.verify(mockActiveStoreStmt, Mockito.times(TABLES.length)).setNull(i, SQL_DATA_TYPE);
            // Should have been called once per history table
            Mockito.verify(mockHistoryStoreStmt, Mockito.times(HISTORY_TABLES.size()))
                    .setNull(i, SQL_DATA_TYPE);
        }
    }

    @Test(expected = DataStoreException.class)
    public void populateOnlineTierFromNearlineTierHandlesNearlineStatementCreationException() throws Exception {
        Mockito.when(mockNearlineConn.prepareCall(Mockito.anyString()))
                .thenThrow(new SQLException("Connection closed by server"));

        // Should handle exception and throw DataStoreException
        dataLoaderApi.populateOnlineTierFromNearlineTier();
    }

    @Test(expected = DataStoreException.class)
    public void populateOnlineTierFromNearlineTierHandlesOnlineStatementCreationException() throws Exception {
        Mockito.when(mockOnlineConn.prepareStatement(Mockito.anyString()))
                .thenThrow(new SQLException("Connection closed by server"));

        // Should handle exception and throw DataStoreException
        dataLoaderApi.populateOnlineTierFromNearlineTier();
    }

    @Test
    public void dropSnapshotTablesFromNearlineTier() throws Exception {

        CallableStatement mockLoadStmt = Mockito.mock(CallableStatement.class);
        Mockito.when(mockNearlineConn.prepareCall("{ call truncatesnapshottablerecords() }")).thenReturn(mockLoadStmt);
        Mockito.when(mockLoadStmt.executeQuery()).thenReturn(mockResultSet);
        dataLoaderApi.dropSnapshotTablesFromNearlineTier();
        Mockito.verify(mockNearlineConn, Mockito.atLeastOnce()).commit();

        Mockito.when(mockNearlineConn.prepareCall("{ call truncatesnapshottablerecords() }")).thenThrow(new SQLException());
        dataLoaderApi.dropSnapshotTablesFromNearlineTier();

    }

    @Test(expected = DataStoreException.class)
    public void populateOnlineTierFromNearlineTierHandlesNearlineLoadException() throws Exception {
        CallableStatement mockLoadStmt = Mockito.mock(CallableStatement.class);
        PreparedStatement mockStoreStmt = Mockito.mock(PreparedStatement.class);

        Mockito.when(mockNearlineConn.prepareCall(Mockito.anyString())).thenReturn(mockLoadStmt);
        Mockito.when(mockOnlineConn.prepareStatement(Mockito.anyString())).thenReturn(mockStoreStmt);
        Mockito.when(mockLoadStmt.executeQuery()).thenThrow(new SQLException("Connection closed by server"));

        // Should handle exception and throw DataStoreException
        dataLoaderApi.populateOnlineTierFromNearlineTier();
    }

    @Test(expected = DataStoreException.class)
    public void populateOnlineTierFromNearlineTierHandlesOnlineStoreException() throws Exception {
        CallableStatement mockLoadStmt = Mockito.mock(CallableStatement.class);
        PreparedStatement mockActiveStoreStmt = Mockito.mock(PreparedStatement.class);
        PreparedStatement mockHistoryStoreStmt = Mockito.mock(PreparedStatement.class);
        ResultSetMetaData mockMetaData = Mockito.mock(ResultSetMetaData.class);
        ResultSet[] mockResultSets = new ResultSet[TABLES.length];
        Object mockValue = Mockito.mock(Object.class);

        // Prepare the populateOnlineTierFromNearlineTier mock objects for full execution.  Return null as the mock
        // value to test that it can handle null values from DB
        preparePopulateOnlineTierFromNearlineTier(mockLoadStmt, mockActiveStoreStmt, mockHistoryStoreStmt, mockValue,
                mockResultSets);
        // Throw exception when storing in Online tier
        Mockito.when(mockActiveStoreStmt.execute()).thenThrow(new SQLException("Connection closed by server"));

        // Should handle exception and throw DataStoreException
        dataLoaderApi.populateOnlineTierFromNearlineTier();
    }

    private void preparePopulateOnlineTierFromNearlineTier(CallableStatement mockLoadStmt,
                                                           PreparedStatement mockActiveStoreStmt,
                                                           PreparedStatement mockHistoryStoreStmt,
                                                           Object mockValue,
                                                           ResultSet[] mockResultSets) throws Exception {
        assert mockResultSets.length == TABLES.length;

        ResultSetMetaData mockMetaData = Mockito.mock(ResultSetMetaData.class);

        for (int i = 0; i < TABLES.length; i++) { // One result set per table
            mockResultSets[i] = Mockito.mock(ResultSet.class);
            Mockito.when(mockResultSets[i].getMetaData()).thenReturn(mockMetaData);
            Mockito.when(mockResultSets[i].next()).thenReturn(true).thenReturn(false); // One record in result set
            Mockito.when(mockResultSets[i].getObject(Mockito.anyString())).thenReturn(mockValue);
        }
        // Return a different result set per load (from Nearline connection)
        Mockito.when(mockLoadStmt.executeQuery()).thenAnswer(
                new Answer() {
                    private int invocationCount = 0;

                    public Object answer(InvocationOnMock invocation) {
                        return mockResultSets[invocationCount++];
                    }
                }
        );

        Mockito.when(mockNearlineConn.prepareCall(Mockito.anyString())).thenReturn(mockLoadStmt);
        Mockito.when(mockOnlineConn.prepareStatement(AdditionalMatchers.not(Mockito.contains("History"))))
                .thenReturn(mockActiveStoreStmt);
        Mockito.when(mockOnlineConn.prepareStatement(Mockito.contains("History")))
                .thenReturn(mockHistoryStoreStmt);
        Mockito.when(mockMetaData.getColumnCount()).thenReturn(DUMMY_TABLE_FIELDS.length);
        for (int i = 0; i < DUMMY_TABLE_FIELDS.length; i++) {
            Mockito.when(mockMetaData.getColumnName(i + 1)).thenReturn(DUMMY_TABLE_FIELDS[i]);
        }
        Mockito.when(mockMetaData.getColumnType(Mockito.anyInt())).thenReturn(SQL_DATA_TYPE);

        // Use mock data instead of actual table data
        dataLoaderApi.tables = TABLES;
        dataLoaderApi.tableFields = TABLE_FIELDS;
        dataLoaderApi.historyTables = HISTORY_TABLES;
        dataLoaderApi.tableToProcedure = TABLE_TO_PROCEDURE;
        dataLoaderApi.historyTableFields = HISTORY_TABLE_FIELDS;
    }

    private static final String[] TABLES = {"DummyTable", "DummyTable2"};
    private static final String[] DUMMY_TABLE_FIELDS = {"field1", "field2", "field3"};
    private static final Map<String, String> HISTORY_TABLES;
    private static final Map<String, String> TABLE_TO_PROCEDURE;
    private static final Map<String, String[]> TABLE_FIELDS;
    private static final Map<String, String[]> HISTORY_TABLE_FIELDS;

    private static final int SQL_DATA_TYPE = Types.CHAR;

    static {
        TABLE_FIELDS = new HashMap<>();
        TABLE_FIELDS.put("DummyTable", DUMMY_TABLE_FIELDS);
        TABLE_FIELDS.put("DummyTable2", DUMMY_TABLE_FIELDS);

        HISTORY_TABLES = new HashMap<>();
        HISTORY_TABLES.put("DummyTable", "DummyTable_History");
        // Note: not every table has a history table

        HISTORY_TABLE_FIELDS = new HashMap<>();
        HISTORY_TABLE_FIELDS.put("DummyTable_History", DUMMY_TABLE_FIELDS);

        TABLE_TO_PROCEDURE = new HashMap<>();
        TABLE_TO_PROCEDURE.put("DummyTable", "get_latest_dummytable_records()");
        TABLE_TO_PROCEDURE.put("DummyTable2", "get_latest_dummytable2_records()");
    }

    private Connection mockNearlineConn;
    private Connection mockOnlineConn;
    private PreparedStatement mockStmt;
    private ResultSet mockResultSet;
    private Logger mockLog;
    private DataLoaderApiJdbc dataLoaderApi;
}