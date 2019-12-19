// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.networking.sink.rabbitmq;

import com.intel.logging.Logger;
import com.intel.networking.sink.NetworkDataSinkDelegate;

import com.rabbitmq.client.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class NetworkDataSinkRabbitMQTest implements Logger, NetworkDataSinkDelegate {
    private boolean listenException_;
    private MockNetworkDataSinkRabbitMQ instance_;
    private ConnectionFactory factory_;
    private Connection connection_;
    private Channel channel_;

    private class MockNetworkDataSinkRabbitMQ extends NetworkDataSinkRabbitMQ {
        public MockNetworkDataSinkRabbitMQ(Map<String, String> args) {
            super(mock(Logger.class), args);
        }

        @Override
        ConnectionFactory createConnectionFactory() {
            return factory_;
        }

        @Override
        void consumeData(String tag) throws IOException {
            if (listenException_)
                throw new IOException("Test exception!");
        }
    }

    @Override
    public void initialize() {
    }

    @Override
    public void fatal(String fmt, Object... args) {
    }

    @Override
    public void error(String msg) {
    }

    @Override
    public void error(String fmt, Object... args) {
    }

    @Override
    public void warn(String msg) {
    }

    @Override
    public void warn(String fmt, Object... args) {
    }

    @Override
    public void info(String msg) {
    }

    @Override
    public void info(String fmt, Object... args) {
    }

    @Override
    public void debug(String msg) {
    }

    @Override
    public void debug(String fmt, Object... args) {
    }

    @Override
    public void exception(Throwable e, String msg) {
        error(String.format("%s: %s", msg, e.getMessage()));
        for(StackTraceElement element: e.getStackTrace())
            debug(element.toString());
    }

    @Override
    public void exception(Throwable e, String fmt, Object... args) {
        exception(e, String.format(fmt, args));
    }

    @Override
    public void exception(Throwable e) {
        error(e.getMessage());
    }

    @Override
    public void processIncomingData(String subject, String payload) {
    }

    @Before
    public void setUp() throws Exception {
        listenException_ = false;
        Map<String,String> sinkArgs = new HashMap<>();
        sinkArgs.put("exchangeName", "exchangeName");
        sinkArgs.put("uri", "uri");
        sinkArgs.put("subjects", "env");
        factory_ = mock(ConnectionFactory.class);
        connection_ = mock(Connection.class);
        channel_ = mock(Channel.class);
        when(factory_.newConnection()).thenReturn(connection_);
        when(connection_.createChannel()).thenReturn(channel_);
        when(channel_.queueDeclare()).thenReturn(mock(AMQP.Queue.DeclareOk.class));
        instance_ = new MockNetworkDataSinkRabbitMQ(sinkArgs);
    }

    @After
    public void tearDown() {
        instance_.stopListening();
        instance_ = null;
    }

    @Test
    public void ctor() {
        Map<String,String> sinkArgs = new HashMap<>();
        sinkArgs.put("exchangeName", "exchange");
        sinkArgs.put("subjects", "env");
        sinkArgs.put("uri", "amqp://127.0.0.1");
        MockNetworkDataSinkRabbitMQ sink = new MockNetworkDataSinkRabbitMQ(sinkArgs);
        sink.setCallbackDelegate(this);
        sink.setConnectionInfo("amqp://127.0.0.1");
        sink.error(new RuntimeException("Some exception!"));
        sink.setLogger(this);
        sink.setMonitoringSubject("env");
        sink.setMonitoringSubjects(new ArrayList<>());
        assertFalse(sink.isListening());
        sink.startListening();
        assertTrue(sink.isListening());
        sink.startListening();
        assertTrue(sink.isListening());
        sink.error(new RuntimeException("Some exception!"));
        sink.stopListening();
        sink.clearSubjects();
        assertEquals("rabbitmq", sink.getProviderName());
    }

    @Test
    public void ctorNegative() {
        MockNetworkDataSinkRabbitMQ sink;
        try {
            Map<String,String> sinkArgs = null;
            sink = new MockNetworkDataSinkRabbitMQ(sinkArgs);
            fail("Zero length test");
        } catch(IllegalArgumentException e) {
            // Pass...
        }
        try {
            Map<String,String> sinkArgs = new HashMap<>();
            sink = new MockNetworkDataSinkRabbitMQ(sinkArgs);
            fail("Zero length test");
        } catch(IllegalArgumentException e) {
            // Pass...
        }
        try {
            Map<String,String> sinkArgs = new HashMap<>();
            sinkArgs.put("exchangeName", null);
            sink = new MockNetworkDataSinkRabbitMQ(sinkArgs);
            fail("null first value test");
        } catch(IllegalArgumentException e) {
            // Pass...
        }
        try {
            Map<String,String> sinkArgs = new HashMap<>();
            sinkArgs.put("exchangeName", "  \n \t ");
            sink = new MockNetworkDataSinkRabbitMQ(sinkArgs);
            fail("empty string first value test");
        } catch(IllegalArgumentException e) {
            // Pass...
        }
        try {
            Map<String,String> sinkArgs = new HashMap<>();
            sinkArgs.put("exchangeName", "test");
            sink = new MockNetworkDataSinkRabbitMQ(sinkArgs);
            fail("missing subjects key");
        } catch(IllegalArgumentException e) {
            // Pass...
        }
        try {
            Map<String,String> sinkArgs = new HashMap<>();
            sinkArgs.put("exchangeName", "test");
            sinkArgs.put("subjects", null);
            sink = new MockNetworkDataSinkRabbitMQ(sinkArgs);
            fail("null subjects key");
        } catch(IllegalArgumentException e) {
            // Pass...
        }
        try {
            Map<String,String> sinkArgs = new HashMap<>();
            sinkArgs.put("exchangeName", "test");
            sinkArgs.put("subjects", " \n \t ");
            sink = new MockNetworkDataSinkRabbitMQ(sinkArgs);
            fail("empty subjects key");
        } catch(IllegalArgumentException e) {
            // Pass...
        }
    }

    @Test
    public void createConnectionFactory() {
        Map<String,String> sinkArgs = new HashMap<>();
        sinkArgs.put("exchangeName", "exchangeName");
        sinkArgs.put("uri", "uri");
        sinkArgs.put("subjects", "env");
        NetworkDataSinkRabbitMQ instance = new NetworkDataSinkRabbitMQ(mock(Logger.class), sinkArgs);
        instance.createConnectionFactory();
    }

    @Test
    public void startListeningSetUri() throws Exception {
        doAnswer(invocation -> {
            throw new IOException();
        }).when(factory_).setUri(anyString());
        instance_.startListening();
    }

    @Test
    public void startListeningConnectionException() throws Exception {
        when(factory_.newConnection()).thenThrow(new IOException());
        instance_.startListening();
    }

    @Test
    public void startListeningChannelException() throws Exception {
        when(connection_.createChannel()).thenThrow(new IOException());
        instance_.startListening();
    }

    @Test
    public void startListeningChannelException2() throws Exception {
        when(channel_.queueDeclare()).thenThrow(new IOException());
        instance_.startListening();
    }

    @Test
    public void startListeningQueueDeclareException() throws Exception {
        when(channel_.queueDeclare(anyString(), anyBoolean(), anyBoolean(), anyBoolean(), any())).thenThrow(new IOException());
        instance_.startListening();
    }

    @Test
    public void StartAndStopListeningWithCloseExceptions() throws Exception {
        doThrow(IOException.class).when(channel_).close();
        doThrow(IOException.class).when(connection_).close();
        instance_.startListening();
        instance_.stopListening();
    }

    @Test
    public void StartAndStopListeningWithBasicCancelExceptions() throws Exception {
        doThrow(IOException.class).when(channel_).basicCancel(anyString());
        instance_.startListening();
        instance_.stopListening();
    }

    @Test
    public void StartAndStopListeningWithListenExceptions() {
        listenException_ = true;
        instance_.startListening();
        instance_.stopListening();
    }

    @Test
    public void startListeningConnectionNull() throws Exception {
        when(factory_.newConnection()).thenReturn(null);
        instance_.startListening();
    }

    @Test
    public void startListeningChannelNull() throws Exception {
        when(connection_.createChannel()).thenReturn(null);
        instance_.startListening();
    }
}
