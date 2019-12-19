// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.partitioned_monitor;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import org.apache.logging.log4j.core.config.Property;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class PartitionedMonitorConfigTest {
    @Before
    public void setUp() throws Exception {
        assertNotNull(parser_);
        info_ = new AdapterInformation("TEST_TYPE", "TEST_NAME", arguments_[1],arguments_[2]);
        config_ = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json_.getBytes())) {
            config_.loadFromStream(stream);
        }
        config_.setCurrentProfile(arguments_[3]);
    }

    @Test
    public void useBenchmarkingTest() {
        assertFalse(config_.useBenchmarking());
    }

    @Test(expected = RuntimeException.class)
    public void ctorNegative1() {
        new PartitionedMonitorConfig(null, mock(Logger.class));
    }

    @Test(expected = AssertionError.class)
    public void ctorNegative2() {
        new PartitionedMonitorConfig(info_, null);
    }

    @Test(expected = ConfigIOParseException.class)
    public void loadFromStreamMissingRequiredSection() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.remove("adapterProfiles");
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void loadFromStreamMissingNetworkStreamRef() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("adapterProfiles").getMap(arguments_[3]).getArray("networkStreamsRef").add("NoSuchStream");
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile1() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("adapterProfiles").getMap("telemetryPart1").put("unknown", "something");
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile2() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("adapterProfiles").getMap("telemetryPart1").put("networkStreamsRef", "something");
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile3() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("adapterProfiles").getMap("telemetryPart1").put("subjects", "something");
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile4() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("adapterProfiles").getMap("telemetryPart1").put("networkStreamsRef", new PropertyArray());
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile5() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        PropertyArray array = new PropertyArray();
        array.add(20);
        map.getMap("adapterProfiles").getMap("telemetryPart1").put("networkStreamsRef", array);
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile6() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        PropertyArray array = new PropertyArray();
        map.getMap("adapterProfiles").getMap("telemetryPart1").put("subjects", array);
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile7() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        PropertyArray array = new PropertyArray();
        array.add(20);
        map.getMap("adapterProfiles").getMap("telemetryPart1").put("subjects", array);
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile8() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        PropertyArray array = new PropertyArray();
        array.add("unknown");
        map.getMap("adapterProfiles").getMap("telemetryPart1").put("subjects", array);
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test
    public void validatingProfile9() throws Exception {
        System.setProperty(PartitionedMonitorConfig.class.getCanonicalName() + ".DEBUG", "true");
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json_.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
        System.setProperty(PartitionedMonitorConfig.class.getCanonicalName() + ".DEBUG", "false");
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingNetworkStream1() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        PropertyArray array = new PropertyArray();
        array.add(20);
        map.getMap("networkStreams").getMap("nodeSyslog1").put("arguments", "string");
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingNetworkStream2() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        PropertyArray array = new PropertyArray();
        array.add(20);
        map.getMap("networkStreams").getMap("nodeSyslog1").put("unknown", "string");
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test
    public void validatingNetworkStream3() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("networkStreams").getMap("nodeRasEvents").getMap("arguments").remove("requestType");
        map.getMap("networkStreams").getMap("nodeRasEvents").getMap("arguments").put("requestType", null);
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test
    public void validatingNetworkStream4() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("networkStreams").getMap("nodeRasEvents").getMap("arguments").remove("requestType");
        map.getMap("networkStreams").getMap("nodeRasEvents").getMap("arguments").put("requestType", "METHOD");
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test
    public void validatingNetworkStream5() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("networkStreams").getMap("nodeRasEvents").getMap("arguments").remove("requestType");
        map.getMap("networkStreams").getMap("nodeRasEvents").getMap("arguments").put("requestType", "POST");
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test
    public void validatingNetworkStream6() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("networkStreams").getMap("nodeRasEvents").getMap("arguments").remove("requestBuilder");
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingNetworkStream7() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("networkStreams").getMap("nodeRasEvents").getMap("arguments").replace("requestType", "METHOD");
        map.getMap("networkStreams").getMap("nodeRasEvents").getMap("arguments").replace("requestBuilder", null);
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingNetworkStream8() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("networkStreams").getMap("nodeRasEvents").getMap("arguments").replace("requestType", "METHOD");
        map.getMap("networkStreams").getMap("nodeRasEvents").getMap("arguments").replace("requestBuilder", "unknown");
        String json = parser_.toString(map);
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test
    public void getAdapterInformation() {
        assertEquals(info_, config_.getAdapterInformation());
    }

    @Test
    public void getCurrentProfile() {
        assertEquals("telemetryPart1", config_.getCurrentProfile());
    }

    @Test
    public void getLoggerProvider() {
        assertEquals("console", config_.getLoggerProvider());
    }

    @Test
    public void getProfileSubjects() {
        List<String> subjects = config_.getProfileSubjects();
        assertEquals(1, subjects.size());
        assertEquals("telemetry", subjects.get(0));
    }

    @Test
    public void getProfileDataTransformerName() {
        assertEquals("com.intel.dai.foreign.transforms.ApiDataTransformer", config_.getProfileDataTransformerName());
    }

    @Test
    public void getProfileDataActionName() {
        assertEquals("com.intel.dai.foreign.actions.ApiDataAction", config_.getProfileDataActionName());
    }

    @Test
    public void getProfileStreams() {
        Collection<String> streams = config_.getProfileStreams();
        assertEquals(2, streams.size());
    }

    @Test
    public void getNetworkName() {
        assertEquals("sse", config_.getNetworkName("nodeSyslog1"));
    }

    @Test
    public void getNetworkArguments() {
        PropertyMap args = config_.getNetworkArguments("nodeSyslog1");
        assertEquals("foreignPostBuilder", args.getStringOrDefault("requestBuilder", null));
    }

    @Test
    public void getProviderClassNameFromName() {
        assertEquals("com.intel.dai.foreign.builders.ApiPostBuilder",
                config_.getProviderClassNameFromName("foreignPostBuilder"));
    }

    @Test
    public void getProviderConfigurationFromClassName() {
        PropertyMap map = config_.getProviderConfigurationFromClassName("com.intel.dai.foreign.actions.ApiDataAction");
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    @Test
    public void getFirstNetworkBaseUrl() throws Exception {
        config_.setCurrentProfile("allStateChanges");
        assertEquals("http://127.0.0.2:9999", config_.getFirstNetworkBaseUrl(false));
        assertEquals("https://127.0.0.2:9999", config_.getFirstNetworkBaseUrl(true));
    }

    private PartitionedMonitorConfig config_;
    private AdapterInformation info_;
    private static ConfigIO parser_ = ConfigIOFactory.getInstance("json");
    private static String[] arguments_ = new String[] {"127.0.0.2", "location", "hostname", "telemetryPart1"};
    private static String json_ = "{\n" +
            "  \"providerClassMap\": {\n" +
            "    \"foreignInventoryAction\": \"com.intel.dai.foreign.actions.ApiInventoryAction\",\n" +
            "    \"foreignPostBuilder\": \"com.intel.dai.foreign.builders.ApiPostBuilder\",\n" +
            "    \"foreignTelemetryAction\": \"com.intel.dai.foreign.actions.ApiDataAction\",\n" +
            "    \"foreignEventsAction\": \"com.intel.dai.foreign.actions.ApiEventAction\",\n" +
            "    \"foreignStateAction\": \"com.intel.dai.foreign.actions.ApiStateAction\",\n" +
            "    \"foreignInventoryData\": \"com.intel.dai.foreign.transforms.ApiInventoryTransformer\",\n" +
            "    \"foreignStateData\": \"com.intel.dai.foreign.transforms.ApiStateTransformer\",\n" +
            "    \"foreignTelemetryData\": \"com.intel.dai.foreign.transforms.ApiDataTransformer\",\n" +
            "    \"foreignEventsData\": \"com.intel.dai.foreign.transforms.ApiEventTransformer\",\n" +
            "    \"foreignGetBuilder\": \"com.intel.dai.foreign.builders.ApiGetBuilder\",\n" +
            "    \"foreignLogsData\": \"com.intel.dai.foreign.builders.LogsTransformer\",\n" +
            "    \"foreignLogsAction\": \"com.intel.dai.foreign.builders.LogsAction\"\n" +
            "  },\n" +
            "  \"providerConfigurations\": {\n" +
            "    \"com.intel.dai.foreign.actions.ApiDataAction\": {},\n" +
            "    \"com.intel.dai.foreign.transforms.ApiDataTransformer\": {}\n" +
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
            "        \"urlPath\": \"/streams/nodeSyslog\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"rackTelemetry\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"urlPath\": \"/streams/rackTelemetry\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"rackRasEvents\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"urlPath\": \"/streams/stateChanges\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"nodeTelemetry3\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/3\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"nodeTelemetry2\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/2\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
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
            "      \"name\": \"sse\"\n" +
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
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"nodeTelemetry1\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/1\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"nodeRasEvents\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"urlPath\": \"/streams/stateChanges\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"nodeTelemetry0\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/0\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
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
            "      \"name\": \"sse\"\n" +
            "    },\n" +
            "    \"stateChanges\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 9999,\n" +
            "        \"connectAddress\": \"127.0.0.2\",\n" +
            "        \"requestBuilder\": \"foreignGetBuilder\",\n" +
            "        \"requestType\": \"GET\",\n" +
            "        \"urlPath\": \"/streams/stateChanges\"\n" +
            "      },\n" +
            "      \"name\": \"sse\"\n" +
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
            "  \"logProvider\": \"console\",\n" +
            "  \"subjectMap\": {\n" +
            "    \"telemetry\": \"EnvironmentalData\",\n" +
            "    \"inventoryChanges\": \"InventoryChangeEvent\",\n" +
            "    \"logs\": \"LogData\",\n" +
            "    \"events\": \"RasEvent\",\n" +
            "    \"stateChanges\": \"StateChangeEvent\"\n" +
            "  }\n" +
            "}";
}