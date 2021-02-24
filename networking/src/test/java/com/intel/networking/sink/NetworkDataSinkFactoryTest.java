// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.networking.sink;

import com.intel.logging.Logger;
import com.intel.networking.sink.for_benchmarking.NetworkDataSinkBenchmark;
import com.intel.networking.sink.kafka.NetworkDataSinkKafka;
import com.intel.networking.sink.rabbitmq.NetworkDataSinkRabbitMQ;
import com.intel.networking.sink.sse.NetworkDataSinkEventSource;
import com.intel.networking.sink.zmq.NetworkDataSinkZMQ;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class NetworkDataSinkFactoryTest {
    static class TestImpl implements NetworkDataSink {
        public TestImpl(Logger logger, Map<String,String> args) {}
        @Override public void initialize() {}
        @Override public void clearSubjects() {}
        @Override public void setMonitoringSubject(String subject) {}
        @Override public void setMonitoringSubjects(Collection<String> subjects) {}
        @Override public void setConnectionInfo(String info) {}
        @Override public void setCallbackDelegate(NetworkDataSinkDelegate delegate) {}
        @Override public void startListening() {}
        @Override public void stopListening() {}
        @Override public boolean isListening() { return false; }
        @Override public void setLogger(Logger logger) {}
        @Override public String getProviderName() { return null; }
    }
    static class BadTestImpl implements NetworkDataSink {
        public BadTestImpl() {}

        @Override public void initialize() {}
        @Override public void clearSubjects() {}
        @Override public void setMonitoringSubject(String subject) {}
        @Override public void setMonitoringSubjects(Collection<String> subjects) {}
        @Override public void setConnectionInfo(String info) {}
        @Override public void setCallbackDelegate(NetworkDataSinkDelegate delegate) {}
        @Override public void startListening() {}
        @Override public void stopListening() {}
        @Override public boolean isListening() { return false; }
        @Override public void setLogger(Logger logger) {}
        @Override public String getProviderName() { return null; }
    }

    private static void resetFactory() {
        NetworkDataSinkFactory.registeredImplementations_.clear();
        NetworkDataSinkFactory.registerNewImplementation("benchmark", NetworkDataSinkBenchmark.class);
        NetworkDataSinkFactory.registerNewImplementation("zmq", NetworkDataSinkZMQ.class);
        NetworkDataSinkFactory.registerNewImplementation("rabbitmq", NetworkDataSinkRabbitMQ.class);
        NetworkDataSinkFactory.registerNewImplementation("eventSource", NetworkDataSinkEventSource.class);
        NetworkDataSinkFactory.registerNewImplementation("kafka", NetworkDataSinkKafka.class);
    }

    @Before
    public void setUp() {
        resetFactory();
    }

    @AfterClass
    public static void tearDownClass() {
        resetFactory();
    }

    @Test
    public void factory() throws Exception {
        NetworkDataSink sink = NetworkDataSinkFactory.createInstance(mock(Logger.class), "none", null);
        assertNull(sink);
        Map<String,String> args = new HashMap<>();
        args.put("exchangeName", "exchange");
        args.put("subjects", "evt");
        sink = NetworkDataSinkFactory.createInstance(mock(Logger.class), "rabbitmq", args);
        assertNotNull(sink);
    }
    @Test
    public void ctor() {
        new NetworkDataSinkFactory();
    }

    @Test
    public void factoryZMQ() throws Exception {
        Map<String,String> args = new HashMap<>();
        args.put("uri", "tcp://127.0.0.1:5401");
        args.put("subjects","env,evt,avg");
        NetworkDataSink sink = NetworkDataSinkFactory.createInstance(mock(Logger.class), "zmq", args);
        assertNotNull(sink);
    }

    @Test
    public void registerAndUnregister() {
        assertFalse(NetworkDataSinkFactory.registerNewImplementation(null, TestImpl.class));
        assertFalse(NetworkDataSinkFactory.registerNewImplementation("test", null));
        assertFalse(NetworkDataSinkFactory.registerNewImplementation("zmq", TestImpl.class));
        assertFalse(NetworkDataSinkFactory.registerNewImplementation("rabbitmq", TestImpl.class));
        assertTrue(NetworkDataSinkFactory.registerNewImplementation("test", TestImpl.class));
        assertFalse(NetworkDataSinkFactory.registerNewImplementation("test", TestImpl.class));
        assertTrue(NetworkDataSinkFactory.unregisterImplementation("test"));
        assertFalse(NetworkDataSinkFactory.unregisterImplementation("test"));
    }

    @Test
    public void registerAndCreate() throws Exception {
        assertTrue(NetworkDataSinkFactory.registerNewImplementation("test", TestImpl.class));
        assertNotNull(NetworkDataSinkFactory.createInstance(mock(Logger.class), "test", new HashMap<>()));
    }

    @Test(expected = NetworkDataSinkFactory.FactoryException.class)
    public void registerAndCreateBadImpl() throws Exception {
        assertNull(NetworkDataSinkFactory.createInstance(mock(Logger.class), "test", new HashMap<>()));
        assertTrue(NetworkDataSinkFactory.registerNewImplementation("test", BadTestImpl.class));
        NetworkDataSinkFactory.createInstance(mock(Logger.class), "test", new HashMap<>());
    }
}
