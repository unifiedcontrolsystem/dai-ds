// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.populate;

import com.intel.logging.LoggerFactory;
import com.intel.logging.Logger;

import com.intel.dai.dsimpl.DataStoreFactoryImpl;

public class OnlineTierDataLoaderApp {
    public static void main(String[] args) {
        Logger log = LoggerFactory.getInstance("INITIALIZATION", OnlineTierDataLoader.class.getName(), DEFAULT_LOGGER);

        if (args.length != 4) {
            log.error("starting - Invalid set of arguments specified. The first argument must be the VoltDB " +
                    "server, the second must be the System Manifest file name, the third must be the Machine " +
                    "Configuration file name and the fourth must be the RAS Event Meta Data file name.");
            for (String arg : args) log.error("arg: %s", arg);
            System.exit(1);
        }

        String servers = args[0];
        String manifestFile = args[1];
        String machineConfig = args[2];
        String rasMetaData = args[3];

        DataStoreFactoryImpl dsFactory = new DataStoreFactoryImpl(servers, log);
        OnlineTierDataLoader dataLoader =
                new OnlineTierDataLoader(dsFactory, servers, manifestFile, machineConfig, rasMetaData, log);

        System.exit(dataLoader.populateOnlineTier());
    }

    private static final String DEFAULT_LOGGER = "console";
}
