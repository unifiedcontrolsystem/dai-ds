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

class TopicMetricPduPowerSpec extends Specification {
    def underTest_

    void setup() {
        underTest_ = new TopicMetricPduPower(Mock(Logger), false)
    }

    def "ProcessTopic"() {
        given:
            List<CommonDataFormat> results = new ArrayList<>()
            EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "location")
            PropertyMap map = new PropertyMap()
            if(!SKIP)
                map.put("power", VALUE)
            underTest_.processTopic(envelope, map, results)
        expect:
            results.size() == RESULT
        where:
            VALUE | SKIP  || RESULT
            100.0 | false || 1
            null  | false || 0
            100.0 | true  || 0
    }
}
