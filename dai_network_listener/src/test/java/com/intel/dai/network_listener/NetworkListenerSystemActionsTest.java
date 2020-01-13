// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.*;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.networking.source.NetworkDataSource;
import com.intel.networking.source.NetworkDataSourceFactory;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


public class NetworkListenerSystemActionsTest {
    public static class MockNetworkDataSource implements NetworkDataSource {
        public MockNetworkDataSource(Map<String, String> args) {}

        @Override public void initialize() { }
        @Override public void connect(String info) { }
        @Override public void setLogger(Logger logger) { }
        @Override public String getProviderName() { return null; }
        @Override public boolean sendMessage(String subject, String message) { return false; }
        @Override public void close() throws IOException { }
    }
    static class BadMockNetworkDataSource implements NetworkDataSource {
        BadMockNetworkDataSource(Map<String, String> args) {}

        @Override public void initialize() { }
        @Override public void connect(String info) { }
        @Override public void setLogger(Logger logger) { }
        @Override public String getProviderName() { return null; }
        @Override public boolean sendMessage(String subject, String message) { return false; }
        @Override public void close() throws IOException { }
    }

    @BeforeClass
    public static void setUpClass() {
        NetworkDataSourceFactory.registerNewImplementation("mock_action_source", MockNetworkDataSource.class);
        NetworkDataSourceFactory.registerNewImplementation("bad_mock_action_source", BadMockNetworkDataSource.class);
    }

    @Before
    public void setUp() throws Exception {
        factory_ = mock(DataStoreFactory.class);
        storeTelemetry_ = mock(StoreTelemetry.class);
        bootImage_ = mock(BootImage.class);
        when(factory_.createStoreTelemetry()).thenReturn(storeTelemetry_);
        when(factory_.createRasEventLog(any(AdapterInformation.class))).thenReturn(mock(RasEventLog.class));
        when(factory_.createAdapterOperations(any(AdapterInformation.class))).
                thenReturn(mock(AdapterOperations.class));
        when(factory_.createBootImageApi(any(AdapterInformation.class))).thenReturn(bootImage_);
        adapter_ = new AdapterInformation("TEST_TYPE", "TEST_NAME", "LOCATION", "HOSTNAME", 333L);
        config_ = mock(NetworkListenerConfig.class);
        actionConfig_ = new PropertyMap();
        actionConfig_.put("sourceType", "mock_action_source");
        actionConfig_.put("uri", "amqp://127.0.0.1");
        actionConfig_.put("exchange", null);
        when(config_.getProviderConfigurationFromClassName(anyString())).thenReturn(actionConfig_);
        actions_ = new NetworkListenerSystemActions(mock(Logger.class), factory_, adapter_, config_);
    }

    @Test
    public void storeNormalizedData() {
        actions_.storeNormalizedData("Power", "here", 9999999L, 145.2);
    }

    @Test
    public void storeAggregatedData() {
        actions_.storeAggregatedData("Power", "here", 9999998L, 33.5, 155.6, 117.9);
    }

    @Test
    public void storeAggregatedDataNegative() throws Exception {
        when(storeTelemetry_.logEnvDataAggregated(anyString(), anyString(), anyLong(), anyDouble(), anyDouble(),
                anyDouble(), anyString(), anyLong())).thenThrow(DataStoreException.class);
        actions_.storeAggregatedData("Power", "here", 9999998L, 33.5, 155.6, 117.9);
    }

    @Test
    public void publishNormalizedData() throws Exception {
        actions_.publishNormalizedData("ucs_raw_data", "Power", "here", 9999999L, 145.2);
        actions_.publishNormalizedData("ucs_raw_data", "Power", "here", 9999999L, 145.2);
        actions_.close();
    }

    @Test
    public void publishNormalizedDataNegative() throws Exception {
        actionConfig_.put("sourceType", "bad_mock_action_source");
        actions_ = new NetworkListenerSystemActions(mock(Logger.class), factory_, adapter_, config_);
        actions_.publishNormalizedData("ucs_raw_data", "Power", "here", 9999999L, 145.2);
        actions_.publishNormalizedData("ucs_raw_data", "Power", "here", 9999999L, 145.2);
    }

    @Test
    public void publishAggregatedData() {
        actions_.publishAggregatedData("ucs_aggregate_data", "Power", "here", 9999998L, 33.5, 155.6, 117.9);
        actions_.publishAggregatedData("ucs_aggregate_data", "Power", "here", 9999998L, 33.5, 155.6, 117.9);
    }

    @Test
    public void publishAggregatedDataNegative() throws Exception {
        actionConfig_.put("sourceType", "bad_mock_action_source");
        actions_ = new NetworkListenerSystemActions(mock(Logger.class), factory_, adapter_, config_);
        actions_.publishAggregatedData("ucs_aggregate_data", "Power", "here", 9999998L, 33.5, 155.6, 117.9);
        actions_.publishAggregatedData("ucs_aggregate_data", "Power", "here", 9999998L, 33.5, 155.6, 117.9);
    }

    @Test
    public void setUpPublisher() throws Exception {
        actionConfig_.remove("sourceType");
        actions_ = new NetworkListenerSystemActions(mock(Logger.class), factory_, adapter_, config_);
        actions_.publishAggregatedData("ucs_aggregate_data", "Power", "here", 9999998L, 33.5, 155.6, 117.9);
        actionConfig_.put("sourceType", "  ");
        actions_ = new NetworkListenerSystemActions(mock(Logger.class), factory_, adapter_, config_);
        actions_.publishAggregatedData("ucs_aggregate_data", "Power", "here", 9999998L, 33.5, 155.6, 117.9);
    }

    @Test
    public void StoreRasEvent() throws Exception {
        actions_.storeRasEvent("eventName", "data", "location", 100000000L);
    }

    @Test
    public void publishRasEvent() throws Exception {
        actions_.publishRasEvent("ucs_ras_event", "eventName", "data", "location", 100000000L);
    }

    @Test
    public void publishBootEvent() throws Exception {
        actions_.publishBootEvent("ucs_ras_event", BootState.NODE_ONLINE, "location", 100000000L);
    }

    @Test
    public void changeNodeStateTo() throws Exception {
        actions_.changeNodeStateTo(BootState.NODE_ONLINE, "location", 12343454564L, false);
    }

    @Test
    public void changeNodeBootImageId() throws Exception {
        actions_.changeNodeBootImageId("location", "ID");
        try {
            doThrow(DataStoreException.class).when(bootImage_).updateComputeNodeBootImageId(anyString(), anyString(),
                    anyString());
        } catch(DataStoreException e) { /* Cannot happen in this context! */ }
        actions_.changeNodeBootImageId("location", "ID");
    }

    @Test
    public void close() throws Exception {
        actions_.close();
    }

    @Test
    public void upsertBootImages() {
        actions_.upsertBootImages(new ArrayList<>() {{ add(new HashMap<>()); }});
        try {
            doThrow(DataStoreException.class).when(bootImage_).editBootImageProfile(any());
        } catch(DataStoreException e) { /* Cannot happen in this context! */ }
        actions_.upsertBootImages(new ArrayList<>() {{ add(new HashMap<>()); }});
    }

    private NetworkListenerSystemActions actions_;
    private DataStoreFactory factory_;
    private AdapterInformation adapter_;
    private NetworkListenerConfig config_;
    private PropertyMap actionConfig_;
    private StoreTelemetry storeTelemetry_;
    private BootImage bootImage_;
}