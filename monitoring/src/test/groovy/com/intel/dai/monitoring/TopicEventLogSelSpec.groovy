// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring

import com.intel.dai.network_listener.CommonDataFormat
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import spock.lang.Specification

import static com.intel.config_io.ConfigIOFactory.getInstance

class TopicEventLogSelSpec extends Specification {
    def underTest_
    void setup() {
        underTest_ = new TopicEventLogSel(Mock(Logger), false)
    }

    def "ProcessTopic"() {
        given:
            List<CommonDataFormat> results = new ArrayList<>()
            EnvelopeData envelope = new EnvelopeData("_test", 1619794926000, "location")
            PropertyMap map = getInstance("json").fromString(json).getAsMap()
            underTest_.processTopic(envelope, map, results)
        expect:
            results.size() == 1
    }

    def json="""{
    "Sensor_Number": "12",
    "Sensor_Type": "1",
    "Generator": "03",
    "Timestamp": 1619794926000,
    "Event_Data": "01f3ab",
    "host": "admin1",
    "Event_Direction": "assert",
    "MAC": "0c:54:15:82:c9:1d",
    "Record_Type": "04",
    "Description": "Test RAS Event",
    "Event_Type": "00",
    "Record_ID": "95a",
    "EvM": "1.1"
}"""
}
