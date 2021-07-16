package com.intel.dai.monitoring

import com.intel.dai.network_listener.CommonDataFormat
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import com.intel.runtime_utils.TimeUtils
import spock.lang.Specification

class TopicMetricFabricPerfTelemetrySpec extends Specification {
    def underTest_

    void setup() {
        underTest_ = new TopicMetricFabricPerfTelemetry(Mock(Logger), false)
    }

    def "ProcessTopic positive format"() {
        given:
        List<CommonDataFormat> results = new ArrayList<>()
        EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "location")
        PropertyMap map = new PropertyMap()
        map.put("PhysicalContext", PhysicalContext)
        map.put("Location", LOCATION)
        map.put("Value", VALUE)
        map.put("Timestamp", "2019-03-11T03:35:18.580Z")
        PropertyMap test = new PropertyMap()
        test.put("fields", map)
        underTest_.processTopic(envelope, test, results)
        expect:
        results.size() == RESULT
        where:
        VALUE |   LOCATION  | PhysicalContext || RESULT
        100.0 |    "x0"     | "sensor 1"      || 1
        100.0 |    "x1"     | "sensor 2"      || 0
    }

    def "ProcessTopic negative format"() {
        given:
        List<CommonDataFormat> results = new ArrayList<>()
        EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "location")
        PropertyMap map = new PropertyMap()
        map.put("PhysicalContext", PhysicalContext)
        map.put("Value", VALUE)
        map.put("Timestamp", "2019-03-11T03:35:18.580Z")
        PropertyMap test = new PropertyMap()
        test.put("fields", map)
        underTest_.processTopic(envelope, test, results)
        expect:
        results.size() == RESULT
        where:
        VALUE |   PhysicalContext || RESULT
        100.0 |    "sensor 1"     || 0
        100.0 |    "sensor 2"     || 0
    }
}
