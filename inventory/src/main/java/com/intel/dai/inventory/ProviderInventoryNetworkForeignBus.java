// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.inventory.api.database.RawInventoryDataIngester;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Description of class ForeignBusProvisionerProvider.
 */
public class ProviderInventoryNetworkForeignBus extends AdapterInventoryNetworkBase {
    private static DataStoreFactory factory_;

    private ProviderInventoryNetworkForeignBus(Logger logger, DataStoreFactory factory, AdapterInformation info,
                                               String benchmarkingFile, long maxBurstSeconds) {
        super(logger, factory, info, benchmarkingFile, maxBurstSeconds);
    }

    public static void main(String[] args) {
        String adapterName = ProviderInventoryNetworkForeignBus.class.getSimpleName();

        // Sanity check of args
        if (args == null || args.length != 3)
            throw new RuntimeException(String.format("Wrong number of arguments for this provider (%s), must " +
                    "use 3 arguments: voltdb_servers, location, and hostname in that order", adapterName));

        // Create logger
        Logger logger = LoggerFactory.getInstance(AdapterInventoryNetworkBase.ADAPTER_TYPE, adapterName, "console");
        logger.info("main(args=[%s, %s, %s])", args[0], args[1], args[2]);

        // Seems to be setting some adapter registry values
        AdapterInformation adapterInfo = new AdapterInformation(AdapterInventoryNetworkBase.ADAPTER_TYPE, adapterName,
                args[1], args[2], -1L);
        adapterInfo.setServers(args[0].split(","));

        // Factory for database API to voltdb and postgres
        factory_ = new DataStoreFactoryImpl(adapterInfo.getServers(), logger);

        // Initialize only once; the methods in this class are all static
        RawInventoryDataIngester.initialize(factory_, logger);

        // App is an singleton of this class; the code above primarily creates the objects to be used by this object
        ProviderInventoryNetworkForeignBus app = new ProviderInventoryNetworkForeignBus(logger, factory_, adapterInfo,
                "/opt/ucs/log/ProviderInventoryNetworkForeignBus-Benchmarking.json", 5);

        // Config file in /opt/ucs/etc: ProviderInventoryNetworkForeignBus.json
        String configName = ProviderInventoryNetworkForeignBus.class.getSimpleName() + ".json";
        logger.info("Using configName: %s", configName);

        // Perform tasks prior to main work item processing loop; work items primarily concerns the adapter lifecycle
        try (InputStream configStream = AdapterInventoryNetworkBase.getConfigStream(configName)) {
            app.preInitialize(configStream);
        } catch (IOException | ConfigIOParseException e) {
            logger.exception(e, "preInitialize: missing or unreadable configuration is fatal, halting the provider process.");
        }

        try (InputStream configStream = AdapterInventoryNetworkBase.getConfigStream(configName)) {
            app.entryPoint(configStream);
        } catch (IOException | ConfigIOParseException e) {
            logger.exception(e, "entryPoint: missing or unreadable configuration is fatal, halting the provider process.");
        }
    }

    public static DataStoreFactory getDataStoreFactory() {
        return factory_;
    }
}
