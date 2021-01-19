// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.networking.source.rabbitmq;

import com.intel.logging.Logger;
import com.intel.networking.source.NetworkDataSourceFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NetworkDataSourceRabbitMQTest {
    private HashMap<String,String> sourceArgs_;
    private MockNetworkDataSourceRabbitMQ instance_;
    private ConnectionFactory factory_;
    private Connection connection_;
    private Channel channel_;
    private boolean failInitialize;
    private boolean processFail;

    private final class MockNetworkDataSourceRabbitMQ extends NetworkDataSourceRabbitMQ {
        MockNetworkDataSourceRabbitMQ(Map<String, String> args) {
            super(mock(Logger.class), args);
        }

        @Override
        ConnectionFactory createFactory() {
            return factory_;
        }

        @Override
        void initializePublisher() throws Exception {
            if(failInitialize)
                throw new IOException("Test Exception");
            super.initializePublisher();
        }

        @Override
        void processSendRequests() throws IOException {
            if(processFail)
                while (!stopPublisher_.get()) {
                    if (!queue_.isEmpty()) {
                        throw new IOException("Test Exception");
                    }
                }
            else super.processSendRequests();
        }
    }

    @Before
    public void setUp() throws Exception {
        failInitialize = false;
        processFail = false;
        sourceArgs_ = new HashMap<>();
        sourceArgs_.put("exchangeName", "testing");
        factory_ = mock(ConnectionFactory.class);
        connection_ = mock(Connection.class);
        channel_ = mock(Channel.class);
        when(factory_.newConnection()).thenReturn(connection_);
        when(connection_.createChannel()).thenReturn(channel_);
        when(channel_.queueDeclare()).thenReturn(mock(AMQP.Queue.DeclareOk.class));
        instance_ = new MockNetworkDataSourceRabbitMQ(sourceArgs_);
        instance_.setLogger(mock(Logger.class));
    }

    @Test
    public void ctor_negative() {
        try {
            new MockNetworkDataSourceRabbitMQ(null).initialize();
            fail("null sourceArgs");
        } catch(IllegalArgumentException e) { /*PASS*/ }
        Map<String, String> sourceArgs = new HashMap<>();
        try {
            new MockNetworkDataSourceRabbitMQ(sourceArgs).initialize();
            fail("Missing exchangeName");
        } catch(IllegalArgumentException e) { /*PASS*/ }
        sourceArgs.put("exchangeName",null);
        try {
            new MockNetworkDataSourceRabbitMQ(sourceArgs).initialize();
            fail("null exchangeName");
        } catch(IllegalArgumentException e) { /*PASS*/ }
        sourceArgs.put("exchangeName"," \n \t ");
        try {
            new MockNetworkDataSourceRabbitMQ(sourceArgs).initialize();
            fail("empty exchangeName");
        } catch(IllegalArgumentException e) { /*PASS*/ }
    }

    @Test
    public void connect() throws IOException {
        instance_.connect(null);
        instance_.connect(null);
        instance_.close();
        instance_.connect(" \n \t ");
        instance_.close();
        instance_.connect("amqp://localhost");
        instance_.close();
        instance_.close();
    }

    @Test
    public void connectNegative() throws IOException {
        failInitialize = true;
        instance_.connect(null);
    }

    @Test
    public void getProviderName() {
        assertEquals("rabbitmq", instance_.getProviderName());
    }

    @Test
    public void sendMessageNegative() throws IOException {
        processFail = true;
        instance_.connect(null);
        try {
            instance_.sendMessage("key", "message");
        } finally {
            instance_.close();
        }
    }

    @Test
    public void sendMessage() throws IOException {
        assertFalse(instance_.sendMessage("key", "message"));
        instance_.connect(null);
        assertTrue(instance_.sendMessage("key", "message"));
        instance_.close();
    }

    @Test
    public void createNewFactory() throws Exception {
        Map<String, String> sourceArgs = new HashMap<>();
        sourceArgs.put("exchangeName", "testing");
        NetworkDataSourceRabbitMQ instance =
                (NetworkDataSourceRabbitMQ)NetworkDataSourceFactory.createInstance(mock(Logger.class),
                        "rabbitmq", sourceArgs);
        assertNotNull(instance.createFactory());
    }
}
