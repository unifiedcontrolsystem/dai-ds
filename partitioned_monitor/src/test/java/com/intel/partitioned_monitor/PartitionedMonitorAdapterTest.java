// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.partitioned_monitor;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.AdapterOperations;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.WorkQueue;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.restclient.SSERequestBuilder;
import com.intel.networking.sink.NetworkDataSink;
import com.intel.networking.sink.NetworkDataSinkDelegate;
import com.intel.networking.sink.NetworkDataSinkFactory;
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
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class Action implements DataAction, Initializer {
    public Action(Logger logger) {}

    @Override
    public void actOnData(CommonDataFormat data, PartitionedMonitorConfig config, SystemActions system) {
    }

    @Override public void initialize() { }
}

class Tranformer implements DataTransformer, Initializer {
    public Tranformer(Logger logger) {}

    @Override
    public List<CommonDataFormat> processRawStringData(String data, PartitionedMonitorConfig config)
            throws DataTransformerException {
        return new ArrayList<>() {{
            add(new CommonDataFormat(100000L, "location", DataType.EnvironmentalData));
        }};
    }

    @Override public void initialize() { }
}

public class PartitionedMonitorAdapterTest {
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
        System.setProperty(PartitionedMonitorConfig.class.getCanonicalName() + ".DEBUG", "true");
        info_ = new AdapterInformation("TEST_TYPE", "TEST_NAME", "LOCATION", "HOSTNAME", 0L);
        forStopNetwork_ = info_;
        messages_.clear();
        Logger log = mock(Logger.class);
        PartitionedMonitorConfig config_ = new PartitionedMonitorConfig(info_, log);
        try (InputStream stream = new ByteArrayInputStream(json_.getBytes())) {
            config_.loadFromStream(stream);
        }
        adapter_ = new PartitionedMonitorAdapter(log, config_);
        adapter_.factory_ = mock(DataStoreFactory.class);
        workQueue_ = mock(WorkQueue.class);
        when(workQueue_.grabNextAvailWorkItem()).thenReturn(true);
        operations_ = mock(AdapterOperations.class);
        when(workQueue_.workToBeDone()).thenReturn("HandleInputFromExternalComponent");
        when(workQueue_.getClientParameters()).thenReturn(new HashMap<>() {{
            put("Profile", "allEvents");
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

    @Test(expected = PartitionedMonitorAdapter.Exception.class)
    public void exceptionCreation() throws Exception {
        throw new PartitionedMonitorAdapter.Exception("message");
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

    private PartitionedMonitorAdapter adapter_;
    private AdapterInformation info_;
    private WorkQueue workQueue_;
    private AdapterOperations operations_;
    private static final ConfigIO parser_ = ConfigIOFactory.getInstance("json");

    private static String json_ = "{\n" +
            "  \"providerClassMap\": {\n" +
            "    \"foreignInventoryAction\": \"com.intel.dai.foreign.actions.ApiInventoryAction\",\n" +
            "    \"foreignPostBuilder\": \"com.intel.partitioned_monitor.Builder\",\n" +
            "    \"foreignTelemetryAction\": \"com.intel.partitioned_monitor.Action\",\n" +
            "    \"foreignEventsAction\": \"com.intel.partitioned_monitor.TestAction\",\n" +
            "    \"foreignStateAction\": \"com.intel.dai.foreign.actions.ApiStateAction\",\n" +
            "    \"foreignInventoryData\": \"com.intel.dai.foreign.transforms.ApiInventoryTransformer\",\n" +
            "    \"foreignStateData\": \"com.intel.dai.foreign.transforms.ApiStateTransformer\",\n" +
            "    \"foreignTelemetryData\": \"com.intel.partitioned_monitor.Tranformer\",\n" +
            "    \"foreignEventsData\": \"com.intel.partitioned_monitor.TestTransformer\",\n" +
            "    \"foreignGetBuilder\": \"com.intel.dai.foreign.builders.ApiGetBuilder\",\n" +
            "    \"foreignLogsData\": \"com.intel.dai.foreign.builders.LogsTransformer\",\n" +
            "    \"foreignLogsAction\": \"com.intel.dai.foreign.builders.LogsAction\"\n" +
            "  },\n" +
            "  \"providerConfigurations\": {\n" +
            "    \"com.intel.authentication.KeycloakTokenAuthentication\":{\n" +
            "      \"tokenServer\": \"https://localhost:8080/auth\",\n" +
            "      \"realm\": \"myKingdom\",\n" +
            "      \"clientId\": \"myKingdom\",\n" +
            "      \"clientSecret\": \"myKingdom\"\n" +
            "    }\n" +
            "  },\n" +
            "  \"networkStreams\": {\n" +
            "    \"nodeSyslog1\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"requestType\": \"POST\",\n" +
            "        \"requestBuilderSelectors\": {\n" +
            "          \"partitions\": [\n" +
            "            2,\n" +
            "            3\n" +
            "          ]\n" +
            "        },\n" +
            "        \"tokenAuthProvider\": \"com.intel.authentication.KeycloakTokenAuthentication\",\n" +
            "        \"urlPath\": \"/streams/nodeSyslog\"\n" +
            "      },\n" +
            "      \"name\": \"test\"\n" +
            "    },\n" +
            "    \"rackTelemetry\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"urlPath\": \"/streams/rackTelemetry\"\n" +
            "      },\n" +
            "      \"name\": \"test\"\n" +
            "    },\n" +
            "    \"rackRasEvents\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"urlPath\": \"/streams/stateChanges\"\n" +
            "      },\n" +
            "      \"name\": \"test\"\n" +
            "    },\n" +
            "    \"nodeTelemetry3\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/3\"\n" +
            "      },\n" +
            "      \"name\": \"test\"\n" +
            "    },\n" +
            "    \"nodeTelemetry2\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/2\"\n" +
            "      },\n" +
            "      \"name\": \"test\"\n" +
            "    },\n" +
            "    \"nodeSyslog0\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"requestType\": \"POST\",\n" +
            "        \"requestBuilderSelectors\": {\n" +
            "          \"partitions\": [\n" +
            "            0,\n" +
            "            1\n" +
            "          ]\n" +
            "        },\n" +
            "        \"urlPath\": \"/streams/nodeSyslog\"\n" +
            "      },\n" +
            "      \"name\": \"test\"\n" +
            "    },\n" +
            "    \"nodeSyslog3\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"requestType\": \"POST\",\n" +
            "        \"requestBuilderSelectors\": {\n" +
            "          \"partitions\": [\n" +
            "            6,\n" +
            "            7\n" +
            "          ]\n" +
            "        },\n" +
            "        \"urlPath\": \"/streams/nodeSyslog\"\n" +
            "      },\n" +
            "      \"name\": \"test\"\n" +
            "    },\n" +
            "    \"nodeTelemetry1\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/1\"\n" +
            "      },\n" +
            "      \"name\": \"test2\"\n" +
            "    },\n" +
            "    \"nodeRasEvents\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"urlPath\": \"/streams/stateChanges\"\n" +
            "      },\n" +
            "      \"name\": \"test\"\n" +
            "    },\n" +
            "    \"nodeTelemetry0\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/0\",\n" +
            "        \"requestBuilderSelector\": {\n" +
            "          \"name\": [\"values\"]\n" +
            "        }\n" +
            "      },\n" +
            "      \"name\": \"test\"\n" +
            "    },\n" +
            "    \"inventoryChanges\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"requestBuilderSelectors\": {\n" +
            "          \"includeRack\": false\n" +
            "        },\n" +
            "        \"urlPath\": \"/streams/inventoryChanges\"\n" +
            "      }\n" +
            "    },\n" +
            "    \"nodeSyslog2\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"requestType\": \"POST\",\n" +
            "        \"requestBuilderSelectors\": {\n" +
            "          \"partitions\": [\n" +
            "            4,\n" +
            "            5\n" +
            "          ]\n" +
            "        },\n" +
            "        \"urlPath\": \"/streams/nodeSyslog\"\n" +
            "      },\n" +
            "      \"name\": \"test\"\n" +
            "    },\n" +
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
            "    \"logPart3\": {\n" +
            "      \"actionProvider\": \"foreignLogsAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeSyslog2\",\n" +
            "        \"nodeSyslog3\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"logs\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignLogsData\"\n" +
            "    },\n" +
            "    \"allInventoryChanges\": {\n" +
            "      \"actionProvider\": \"foreignInventoryAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"inventoryChanges\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"inventoryChanges\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignInventoryData\"\n" +
            "    },\n" +
            "    \"allEvents\": {\n" +
            "      \"actionProvider\": \"foreignEventsAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeRasEvents\",\n" +
            "        \"rackRasEvents\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"events\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignEventsData\"\n" +
            "    },\n" +
            "    \"logPart2\": {\n" +
            "      \"actionProvider\": \"foreignLogsAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeSyslog1\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"logs\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignLogsData\"\n" +
            "    },\n" +
            "    \"telemetryPart1\": {\n" +
            "      \"actionProvider\": \"foreignTelemetryAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeTelemetry0\",\n" +
            "        \"nodeTelemetry1\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"telemetry\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignTelemetryData\"\n" +
            "    },\n" +
            "    \"logPart1\": {\n" +
            "      \"actionProvider\": \"foreignLogsAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeSyslog0\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"logs\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignLogsData\"\n" +
            "    },\n" +
            "    \"telemetryPart2\": {\n" +
            "      \"actionProvider\": \"foreignTelemetryAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeTelemetry2\",\n" +
            "        \"nodeTelemetry3\",\n" +
            "        \"rackTelemetry\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"telemetry\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignTelemetryData\"\n" +
            "    },\n" +
            "    \"allStateChanges\": {\n" +
            "      \"actionProvider\": \"foreignStateAction\",\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"stateChanges\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"stateChanges\"\n" +
            "      ],\n" +
            "      \"dataTransformProvider\": \"foreignStateData\"\n" +
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

class TestTransformer implements DataTransformer, Initializer {
    public TestTransformer(Logger logger) {}

    @Override
    public List<CommonDataFormat> processRawStringData(String data, PartitionedMonitorConfig config) throws DataTransformerException {
        return new ArrayList<>() {{ add(new CommonDataFormat(99999L, "TestLocation", DataType.RasEvent)); }};
    }

    @Override public void initialize() { }
}

class TestAction implements DataAction, Initializer {
    public TestAction(Logger logger) {}

    @Override
    public void actOnData(CommonDataFormat data, PartitionedMonitorConfig config, SystemActions systemActions) {}

    @Override public void initialize() { }
}