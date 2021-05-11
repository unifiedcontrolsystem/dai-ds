// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopicMetricSensorNode extends TopicBaseProcessor {
    TopicMetricSensorNode(Logger log, boolean doAggregation) { super(log, doAggregation); }

    @Override
    void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
        String name = map.getStringOrDefault("name", null);
        if(name != null && !name.trim().isEmpty()) {
            data.appendNameToTopic(name);
            String units = map.getStringOrDefault("units", "");
            String type = unitsToType_.getOrDefault(units, "");
            double factor = unitsToFactor_.getOrDefault(units, 1.0);
            processNumberKeyWithFactor("value", units, type, data, map, results, factor);
        }
    }

    // This schema a general schema for any type of data so what it supports is listed below.
    private final static Map<String,String> unitsToType_ = new HashMap<>() {{
        put("C", "TEMPERATURE");
        put("W", "POWER");
        put("Hz", "FREQUENCY");
        put("", null);
        put(null, null);
    }};
    private final static Map<String,Double> unitsToFactor_ = new HashMap<>() {{
        put("C", 1.0);
        put("W", 1.0);
        put("Hz", 1.0);
        put("", 1.0);
        put(null, 1.0);
    }};
}
