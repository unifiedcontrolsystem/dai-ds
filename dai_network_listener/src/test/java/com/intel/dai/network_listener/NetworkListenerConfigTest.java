// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class NetworkListenerConfigTest {
    @Before
    public void setUp() throws Exception {
        assertNotNull(parser_);
        info_ = new AdapterInformation("TEST_TYPE", "TEST_NAME", arguments_[1],arguments_[2]);
        config_ = new NetworkListenerConfig(info_, mock(Logger.class));
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
        new NetworkListenerConfig(null, mock(Logger.class));
    }

    @Test(expected = AssertionError.class)
    public void ctorNegative2() {
        new NetworkListenerConfig(info_, null);
    }

    @Test(expected = ConfigIOParseException.class)
    public void loadFromStreamMissingRequiredSection() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.remove("adapterProfiles");
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
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
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile1() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("adapterProfiles").getMap("default").put("unknown", "something");
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile2() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("adapterProfiles").getMap("default").put("networkStreamsRef", "something");
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile3() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("adapterProfiles").getMap("default").put("subjects", "something");
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile4() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("adapterProfiles").getMap("default").put("networkStreamsRef", new PropertyArray());
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
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
        map.getMap("adapterProfiles").getMap("default").put("networkStreamsRef", array);
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingProfile6() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        PropertyArray array = new PropertyArray();
        map.getMap("adapterProfiles").getMap("default").put("subjects", array);
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
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
        map.getMap("adapterProfiles").getMap("default").put("subjects", array);
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
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
        map.getMap("adapterProfiles").getMap("default").put("subjects", array);
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test
    public void validatingProfile9() throws Exception {
        System.setProperty(NetworkListenerConfig.class.getCanonicalName() + ".DEBUG", "true");
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json_.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
        System.setProperty(NetworkListenerConfig.class.getCanonicalName() + ".DEBUG", "false");
    }

    @Test(expected = ConfigIOParseException.class)
    public void validatingNetworkStream1() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        PropertyArray array = new PropertyArray();
        array.add(20);
        map.getMap("networkStreams").getMap("nodeTelemetry1").put("arguments", "string");
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
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
        map.getMap("networkStreams").getMap("nodeTelemetry1").put("unknown", "string");
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test
    public void validatingNetworkStream3() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("networkStreams").getMap("nodeTelemetry1").getMap("arguments").remove("requestType");
        map.getMap("networkStreams").getMap("nodeTelemetry1").getMap("arguments").put("requestType", null);
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test
    public void validatingNetworkStream4() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("networkStreams").getMap("nodeTelemetry1").getMap("arguments").remove("requestType");
        map.getMap("networkStreams").getMap("nodeTelemetry1").getMap("arguments").put("requestType", "METHOD");
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test
    public void validatingNetworkStream5() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("networkStreams").getMap("nodeTelemetry1").getMap("arguments").remove("requestType");
        map.getMap("networkStreams").getMap("nodeTelemetry1").getMap("arguments").put("requestType", "POST");
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test
    public void validatingNetworkStream6() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("networkStreams").getMap("nodeTelemetry1").getMap("arguments").remove("requestBuilder");
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test//(expected = ConfigIOParseException.class)
    public void validatingNetworkStream7() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("networkStreams").getMap("nodeTelemetry1").getMap("arguments").replace("requestType", "METHOD");
        map.getMap("networkStreams").getMap("nodeTelemetry1").getMap("arguments").replace("requestBuilder", null);
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
        try (InputStream stream = new ByteArrayInputStream(json.getBytes())) {
            config.loadFromStream(stream);
        }
        config.setCurrentProfile(arguments_[3]);
    }

    @Test//(expected = ConfigIOParseException.class)
    public void validatingNetworkStream8() throws Exception {
        PropertyMap map = parser_.fromString(json_).getAsMap();
        map.getMap("networkStreams").getMap("nodeTelemetry1").getMap("arguments").replace("requestType", "METHOD");
        map.getMap("networkStreams").getMap("nodeTelemetry1").getMap("arguments").replace("requestBuilder", "unknown");
        String json = parser_.toString(map);
        NetworkListenerConfig config = new NetworkListenerConfig(info_, mock(Logger.class));
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
        assertEquals("default", config_.getCurrentProfile());
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
        assertEquals("com.intel.dai.network_listener.Transformer", config_.getProviderName());
    }

    @Test
    public void getProfileStreams() {
        Collection<String> streams = config_.getProfileStreams();
        assertEquals(5, streams.size());
    }

    @Test
    public void getNetworkName() {
        assertEquals("sse", config_.getNetworkName("nodeTelemetry1"));
    }

    @Test
    public void getNetworkArguments() {
        PropertyMap args = config_.getNetworkArguments("nodeTelemetry1");
        assertEquals("foreignPostBuilder", args.getStringOrDefault("requestBuilder", null));
    }

    @Test
    public void getProviderClassNameFromName() {
        assertEquals("com.intel.dai.foreign.builders.ApiPostBuilder",
                config_.getProviderClassNameFromName("foreignPostBuilder"));
    }

    @Test
    public void getProviderConfigurationFromClassName() {
        PropertyMap map = config_.getProviderConfigurationFromClassName("com.intel.dai.network_listener.Transformer");
        assertNotNull(map);
        assertEquals(0, map.size());
    }

    @Test
    public void getFirstNetworkBaseUrl() throws Exception {
        config_.setCurrentProfile("default");
        assertEquals("http://127.0.0.1:5678", config_.getFirstNetworkBaseUrl(false));
        assertEquals("https://127.0.0.1:5678", config_.getFirstNetworkBaseUrl(true));
    }

    private NetworkListenerConfig config_;
    private AdapterInformation info_;
    private static ConfigIO parser_ = ConfigIOFactory.getInstance("json");
    private static String[] arguments_ = new String[] {"127.0.0.2", "location", "hostname", "default"};
    private static String json_ = "{\n" +
            "  \"providerClassMap\": {\n" +
            "    \"foreignPostBuilder\": \"com.intel.dai.foreign.builders.ApiPostBuilder\",\n" +
            "    \"foreignTelemetryData\": \"com.intel.dai.network_listener.Transformer\",\n" +
            "    \"foreignGetBuilder\": \"com.intel.dai.foreign.builders.ApiGetBuilder\"\n" +
            "  },\n" +
            "  \"providerConfigurations\": {\n" +
            "    \"com.intel.dai.network_listener.Transformer\": {}\n" +
            "  },\n" +
            "  \"networkStreams\": {\n" +
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
            "    \"nodeTelemetry1\": {\n" +
            "      \"arguments\": {\n" +
            "        \"connectPort\": 5678,\n" +
            "        \"connectAddress\": \"127.0.0.1\",\n" +
            "        \"requestBuilder\": \"foreignPostBuilder\",\n" +
            "        \"urlPath\": \"/streams/nodeTelemetry/1\"\n" +
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
            "    }\n" +
            "  },\n" +
            "  \"adapterProfiles\": {\n" +
            "    \"default\": {\n" +
            "      \"networkStreamsRef\": [\n" +
            "        \"nodeTelemetry0\",\n" +
            "        \"nodeTelemetry1\"\n" +
            "        \"nodeTelemetry2\",\n" +
            "        \"nodeTelemetry3\",\n" +
            "        \"rackTelemetry\"\n" +
            "      ],\n" +
            "      \"subjects\": [\n" +
            "        \"telemetry\"\n" +
            "      ],\n" +
            "      \"adapterProvider\": \"foreignTelemetryData\"\n" +
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