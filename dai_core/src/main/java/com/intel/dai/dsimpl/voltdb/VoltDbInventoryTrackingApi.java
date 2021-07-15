// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;

import com.intel.dai.dsapi.InventoryTrackingApi;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.properties.PropertyMap;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.config_io.ConfigIO;
import org.voltdb.client.ProcCallException;

public class VoltDbInventoryTrackingApi implements InventoryTrackingApi {
    public VoltDbInventoryTrackingApi(Client voltClient, Logger logger, ConfigIO parser) {
        this.voltClient = voltClient;
        this.logger = logger;
        this.parser = parser;
    }

    public VoltDbInventoryTrackingApi(ConfigIO parser, Logger logger) {
        voltClient = VoltDbClient.getVoltClientInstance();
        this.logger = logger;
        this.parser = parser;
    }

    @Override
    public void removeComputeNode(String location, Instant timestamp, String reqAdapterType, long reqWorkItemId)
            throws DataStoreException {
        try {
            long timeInMicrosec = convertInstantToMicrosec(timestamp);
            ClientResponse response = voltClient.callProcedure(COMPUTE_NODE_STATE_UPDATE_SP, location, "M",
                    timeInMicrosec, reqAdapterType, reqWorkItemId);
            if (response.getStatus() != ClientResponse.SUCCESS) {
                String statusDesc = response.getStatusString();
                logger.error("Error invoking stored procedure: %s.  Request details: Location=%s, AdapterType=%s, " +
                        "WorkItemId=%d.  Status returned by server: %s", COMPUTE_NODE_STATE_UPDATE_SP, location,
                        reqAdapterType, reqWorkItemId, statusDesc);
                throw new DataStoreException("Unable to update compute node's state in the data store.  Status " +
                        "returned by server: " + statusDesc);
            }
        } catch (IOException | ProcCallException ex) {
            logger.exception(ex, "Error invoking stored procedure: %s.  Request details: Location=%s, " +
                    "AdapterType=%s, WorkItemId=%d.", COMPUTE_NODE_STATE_UPDATE_SP, location, reqAdapterType,
                    reqWorkItemId);
            throw new DataStoreException("An error ocurred while updating the compute node's state in the data store",
                    ex);
        }
    }

    @Override
    public void replaceComputeNode(String location, String serialNumber, String fruType, PropertyMap inventoryInfo,
                                   Instant timestamp, String reqAdapterType, long reqWorkItemId)
            throws DataStoreException {
        try {
            long timeInMicrosec = convertInstantToMicrosec(timestamp);
            String inventoryInfoStr = parser.toString(inventoryInfo);
            ClientResponse response = voltClient.callProcedure(COMPUTE_NODE_REPLACEMENT_SP, location, serialNumber,
                    fruType, "A", inventoryInfoStr, timeInMicrosec, reqAdapterType, reqWorkItemId);
            if (response.getStatus() != ClientResponse.SUCCESS) {
                String statusDesc = response.getStatusString();
                logger.error("Error invoking stored procedure: %s.  Request details: Location=%s, SerialNumber=%s, " +
                        "AdapterType=%s, WorkItemId=%d.  Status returned by server: %s", COMPUTE_NODE_REPLACEMENT_SP,
                        location, serialNumber, reqAdapterType, reqWorkItemId, statusDesc);
                throw new DataStoreException("Unable to record compute node replacement.  Status returned by server: " +
                        statusDesc);
            }
        } catch (IOException | ProcCallException ex) {
            logger.exception(ex, "Error invoking stored procedure: %s.  Request details: Location=%s, " +
                    "SerialNumber=%s, AdapterType=%s, WorkItemId=%d.", COMPUTE_NODE_REPLACEMENT_SP, location,
                    reqAdapterType, reqWorkItemId);
            throw new DataStoreException("An error occurred while recording compute node replacement", ex);
        } catch (Exception ex) {
            logger.exception(ex, "An unexpected error occurred while retrieving the results from stored procedure: %s",
                    COMPUTE_NODE_REPLACEMENT_SP);
            throw new DataStoreException("An unexpected error occurred while retrieving the results from stored " +
                    "procedure: " + COMPUTE_NODE_REPLACEMENT_SP);
        }
    }

    @Override
    public void removeServiceNode(String location, Instant timestamp, String reqAdapterType, long reqWorkItemId)
            throws DataStoreException {
        try {
            long timeInMicrosec = convertInstantToMicrosec(timestamp);
            ClientResponse response = voltClient.callProcedure(SERVICE_NODE_STATE_UPDATE_SP, location, "M",
                    timeInMicrosec, reqAdapterType, reqWorkItemId);
            if (response.getStatus() != ClientResponse.SUCCESS) {
                String statusDesc = response.getStatusString();
                logger.error("Error invoking stored procedure: %s.  Request details: Location=%s, AdapterType=%s, " +
                        "WorkItemId=%d.  Status returned by server: %s", SERVICE_NODE_STATE_UPDATE_SP, location,
                        reqAdapterType, reqWorkItemId, statusDesc);
                throw new DataStoreException("Unable to update service node's state in the data store.  Status " +
                        "returned by server: " + statusDesc);
            }
        } catch (IOException | ProcCallException ex) {
            logger.exception(ex, "Error invoking stored procedure: %s.  Request details: Location=%s, " +
                    "AdapterType=%s, WorkItemId=%d.", SERVICE_NODE_STATE_UPDATE_SP, location, reqAdapterType,
                    reqWorkItemId);
            throw new DataStoreException("An error ocurred while updating the service node's state in the data store",
                    ex);
        }
    }

    @Override
    public void replaceServiceNode(String location, String serialNumber, String fruType, PropertyMap inventoryInfo,
                                   Instant timestamp, String reqAdapterType, long reqWorkItemId)
            throws DataStoreException {
        try {
            long timeInMicrosec = convertInstantToMicrosec(timestamp);
            String inventoryInfoStr = parser.toString(inventoryInfo);
            ClientResponse response = voltClient.callProcedure(SERVICE_NODE_REPLACEMENT_SP, location, serialNumber,
                    fruType, "A", inventoryInfoStr, timeInMicrosec, reqAdapterType, reqWorkItemId);
            if (response.getStatus() != ClientResponse.SUCCESS) {
                String statusDesc = response.getStatusString();
                logger.error("Error invoking stored procedure: %s.  Request details: Location=%s, SerialNumber=%s, " +
                        "AdapterType=%s, WorkItemId=%d.  Status returned by server: %s", SERVICE_NODE_REPLACEMENT_SP,
                        location, serialNumber, reqAdapterType, reqWorkItemId, statusDesc);
                throw new DataStoreException("Unable to record service node replacement.  Status returned by server: " +
                        statusDesc);
            }
        } catch (IOException | ProcCallException ex) {
            logger.exception(ex, "Error invoking stored procedure: %s.  Request details: Location=%s, " +
                    "SerialNumber=%s, AdapterType=%s, WorkItemId=%d.", SERVICE_NODE_REPLACEMENT_SP, location,
                    reqAdapterType, reqWorkItemId);
            throw new DataStoreException("An error occurred while recording service node replacement", ex);
        }
    }

    @Override
    public void addDimm(String nodeLocation, String componentLocation, String state, long sizeMB, String moduleLocator, String bankLocator, String serial, long inventoryTS, String adapter, long workItem) throws DataStoreException {
        try {
            ClientResponse response = voltClient.callProcedure(ADD_DIMM_SP, nodeLocation, componentLocation,
                    state, sizeMB, moduleLocator, bankLocator, serial, inventoryTS, adapter, workItem);
            if (response.getStatus() != ClientResponse.SUCCESS) {
                String statusDesc = response.getStatusString();
                logger.error("Error invoking stored procedure: %s.  Request details: Location=%s, ComponentLocation=%s, " +
                                "state=%s, sizeMb=%d, moduleLocator=%s, bankLocator=%s, serial=%s", ADD_DIMM_SP,
                        nodeLocation, componentLocation, state, sizeMB, moduleLocator, bankLocator, serial);
                throw new DataStoreException("Unable to add dimm to history.  Status returned by server: " +
                        statusDesc);
            }
        } catch (IOException | ProcCallException ex) {
            logger.error("Error invoking stored procedure: %s.  Request details: Location=%s, ComponentLocation=%s, " +
                            "state=%s, sizeMb=%d, moduleLocator=%s, bankLocator=%s, serial=%s", ADD_DIMM_SP,
                    nodeLocation, componentLocation, state, sizeMB, moduleLocator, bankLocator, serial);
            throw new DataStoreException("An error occurred while adding dimm to history", ex);
        }
    }

    @Override
    public void addFru(String nodeLocation, long inventoryTS, String inventoryInfo, String sernum, String biosInfo) throws DataStoreException {
        try {
            ClientResponse response = voltClient.callProcedure(ADD_FRU_SP, nodeLocation, inventoryTS, inventoryInfo, sernum, biosInfo);
            if (response.getStatus() != ClientResponse.SUCCESS) {
                String statusDesc = response.getStatusString();
                logger.error("Error invoking stored procedure: %s.  Request details: Location=%s, sernum=%s. ", ADD_FRU_SP, nodeLocation, sernum);
                throw new DataStoreException("Unable to add fru to history.  Status returned by server: " + statusDesc);
            }
        } catch (IOException | ProcCallException ex) {
            logger.error("Error invoking stored procedure: %s.  Request details: Location=%s, sernum=%s. ", ADD_FRU_SP, nodeLocation, sernum);
            throw new DataStoreException("An error occurred while adding fru history", ex);
        }
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
    private static String ADD_DIMM_SP = "DimmAddToHistory";
    private static String ADD_FRU_SP = "FruAddToHistory";

    private Client voltClient;
    private Logger logger;
    private ConfigIO parser;
}