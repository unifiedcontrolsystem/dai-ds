// Copyright (C) 2018-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.networking.source.zmq;

import com.intel.logging.Logger;
import com.intel.networking.source.NetworkDataSourceFactory;
import org.junit.Before;
import org.junit.Test;
import org.zeromq.ZSocket;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NetworkDataSourceZMQTest {
    private HashMap<String,String> sourceArgs_;
    private MockNetworkDataSourceZMQ instance_;
    private ZSocket subscriber_;
    private boolean failInitialize;
    private boolean processFail;

    private final class MockNetworkDataSourceZMQ extends NetworkDataSourceZMQ {
        public MockNetworkDataSourceZMQ(Map<String, String> args) {
            super(mock(Logger.class), args);
        }

        @Override
        ZSocket createSocketZMQ(){
            ZSocket socket =  mock(ZSocket.class);
            when(socket.connect(anyString())).thenReturn(true);
            return socket;
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
        sourceArgs_.put("uri", "tcp://127.0.0.1:5401");
        sourceArgs_.put("subjects", "env,evt,avg");
        subscriber_ = mock(ZSocket.class);
        String subject_ = "env,evt,avg";
        String connectionString_ = "tcp://127.0.0.1:5401";
        for (String topic: subject_.split(",")) {
            subscriber_.subscribe(topic);
        }
        for (String connection: connectionString_.split(",")) {
            if(!subscriber_.connect(connection)) {
                continue;
            }
        }
        instance_ = new NetworkDataSourceZMQTest.MockNetworkDataSourceZMQ(sourceArgs_);
    }

    @Test
    public void ctor_negative() {
        try {
            new NetworkDataSourceZMQTest.MockNetworkDataSourceZMQ(null);
            fail("null sourceArgs");
        } catch(IllegalArgumentException e) { /*PASS*/ }
        Map<String, String> sourceArgs = new HashMap<>();
        try {
            new NetworkDataSourceZMQTest.MockNetworkDataSourceZMQ(sourceArgs);
            fail("Missing uri");
        } catch(IllegalArgumentException e) { /*PASS*/ }
        sourceArgs.put("uri",null);
        try {
            new NetworkDataSourceZMQTest.MockNetworkDataSourceZMQ(sourceArgs);
            fail("null uri");
        } catch(IllegalArgumentException e) { /*PASS*/ }
        sourceArgs.put("uri"," \n \t ");
        try {
            new NetworkDataSourceZMQTest.MockNetworkDataSourceZMQ(sourceArgs);
            fail("empty uri");
        } catch(IllegalArgumentException e) { /*PASS*/ }
    }

    @Test
    public void connect() throws IOException {
        instance_.connect(null);
        instance_.connect("");
        instance_.close();
        instance_.connect(" \n \t ");
        instance_.close();
        instance_.connect("tcp://127.0.0.1:5401");
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
        assertEquals("zmq", instance_.getProviderName());
    }

    @Test
    public void sendMessage() throws IOException {
        assertFalse(instance_.sendMessage("key", "message"));
        instance_.connect(null);
        assertTrue(instance_.sendMessage("key", "message"));
        if(instance_ != null)
            instance_.close();
    }

    @Test
    public void sendMessageNegative() throws IOException {
        processFail = true;
        instance_.connect(null);
        instance_.sendMessage("key", "message");
        if(instance_ != null)
            instance_.close();
    }

    @Test
    public void createNewFactory() throws Exception {
        Map<String, String> sourceArgs = new HashMap<>();
        sourceArgs.put("uri", "tcp://127.0.0.1:5401");
        sourceArgs.put("subjects", "evt,env,avg");
        NetworkDataSourceZMQ instance =
                (NetworkDataSourceZMQ) NetworkDataSourceFactory.createInstance(mock(Logger.class), "zmq",
                        sourceArgs);
        assertNotNull(instance);
    }
}
