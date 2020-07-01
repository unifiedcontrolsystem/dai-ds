// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.BootState;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Description of class BenchmarkingSystemActions. This is used to measure throughput of the dai_network_listener by
 * bypassing all actions and instead recording data for benchmarking. See the NetworkListenerCore class for details.
 */
public class BenchmarkingSystemActions implements SystemActions, Initializer {
    BenchmarkingSystemActions(Logger logger, DataStoreFactory factory, AdapterInformation info,
                                    NetworkListenerConfig config) throws NetworkListenerCore.Exception { }

    @Override
    public void initialize() {}

    @Override
    public void storeNormalizedData(String dataType, String location, long microSecondsTimeStamp, double value) {
        storeRaw_.tick();
    }

    @Override
    public void storeAggregatedData(String dataType, String location, long microSecondsTimeStamp, double min,
                                    double max, double average) {
        storeAggregated_.tick();
    }

    @Override
    public void storeRasEvent(String eventName, String instanceData, String location, long nsTimestamp) {
        storeRas_.tick();
    }

    @Override
    public void publishNormalizedData(String topic, String dataType, String location, long microSecondsTimeStamp,
                                      double value) { }

    @Override
    public void publishAggregatedData(String topic, String dataType, String location, long microSecondsTimeStamp,
                                      double min, double max, double average) { }

    @Override
    public void publishRasEvent(String topic, String eventName, String instanceData, String location,
                                long nsTimestamp) { }

    @Override
    public void publishBootEvent(String topic, BootState event, String location, long nsTimestamp) { }

    @Override
    public void changeNodeStateTo(BootState event, String location, long nsTimestamp, boolean informWlm) {
        storeState_.tick();
    }

    @Override
    public void changeNodeBootImageId(String location, String id) { }

    @Override
    public void upsertBootImages(List<Map<String, String>> bootImageInfoList) { }

    @Override
    public void logFailedToUpdateNodeBootImageId(String location, String instanceData) { }

    @Override
    public void logFailedToUpdateBootImageInfo(String instanceData) { }

    @Override
    public boolean isHWInventoryEmpty() throws IOException, DataStoreException {
        return true;
    }

    @Override
    public void upsertHWInventory(String location, String foreignName) { }

    @Override
    public void deleteHWInventory(String location) { }

    @Override
    public String lastHWInventoryHistoryUpdate() {
        return "";
    }

    @Override
    public void upsertHWInventoryHistory(String canonicalJson) {}

    @Override
    public void close() throws IOException { }

    private final Record storeRaw_ = new Record("storeRaw");
    private final Record storeAggregated_ = new Record("storeAggregated");
    private final Record storeRas_ = new Record("storeRas");
    private final Record storeState_ = new Record("storeState");

    static class Record {
        Record(String name) { this.name = name; }

        synchronized void tick() {
            long now = Instant.now().toEpochMilli();
            if(count == 0L)
                startTimeStamp = now;
            lastTimeStamp = now;
            if(count % 10000L == 0L) System.out.println(toString()); // Intentionally not logged!
            count++;
        }

        @Override
        public String toString() {
            return "*** '" + name + "' ==>> " + count + " in " +
                    (lastTimeStamp - startTimeStamp) + " milliseconds";
        }

        final String name;
        private long startTimeStamp = 0L;
        private long lastTimeStamp = 0L;
        private long count = 0L;
    }
}
