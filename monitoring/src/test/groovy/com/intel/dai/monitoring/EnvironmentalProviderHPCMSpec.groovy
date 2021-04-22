// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring

import com.intel.config_io.ConfigIO
import com.intel.config_io.ConfigIOFactory
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.Location
import com.intel.dai.network_listener.CommonDataFormat
import com.intel.dai.network_listener.NetworkListenerConfig
import com.intel.dai.network_listener.NetworkListenerProviderException
import com.intel.dai.network_listener.SystemActions
import com.intel.dai.result.Result
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import spock.lang.Specification

class EnvironmentalProviderHPCMSpec extends Specification {
    def networkConfig_ = Mock(NetworkListenerConfig)
    def actions_ = Mock(SystemActions)
    def logger_ = Mock(Logger)
    int publishRawCounter_
    int publishAggCounter_
    PropertyMap configMap_
    String topic_ = "__test"
    ConfigIO parser_ = ConfigIOFactory.getInstance("json")

    def underTest_

    void setup() {
        underTest_ = new EnvironmentalProviderHPCM(logger_)
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
        configMap_.put("publishAggregatedTopic", "aggData")
        configMap_.put("publishRawTopic", "rawData")
        configMap_.put("useTimeWindow", false)
        configMap_.put("windowSize", 25)
        configMap_.put("useMovingAverage", false)
        configMap_.put("useAggregation", true)
        underTest_.processorMap_.put(topic_, new TopicTest(logger_, true))
        networkConfig_.getProviderConfigurationFromClassName(_ as String) >> configMap_
        publishRawCounter_ = 0
        publishAggCounter_ = 0
        actions_.publishNormalizedData(_ as String, _ as String, _ as String, _ as Long, _ as Double) >> {
            String topic, String name, String location, Long ns, Double value ->
                publishRawCounter_++
        }
        actions_.publishAggregatedData(_ as String, _ as String, _ as String, _ as Long, _ as Double, _ as Double, _ as Double) >> {
            String topic, String name, String location, Long ns, Double min, Double max, Double avg ->
                publishAggCounter_++
        }
    }

    def "Test Initialize"() {
        expect: true
    }

    def "Test ProcessRawStringData"() {
        given:
            def list = underTest_.processRawStringData(topic_, jsons[0], networkConfig_)
        expect:
            list.size() == 1
    }

    def "Test ProcessRawStringData With no location"() {
        given:
            def list = underTest_.processRawStringData(topic_, jsons[INDEX], networkConfig_)
        expect:
            list.size() == RESULT

        where:
            INDEX || RESULT
            3     || 1
            4     || 1
            5     || 1
            6     || 1
    }

    def "Test ProcessRawStringData With Bad JSON"() {
        when:
            underTest_.processRawStringData(topic_, jsons[1], networkConfig_)
        then:
            thrown(NetworkListenerProviderException)
    }

    def "Test ProcessRawStringData With Timestamp"() {
        given:
            def list = underTest_.processRawStringData(topic_, jsons[2], networkConfig_)
        expect:
            list.size() == 0
    }

    def "Test dispatchProcessing negative"() {
        given:
            def list = new ArrayList<CommonDataFormat>()
            def item = new PropertyMap()
            def envelope = new EnvelopeData(TOPIC, 0L, "location")
            underTest_.dispatchProcessing(list, item, envelope)
        expect:
            list.size() == RESULT
        where:
            TOPIC   || RESULT
            "topic" || 0
            null    || 0
    }

    def "Test ActOnData"() {
        given:
            for (int i = 0; i < 125; i++) {
                def list = underTest_.processRawStringData(topic_, jsons[0], networkConfig_)
                for (CommonDataFormat data : list)
                    underTest_.actOnData(data, networkConfig_, actions_)
            }
        expect:
            publishRawCounter_ == 125 // 125 datum
        and:
            publishAggCounter_ == 5   // 5 data aggregated @ 25 samples
    }

    def "Test ActOnData with time window"() {
        given:
            configMap_.put("useTimeWindow", true)
            List<CommonDataFormat> list = new ArrayList<>()
            for (long i = 0L; i < 125L; i++) {
                PropertyMap map = parser_.fromString(jsons[0]).getAsMap()
                map.put("timestamp", map.getLong("timestamp") + (i * 120_000L)) // every 2 minutes in a 10 minute window
                String json = parser_.toString(map)
                list.addAll(underTest_.processRawStringData(topic_, json, networkConfig_))
            }
            for (CommonDataFormat data : list)
                underTest_.actOnData(data, networkConfig_, actions_)
        expect:
            publishRawCounter_ == 125 // 125 datum
        and:
            publishAggCounter_ == 20  // 20 data aggregated @ 10 minutes window
    }

    def "Test ActOnData with moving average"() {
        given:
            configMap_.put("useMovingAverage", true)
            for (int i = 0; i < 125; i++) {
                def list = underTest_.processRawStringData(topic_, jsons[0], networkConfig_)
                for (CommonDataFormat data : list)
                    underTest_.actOnData(data, networkConfig_, actions_)
            }
        expect:
            publishRawCounter_ == 125 // 125 datum
        and:
            publishAggCounter_ == 101 // 5 data aggregated @ 25 samples
    }

    def "Test ActOnData No Publish"() {
        given:
            configMap_.put("publish", false)
            underTest_.publish_ = false
            underTest_.processorMap_.put(topic_, new TopicTest(logger_, false))
            for (int i = 0; i < 125; i++) {
                def list = underTest_.processRawStringData(topic_, jsons[0], networkConfig_)
                for (CommonDataFormat data : list)
                    underTest_.actOnData(data, networkConfig_, actions_)
            }
        expect:
            publishRawCounter_ == 0   // 125 datum but no published data
        and:
            publishAggCounter_ == 0   // no published data
    }

    String[] jsons = [
            """{
  "timestamp": 1618958773000,
  "location": "x0c0s36b0n0",
  "value": 10.0
}""",
            """{ "crap"
  "timestamp": 1618958773000,
  "location": "x0c0s36b0n0",
  "value": 10.0
}""",
            """{
  "timestamp": "crap",
  "location": "x0c0s36b0n0",
  "value": 10.0
}""",
            """{
  "timestamp": 1618958773000,
  "host": "x0c0s36b0n0",
  "value": 10.0
}""",
            """{
  "IP": "x0c0s36b0n0",
  "value": 10.0
}""",
            """{
  "timestamp": "2021-04-22T19:24:36.123Z",
  "MAC": "x0c0s36b0n0",
  "value": 10.0
}""",
            """{
  "timestamp": 1618958773000,
  "unknown": "x0c0s36b0n0",
  "value": 10.0
}""",
            """[
    "one", "two", "three"
]"""
    ]

    static class TopicTest extends TopicBaseProcessor {
        TopicTest(Logger log, boolean doAggregation) { super(log, doAggregation); }

        @Override
        void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
            double value = map.getDoubleOrDefault("value", 0.0)
            addToResults(data.topic + ".test.double (TEST)", value, "", "TEST",
                    data.nsTimestamp, data.location, results)
        }
    }
}
