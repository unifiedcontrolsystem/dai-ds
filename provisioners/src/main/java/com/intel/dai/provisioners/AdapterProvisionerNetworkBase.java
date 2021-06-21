// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.provisioners;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.network_listener.NetworkListenerConfig;
import com.intel.dai.network_listener.NetworkListenerCore;
import com.intel.logging.Logger;
import com.intel.perflogging.BenchmarkHelper;
import com.intel.xdg.XdgConfigFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Description of class NetworkAdapterProvisionerBase.
 */
abstract class AdapterProvisionerNetworkBase {
    AdapterProvisionerNetworkBase(Logger logger, DataStoreFactory factory, AdapterInformation info,
                                  String benchmarkingFile, long maxBurstSeconds) {
        log_ = logger;
        factory_ = factory;
        adapter_ = info;
        benchmarking_ = new BenchmarkHelper(info.getType(), benchmarkingFile, maxBurstSeconds);
    }

    static InputStream getConfigStream(String baseFilename) throws FileNotFoundException {
        XdgConfigFile xdg = new XdgConfigFile("ucs");
        InputStream result = xdg.Open(baseFilename);
        if(result == null)
            throw new FileNotFoundException("Failed to locate or open '" + baseFilename + "'");
        return result;
    }

    protected boolean execute(NetworkListenerCore adapterCore) {
        Runtime.getRuntime().addShutdownHook(new Thread(adapterCore::shutDown));
        return adapterCore.run() == 0;
    }

    boolean entryPoint(InputStream configStream) throws IOException, ConfigIOParseException {
        NetworkListenerConfig config_ = new NetworkListenerConfig(adapter_, log_);
        config_.loadFromStream(configStream);
        NetworkListenerCore adapterCore = new NetworkListenerCore(log_, config_, factory_, benchmarking_);
        return execute(adapterCore);
    }

    private final AdapterInformation adapter_;
    private final Logger log_;
    private final BenchmarkHelper benchmarking_;
    private final DataStoreFactory factory_;
    static final String ADAPTER_TYPE = "PROVISIONER";
}
