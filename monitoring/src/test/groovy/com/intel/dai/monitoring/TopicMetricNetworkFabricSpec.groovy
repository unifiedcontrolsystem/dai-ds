// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring

import com.intel.dai.network_listener.CommonDataFormat
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import com.intel.runtime_utils.TimeUtils
import spock.lang.Specification

class TopicMetricNetworkFabricSpec extends Specification {
    def underTest_

    void setup() {
        underTest_ = new TopicMetricNetworkFabric(Mock(Logger), false)
    }

    def "Test processTopic"() {
        given:
            List<CommonDataFormat> results = new ArrayList<>()
            EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "location")
            PropertyMap map = new PropertyMap()
            map.put("messages_rx_1min", 10.0)
            map.put("messages_tx_1min", 10.0)
            map.put("bytes_rx_1min", 10.0)
            map.put("bytes_tx_1min", 10.0)
            map.put("messages_rx_cnt", 10.0)
            map.put("messages_tx_cnt", 10.0)
            if(!SKIP)
                map.put("bytes_tx_cnt", VALUE)
            map.put("bytes_rx_cnt", 10.0)
            underTest_.processTopic(envelope, map, results)
        expect:
            results.size() == RESULT
        where:
            VALUE | SKIP  || RESULT
            10.0  | false || 8
            null  | false || 7
            10.0  | true  || 7
    }
}
