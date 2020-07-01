// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.HWInvDbApi;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.network_listener.NetworkListenerConfig;
import com.intel.dai.network_listener.NetworkListenerCore;
import com.intel.logging.Logger;
import com.intel.perflogging.BenchmarkHelper;
import com.intel.xdg.XdgConfigFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

/**
 * Description of class NetworkAdapterInventoryBase.
 */
abstract class AdapterInventoryNetworkBase {
    AdapterInventoryNetworkBase(Logger logger, DataStoreFactory factory, AdapterInformation info,
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
        NetworkListenerConfig config = new NetworkListenerConfig(adapter_, log_);
        config.loadFromStream(configStream);
        NetworkListenerCore adapterCore = new NetworkListenerCore(log_, config, factory_, benchmarking_);
        return execute(adapterCore);
    }

    /**
     * Ingests the initial HW inventory data into data base.
     * Patch missing HW inventory history.
     */
    void postInitialize() {
        ingestCanonicalHWInvHistoryJson(foreignInventoryClient_.getCanonicalHWInvHistoryJson(
                foreignInventoryClient_.lastHWInventoryHistoryUpdate(hwInvDbApi_)));
        ingestCanonicalHWInvJson(foreignInventoryClient_.getCanonicalHWInvJson(""));
    }

    /**
     * Initializes required hardware inventory api instances used to fetch initial hw inventory data and load into db.
     */
    void preInitialize() {
        foreignInventoryClient_ = new ForeignInventoryClient(log_);
        hwInvDbApi_ = factory_.createHWInvApi();
    }

    /**
     * Ingests the HW inventory locations in canonical form.
     * N.B. System actions are not available to the caller of this method.
     * @param canonicalHwInvJson json containing the HW inventory locations in canonical format
     */
    private void ingestCanonicalHWInvJson(String canonicalHwInvJson) {
        if (canonicalHwInvJson == null) return;

        try {
            hwInvDbApi_.ingest(canonicalHwInvJson);
        } catch (InterruptedException e) {
            log_.error("InterruptedException: %s", e.getMessage());
        } catch (IOException e) {
            log_.error("IOException: %s", e.getMessage());
        } catch (DataStoreException e) {
            log_.error("DataStoreException: %s", e.getMessage());
        }
    }

    private void ingestCanonicalHWInvHistoryJson(String canonicalHwInvHistJson) {
        if (canonicalHwInvHistJson == null) return;

        try {
            hwInvDbApi_.ingestHistory(canonicalHwInvHistJson);
        } catch (InterruptedException e) {
            log_.error("InterruptedException: %s", e.getMessage());
        } catch (IOException e) {
            log_.error("IOException: %s", e.getMessage());
        } catch (DataStoreException e) {
            log_.error("DataStoreException: %s", e.getMessage());
        }
    }

    private final AdapterInformation adapter_;
    private final Logger log_;
    private final DataStoreFactory factory_;
    private final BenchmarkHelper benchmarking_;
    static final String ADAPTER_TYPE = "INVENTORY";
    protected HWInvDbApi hwInvDbApi_;
    protected ForeignInventoryClient foreignInventoryClient_;
}
