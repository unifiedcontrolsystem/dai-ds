// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.DataLoaderApi;
import com.intel.dai.exceptions.AdapterException;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;

import java.io.IOException;
import java.sql.Connection;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import static org.mockito.Mockito.*;

public class AdapterNearlineTierJdbcTest {
    class MockAdapterNearlineTierJdbc extends AdapterNearlineTierJdbc {
        MockAdapterNearlineTierJdbc(DataStoreFactory dsFactory)
                throws TimeoutException, IOException, ClassNotFoundException, DataStoreException {
            super(dsFactory, mock(Logger.class));
        }

        @Override
        protected void initializeAdapter() {
            adapter = mock(IAdapter.class);
            WorkQueue workQueue = mock(WorkQueue.class);
            try {
                when(adapter.setUpAdapter(anyString(), anyString())).thenReturn(workQueue);
                this.workQueue = workQueue;
            } catch(AdapterException e) {

            }
        }

        @Override
        NearlineTableUpdater createNearlineTableUpdater(Logger logger) throws DataStoreException {
            return mock(NearlineTableUpdater.class);
        }

        @Override
        DataReceiverAmqp createDataReceiver(String host)
                throws IOException, TimeoutException {
            return mover;
        }

        @Override
        void waitUntilFinishedProcessingMessages() {
        }

    }

    @Before
    public void setUp() throws Exception {
        mockDataLoader = mock(DataLoaderApi.class);
        mockDsFactory = mock(DataStoreFactory.class);
        when(mockDsFactory.createDataLoaderApi()).thenReturn(mockDataLoader);

        LoggerFactory.getInstance("TEST", "Testing", "console");
    }

    private byte[] makeBody(String tableName) {
        if(tableName != null)
            tableName = "\"" + tableName + "\"";
        byte[] body = ("{" +
                "\"AmqpMessageId\": 99," +
                "\"IntervalId\": 1," +
                "\"EndIntvlTsInMsSinceEpoch\": 100," +
                "\"StartIntvlTsInMsSinceEpoch\": 0," +
                String.format("\"TableName\": %s,", tableName) +
                "\"Part\": 1," +
                "\"Of\": 1," +
                "\"status\": 0," +
                "\"schema\": [" +
                "{" +
                "\"name\": \"column1\"," +
                "\"type\": 9" +
                "}," +
                "{" +
                "\"name\": \"column2\"," +
                "\"type\": 9" +
                "}," +
                "{" +
                "\"name\": \"column3\"," +
                "\"type\": 9" +
                "}," +
                "{" +
                "\"name\": \"column4\"," +
                "\"type\": 9" +
                "}" +
                "]," +
                "\"data\": [" +
                "[" +
                "\"row1\"," +
                "\"data1\"," +
                "\"red1\"," +
                "\"blue1\"" +
                "]," +
                "[" +
                "\"row2\"," +
                "\"data2\"," +
                "\"red2\"," +
                "\"blue2\"" +
                "]," +
                "[" +
                "\"row3\"," +
                "\"data3\"," +
                "\"red3\"," +
                "\"blue3\"" +
                "]," +
                "[" +
                "\"row4\"," +
                "\"data4\"," +
                "\"red4\"," +
                "\"blue4\"" +
                "]," +
                "[" +
                "\"row5\"," +
                "\"data5\"," +
                "\"red5\"," +
                "\"blue5\"" +
                "]" +
                "]" +
                "}").getBytes();
        return body;
    }

    private VoltTable[] buildScalarOneTable(long value) {
        VoltTable[] result = new VoltTable[1];
        result[0] = new VoltTable(new VoltTable.ColumnInfo[] {
                new VoltTable.ColumnInfo("count", VoltType.BIGINT
                )});
        result[0].addRow(value);
        return result;
    }

    @Test
    public void queueWorkItemsToHandleDataReceiver() throws Exception {
        AdapterNearlineTierJdbc nearline = new AdapterNearlineTierJdbcTest.MockAdapterNearlineTierJdbc(mockDsFactory);
        Client client = mock(Client.class);
        ClientResponse response = mock(ClientResponse.class);
        VoltTable[] one = buildScalarOneTable(0L);
        when(nearline.adapter.adapterType()).thenReturn("NEARLINE_TIER");
        when(nearline.adapter.client()).thenReturn(client);
        when(client.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(response);
        when(response.getResults()).thenReturn(one);
        when(nearline.workQueue.queueWorkItem(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyLong())).thenReturn(999L);
        nearline.queueWorkItemsToHandleDataReceiver();
    }

    @Test
    public void queueWorkItemsToHandleDataReceiver2() throws Exception {
        AdapterNearlineTierJdbc nearline = new MockAdapterNearlineTierJdbc(mockDsFactory);
        Client client = mock(Client.class);
        ClientResponse response = mock(ClientResponse.class);
        VoltTable[] one = buildScalarOneTable(1L);
        when(nearline.adapter.adapterType()).thenReturn("NEARLINE_TIER");
        when(nearline.adapter.client()).thenReturn(client);
        when(client.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(response);
        when(response.getResults()).thenReturn(one);
        when(nearline.workQueue.queueWorkItem(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(),
                ArgumentMatchers.anyString(), ArgumentMatchers.anyLong())).thenReturn(999L);
        nearline.queueWorkItemsToHandleDataReceiver();
    }

    @Test
    public void receiveDataFromDataMover1() throws Exception {
        AdapterNearlineTierJdbc nearline = new MockAdapterNearlineTierJdbc(mockDsFactory);
        when(nearline.workQueue.isThisNewWorkItem()).thenReturn(false);
        when(nearline.workQueue.workingResults()).thenReturn("Processed through (Timestamp=2018-08-13 18:04:00.000+0) (IntervalId=111) (AmqpMessageId=222) (TableName=WorkItem)");
        Channel channel = mock(Channel.class);
        when(mover.getChannel()).thenReturn(channel);
        when(channel.basicConsume(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(),
                ArgumentMatchers.any(AdapterNearlineTierJdbc.AmqpDataReceiverMsgConsumer.class))).thenReturn("");
        nearline.receiveDataFromDataMover();
    }

    @Test
    public void receiveDataFromDataMover2() throws Exception {
        AdapterNearlineTierJdbc nearline = new MockAdapterNearlineTierJdbc(mockDsFactory);
        when(nearline.workQueue.isThisNewWorkItem()).thenReturn(false);
        when(nearline.workQueue.workingResults()).thenReturn("Other string...");
        Channel channel = mock(Channel.class);
        when(mover.getChannel()).thenReturn(channel);
        when(channel.basicConsume(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(),
                ArgumentMatchers.any(AdapterNearlineTierJdbc.AmqpDataReceiverMsgConsumer.class))).thenReturn("");
        nearline.receiveDataFromDataMover();
    }

    @Test
    public void receiveDataFromDataMover3() throws Exception {
        AdapterNearlineTierJdbc nearline = new MockAdapterNearlineTierJdbc(mockDsFactory);
        when(nearline.workQueue.isThisNewWorkItem()).thenReturn(true);
        when(nearline.workQueue.workingResults()).thenReturn("Other string...");
        Channel channel = mock(Channel.class);
        when(mover.getChannel()).thenReturn(channel);
        when(channel.basicConsume(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(),
                ArgumentMatchers.any(AdapterNearlineTierJdbc.AmqpDataReceiverMsgConsumer.class))).thenReturn("");
        nearline.receiveDataFromDataMover();
    }

    @Test
    public void handleDelivery1() throws Exception {
        IAdapter adapter = mock(IAdapter.class);
        AdapterNearlineTierJdbc nearline = new MockAdapterNearlineTierJdbc(mockDsFactory);
        AdapterNearlineTierJdbc.AmqpDataReceiverMsgConsumer handler = new AdapterNearlineTierJdbc.AmqpDataReceiverMsgConsumer(
                mover, 98L, adapter, mock(Logger.class), mock(NearlineTableUpdater.class), nearline);
        Envelope envelope = new Envelope(555L, false, "exchange", "routing_key");
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties("","",new HashMap<String, Object>(),
                0,0,"","","","",
                new Date(),"","","","");
        Channel channel = mock(Channel.class);
        when(mover.getChannel()).thenReturn(channel);
        doAnswer((Answer) invoke -> null).when(channel).basicAck(ArgumentMatchers.anyLong(), ArgumentMatchers.anyBoolean());
        Client client = mock(Client.class);
        when(adapter.client()).thenReturn(client);
        handler.handleDelivery("", envelope, basicProperties, makeBody("Adapter"));
    }

    @Test
    public void handleDelivery2() throws Exception {
        IAdapter adapter = mock(IAdapter.class);
        AdapterNearlineTierJdbc nearline = new MockAdapterNearlineTierJdbc(mockDsFactory);
        AdapterNearlineTierJdbc.AmqpDataReceiverMsgConsumer handler = new AdapterNearlineTierJdbc.AmqpDataReceiverMsgConsumer(
                mover, 99L, adapter, mock(Logger.class), mock(NearlineTableUpdater.class), nearline);
        Envelope envelope = new Envelope(555L, false, "exchange", "routing_key");
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties("","",new HashMap<String, Object>(),
                0,0,"","","","",
                new Date(),"","","","");
        Channel channel = mock(Channel.class);
        when(mover.getChannel()).thenReturn(channel);
        doAnswer((Answer) invoke -> null).when(channel).basicAck(ArgumentMatchers.anyLong(), ArgumentMatchers.anyBoolean());
        handler.handleDelivery("", envelope, basicProperties, makeBody("Adapter"));
        doThrow(new RuntimeException()).when(adapter).abend(ArgumentMatchers.anyString());
        handler.handleDelivery("", envelope, basicProperties, makeBody("Adapter"));
    }

    @Test
    public void handleDelivery3() throws Exception {
        AdapterNearlineTierJdbc nearline = new MockAdapterNearlineTierJdbc(mockDsFactory);
        AdapterNearlineTierJdbc.AmqpDataReceiverMsgConsumer handler = new AdapterNearlineTierJdbc.AmqpDataReceiverMsgConsumer(
                mover, 98L, mock(IAdapter.class), mock(Logger.class), mock(NearlineTableUpdater.class), nearline);
        Envelope envelope = new Envelope(555L, false, "exchange", "routing_key");
        AMQP.BasicProperties basicProperties = new AMQP.BasicProperties("","",new HashMap<String, Object>(),
                0,0,"","","","",
                new Date(),"","","","");
        Channel channel = mock(Channel.class);
        when(mover.getChannel()).thenReturn(channel);
        doAnswer((Answer) invoke -> null).when(channel).basicAck(ArgumentMatchers.anyLong(), ArgumentMatchers.anyBoolean());
        handler.handleDelivery("", envelope, basicProperties, makeBody(null));
    }

    private DataReceiverAmqp mover = mock(DataReceiverAmqp.class);
    private DataLoaderApi mockDataLoader;
    private DataStoreFactory mockDsFactory;
}
