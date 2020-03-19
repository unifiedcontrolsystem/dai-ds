// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.AdapterOperations;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.logging.Logger;
import com.intel.networking.sink.NetworkDataSink;
import com.intel.networking.sink.NetworkDataSinkDelegate;
import com.intel.networking.sink.NetworkDataSinkFactory;
import com.intel.perflogging.BenchmarkHelper;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class NetworkListenerCoreTest {
    static class MultipartMessage {
        MultipartMessage(String subject, String message) {
            this.subject = subject;
            this.message = message;
        }
        final String subject;
        final String message;
    }
    static ConcurrentLinkedQueue<MultipartMessage> messages_ = new ConcurrentLinkedQueue<>();
    static AdapterInformation forStopNetwork_ = null;

    @BeforeClass
    public static void setUpClass() {
        NetworkDataSinkFactory.registerNewImplementation("test", TestSink.class);
        System.setProperty("daiLoggingLevel", "DEBUG");
    }

    @AfterClass
    public static void tearDownClass() {
        NetworkDataSinkFactory.unregisterImplementation("test");
    }

    @Before
    public void setUp() throws Exception {
        assert parser_ != null;
        System.setProperty(NetworkListenerConfig.class.getCanonicalName() + ".DEBUG", "true");
        info_ = new AdapterInformation("TEST_TYPE", "TEST_NAME", "LOCATION", "HOSTNAME", 0L);
        forStopNetwork_ = info_;
        messages_.clear();
        Logger log = mock(Logger.class);
        NetworkListenerConfig config_ = new NetworkListenerConfig(info_, log);
        try (InputStream stream = new ByteArrayInputStream(json_.getBytes())) {
            config_.loadFromStream(stream);
        }
        adapter_ = new NetworkListenerCore(log, config_, mock(DataStoreFactory.class), mock(BenchmarkHelper.class));
        workQueue_ = mock(WorkQueue.class);
        when(workQueue_.grabNextAvailWorkItem()).thenReturn(true);
        operations_ = mock(AdapterOperations.class);
        when(workQueue_.workToBeDone()).thenReturn("HandleInputFromExternalComponent");
        when(workQueue_.getClientParameters()).thenReturn(new HashMap<>() {{
            put("Profile", "default");
        }});
        when(adapter_.factory_.createWorkQueue(any(AdapterInformation.class))).thenReturn(workQueue_);
        when(adapter_.factory_.createAdapterOperations(any(AdapterInformation.class))).
                thenReturn(operations_);
    }

    @Test
    public void run1() {
        info_.signalToShutdown();
        assertEquals(0, adapter_.run());
    }

    @Test
    public void run2() throws Exception {
        doThrow(IOException.class).when(operations_).registerAdapter();
        when(workQueue_.isThisNewWorkItem()).thenReturn(true);
        info_.signalToShutdown();
        assertEquals(1, adapter_.run());
    }

    @Test
    public void run3() {
        messages_.add(new MultipartMessage("telemetry", "{\"message\":\"some message\"}"));
        messages_.add(new MultipartMessage("telemetry", "{\"message\":\"some message\"}"));
        messages_.add(new MultipartMessage("telemetry", "{\"message\":\"some message\"}"));
        new Thread(()-> {
            while(!messages_.isEmpty()) safeSleep(25);
            safeSleep(250);
            while(!adapter_.queue_.isEmpty()) safeSleep(25);
            safeSleep(250);
            adapter_.shutDown();
        }).start();
        assertEquals(0, adapter_.run());
    }

    @Test(expected = NetworkListenerCore.Exception.class)
    public void exceptionCreation() throws Exception {
        throw new NetworkListenerCore.Exception("message");
    }

    @Test
    public void parseTokenConfig() {
        Map<String,String> result = new HashMap<>();
        PropertyMap properties = new PropertyMap();
        properties.put("key", "value");
        adapter_.parseTokenConfig(properties, result);
    }

    @Test
    public void parseSelector() {
        Map<String,String> result = new HashMap<>();
        PropertyMap properties = new PropertyMap();
        PropertyArray array = new PropertyArray();
        array.add("value");
        properties.put("key", "value");
        properties.put("map", new PropertyMap());
        properties.put("array", array);
        adapter_.parseSelector(properties, result);
    }

    private void safeSleep(long ms) {
        try { Thread.sleep(ms); } catch(InterruptedException e) { /* Ignore */ }
    }

    private NetworkListenerCore adapter_;
    private AdapterInformation info_;
    private WorkQueue workQueue_;
    private AdapterOperations operations_;
    private static final ConfigIO parser_ = ConfigIOFactory.getInstance("json");

    private static String json_ = "{\n" +
            "  \"providerClassMap\": {\n" +
            "    \"foreignStateData\": \"com.intel.dai.network_listener.TestTransformer\",\n" +
            "    \"foreignGetBuilder\": \"com.intel.dai.foreign.builders.ApiGetBuilder\"\n" +
            "  },\n" +
            "  \"networkStreams\": {\n" +
            "    \"stateChanges\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"urlPath\": \"/streams/stateChanges\"\n" +
            "      },\n" +
            "      \"name\": \"test\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"adapterProfiles\": {\n" +
            "    \"default\": {\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"stateChanges\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"stateChanges\"\n" +
            "      ],\n" +
            "      \"adapterProvider\": \"foreignStateData\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"subjectMap\": {\n" +
            "    \"telemetry\": \"EnvironmentalData\",\n" +
            "    \"inventoryChanges\": \"InventoryChangeEvent\",\n" +
            "    \"logs\": \"LogData\",\n" +
            "    \"events\": \"RasEvent\",\n" +
            "    \"stateChanges\": \"StateChangeEvent\"\n" +
            "  }\n" +
            "}";
    public static class TestSink implements NetworkDataSink {
        public TestSink(Logger logger, Map<String,String> args) {
            args_ = args;
        }

        @Override public void initialize() {
            if(args_.containsKey("subjects"))
                setMonitoringSubjects(Arrays.asList(args_.get("subjects").split(",")));
        }

        @Override
        public void clearSubjects() {
            subjects_.clear();
        }

        @Override
        public void setMonitoringSubject(String subject) { subjects_.add(subject); }

        @Override
        public void setMonitoringSubjects(Collection<String> subjects) {
            for(String subject: subjects)
                setMonitoringSubject(subject);
        }

        @Override
        public void setConnectionInfo(String info) { }

        @Override
        public void setCallbackDelegate(NetworkDataSinkDelegate delegate) { callback_ = delegate; }

        @Override
        public void startListening() {
            new Thread(() -> {
                while(!forStopNetwork_.isShuttingDown()) {
                    while(!messages_.isEmpty()) {
                        MultipartMessage msg = messages_.poll();
                        callback_.processIncomingData(msg.subject, msg.message);
                    }
                }
            }).start();
        }

        @Override
        public void stopListening() { }

        @Override
        public boolean isListening() { return true; }

        @Override
        public void setLogger(Logger logger) { }

        @Override
        public String getProviderName() { return "test"; }

        private NetworkDataSinkDelegate callback_;
        private List<String> subjects_ = new ArrayList<>();
        private Map<String,String> args_;
    }
}
