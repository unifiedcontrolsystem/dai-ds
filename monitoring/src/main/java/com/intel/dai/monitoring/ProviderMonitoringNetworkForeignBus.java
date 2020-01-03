// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

/**
 * Description of class ProviderEnvironmentalForeignBus.
 */
public class ProviderMonitoringNetworkForeignBus extends AdapterMonitoringNetworkBase {
    private ProviderMonitoringNetworkForeignBus(Logger logger, DataStoreFactory factory, AdapterInformation info) {
        super(logger, factory, info);
    }

    public static void main(String[] args) {
        String adapterName = ProviderMonitoringNetworkForeignBus.class.getSimpleName();
        if(args == null || args.length != 3)
            throw new RuntimeException(String.format("Wrong number of arguments for this provider (%s), must " +
                    "use 3 arguments: voltdb_servers, location, and hostname in that order", adapterName));

        Logger logger = LoggerFactory.getInstance(AdapterMonitoringNetworkBase.ADAPTER_TYPE, adapterName, "console");
        AdapterInformation adapterInfo = new AdapterInformation(AdapterMonitoringNetworkBase.ADAPTER_TYPE, adapterName,
                args[1],args[2], -1L);
        adapterInfo.setServers(args[0].split(","));
        DataStoreFactory factory = new DataStoreFactoryImpl(adapterInfo.getServers(), logger);

        ProviderMonitoringNetworkForeignBus app = new ProviderMonitoringNetworkForeignBus(logger, factory, adapterInfo);
        String configName = ProviderMonitoringNetworkForeignBus.class.getSimpleName() + ".json";
        try (InputStream configStream = AdapterMonitoringNetworkBase.getConfigStream(configName)) {
            app.entryPoint(configStream);
        } catch (IOException | ConfigIOParseException e) {
            logger.exception(e, "Missing or unreadable configuration is fatal, halting the provider process.");
        }
    }
}
