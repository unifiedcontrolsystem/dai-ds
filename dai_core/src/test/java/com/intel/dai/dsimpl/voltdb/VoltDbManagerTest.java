// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;

import java.util.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class VoltDbManagerTest {

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
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
    }

    @Test
    public void getRackConfiguration() throws DataStoreException {

        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("LCTN", VoltType.STRING),
                new VoltTable.ColumnInfo("SERNUM", VoltType.STRING),
                new VoltTable.ColumnInfo("TYPE", VoltType.STRING));
        t.addRow("R0", "A23456", "service-rack");
        t.addRow("R1", "A23457", "compute-rack");
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);

        VoltDbManager voltMgr = new VoltDbManagerTest.MockVoltDbManager(null);

        PropertyArray array = voltMgr.getRackConfiguration();
        Assert.assertEquals(array.size(), 2);
    }

    @Test
    public void getComputeNodeConfiguration() throws DataStoreException {

        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("LCTN", VoltType.STRING),
                new VoltTable.ColumnInfo("HOSTNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("STATE", VoltType.STRING));
        t.addRow("R0_CH0-CN0", "A23456", "A");
        t.addRow("R0-CH1-CN0", "A23457", "A");
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);

        VoltDbManager voltMgr = new VoltDbManagerTest.MockVoltDbManager(null);

        PropertyArray array = voltMgr.getComputeNodeConfiguration();
        Assert.assertEquals(array.size(), 2);
    }

    @Test
    public void getServiceNodeConfiguration() throws DataStoreException {

        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("LCTN", VoltType.STRING),
                new VoltTable.ColumnInfo("HOSTNAME", VoltType.STRING),
                new VoltTable.ColumnInfo("STATE", VoltType.STRING));
        t.addRow("R0_CH0-SN0", "A23456", "A");
        t.addRow("R0-CH1-SN0", "A23457", "A");
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);

        VoltDbManager voltMgr = new VoltDbManagerTest.MockVoltDbManager(null);

        PropertyArray array = voltMgr.getServiceNodeConfiguration();
        Assert.assertEquals(array.size(), 2);
    }

    @Test
    public void generateSetForKeyNullObject() {
        PropertyArray jsonArray = new PropertyArray();
        jsonArray.add(null);

        VoltDbManager voltMgr = new VoltDbManagerTest.MockVoltDbManager(null);

        Assert.assertEquals(voltMgr.generateSetForKey(jsonArray, "").size(), 0);
    }

    @Test
    public void generateSetForKeyAddNonJsonObjectType() {
        PropertyArray jsonArray = new PropertyArray();
        jsonArray.add(1);

        VoltDbManager voltMgr = new VoltDbManagerTest.MockVoltDbManager(null);

        Assert.assertEquals(voltMgr.generateSetForKey(jsonArray, "").size(), 0);
    }

}