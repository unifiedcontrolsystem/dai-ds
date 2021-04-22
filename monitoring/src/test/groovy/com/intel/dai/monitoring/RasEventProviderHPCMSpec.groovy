// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring

import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.Location
import com.intel.dai.network_listener.CommonDataFormat
import com.intel.dai.network_listener.DataType
import com.intel.dai.network_listener.NetworkListenerConfig
import com.intel.dai.network_listener.NetworkListenerProviderException
import com.intel.dai.network_listener.SystemActions
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import spock.lang.Specification

class RasEventProviderHPCMSpec extends Specification {
    def networkConfig_ = Mock(NetworkListenerConfig)
    def actions_ = Mock(SystemActions)
    def logger_ = Mock(Logger)
    int publishCounter_
    PropertyMap configMap_
    static String topic_ = "__test"
    static TopicBaseProcessor processor_

    def underTest_
    void setup() {
        underTest_ = new RasEventProviderHPCM(Mock(Logger))
        def factory = Mock(DataStoreFactory)
        def locationApi = Mock(Location)
        locationApi.getLocationFromHostname(_ as String) >> _
        locationApi.getLocationFromIP(_ as String) >> _
        locationApi.getLocationFromMAC(_ as String) >> _
        factory.createLocation() >> locationApi
        underTest_.setFactory(factory)
        underTest_.initialize()
        configMap_ = new PropertyMap()
        configMap_.put("publish", true)
        configMap_.put("publishTopic", "events")
        processor_ = new TopicTest(logger_, true)
        underTest_.processorMap_.put(topic_, processor_)
        networkConfig_.getProviderConfigurationFromClassName(_ as String) >> configMap_
        publishCounter_ = 0
        actions_.publishRasEvent(_ as String, _ as String, _ as String, _ as String, _ as Long) >> {
            String topic, String name, String data, String location, Long ns ->
                publishCounter_++
        }
    }

    def "Test processRawStringData"() {
        given:
            List<CommonDataFormat> list = underTest_.processRawStringData(topic_, jsons[INDEX], networkConfig_)
        expect:
            list.size() == RESULT
        where:
            INDEX   || RESULT
            0       || 1
            2       || 0
            3       || 1
            4       || 1
            5       || 1
            6       || 1
    }

    def "Test processRawStringData Negative"() {
        when:
            List<CommonDataFormat> list = underTest_.processRawStringData(topic_, jsons[INDEX], networkConfig_)
        then:
            thrown(NetworkListenerProviderException)
        where:
            INDEX   || RESULT
            1       || 1
            7       || 0
    }

    def "Test processRawStringData Missing Dispatch"() {
        given:
            underTest_.processorMap_.clear()
            underTest_.processorMap_.put(TOPIC, DISPATCH)
            processor_ = DISPATCH
            def list = underTest_.processRawStringData(TOPIC, jsons[0], networkConfig_)
            underTest_.getConfig(networkConfig_)
        expect:
            list.size() == RESULT
        where:
            TOPIC   | DISPATCH    || RESULT
            null    | processor_  || 0
            topic_  | null        || 0
            "other" | processor_  || 1
            "other" | processor_  || 1
    }

    def "Test actOnData"() {
        given:
            underTest_.publish_ = PUBLISH
            CommonDataFormat data = new CommonDataFormat(1619732019000000000, "x0c0s36b0n0", DataType.RasEvent)
            data.setRasEvent("RasEvent", "payload")
            data.setDescription(topic_)
            underTest_.actOnData(data, networkConfig_, actions_)
        expect:
            publishCounter_ == RESULT
        where:
            PUBLISH || RESULT
            true    || 1
            false   || 0
    }

    String[] jsons = [
            """{
  "timestamp": 1618958773000,
  "location": "x0c0s36b0n0"
}""",
            """{ "crap"
  "timestamp": 1618958773000,
  "location": "x0c0s36b0n0"
}""",
            """{
  "timestamp": "crap",
  "location": "x0c0s36b0n0"
}""",
            """{
  "timestamp": 1618958773000,
  "host": "x0c0s36b0n0"
}""",
            """{
  "IP": "x0c0s36b0n0"
}""",
            """{
  "timestamp": "2021-04-22T19:24:36.123Z",
  "MAC": "x0c0s36b0n0"
}""",
            """{
  "timestamp": 1618958773000,
  "unknown": "x0c0s36b0n0"
}""",
            """[
    "one", "two", "three"
]"""
    ]

    static class TopicTest extends TopicBaseProcessor {
        TopicTest(Logger log, boolean doAggregation) { super(log, doAggregation); }

        @Override
        void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
            addToResults(data.topic, "RasEvent", data.originalJsonText, data.location, data.nsTimestamp, results)
        }
    }
}
