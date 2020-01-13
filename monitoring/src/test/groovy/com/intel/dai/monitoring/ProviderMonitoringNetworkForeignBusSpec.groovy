package com.intel.dai.monitoring

import spock.lang.Specification

class ProviderMonitoringNetworkForeignBusSpec extends Specification {
    def args_ = new String[3]
    void setup() {
        args_[0] = "127.0.0.1"
        args_[1] = "location"
        args_[2] = "hostname"
    }

    void cleanup() {
    }

    def "Test Main too many arguments"() {
        def bad = new String[4]
        when: ProviderMonitoringNetworkForeignBus.main(bad)
        then: thrown(RuntimeException)
    }

    def "Test Main not enough arguments"() {
        def bad = new String[2]
        when: ProviderMonitoringNetworkForeignBus.main(bad)
        then: thrown(RuntimeException)
    }

    def "Test Main null arguments"() {
        def bad = null
        when: ProviderMonitoringNetworkForeignBus.main(bad)
        then: thrown(RuntimeException)
    }

    def "Test Main with no config file"() {
        ProviderMonitoringNetworkForeignBus.main(args_)
        expect: true
    }
}
