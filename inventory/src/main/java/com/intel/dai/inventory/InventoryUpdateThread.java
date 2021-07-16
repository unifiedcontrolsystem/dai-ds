// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.dai.dsapi.*;
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.inventory.api.es.Elasticsearch;
import com.intel.dai.inventory.api.es.ElasticsearchIndexIngester;
import com.intel.dai.inventory.api.es.NodeInventoryIngester;
import com.intel.dai.network_listener.NetworkListenerConfig;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.elasticsearch.client.RestHighLevelClient;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Performs background update of the HW inventory DB.
 * Note that we assume the the initial loading of inventory to the
 * voltdb tables is performed by a thread started by app.postInitialize(),
 * and that this may be ongoing!
 */
class InventoryUpdateThread implements Runnable {
    private final Logger log_;
    private final NetworkListenerConfig config_;

    InventoryUpdateThread(Logger log, NetworkListenerConfig config) {
        log_ = log;
        config_ = config;
    }

    /**
     * Runs a background thread that updates the DAI inventory database tables using
     * information from the foreign server.
     */
    public void run() {
        log_.debug("run()");
//        final DataStoreFactory factory_ = ProviderInventoryNetworkForeignBus.getDataStoreFactory();
//        if (factory_ == null) {
//            log_.error("ProviderInventoryNetworkForeignBus.getDataStoreFactory() => null");
//            return;
//        }
        try {
            log_.info("InventoryUpdateThread started");
            DatabaseSynchronizer synchronizer = new DatabaseSynchronizer(log_, config_);
            synchronizer.updateDaiInventoryTables();
        } finally {
            log_.info("InventoryUpdateThread terminated");
        }
    }
}
