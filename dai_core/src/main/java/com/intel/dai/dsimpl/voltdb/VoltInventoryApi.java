// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.InventoryApi;
import com.intel.dai.dsapi.NodeInformation;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.voltdb.VoltTable;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;

/**
 * Description of class VoltInventoryApi.
 */
public class VoltInventoryApi implements InventoryApi {
    public VoltInventoryApi(Logger log, AdapterInformation adapter, String[] servers) {
        log_ = log;
        adapter_ = adapter;
        servers_ = servers;
    }

    @Override
    public void initialize() {
        client_ = getClient();
    }

    protected Client getClient() {
        VoltDbClient.initializeVoltDbClient(servers_);
        return VoltDbClient.getVoltClientInstance();
    }

    protected NodeInformation getNodeInformation() {
        return new VoltDbNodeInformation(log_, servers_);
    }

    @Override
    public String getNodesInvInfoFromDb(String location) throws DataStoreException {
        // Check & see if this is a compute node or a service node.
        NodeInformation info = getNodeInformation();
        String sTempStoredProcedure;
        if (info.isComputeNodeLocation(location))
            sTempStoredProcedure = "ComputeNodeInventoryInfo";
        else if (info.isServiceNodeLocation(location))
            sTempStoredProcedure = "ServiceNodeInventoryInfo";
        else {
            String sErrorMsg = "Specified location is neither a compute node nor a service node - Location=" +
                    location + "!";
            log_.error(sErrorMsg);
            throw new DataStoreException(sErrorMsg);
        }

        try {
            // Get the location's existing inventory info out of the DB.
            String sDbInvInfo = null;
            ClientResponse responseGetInventoryInfo = client_.callProcedure(sTempStoredProcedure, location);
            if (responseGetInventoryInfo.getStatus() != ClientResponse.SUCCESS) {
                // stored procedure failed.
                log_.error("stored procedure %s failed - Status=%s, StatusString=%s, ThisAdapterId=%d!",
                        sTempStoredProcedure, VoltDbClient.statusByteAsString(responseGetInventoryInfo.getStatus()),
                        responseGetInventoryInfo.getStatusString(), adapter_.getId());
                throw new RuntimeException(responseGetInventoryInfo.getStatusString());
            }
            VoltTable vtGetInventoryInfo = responseGetInventoryInfo.getResults()[0];

            // Check & see if we got inventory info for this node.
            if (vtGetInventoryInfo.getRowCount() == 0) {
                // no inventory information was returned.
                log_.warn("There is no inventory information for lctn %s", location);
                return null;
            }
            // Grab the inventory info and return it to the caller.
            vtGetInventoryInfo.advanceRow();
            sDbInvInfo = vtGetInventoryInfo.getString("InventoryInfo");
            return sDbInvInfo;
        } catch(IOException | ProcCallException e) {
            throw new DataStoreException("Failed to get inventory for location " + location, e);
        }
    }

    private Logger log_;
    private AdapterInformation adapter_;
    private String[] servers_;
    private Client client_ = null;
}
