// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;


import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.*;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

public class VoltDbManagerGroupsTest {
    private Client client_;
    private ClientResponse response_;

    private class MockVoltDbManager extends VoltDbManager {
         MockVoltDbManager(String[] servers) { super(servers, mock(Logger.class)); initialize(); }
        @Override protected Client getClient() { return client_; }
    }

    @Before
    public void setUp() throws Exception {
        client_ = mock(Client.class);
        response_ = mock(ClientResponse.class);
        when(client_.callProcedure(anyString())).thenReturn(response_);
        when(client_.callProcedure(anyString(), anyString())).thenReturn(response_);
        when(client_.callProcedure(anyString(), anyString(), anyString())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
    }

    @Test
    public void addDevicesToExistingGroup() throws DataStoreException
    {
        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("GROUPNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("DEVICELIST", VoltType.STRING));
        t.addRow("group1", "test1");
        t.addRow("group1", "test2");
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);

        VoltDbManager voltMgr = new MockVoltDbManager(null);

        Set <String> devices = new HashSet<>();
        devices.add("test3");
        String result = voltMgr.addDevicesToGroup("group1", devices);
        assertTrue(result.contains("Successfully"));
    }

    @Test
    public void addDevicesToEmptyGroup() throws DataStoreException
    {
        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("GROUPNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("DEVICELIST", VoltType.STRING));
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);

        VoltDbManager voltMgr = new MockVoltDbManager(null);

        Set <String> devices = new HashSet<>();
        devices.add("test1");
        String result = voltMgr.addDevicesToGroup("group1", devices);
        assertTrue(result.contains("Successfully"));
    }

    @Test(expected = DataStoreException.class)
    public void addDevicesToGroupReturnErrorForVoltException() throws IOException, ProcCallException,
            DataStoreException {

        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("GROUPNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("DEVICELIST", VoltType.STRING));
        t.addRow("group1", "test1");
        t.addRow("group1", "test2");
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);

        when(client_.callProcedure(eq("UpsertLogicalGroups"), anyString(),
                anyString())).thenThrow(new IOException());
        VoltDbManager voltMgr = new MockVoltDbManager(null);

        Set<String> devices = new HashSet<>();
        devices.add("device1");
        devices.add("device2");
        String result = voltMgr.addDevicesToGroup("group1", devices);
        assertTrue(result.contains("Error"));
    }

    @Test
    public void deleteDeviceFromGroupButNotDeletingTheGroup() throws DataStoreException
    {
        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("GROUPNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("DEVICELIST", VoltType.STRING));
        t.addRow("group1", "test1");
        t.addRow("group1", "test2");
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);

        VoltDbManager voltMgr = new MockVoltDbManager(null);

        Set <String> devices = new HashSet<>();
        devices.add("test1");
        String result = voltMgr.deleteDevicesFromGroup("group1", devices);
        assertTrue(result.contains("Successfully"));
    }

    @Test
    public void deleteDeviceFromGroupAndDeletingTheGroup() throws DataStoreException
    {
        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("GROUPNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("DEVICELIST", VoltType.STRING));
        t.addRow("group1", "test1");
        voltArray[0] = t;

        VoltTable[] voltArray1 = new VoltTable[1];
        VoltTable t1 = new VoltTable(
                new VoltTable.ColumnInfo("GROUPNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("DEVICELIST", VoltType.STRING));
        t1.addRow("group2", "group1");
        t1.addRow("group3", "group1");
        t1.addRow("group3", "test2");
        voltArray1[0] = t1;

        when(response_.getResults()).thenReturn(voltArray, voltArray1);

        VoltDbManager voltMgr = new MockVoltDbManager(null);

        Set <String> devices = new HashSet<>();
        devices.add("test1");
        String result = voltMgr.deleteDevicesFromGroup("group1", devices);
        assertTrue(result.contains("Successfully"));
    }

    @Test(expected = DataStoreException.class)
    public void deleteDevicesErrorBecauseAllDevicesDontexist()throws DataStoreException {

        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("GROUPNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("DEVICELIST", VoltType.STRING));
        t.addRow("group1", "test1");
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);

        VoltDbManager voltMgr = new MockVoltDbManager(null);

        Set<String> devices = new HashSet<>();
        devices.add("device1");
        devices.add("device2");
        String result = voltMgr.deleteDevicesFromGroup("group1", devices);
        assertTrue(result.contains("Error"));
    }

    @Test(expected = DataStoreException.class)
    public void deleteDevicesErrorBecauseVoltDbException() throws IOException, ProcCallException, DataStoreException {

        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("GROUPNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("DEVICELIST", VoltType.STRING));
        t.addRow("group1", "test1");
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);
        when(client_.callProcedure(eq("DeleteGroupInLogicalGroups"), anyString())).thenThrow(new IOException());

        VoltDbManager voltMgr = new MockVoltDbManager(null);

        Set<String> devices = new HashSet<>();
        devices.add("test1");
        String result = voltMgr.deleteDevicesFromGroup("group1", devices);
        assertTrue(result.contains("Error"));
    }

    @Test
    public void getDevicesFromGroup() throws DataStoreException {
        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("GROUPNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("DEVICELIST", VoltType.STRING));
        t.addRow("group1", "test1");
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);

        VoltDbManager voltMgr = new MockVoltDbManager(null);
        Set<String> devices = new HashSet<>();
        devices.add("test1");

        Set<String> result = voltMgr.getDevicesFromGroup("group1");
        assertEquals(result, devices);
    }

    @Test
    public void getDevicesFromGroupEmpty() throws DataStoreException {
        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("GROUPNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("DEVICELIST", VoltType.STRING));
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);
        VoltDbManager voltMgr = new MockVoltDbManager(null);

        Set<String> result = voltMgr.getDevicesFromGroup("group1");
        assertTrue(result.isEmpty());
    }

    @Test
    public void listGroupsEmpty() throws  IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString())).thenReturn(response_);
        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("GROUPNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("DEVICELIST", VoltType.STRING));
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);
        VoltDbManager voltMgr = new MockVoltDbManager(null);

        Set<String> result = voltMgr.listGroups();
        assertTrue(result.isEmpty());
    }
}
