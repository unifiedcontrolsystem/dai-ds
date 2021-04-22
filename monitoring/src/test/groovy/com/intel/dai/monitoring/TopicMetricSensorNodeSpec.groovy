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

class TopicMetricSensorNodeSpec extends Specification {
    def underTest_

    void setup() {
        underTest_ = new TopicMetricSensorNode(Mock(Logger), false)
    }

    def "ProcessTopic"() {
        given:
            List<CommonDataFormat> results = new ArrayList<>()
            EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "location")
            PropertyMap map = new PropertyMap()
            map.put("name", NAME)
            map.put("units", UNITS)
            map.put("value", VALUE)
            underTest_.processTopic(envelope, map, results)
        expect:
            results.size() == RESULT
        where:
            VALUE | UNITS  | NAME           || RESULT
            100.0 | "C"    | "temperature1" || 1
            100.0 | ""     | "somevalue2"   || 1
            100.0 | null   | "somevalue3"   || 1
            100.0 | "C"    | null           || 0
    }
}
