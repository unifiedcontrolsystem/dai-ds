// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import java.util.List;

public class TopicMetricPduEnergy extends TopicBaseProcessor {
    TopicMetricPduEnergy(Logger log, boolean doAggregation) { super(log, doAggregation); }

    @Override
    void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
        processNumberKeyWithFactor("energy", "Wh", "ENERGY", data, map, results, 1000.0);
    }
}
