// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.exceptions.AdapterException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.Ignore;

import com.rabbitmq.client.*;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.*;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdapterOnlineTierVoltTest {
    class MockAdapterOnlineTierVolt extends AdapterOnlineTierVolt {
        MockAdapterOnlineTierVolt() throws IOException, TimeoutException {
            super(mock(Logger.class));
        }

        @Override
        void initializeAdapter() {
            adapter = mock(IAdapter.class);
            WorkQueue workQueue = mock(WorkQueue.class);
            try {
                when(adapter.setUpAdapter(anyString(), anyString())).thenReturn(workQueue);
                this.workQueue = workQueue;
            } catch(AdapterException e) {

            }
        }
    }

    @Before
    public void setUp() throws Exception {
        LoggerFactory.getInstance("TEST", "Testing", "console");
    }

    private VoltTable[] buildScalarOneTable(long value) {
        VoltTable[] result = new VoltTable[1];
        result[0] = new VoltTable(new VoltTable.ColumnInfo[] {
                new VoltTable.ColumnInfo("count", VoltType.BIGINT
                )});
        result[0].addRow(value);
        return result;
    }

    private VoltTable[] buildTable(String[] columns, int rowCount) {
        return buildTable(columns, rowCount, true);
    }
    private VoltTable[] buildTable(String[] columns, int rowCount, boolean includeData) {
        VoltTable[] result = new VoltTable[1];
        VoltTable.ColumnInfo[] columnInfos = new VoltTable.ColumnInfo[columns.length];
        for (int i = 0; i < columns.length; i++)
            columnInfos[i] = new VoltTable.ColumnInfo(columns[i], VoltType.STRING);
        result[0] = new VoltTable(columnInfos);
        if (includeData)
            for (int i = 0; i < rowCount; i++) {
                Object[] params = new Object[columns.length];
                for (int j = 0; j < columns.length; j++)
                    params[j] = columns[(i + j) % columns.length];
                result[0].addRow(params);
            }
        return result;
    }

    @Test
    public void handlePurgingData1() throws Exception {
        AdapterOnlineTierVolt online = new MockAdapterOnlineTierVolt();
        doAnswer((Answer)invoke -> null).when(online.adapter).logRasEventNoEffectedJob(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
        AdapterOnlineTierVolt.lSavePreviousPurgeTimeInMillis = -1L;
        online.handlePurgingData(-1L, 1L);
    }

    @Test
    public void handlePurgingData2() throws Exception {
        AdapterOnlineTierVolt online = new MockAdapterOnlineTierVolt();
        doAnswer((Answer)invoke -> null).when(online.adapter).logRasEventNoEffectedJob(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
        HashSet<String> set = online.setOfTablesToBePurged();
        set.add("RasMetaData");
        VoltTable[] one = buildScalarOneTable(0L);
        ClientResponse clientResponse = mock(ClientResponse.class);
        Client client = mock(Client.class);
        when(client.callProcedure(ArgumentMatchers.matches("@AdHoc"), ArgumentMatchers.anyString())).thenReturn(clientResponse);
        when(client.callProcedure(ArgumentMatchers.matches("NodePurgeInventory_History"), ArgumentMatchers.anyString())).thenReturn(clientResponse);
        when(client.callProcedure(ArgumentMatchers.matches("ReservationPurging"), ArgumentMatchers.anyString())).thenReturn(clientResponse);
        when(clientResponse.getResults()).thenReturn(one);
        when(online.adapter.client()).thenReturn(client);
        AdapterOnlineTierVolt.lSavePreviousPurgeTimeInMillis = -1L;
        online.handlePurgingData(5L, 1L);
    }

    @Test
    public void handlePurgingData3() throws Exception {
        AdapterOnlineTierVolt online = new MockAdapterOnlineTierVolt();
        doAnswer((Answer)invoke -> null).when(online.adapter).logRasEventNoEffectedJob(ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyLong(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyLong());
        HashSet<String> set = online.setOfTablesToBePurged();
        set.add("RasMetaData");
        VoltTable[] one = buildScalarOneTable(3L);
        ClientResponse clientResponse = mock(ClientResponse.class);
        Client client = mock(Client.class);
        when(client.callProcedure(ArgumentMatchers.matches("@AdHoc"), ArgumentMatchers.anyString())).thenReturn(clientResponse);
        when(client.callProcedure(ArgumentMatchers.matches("NodePurgeInventory_History"), ArgumentMatchers.anyString())).thenReturn(clientResponse);
        when(client.callProcedure(ArgumentMatchers.matches("ReservationPurging"), ArgumentMatchers.anyString())).thenReturn(clientResponse);
        when(clientResponse.getResults()).thenReturn(one);
        when(online.adapter.client()).thenReturn(client);
        AdapterOnlineTierVolt.lSavePreviousPurgeTimeInMillis = -1L;
        online.handlePurgingData(5L, 1L);
    }

    private DataMoverAmqp mover = mock(DataMoverAmqp.class);
}
