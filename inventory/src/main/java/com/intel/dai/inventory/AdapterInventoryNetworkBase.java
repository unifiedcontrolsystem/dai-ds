// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.HWInvApi;
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.inventory.api.HWInvDiscovery;
import com.intel.dai.inventory.api.HWInvTranslator;
import com.intel.dai.network_listener.NetworkListenerConfig;
import com.intel.dai.network_listener.NetworkListenerCore;
import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClientException;
import com.intel.perflogging.BenchmarkHelper;
import com.intel.xdg.XdgConfigFile;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

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
        ingestCanonicalHWInvJson(
                toCanonicalHWInvJson(
                        getForeignHWInvJson()));

        // Not needed for this milestone CMC
/*        ingestCanonicalHWInvHistoryJson(
                toCanonicalHWinvHistoryJson(
                        getForeignHWInvHistoryJson(
                                determineMissingStartTime(), determineMissingEndTime())));*/
    }

    private String toCanonicalHWinvHistoryJson(String foreignHWInvHistoryJson) {
        if (foreignHWInvHistoryJson == null) return null;

        HWInvTranslator tr = new HWInvTranslator(new HWInvUtilImpl());
        ImmutablePair<String, String> canonicalHwInvHistory = tr.foreignHistoryToCanonical(foreignHWInvHistoryJson);
        if (canonicalHwInvHistory.getKey() == null) {
            log_.error("failed to translate foreign HW inventory history json");
            return null;
        }
        return canonicalHwInvHistory.getValue();
    }

    /**
     * Initialises required hardware inventory api instances used to fetch initial hw inventory data and load into db.
     */
    void preInitialise() {
        hwInvDiscovery_ = new HWInvDiscovery(log_);
        hwInvApi_ = factory_.createHWInvApi();
    }

    private String determineMissingStartTime() {
        try {
            return hwInvApi_.lastHwInvHistoryUpdate();
        } catch (IOException | DataStoreException e) {
            return determineMissingEndTime();
        }
    }

    private String determineMissingEndTime() {
        return Instant.now().toString();
    }

    private  String getForeignHWInvHistoryJson(String startTime, String endTime) {
        try {
            hwInvDiscovery_.initialize();
            log_.info("rest client created");

        } catch (RESTClientException e) {
            log_.fatal("Fail to create REST client: %s", e.getMessage());
            return null;
        }

        ImmutablePair<Integer, String> foreignHwInvHistory = hwInvDiscovery_.queryHWInvHistory(startTime, endTime);

        if (foreignHwInvHistory.getLeft() != 0) {
            log_.error("failed to acquire foreign HW inventory history json");
            return null;
        }
        return foreignHwInvHistory.getRight();
    }

    private void ingestCanonicalHWInvHistoryJson(String canonicalHwInvhistoryJson) {
        if (canonicalHwInvhistoryJson == null) return;

        try {
            hwInvApi_.ingestHistory(canonicalHwInvhistoryJson);
        } catch (InterruptedException e) {
            log_.error("InterruptedException: %s", e.getMessage());
        } catch (IOException e) {
            log_.error("IOException: %s", e.getMessage());
        } catch (DataStoreException e) {
            log_.error("DataStoreException: %s", e.getMessage());
        }
    }

    /**
     * Ingests the HW inventory locations in canonical form.
     * @param canonicalHwInvJson json containing the HW inventory locations in canonical format
     */
    private void ingestCanonicalHWInvJson(String canonicalHwInvJson) {
        if (canonicalHwInvJson == null) return;

        try {
            hwInvApi_.ingest(canonicalHwInvJson);
        } catch (InterruptedException e) {
            log_.error("InterruptedException: %s", e.getMessage());
        } catch (IOException e) {
            log_.error("IOException: %s", e.getMessage());
        } catch (DataStoreException e) {
            log_.error("DataStoreException: %s", e.getMessage());
        }
    }

    /**
     * Converts the HW inventory locations in foreign format into canonical format.
     * @param foreignHWInvJson json containing the HW inventory in foreign format
     * @return json containing the HW inventory in canonical format
     */
    private String toCanonicalHWInvJson(String foreignHWInvJson) {
        if (foreignHWInvJson == null) return null;

        HWInvTranslator tr = new HWInvTranslator(new HWInvUtilImpl());
        ImmutablePair<String, String> canonicalHwInv = tr.foreignToCanonical(foreignHWInvJson);
        if (canonicalHwInv.getKey() == null) {
            log_.error("failed to translate foreign HW inventory json");
            return null;
        }
        return canonicalHwInv.getValue();
    }

    /**
     * Obtains the HW inventory of all locations of the HPC is returned.
     * @return json containing the all locations
     */
    private String getForeignHWInvJson() {

        try {
            hwInvDiscovery_.initialize();
            log_.info("rest client created");

        } catch (RESTClientException e) {
            log_.fatal("Fail to create REST client: %s", e.getMessage());
            return null;
        }

        ImmutablePair<Integer, String> foreignHwInv;
        foreignHwInv = hwInvDiscovery_.queryHWInvTree();

        if (foreignHwInv.getLeft() != 0) {
            log_.error("failed to acquire foreign HW inventory json");
            return null;
        }
        return foreignHwInv.getRight();
    }

    private final AdapterInformation adapter_;
    private final Logger log_;
    private final DataStoreFactory factory_;
    private final BenchmarkHelper benchmarking_;
    static final String ADAPTER_TYPE = "INVENTORY";
    protected HWInvApi hwInvApi_;
    protected HWInvDiscovery hwInvDiscovery_;
}
