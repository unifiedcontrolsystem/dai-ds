// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import java.util.List;

class TopicMetricNetworkFabric extends TopicBaseProcessor {
    TopicMetricNetworkFabric(Logger log, boolean doAggregation) { super(log, doAggregation); }

    @Override
    public void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
        processNumberKey("messages_rx_1min", "msgs/sec", "BANDWIDTH", data, map, results);
        processNumberKey("messages_tx_1min", "msgs/sec", "BANDWIDTH", data, map, results);
        processNumberKey("bytes_rx_1min", "bytes/sec", "BANDWIDTH", data, map, results);
        processNumberKey("bytes_tx_1min", "bytes/sec", "BANDWIDTH", data, map, results);
        processNumberKey("messages_rx_cnt", "count", "COUNT", data, map, results);
        processNumberKey("messages_tx_cnt", "count", "COUNT", data, map, results);
        processNumberKey("bytes_tx_cnt", "bytes", "COUNT", data, map, results);
        processNumberKey("bytes_rx_cnt", "bytes", "COUNT", data, map, results);
    }
}
