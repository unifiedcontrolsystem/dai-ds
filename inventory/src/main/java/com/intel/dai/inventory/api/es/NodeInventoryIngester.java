// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.inventory.api.es;

import com.google.gson.Gson;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.HWInvDbApi;
import com.intel.dai.dsapi.pojo.Dimm;
import com.intel.dai.dsapi.pojo.FruHost;
import com.intel.dai.dsapi.pojo.NodeInventory;
import com.intel.dai.dsapi.InventoryTrackingApi;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.properties.PropertyMap;

import java.util.List;
import java.util.Map;

public class NodeInventoryIngester {
    private final Logger log_;
    protected HWInvDbApi onlineInventoryDatabaseClient_;                // voltdb
    protected InventoryTrackingApi inventoryApi_;
    protected ConfigIO jsonParser;
    private final static Gson gson = new Gson();
    private long numberNodeInventoryJsonIngested = 0;

    public NodeInventoryIngester(DataStoreFactory factory, Logger log) {
        log_ = log;
        onlineInventoryDatabaseClient_ = factory.createHWInvApi();
        onlineInventoryDatabaseClient_.initialize();
        inventoryApi_ = factory.createInventoryTrackingApi();
        jsonParser = ConfigIOFactory.getInstance("json");
    }

    String constructAndIngestNodeInventoryJson(FruHost fruHost) throws DataStoreException {
        log_.info("Constructing node inventory from %s", fruHost.hostname);
        NodeInventory nodeInventory = new NodeInventory(fruHost);

        Map<String, String> dimmJsons = onlineInventoryDatabaseClient_.getDimmJsonsOnFruHost(fruHost.mac);
        for (String locator : dimmJsons.keySet()) {
            String dimmJson = dimmJsons.get(locator);
            addDimmJsonsToFruHostJson(nodeInventory, locator, dimmJson);
            String hostname = fruHost.hostname;
            long doc_timestamp = fruHost.timestamp; // epoch seconds


            long sizeMB = 0L;
            String serial = "";
            try {
                PropertyMap dimm = jsonParser.fromString(dimmJson).getAsMap();
                PropertyMap ib_dimm = dimm.getMap("ib_dimm");
                sizeMB = Long.valueOf(ib_dimm.getString("Size").split(" ")[0]);
                serial = ib_dimm.getString("Serial Number");
            } catch (Exception e) {
                log_.exception(e, "Failed retrieving dimm info from json file.");
            }

            try {
                inventoryApi_.addDimm(hostname, hostname + "_" + locator,
                        "A", sizeMB, locator, null, serial,
                        doc_timestamp * 1000000L, "INVENTORY", -1);
            } catch (DataStoreException e) {
                log_.error(e.getMessage());
            }
        }

        numberNodeInventoryJsonIngested += onlineInventoryDatabaseClient_.ingest(nodeInventory);
        return gson.toJson(nodeInventory);
    }

    void addDimmJsonsToFruHostJson(NodeInventory nodeInventory, String locator, String json) {
        log_.debug("  Adding %s => %s", locator, json);
        switch (locator) {
            case "CPU0_DIMM_A1":
                nodeInventory.CPU0_DIMM_A1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_B1":
                nodeInventory.CPU0_DIMM_B1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_C1":
                nodeInventory.CPU0_DIMM_C1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_D1":
                nodeInventory.CPU0_DIMM_D1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_E1":
                nodeInventory.CPU0_DIMM_E1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_F1":
                nodeInventory.CPU0_DIMM_F1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_G1":
                nodeInventory.CPU0_DIMM_G1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU0_DIMM_H1":
                nodeInventory.CPU0_DIMM_H1 = gson.fromJson(json, Dimm.class);
                break;

            case "CPU1_DIMM_A1":
                nodeInventory.CPU1_DIMM_A1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_B1":
                nodeInventory.CPU1_DIMM_B1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_C1":
                nodeInventory.CPU1_DIMM_C1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_D1":
                nodeInventory.CPU1_DIMM_D1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_E1":
                nodeInventory.CPU1_DIMM_E1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_F1":
                nodeInventory.CPU1_DIMM_F1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_G1":
                nodeInventory.CPU1_DIMM_G1 = gson.fromJson(json, Dimm.class);
                break;
            case "CPU1_DIMM_H1":
                nodeInventory.CPU1_DIMM_H1 = gson.fromJson(json, Dimm.class);
                break;

            default:
                log_.error("Unknown location %s", locator);
        }
    }

    public void ingestInitialNodeInventoryHistory() {
        List<FruHost> fruHosts = onlineInventoryDatabaseClient_.enumerateFruHosts();
        if (fruHosts == null) {
            log_.error("fruHosts is null");
            return;
        }

        for (FruHost fruHost : fruHosts) {
            try {
                String nodeInventoryJson = constructAndIngestNodeInventoryJson(fruHost);
                inventoryApi_.addFru(fruHost.hostname, fruHost.timestamp * 1000000L, nodeInventoryJson, fruHost.boardSerial, fruHost.rawIbBios);
            } catch (DataStoreException e) {
                log_.error("DataStoreException: %s", e.getMessage());
            }
        }
    }

    public long getNumberNodeInventoryJsonIngested() {
        return numberNodeInventoryJsonIngested;
    }
}
