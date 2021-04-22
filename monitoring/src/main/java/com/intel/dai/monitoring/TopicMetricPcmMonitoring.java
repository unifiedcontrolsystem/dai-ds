// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import java.util.List;

public class TopicMetricPcmMonitoring extends TopicBaseProcessor {
    TopicMetricPcmMonitoring(Logger log, boolean doAggregation) { super(log, doAggregation); }

    @Override
    void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
        data.adjustTimestamp(1_000); // Original was seconds not milliseconds, correct for that.
        // Will get MHz but want to store Hz...
        processNumberKeyWithFactor("cpu_frequency","Hz", "FREQUENCY", data, map, results, 1_000_000.0);
    }
}
