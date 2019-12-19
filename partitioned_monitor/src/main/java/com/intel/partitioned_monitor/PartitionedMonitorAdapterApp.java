// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.partitioned_monitor;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.sink.NetworkDataSinkFactory;
import com.intel.networking.sink.restsse.NetworkDataSinkSSE;
import com.intel.xdg.XdgConfigFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Description of class PartitionedMonitorAdapterApp.
 */
public class PartitionedMonitorAdapterApp {
    public int entryPoint(String[] args, Logger logger, AdapterInformation baseAdapter) {
        baseAdapter.setServers(args[0].split(","));
        PartitionedMonitorConfig config = new PartitionedMonitorConfig(baseAdapter, logger);
        try (InputStream stream = getConfigStream()) {
            config.loadFromStream(stream);
        } catch(IOException | ConfigIOParseException e) {
            throw new RuntimeException("Failed to load the configuration file for this Adapter", e);
        }
        PartitionedMonitorAdapter adapter = new PartitionedMonitorAdapter(logger, config);
        Runtime.getRuntime().addShutdownHook(new Thread(adapter::shutDown));
        return execute(adapter);
    }

    int execute(PartitionedMonitorAdapter adapter) {

        return adapter.run();
    }

    InputStream getConfigStream() throws FileNotFoundException {
        XdgConfigFile xdg = new XdgConfigFile("ucs");
        InputStream result = xdg.Open(PartitionedMonitorConfig.class.getSimpleName() + ".json");
        if(result == null)
            throw new FileNotFoundException("Failed to locate or open '" + PartitionedMonitorAdapter.ADAPTER_NAME +
                    ".json'");
        return result;
    }

    public static void main(String[] args) {
        if(args == null || args.length != 3)
            throw new RuntimeException(String.format("Wrong number of arguments for this adapter (%s), must " +
                            "use 4 arguments: voltdb_servers, location, hostname, and profileName in that order",
                    PartitionedMonitorAdapterApp.class.getCanonicalName()));
        AdapterInformation baseAdapter = new AdapterInformation(PartitionedMonitorAdapter.ADAPTER_TYPE,
                PartitionedMonitorAdapter.ADAPTER_NAME, args[1], args[2], -1L);
        Logger logger = LoggerFactory.getInstance(PartitionedMonitorAdapter.ADAPTER_TYPE, baseAdapter.getBaseName(),
                "console");
        PartitionedMonitorAdapterApp app = new PartitionedMonitorAdapterApp();
        System.exit(app.entryPoint(args, logger, baseAdapter));
    }
}
