package com.intel.dai.fabric

import com.intel.config_io.ConfigIOParseException
import spock.lang.Specification

class FabricTelemetryItemSpec extends Specification {
    def underTest_
    void setup() {
        underTest_ = new FabricTelemetryItem(1L, "sensor", "r1", 20.1)
    }

    def "Test GetValue"() {
        expect: underTest_.getValue() == 20.1
    }

    def "Test Ctor Using JSON"() {
        def json = JSON
        def inst = new FabricTelemetryItem(json)
        println JSON
        expect: inst.getAverage() == RESULT
        where:
        JSON                                                                                                                 || RESULT
        """{"name":"name","location":"location","timestamp":99,"value":20.1,"minimum":20.1}"""                               || Double.MIN_VALUE
        """{"name":"name","location":"location","timestamp":99,"value":20.1,"average":20.1}"""                               || Double.MIN_VALUE
        """{"name":"name","location":"location","timestamp":99,"value":20.1,"maximum":20.1}"""                               || Double.MIN_VALUE
        """{"name":"name","location":"location","timestamp":99,"value":20.1,"minimum":20.1,"average":20.1}"""                || Double.MIN_VALUE
        """{"name":"name","location":"location","timestamp":99,"value":20.1}"""                                              || Double.MIN_VALUE
        """{"name":"name","location":"location","timestamp":99,"value":20.1,"minimum":20.1,"average":20.1,"maximum":20.1}""" || 20.1
    }

    def "Test Ctor Using JSON Negative"() {
        def json = """{"name":"name","location":"location","timestamp":99,"value":null}"""
        when: new FabricTelemetryItem(json)
        then: thrown(ConfigIOParseException)
    }

    def "Test toString"() {
        expect: underTest_.toString() == """{"skipStore":false,"name":"sensor","location":"r1","value":20.1,"timestamp":1}"""
    }

    def "Test toString With Stats"() {
        underTest_.setStatistics(20.0, 20.1, 20.2)
        expect: underTest_.toString() == """{"average":20.1,"skipStore":false,"name":"sensor","maximum":20.2,"location":"r1","value":20.1,"minimum":20.0,"timestamp":1}"""
    }

    def "Test setStatistics Negative"() {
        when: underTest_.setStatistics(MIN, AVG, MAX)
        then: thrown(IllegalArgumentException)
        where:
        MIN              | AVG              | MAX
        Double.MIN_VALUE | 1.0              | 1.0
        1.0              | Double.MIN_VALUE | 1.0
        1.0              | 1.0              | Double.MIN_VALUE
    }

    def "Test haveStatistics"() {
        underTest_.minimum_ = MIN
        underTest_.average_ = AVG
        underTest_.maximum_ = MAX
        expect: underTest_.haveStatistics() == RESULT
        where:
        MIN              | AVG              | MAX              || RESULT
        Double.MIN_VALUE | 1.0              | 1.0              || false
        1.0              | Double.MIN_VALUE | 1.0              || false
        1.0              | 1.0              | Double.MIN_VALUE || false
        1.0              | 1.0              | 1.0              || true
    }

    def "Test skipAggregationAndStore"() {
        expect: underTest_.skipAggregationAndStore() == false
    }
}
