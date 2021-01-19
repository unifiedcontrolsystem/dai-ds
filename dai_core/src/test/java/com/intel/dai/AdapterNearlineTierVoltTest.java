// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.exceptions.AdapterException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.junit.Ignore;
import org.mockito.ArgumentMatchers;
import org.mockito.stubbing.Answer;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import com.rabbitmq.client.*;
import com.rabbitmq.client.AMQP.BasicProperties;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.concurrent.TimeoutException;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdapterNearlineTierVoltTest {
    class MockAdapterNearlineTierVolt extends AdapterNearlineTierVolt {
        MockAdapterNearlineTierVolt() throws IOException, TimeoutException {
            super(mock(Logger.class));
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
        client_ = mock(Client.class);
        response_ = mock(ClientResponse.class);
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

    @Test
    public void receiveDataFromDataMover1() throws Exception {
        AdapterNearlineTierVolt nearline = new MockAdapterNearlineTierVolt();
        when(nearline.adapter.client()).thenReturn(client_);
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getResults()).thenReturn(buildScalarOneTable(3));
        when(nearline.workQueue.isThisNewWorkItem()).thenReturn(false);
        when(nearline.workQueue.workingResults()).thenReturn("Processed through (Timestamp=2018-08-13 18:04:00.000+0) (IntervalId=111) (AmqpMessageId=222) (TableName=WorkItem)");
        Channel channel = mock(Channel.class);
        when(mover.getChannel()).thenReturn(channel);
        when(channel.basicConsume(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(),
                ArgumentMatchers.any(AdapterNearlineTierVolt.AmqpDataReceiverMsgConsumer.class))).thenReturn("");
        nearline.receiveDataFromDataMover();
    }

    @Test
    public void receiveDataFromDataMover2() throws Exception {
        AdapterNearlineTierVolt nearline = new MockAdapterNearlineTierVolt();
        when(nearline.adapter.client()).thenReturn(client_);
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getResults()).thenReturn(buildScalarOneTable(3));
        when(nearline.workQueue.isThisNewWorkItem()).thenReturn(false);
        when(nearline.workQueue.workingResults()).thenReturn("Other string...");
        Channel channel = mock(Channel.class);
        when(mover.getChannel()).thenReturn(channel);
        when(channel.basicConsume(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(),
                ArgumentMatchers.any(AdapterNearlineTierVolt.AmqpDataReceiverMsgConsumer.class))).thenReturn("");
        nearline.receiveDataFromDataMover();
    }

    @Test
    public void receiveDataFromDataMover3() throws Exception {
        AdapterNearlineTierVolt nearline = new MockAdapterNearlineTierVolt();
        when(nearline.adapter.client()).thenReturn(client_);
        when(client_.callProcedure(ArgumentMatchers.anyString(), ArgumentMatchers.anyString())).thenReturn(response_);
        when(response_.getResults()).thenReturn(buildScalarOneTable(3));
        when(nearline.workQueue.isThisNewWorkItem()).thenReturn(true);
        when(nearline.workQueue.workingResults()).thenReturn("Other string...");
        Channel channel = mock(Channel.class);
        when(mover.getChannel()).thenReturn(channel);
        when(channel.basicConsume(ArgumentMatchers.anyString(), ArgumentMatchers.anyBoolean(),
                ArgumentMatchers.any(AdapterNearlineTierVolt.AmqpDataReceiverMsgConsumer.class))).thenReturn("");
        nearline.receiveDataFromDataMover();
    }

    private byte[] makeBody(String tableName) {
        byte[] body = ("{" +
                "\"AmqpMessageId\": 99," +
                "\"IntervalId\": 1," +
                "\"EndIntvlTsInMsSinceEpoch\": 100," +
                "\"StartIntvlTsInMsSinceEpoch\": 0," +
                String.format("\"TableName\": \"%s\",", tableName) +
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

    @Test
    public void handleDelivery1() throws Exception {
        IAdapter adapter = mock(IAdapter.class);
        AdapterNearlineTierVolt.AmqpDataReceiverMsgConsumer handler = new MockAdapterNearlineTierVolt().new AmqpDataReceiverMsgConsumer(mover);
        Envelope envelope = new Envelope(555L, false, "exchange", "routing_key");
        BasicProperties basicProperties = new BasicProperties("","",new HashMap<String, Object>(),
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
        AdapterNearlineTierVolt.AmqpDataReceiverMsgConsumer handler = new MockAdapterNearlineTierVolt().new AmqpDataReceiverMsgConsumer(mover);
        Envelope envelope = new Envelope(555L, false, "exchange", "routing_key");
        BasicProperties basicProperties = new BasicProperties("","",new HashMap<String, Object>(),
                0,0,"","","","",
                new Date(),"","","","");
        Channel channel = mock(Channel.class);
        when(mover.getChannel()).thenReturn(channel);
        doAnswer((Answer) invoke -> null).when(channel).basicAck(ArgumentMatchers.anyLong(), ArgumentMatchers.anyBoolean());
        handler.handleDelivery("", envelope, basicProperties, makeBody("Adapter"));
    }

    @Test
    public void handleDelivery3() throws Exception {
        AdapterNearlineTierVolt.AmqpDataReceiverMsgConsumer handler = new MockAdapterNearlineTierVolt().new AmqpDataReceiverMsgConsumer(mover);
        Envelope envelope = new Envelope(555L, false, "exchange", "routing_key");
        BasicProperties basicProperties = new BasicProperties("","",new HashMap<String, Object>(),
                0,0,"","","","",
                new Date(),"","","","");
        Channel channel = mock(Channel.class);
        when(mover.getChannel()).thenReturn(channel);
        doAnswer((Answer) invoke -> null).when(channel).basicAck(ArgumentMatchers.anyLong(), ArgumentMatchers.anyBoolean());
        handler.handleDelivery("", envelope, basicProperties, makeBody("Unknown"));
    }

    private DataReceiverAmqp mover = mock(DataReceiverAmqp.class);
    private Client client_;
    private ClientResponse response_;
}
