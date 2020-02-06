package com.intel.dai.monitoring

import com.intel.dai.network_listener.CommonDataFormat
import com.intel.dai.network_listener.NetworkListenerConfig
import com.intel.dai.network_listener.NetworkListenerProviderException
import com.intel.dai.network_listener.SystemActions
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import spock.lang.Specification

class DemoEnvironmentalProviderForeignBusSpec extends Specification {
    NetworkListenerConfig config_
    static String data0_ = """{"metrics":{"messages":[]}}"""
    static String data1_ = """{"metrics":{"messages":[{"name":"TEST","timestamp":1577880000,"value":42}]}}"""
    static String dataBad1_ = """{"Metrics":{"messages":[{"name":"TEST","timestamp":1577880000,"value":42}]}}"""
    static String dataBad2_ = """{"metrics":{"Messages":[{"name":"TEST","timestamp":1577880000,"value":42}]}}"""
    static String dataBad3_ = """{"metrics":{"messages":[{"Name":"TEST","timestamp":1577880000,"value":42}]}}"""
    static String dataBad4_ = """{"metrics":{"messages":[{"name":null,"timestamp":1577880000,"value":42}]}}"""
    static String dataBad5_ = """{"metrics":null}"""
    static String dataBad6_ = """{"metrics":{"messages":null}}"""
    static String dataBad7_ = """{"metrics":{"messages":{}}}}"""
    static String dataBad8_ = """{"metrics":{"messages":[{"name":"TEST","timestamp":1577880000,"value":42}]}}"""
    static String badJson_ = """{]"""
    SystemActions actions_ = Mock(SystemActions)

    def underTest_
    void setup() {
        underTest_ = new DemoEnvironmentalProviderForeignBus(Mock(Logger))
        config_ = GroovyMock(NetworkListenerConfig)
        PropertyMap map = new PropertyMap()
        map.put("publish", true)
        map.put("publishRawTopic", "raw_test_topic")
        map.put("publishAggregatedTopic", "test_topic")
        map.put("useTimeWindow", false)
        map.put("windowSize", 2)
        map.put("useMovingAverage", false)
        map.put("timeWindowSeconds", 600)
        map.put("useAggregation", true)
        config_.getProviderConfigurationFromClassName(_ as String) >> { map }
    }

    def "Initialize"() {
        underTest_.initialize()
        expect: true
    }

    def "Test ProcessRawStringData and ActOnData"() {
        List<CommonDataFormat> list = underTest_.processRawStringData(DATA, config_)
        List<CommonDataFormat> list2 = underTest_.processRawStringData(DATA, config_)
        List<CommonDataFormat> list3 = underTest_.processRawStringData(DATA, config_)
        if(list.size() > 0) {
            underTest_.actOnData(list.get(0), config_, actions_)
            underTest_.actOnData(list.get(0), config_, actions_)
        }
        expect: list.size() == RESULT
        where:
        DATA      | RESULT
        badJson_  | 0
        data0_    | 0
        data1_    | 1
        dataBad3_ | 0
        dataBad4_ | 0
        dataBad7_ | 0
        dataBad8_ | 1
    }

    def "Test ProcessRawStringData Negative"() {
        given:
        when: underTest_.processRawStringData(DATA, config_)
        then: thrown(NetworkListenerProviderException)
        where:
        DATA      | NOOP
        dataBad1_ | 0
        dataBad2_ | 0
        dataBad5_ | 0
        dataBad6_ | 0
    }
}
