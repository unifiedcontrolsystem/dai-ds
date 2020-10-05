// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Description of class ForeignBusProvisionerProvider.
 */
public class ProviderInventoryNetworkForeignBus extends AdapterInventoryNetworkBase {
    private ProviderInventoryNetworkForeignBus(Logger logger, DataStoreFactory factory, AdapterInformation info,
                                               String benchmarkingFile, long maxBurstSeconds) {
        super(logger, factory, info, benchmarkingFile, maxBurstSeconds);
    }

    public static void main(String[] args) {
        String adapterName = ProviderInventoryNetworkForeignBus.class.getSimpleName();
        if(args == null || args.length != 3)
            throw new RuntimeException(String.format("Wrong number of arguments for this provider (%s), must " +
                    "use 3 arguments: voltdb_servers, location, and hostname in that order", adapterName));

        Logger logger = LoggerFactory.getInstance(AdapterInventoryNetworkBase.ADAPTER_TYPE, adapterName, "console");
        logger.info("HWI:%n  main(args=[%s, %s, %s])", args[0], args[1], args[2]);

        AdapterInformation adapterInfo = new AdapterInformation(AdapterInventoryNetworkBase.ADAPTER_TYPE, adapterName,
                args[1],args[2], -1L);
        adapterInfo.setServers(args[0].split(","));
        factory_= new DataStoreFactoryImpl(adapterInfo.getServers(), logger);

        ProviderInventoryNetworkForeignBus app = new ProviderInventoryNetworkForeignBus(logger, factory_, adapterInfo,
                "/opt/ucs/log/ProviderInventoryNetworkForeignBus-Benchmarking.json", 5);
        String configName = ProviderInventoryNetworkForeignBus.class.getSimpleName() + ".json";

        app.preInitialize();
        app.postInitialize();

        try (InputStream configStream = AdapterInventoryNetworkBase.getConfigStream(configName)) {
            app.entryPoint(configStream);
        } catch (IOException | ConfigIOParseException e) {
            logger.exception(e, "Missing or unreadable configuration is fatal, halting the provider process.");
        }
    }

    public static DataStoreFactory getDataStoreFactory() {
        return factory_;
    }

    private static DataStoreFactory factory_;
}
