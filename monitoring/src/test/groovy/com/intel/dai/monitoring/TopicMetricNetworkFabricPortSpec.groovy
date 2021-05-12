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

class TopicMetricNetworkFabricPortSpec extends Specification {
    def underTest_

    void setup() {
        underTest_ = new TopicMetricNetworkFabricPort(Mock(Logger), false)
    }

    def "Test processTopic"() {
        given:
            List<CommonDataFormat> results = new ArrayList<>()
            EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "sw_node_name")
            PropertyMap map = new PropertyMap()
            if(!SKIP_NAME)
                map.put("sw_node_name", "nodename")
            map.put("sw_number", 24)
            map.put("rx_pps", 10.0)
            map.put("tx_pps", 10.0)
            map.put("rx_bytes", 100.0)
            map.put("tx_bytes", 100.0)
            map.put("rx_bps", 10.0)
            map.put("tx_bps", 10.0)
            map.put("rx_ber", 10.0)
            map.put("tx_ber", 10.0)
            map.put("discards", 10.0)
            map.put("negotiations", 10.0)
            if(!SKIP)
                map.put("corrections", VALUE)
            map.put("cable_temperature_C", 20.0)
            map.put("sw_temperature_C", 20.0)
            underTest_.processTopic(envelope, map, results)
        expect:
            results.size() == RESULT
        where:
            VALUE | SKIP  | SKIP_NAME || RESULT
            15.0  | false | false    || 13
            null  | false | false    || 12
            0.0   | true  | false    || 12
            0.0   | false | true     || 0
    }
}
