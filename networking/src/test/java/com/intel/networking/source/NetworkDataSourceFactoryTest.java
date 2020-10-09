// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.networking.source;

import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.source.rabbitmq.NetworkDataSourceRabbitMQ;
import com.intel.networking.source.restsse.NetworkDataSourceSSE;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class NetworkDataSourceFactoryTest {
    public static class TestImpl implements NetworkDataSource {
        public TestImpl(Logger logger, Map<String,String> args) {}

        @Override public void initialize() {}
        @Override public void connect(String info) {}
        @Override public void setLogger(Logger logger) {}
        @Override public String getProviderName() { return null; }
        @Override public boolean sendMessage(String subject, String message) { return false; }
        @Override public void close() throws IOException { }
    }

    public static class BadTestImpl implements NetworkDataSource {
        public BadTestImpl() {}
        @Override public void initialize() {}
        @Override public void connect(String info) {}
        @Override public void setLogger(Logger logger) {}
        @Override public String getProviderName() { return null; }
        @Override public boolean sendMessage(String subject, String message) { return false; }
        @Override public void close() throws IOException { }
    }

    @Before
    public void setUp() {
        NetworkDataSourceFactory.registeredImplementations_ =
                new HashMap<String, Class<? extends NetworkDataSource>>() {{
            put("rabbitmq", NetworkDataSourceRabbitMQ.class);
            put("sse", NetworkDataSourceSSE.class);
        }};
    }

    @Test
    public void createInstance() throws Exception {
        Map<String, String> args = new HashMap<>();
        args.put("exchangeName", "test");
        assertNotNull(NetworkDataSourceFactory.createInstance(mock(Logger.class), "rabbitmq", args));
        assertNull(NetworkDataSourceFactory.createInstance(mock(Logger.class), "unknown", args));
    }

    @Test
    public void registerAndUnregister() {
        assertFalse(NetworkDataSourceFactory.registerNewImplementation(null, TestImpl.class));
        assertFalse(NetworkDataSourceFactory.registerNewImplementation("test", null));
        assertFalse(NetworkDataSourceFactory.registerNewImplementation("rabbitmq", TestImpl.class));
        assertTrue(NetworkDataSourceFactory.registerNewImplementation("test", TestImpl.class));
        assertFalse(NetworkDataSourceFactory.registerNewImplementation("test", TestImpl.class));
        assertTrue(NetworkDataSourceFactory.unregisterImplementation("test"));
        assertFalse(NetworkDataSourceFactory.unregisterImplementation("test"));
    }

    @Test
    public void registerAndCreate() throws Exception {
        assertTrue(NetworkDataSourceFactory.registerNewImplementation("test", TestImpl.class));
        assertNotNull(NetworkDataSourceFactory.createInstance(mock(Logger.class), "test", new HashMap<>()));
    }

    @Test(expected = NetworkDataSourceFactory.FactoryException.class)
    public void registerAndCreateBadImpl() throws Exception {
        assertNull(NetworkDataSourceFactory.createInstance(mock(Logger.class), "test", new HashMap<>()));
        assertTrue(NetworkDataSourceFactory.registerNewImplementation("test", BadTestImpl.class));
        NetworkDataSourceFactory.createInstance(mock(Logger.class), "test", new HashMap<>());
    }
}
