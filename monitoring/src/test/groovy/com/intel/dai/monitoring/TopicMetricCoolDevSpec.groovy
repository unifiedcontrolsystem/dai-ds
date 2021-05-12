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

class TopicMetricCoolDevSpec extends Specification {
    def underTest_

    void setup() {
        underTest_ = new TopicMetricCoolDev(Mock(Logger), false)
    }

    def "Test processTopic"() {
        given:
            List<CommonDataFormat> results = new ArrayList<>()
            EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "location")
            PropertyMap map = new PropertyMap()
            map.put("Pump_Delta_Pressure", 10.0)
            map.put("PS1_IT_Return_Pressure", 10.0)
            map.put("Primary_Delta_Pressure", 10.0)
            if(!SKIP)
                map.put("MCS_kWatts_of_heat_removed", VALUE)
            map.put("T3_CDU_Temperature", 10.0)
            underTest_.processTopic(envelope, map, results)
        expect:
            results.size() == RESULT
        where:
            VALUE | SKIP  || RESULT
            10.0  | false || 5
            null  | false || 4
            10.0  | true  || 4
    }
}
