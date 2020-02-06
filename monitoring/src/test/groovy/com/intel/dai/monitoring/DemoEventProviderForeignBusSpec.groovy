package com.intel.dai.monitoring

import com.intel.dai.network_listener.CommonDataFormat
import com.intel.dai.network_listener.NetworkListenerConfig
import com.intel.dai.network_listener.NetworkListenerProviderException
import com.intel.dai.network_listener.SystemActions
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import spock.lang.Specification

class DemoEventProviderForeignBusSpec extends Specification {
    NetworkListenerConfig config_
    static String data0_ = """{"metrics":{"messages":[]}}"""
    static String data1_ = """{"metrics":{"messages":[{"message":"TEST","timereported":"2020-02-06T22:45:00.000Z"}]}}"""
    static String dataBad1_ = """{"Metrics":{"messages":[{"message":"TEST","timereported":"2020-02-06T22:45:00.000Z"}]}}"""
    static String dataBad2_ = """{"metrics":{"Messages":[{"message":"TEST","timereported":"2020-02-06T22:45:00.000Z"}]}}"""
    static String dataBad3_ = """{"metrics":{"messages":[{"Message":"TEST","timereported":"2020-02-06T22:45:00.000Z"}]}}"""
    static String dataBad4_ = """{"metrics":{"messages":[{"message":null,"timereported":"2020-02-06T22:45:00.000Z"}]}}"""
    static String dataBad5_ = """{"metrics":null}"""
    static String dataBad6_ = """{"metrics":{"messages":null}}"""
    static String dataBad7_ = """{"metrics":{"messages":{}}}}"""
    static String dataBad8_ = """{"metrics":{"messages":[{"message":"TEST","timereported":"2020/02/06T22:45:00.000Z"}]}}"""
    static String badJson_ = """{]"""
    SystemActions actions_ = Mock(SystemActions)

    def underTest_
    void setup() {
        underTest_ = new DemoEventProviderForeignBus(Mock(Logger))
        config_ = GroovyMock(NetworkListenerConfig)
        PropertyMap map = new PropertyMap()
        map.put("publish", true)
        map.put("publishTopic", "test_topic")
        config_.getProviderConfigurationFromClassName(_ as String) >> { map }
    }

    def "Test Initialize"() {
        underTest_.initialize()
        expect: true
    }

    def "Test ProcessRawStringData and ActOnData"() {
        List<CommonDataFormat> list = underTest_.processRawStringData(DATA, config_)
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
        dataBad8_ | 0
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
