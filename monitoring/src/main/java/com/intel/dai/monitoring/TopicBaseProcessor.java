// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.dai.network_listener.DataType;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

abstract class TopicBaseProcessor {
    TopicBaseProcessor(Logger log, boolean doAggregation) {
        log_ = log;
        doAggregation_ = doAggregation;
    }

    abstract void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results);

    void addToResults(String name, double value, String units, String type, long nsTimestamp, String location,
                              List<CommonDataFormat> results) {
        CommonDataFormat common = new CommonDataFormat(nsTimestamp, location, DataType.EnvironmentalData);
        common.setValueAndUnits(value, units, type);
        common.setDescription(name);
        results.add(aggregateData(common));
    }

    void addToResults(String topic, String rasEventName, String payload, String location, long nsTimestamp,
                      List<CommonDataFormat> results) {
        CommonDataFormat common = new CommonDataFormat(nsTimestamp, location, DataType.RasEvent);
        common.setDescription(topic);
        common.setRasEvent(rasEventName, payload);
        results.add(common);
    }

    void processNumberKey(String key, String units, String type, EnvelopeData data, PropertyMap map,
                          List<CommonDataFormat> results) {
        processNumberKeyWithFactor(key, units, type, data, map, results, 1.0);
    }

    void processNumberKeyWithFactor(String key, String units, String type, EnvelopeData data, PropertyMap map,
                                    List<CommonDataFormat> results, double factor) {
        if(map.containsKey(key) && map.get(key) != null && map.get(key) instanceof Number) {
            double value = map.getDoubleOrDefault(key, 0.0) * factor; // Can't ever return the default!
            addToResults(data.topic + "." + key, value, units, type, data.nsTimestamp,
                    data.location, results);
        }
    }

    private synchronized CommonDataFormat aggregateData(CommonDataFormat raw) {
        if(doAggregation_) {
            log_.debug("Aggregating data...");
            String type = raw.getTelemetryDataType();
            String name = raw.getDescription();
            if(type != null && !type.trim().isEmpty())
                name = name + " (" + type + ")";
            String key = raw.getLocation() + ":" + name;
            Accumulator accum = accumulators_.getOrDefault(key, null);
            if (accum == null) {
                accum = new Accumulator(log_);
                accumulators_.put(key, accum);
            }
            accum.addValue(raw);
        }
        return raw;
    }

    protected final Logger log_;
    private final Map<String, Accumulator> accumulators_ = new HashMap<>();
    private final boolean doAggregation_;
}
