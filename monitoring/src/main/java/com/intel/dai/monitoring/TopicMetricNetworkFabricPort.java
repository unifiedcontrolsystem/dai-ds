// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import java.util.List;

public class TopicMetricNetworkFabricPort extends TopicBaseProcessor {
    TopicMetricNetworkFabricPort(Logger log, boolean doAggregation) { super(log, doAggregation); }

    @Override
    void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
        if(!checkMap(map))
            return;
        String prefix;
        // Defaults never happen in code flow, checkMap short circuits it.
        prefix = String.format("%s[%d]", map.getStringOrDefault("sw_name", "MISSING"),
                map.getIntOrDefault("sw_number", -1));
        processNumberKey(prefix, "rx_pps", "pkts/sec", "BANDWIDTH", data, map, results);
        processNumberKey(prefix, "tx_pps", "pkts/sec", "BANDWIDTH", data, map, results);
        processNumberKey(prefix, "discards", "count", "STATS", data, map, results);
        processNumberKey(prefix, "negotiations", "count", "STATS", data, map, results);
        processNumberKey(prefix, "corrections", "count", "STATS", data, map, results);
        processNumberKey(prefix, "rx_bytes", "bytes", "STATS", data, map, results);
        processNumberKey(prefix, "tx_bytes", "bytes", "STATS", data, map, results);
        processNumberKey(prefix, "rx_bps", "bits/sec", "BANDWIDTH", data, map, results);
        processNumberKey(prefix, "tx_bps", "bits/sec", "BANDWIDTH", data, map, results);
        processNumberKey(prefix, "rx_ber", "bits", "ERRORS", data, map, results);
        processNumberKey(prefix, "tx_ber", "bits", "ERRORS", data, map, results);
        processNumberKey(prefix, "cable_temperature_C", "C", "TEMPERATURE", data, map, results);
        processNumberKey(prefix, "sw_temperature_C", "C", "TEMPERATURE", data, map, results);
    }

    private void processNumberKey(String prefix, String key, String units, String type, EnvelopeData data, PropertyMap map,
                                  List<CommonDataFormat> results) {
        if(map.containsKey(key) && map.get(key) != null) {
            double value = map.getDoubleOrDefault(key, 0.0); // Can't ever return the default!
            addToResults(data.topic + "." + prefix + "." + key, value, units, type, data.nsTimestamp,
                    data.location, results);
        }
    }

    private boolean checkMap(PropertyMap map) {
        for(String key: requiredKeys_)
            if(!map.containsKey(key)) {
                log_.warn("Expected field in JSON was missing: %s", key);
                return false;
            }
        return true;
    }

    private String[] requiredKeys_ = new String[] {
            "sw_node_name",
            "sw_number"
    };
}
