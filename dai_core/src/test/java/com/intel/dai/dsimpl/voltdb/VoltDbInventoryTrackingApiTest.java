// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public class VoltDbInventoryTrackingApiTest {
    @Before
    public void initMockObjects() {
        mockClient = Mockito.mock(Client.class);
        mockLogger = Mockito.mock(Logger.class);
        mockParser = Mockito.mock(ConfigIO.class);
        mockResponse = Mockito.mock(ClientResponse.class);
    }

    @Test
    public void removeComputeNodeUpdatesState() throws IOException, ProcCallException, DataStoreException {
        configureMockResponse(ClientResponse.SUCCESS, "Woohoo!");
        configureMockClientWithResponseForRemoval();

        VoltDbInventoryTrackingApi invApi = new VoltDbInventoryTrackingApi(mockClient, mockLogger, mockParser);

        // Remove the node :o
        invApi.removeComputeNode(LOCATION, TIMESTAMP, REQ_ADAPTER, REQ_WORK_ITEM);

        // Must call the appropriate stored procedure to update the node's state to "missing".
        Mockito.verify(mockClient, Mockito.times(1))
                .callProcedure(COMPUTE_NODE_STATE_UPDATE_SP, LOCATION, MISSING_STATE, TIME_IN_MICROSEC, REQ_ADAPTER,
                        REQ_WORK_ITEM);
    }

    @Test(expected = DataStoreException.class)
    public void removeComputeNodeThrowsWhenProcFails() throws IOException, ProcCallException, DataStoreException {
        configureMockResponse(ClientResponse.CONNECTION_LOST, "Ooops!");
        configureMockClientWithResponseForRemoval();

        VoltDbInventoryTrackingApi invApi = new VoltDbInventoryTrackingApi(mockClient, mockLogger, mockParser);

        // Remove the node :o.  Must throw DataStoreException() in response to stored procedure failure status
        invApi.removeComputeNode(LOCATION, TIMESTAMP, REQ_ADAPTER, REQ_WORK_ITEM);
    }

    @Test(expected = DataStoreException.class)
    public void removeComputeNodeHandlesProcException() throws IOException, ProcCallException, DataStoreException {
        configureMockResponse(ClientResponse.CONNECTION_LOST, "Ooops!");
        configureMockClientToThrowForRemoval();

        VoltDbInventoryTrackingApi invApi = new VoltDbInventoryTrackingApi(mockClient, mockLogger, mockParser);

        // Remove the node :o.  Must throw DataStoreException() in response to VoltDb exception
        invApi.removeComputeNode(LOCATION, TIMESTAMP, REQ_ADAPTER, REQ_WORK_ITEM);
    }

    @Test
    public void replaceComputeNodeRecordsReplacement() throws IOException, ProcCallException, DataStoreException {
        configureMockResponse(ClientResponse.SUCCESS, "Woohoo!");
        configureMockClientWithResponseForReplacement();
        ConfigIO jsonParser = ConfigIOFactory.getInstance("json");

        VoltDbInventoryTrackingApi invApi = new VoltDbInventoryTrackingApi(mockClient, mockLogger, jsonParser);

        // Replace the compute node :)
        invApi.replaceComputeNode(LOCATION, SERIAL_NUMBER, FRU_TYPE, INVENTORY_INFO, TIMESTAMP, REQ_ADAPTER,
                REQ_WORK_ITEM);

        // Must call the appropriate stored procedure to update the node's state to "missing".
        Mockito.verify(mockClient, Mockito.times(1))
                .callProcedure(COMPUTE_NODE_REPLACEMENT_SP, LOCATION, SERIAL_NUMBER, FRU_TYPE, "A",
                        jsonParser.toString(INVENTORY_INFO), TIME_IN_MICROSEC, REQ_ADAPTER, REQ_WORK_ITEM);
    }

    @Test(expected = DataStoreException.class)
    public void replaceComputeNodeThrowsWhenProcFails() throws IOException, ProcCallException, DataStoreException {
        configureMockResponse(ClientResponse.CONNECTION_LOST, "Ooops!");
        configureMockClientWithResponseForReplacement();
        ConfigIO jsonParser = ConfigIOFactory.getInstance("json");

        VoltDbInventoryTrackingApi invApi = new VoltDbInventoryTrackingApi(mockClient, mockLogger, jsonParser);

        // Remove the node :o.  Must throw DataStoreException() in response to stored procedure failure status
        invApi.replaceComputeNode(LOCATION, SERIAL_NUMBER, FRU_TYPE, INVENTORY_INFO, TIMESTAMP, REQ_ADAPTER,
                REQ_WORK_ITEM);
    }

    @Test(expected = DataStoreException.class)
    public void replaceComputeNodeHandlesProcException() throws IOException, ProcCallException, DataStoreException {
        configureMockResponse(ClientResponse.CONNECTION_LOST, "Ooops!");
        configureMockClientToThrowForReplacement();
        ConfigIO jsonParser = ConfigIOFactory.getInstance("json");

        VoltDbInventoryTrackingApi invApi = new VoltDbInventoryTrackingApi(mockClient, mockLogger, jsonParser);

        // Remove the node :o.  Must throw DataStoreException() in response to VoltDb exception
        invApi.replaceComputeNode(LOCATION, SERIAL_NUMBER, FRU_TYPE, INVENTORY_INFO, TIMESTAMP, REQ_ADAPTER,
                REQ_WORK_ITEM);
    }

    @Test
    public void removeServiceNodeUpdatesState() throws IOException, ProcCallException, DataStoreException {
        configureMockResponse(ClientResponse.SUCCESS, "Woohoo!");
        configureMockClientWithResponseForRemoval();

        VoltDbInventoryTrackingApi invApi = new VoltDbInventoryTrackingApi(mockClient, mockLogger, mockParser);

        // Remove the node :o
        invApi.removeServiceNode(LOCATION, TIMESTAMP, REQ_ADAPTER, REQ_WORK_ITEM);

        // Must call the appropriate stored procedure to update the node's state to "missing".
        Mockito.verify(mockClient, Mockito.times(1))
                .callProcedure(SERVICE_NODE_STATE_UPDATE_SP, LOCATION, MISSING_STATE, TIME_IN_MICROSEC, REQ_ADAPTER,
                        REQ_WORK_ITEM);
    }

    @Test(expected = DataStoreException.class)
    public void removeServiceNodeThrowsWhenProcFails() throws IOException, ProcCallException, DataStoreException {
        configureMockResponse(ClientResponse.CONNECTION_LOST, "Ooops!");
        configureMockClientWithResponseForRemoval();

        VoltDbInventoryTrackingApi invApi = new VoltDbInventoryTrackingApi(mockClient, mockLogger, mockParser);

        // Remove the node :o.  Must throw DataStoreException() in response to stored procedure failure status
        invApi.removeServiceNode(LOCATION, TIMESTAMP, REQ_ADAPTER, REQ_WORK_ITEM);
    }

    @Test(expected = DataStoreException.class)
    public void removeServiceNodeHandlesProcException() throws IOException, ProcCallException, DataStoreException {
        configureMockResponse(ClientResponse.CONNECTION_LOST, "Ooops!");
        configureMockClientToThrowForRemoval();

        VoltDbInventoryTrackingApi invApi = new VoltDbInventoryTrackingApi(mockClient, mockLogger, mockParser);

        // Remove the node :o.  Must throw DataStoreException() in response to VoltDb exception
        invApi.removeServiceNode(LOCATION, TIMESTAMP, REQ_ADAPTER, REQ_WORK_ITEM);
    }

    @Test
    public void replaceServiceNodeRecordsReplacement() throws IOException, ProcCallException, DataStoreException {
        configureMockResponse(ClientResponse.SUCCESS, "Woohoo!");
        configureMockClientWithResponseForReplacement();
        ConfigIO jsonParser = ConfigIOFactory.getInstance("json");

        VoltDbInventoryTrackingApi invApi = new VoltDbInventoryTrackingApi(mockClient, mockLogger, jsonParser);

        // Replace the compute node :)
        invApi.replaceServiceNode(LOCATION, SERIAL_NUMBER, FRU_TYPE, INVENTORY_INFO, TIMESTAMP, REQ_ADAPTER,
                REQ_WORK_ITEM);

        // Must call the appropriate stored procedure to update the node's state to "missing".
        Mockito.verify(mockClient, Mockito.times(1))
                .callProcedure(SERVICE_NODE_REPLACEMENT_SP, LOCATION, SERIAL_NUMBER, FRU_TYPE, "A",
                        jsonParser.toString(INVENTORY_INFO), TIME_IN_MICROSEC, REQ_ADAPTER, REQ_WORK_ITEM);
    }

    @Test(expected = DataStoreException.class)
    public void replaceServiceNodeThrowsWhenProcFails() throws IOException, ProcCallException, DataStoreException {
        configureMockResponse(ClientResponse.CONNECTION_LOST, "Ooops!");
        configureMockClientWithResponseForReplacement();
        ConfigIO jsonParser = ConfigIOFactory.getInstance("json");

        VoltDbInventoryTrackingApi invApi = new VoltDbInventoryTrackingApi(mockClient, mockLogger, jsonParser);

        // Remove the node :o.  Must throw DataStoreException() in response to stored procedure failure status
        invApi.replaceServiceNode(LOCATION, SERIAL_NUMBER, FRU_TYPE, INVENTORY_INFO, TIMESTAMP, REQ_ADAPTER,
                REQ_WORK_ITEM);
    }

    @Test(expected = DataStoreException.class)
    public void replaceServiceNodeHandlesProcException() throws IOException, ProcCallException, DataStoreException {
        configureMockResponse(ClientResponse.CONNECTION_LOST, "Ooops!");
        configureMockClientToThrowForReplacement();
        ConfigIO jsonParser = ConfigIOFactory.getInstance("json");

        VoltDbInventoryTrackingApi invApi = new VoltDbInventoryTrackingApi(mockClient, mockLogger, jsonParser);

        // Remove the node :o.  Must throw DataStoreException() in response to VoltDb exception
        invApi.replaceServiceNode(LOCATION, SERIAL_NUMBER, FRU_TYPE, INVENTORY_INFO, TIMESTAMP, REQ_ADAPTER,
                REQ_WORK_ITEM);
    }

    public void configureMockResponse(byte response, String msg) {
        Mockito.when(mockResponse.getStatus()).thenReturn(response);
        Mockito.when(mockResponse.getStatusString()).thenReturn(msg);
    }

    public void configureMockClientWithResponseForRemoval() throws IOException, ProcCallException {
        Mockito.when(mockClient
                .callProcedure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                        Mockito.anyString(), Mockito.anyLong()))
                .thenReturn(mockResponse);
    }

    public void configureMockClientToThrowForRemoval() throws IOException, ProcCallException {
        Mockito.when(mockClient
                .callProcedure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(),
                        Mockito.anyString(), Mockito.anyLong()))
                .thenThrow(ProcCallException.class);
    }

    public void configureMockClientWithResponseForReplacement() throws IOException, ProcCallException {
        Mockito.when(mockClient
                .callProcedure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                        Mockito.anyLong()))
                .thenReturn(mockResponse);
    }

    public void configureMockClientToThrowForReplacement() throws IOException, ProcCallException {
        Mockito.when(mockClient
                .callProcedure(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString(),
                        Mockito.anyString(), Mockito.anyString(), Mockito.anyLong(), Mockito.anyString(),
                        Mockito.anyLong()))
                .thenThrow(ProcCallException.class);
    }

    private static long convertInstantToMicrosec(Instant timestamp) {
        long microsec = TimeUnit.SECONDS.toMicros(timestamp.getEpochSecond()) +
                TimeUnit.NANOSECONDS.toMicros(timestamp.getNano());
        return microsec;
    }

    private static String COMPUTE_NODE_STATE_UPDATE_SP = "ComputeNodeSetState";
    private static String COMPUTE_NODE_REPLACEMENT_SP = "ComputeNodeReplaced";
    private static String SERVICE_NODE_STATE_UPDATE_SP = "ServiceNodeSetState";
    private static String SERVICE_NODE_REPLACEMENT_SP = "ServiceNodeReplaced";

    private static String LOCATION = "somewhere-in-the-cluster";
    private static Instant TIMESTAMP = Instant.now();
    private static Long TIME_IN_MICROSEC = convertInstantToMicrosec(TIMESTAMP);
    private static String REQ_ADAPTER = "test";
    private static Long REQ_WORK_ITEM = 1L;
    private static String MISSING_STATE = "M";

    private static String SERIAL_NUMBER = "ABCD";
    private static String FRU_TYPE = "ACME_NODE";
    private static PropertyMap INVENTORY_INFO;

    static {
        INVENTORY_INFO = new PropertyMap();
        INVENTORY_INFO.put("manufacturer", "ACME");
        INVENTORY_INFO.put("cores", "256");
    }

    private Client mockClient;
    private Logger mockLogger;
    private ConfigIO mockParser;
    private ClientResponse mockResponse;
}