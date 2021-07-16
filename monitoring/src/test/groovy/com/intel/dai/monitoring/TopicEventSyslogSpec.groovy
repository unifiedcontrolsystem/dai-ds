package com.intel.dai.monitoring

import com.intel.config_io.ConfigIO
import com.intel.config_io.ConfigIOFactory
import com.intel.dai.network_listener.CommonDataFormat
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import com.intel.runtime_utils.TimeUtils
import spock.lang.Specification

class TopicEventSyslogSpec extends Specification {
    def underTest_
    ConfigIO parser_ = ConfigIOFactory.getInstance("json")

    void setup() {
        underTest_ = new TopicEventSyslog(Mock(Logger), false)
    }

    def "ProcessTopic positive format"() {
        given:
        PropertyMap map = parser_.fromString(input)
        List<CommonDataFormat> results = new ArrayList<>()
        EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "location")
        underTest_.processTopic(envelope, map, results)
        expect:
        results.size() == 1
        where:
        input || RESULT
        "{\"@timestamp\":\"2021-07-14T21:54:24.247Z\",\"syslog\":{\"priority\":3},\"host\":{\"name\":\"aus-admin1\"}}" || 1
        "{\"@timestamp\":\"2021-07-14T21:54:24.247Z\",\"syslog\":{\"priority\":4},\"host\":{\"name\":\"aus-admin1\"}}" || 0

    }

    def "ProcessTopic negative format"() {
        given:
        PropertyMap map = parser_.fromString(format_negative)
        List<CommonDataFormat> results = new ArrayList<>()
        EnvelopeData envelope = new EnvelopeData("test", TimeUtils.getNsTimestamp(), "location")
        underTest_.processTopic(envelope, map, results)
        expect:
        results.size() == 0
    }

    private final String format_negative = "{\n" +
            "  \"@timestamp\": \"2021-07-14T21:54:24.247Z\",\n" +
            "  \"host\": {\n" +
            "    \"name\": \"test01\"\n" +
            "  }\n" +
            "}"
}

