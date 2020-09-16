// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.jdbc;

import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.CallableStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HashMap;

public class InventorySnapshotJdbcTest {
    @Before
    public void setUp() throws Exception {
        mockDbConn = Mockito.mock(Connection.class);
        mockStmt = Mockito.mock(PreparedStatement.class);

        mockLogger = Mockito.mock(Logger.class);

        Mockito.when(mockDbConn.prepareStatement(Mockito.anyString())).thenReturn(mockStmt);

        invApi = new InventorySnapshotJdbc(mockLogger);
        invApi.setDbConn(mockDbConn);
    }

    @Test
    public void storesInventorySnapshot() throws Exception {
        Instant timestamp = Instant.now();
        HashMap<String, String> dummy_props = new HashMap<>();
        dummy_props.put(PROP1, VALUE1);
        dummy_props.put(PROP2, VALUE2);
        String info = dummy_props.toString();

        invApi.storeInventorySnapshot(LOCATION, timestamp, info);

        // Verify it inserts into the right table
        Mockito.verify(mockDbConn, Mockito.times(1))
                .prepareStatement(Mockito.contains("insert into Tier2_InventorySnapshot"));
        // Verify the right parameters are passed
        Mockito.verify(mockStmt, Mockito.times(1)).setString(Mockito.anyInt(), Mockito.eq(LOCATION));
        Mockito.verify(mockStmt, Mockito.times(1))
                .setTimestamp(Mockito.anyInt(), Mockito.eq(Timestamp.from(timestamp)));
        Mockito.verify(mockStmt, Mockito.times(1)).setString(Mockito.anyInt(), Mockito.eq(info));
        // Verify statement is executed
        Mockito.verify(mockStmt, Mockito.times(1)).execute();
    }

    @Test
    public void retrievesRefSnapshot() throws Exception {
        ResultSet mockResult = Mockito.mock(ResultSet.class);
        Mockito.when(mockStmt.executeQuery()).thenReturn(mockResult);
        Mockito.when(mockResult.next()).thenReturn(true).thenReturn(false); // The result set will contain one row
        Mockito.when(mockResult.getString(Mockito.anyString())).thenReturn(INVENTORY_INFO);

        PropertyMap snapshot = invApi.retrieveRefSnapshot(LOCATION);

        // Verify it selects from the right table
        Mockito.verify(mockDbConn, Mockito.times(1))
                .prepareStatement(Mockito.contains("select InventoryInfo from Tier2_InventorySnapshot"));
        // Verify the right parameters are passed
        Mockito.verify(mockStmt, Mockito.times(1)).setString(Mockito.anyInt(), Mockito.eq(LOCATION));
        // Verify the statement is executed
        Mockito.verify(mockStmt, Mockito.times(1)).executeQuery();
        // Verify the data is retrieved
        Mockito.verify(mockResult, Mockito.times(1)).next();
        Mockito.verify(mockResult, Mockito.times(1)).getString("InventoryInfo");
        // Verify the returned result contains the expected data
        Assert.assertEquals(snapshot.getString(PROP1), VALUE1);
        Assert.assertEquals(snapshot.getString(PROP2), VALUE2);
    }

    @Test
    public void retrievesSnapshotById() throws Exception {
        ResultSet mockResult = Mockito.mock(ResultSet.class);
        Mockito.when(mockStmt.executeQuery()).thenReturn(mockResult);
        Mockito.when(mockResult.next()).thenReturn(true).thenReturn(false); // The result set will contain one row
        Mockito.when(mockResult.getString(Mockito.anyString())).thenReturn(INVENTORY_INFO);

        PropertyMap snapshot = invApi.retrieveSnapshot(SNAPSHOT_ID);

        // Verify it selects from the right table
        Mockito.verify(mockDbConn, Mockito.times(1))
                .prepareStatement(Mockito.contains("select InventoryInfo from Tier2_InventorySnapshot"));
        // Verify the right parameters are passed
        Mockito.verify(mockStmt, Mockito.times(1)).setLong(Mockito.anyInt(), Mockito.eq(SNAPSHOT_ID));
        // Verify the statement is executed
        Mockito.verify(mockStmt, Mockito.times(1)).executeQuery();
        // Verify the data is retrieved
        Mockito.verify(mockResult, Mockito.times(1)).next();
        Mockito.verify(mockResult, Mockito.times(1)).getString("InventoryInfo");
        // Verify the returned result contains the expected data
        Assert.assertEquals(snapshot.getString(PROP1), VALUE1);
        Assert.assertEquals(snapshot.getString(PROP2), VALUE2);
    }

    @Test
    public void setsReferenceSnapshot() throws Exception {
        CallableStatement mockStmtCall = Mockito.mock(CallableStatement.class);
        Mockito.when(mockDbConn.prepareCall(Mockito.anyString())).thenReturn(mockStmtCall);

        invApi.setReferenceSnapshot(1);

        // Verify it update to the table
        Mockito.verify(mockDbConn, Mockito.times(1)).prepareCall(Mockito.contains("call SetRefSnapshotDataForLctn"));
        // Verify the right parameters are passed
        Mockito.verify(mockStmtCall, Mockito.times(1)).setInt(Mockito.anyInt(), Mockito.eq(1));
        Mockito.verify(mockStmtCall, Mockito.times(1)).execute();
    }

    @Test(expected = DataStoreException.class)
    public void handlesSetReferenceProcErrors() throws Exception {
        CallableStatement mockStmtCall = Mockito.mock(CallableStatement.class);
        Mockito.when(mockDbConn.prepareCall(Mockito.anyString())).thenReturn(mockStmtCall);
        Mockito.when(mockStmtCall.execute()).thenThrow(new SQLException("Inventory Snapshot not found."));
        invApi.setReferenceSnapshot(1);
    }

    @Test(expected = DataStoreException.class)
    public void throwsWhenRefSnapshotNotFound() throws Exception {
        ResultSet mockResult = Mockito.mock(ResultSet.class);
        Mockito.when(mockStmt.executeQuery()).thenReturn(mockResult);
        Mockito.when(mockResult.next()).thenReturn(false).thenReturn(false); // Empty result set

        invApi.retrieveRefSnapshot(LOCATION);
    }

    private static final String LOCATION = "R48-CH00-CN2";
    private static final long SNAPSHOT_ID = 2;
    private static final String PROP1 = "prop1";
    private static final String PROP2 = "prop2";
    private static final String VALUE1 = "value1";
    private static final String VALUE2 = "value2";
    private static final String INVENTORY_INFO = "{\"" +
            PROP1 + "\": \"" + VALUE1 + "\", " +
            "\"" + PROP2 + "\": \"" + VALUE2 + "\"}";

    Connection mockDbConn;
    PreparedStatement mockStmt;
    InventorySnapshotJdbc invApi;
    Logger mockLogger;
}

