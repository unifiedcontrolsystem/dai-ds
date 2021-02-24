package com.intel.dai.inventory

import spock.lang.Ignore
import spock.lang.Specification

class ProviderInventoryNetworkForeignBusSpec extends Specification {
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
        when: ProviderInventoryNetworkForeignBus.main(bad)
        then: thrown(RuntimeException)
    }

    def "Test Main not enough arguments"() {
        def bad = new String[2]
        when: ProviderInventoryNetworkForeignBus.main(bad)
        then: thrown(RuntimeException)
    }

    def "Test Main null arguments"() {
        def bad = null
        when: ProviderInventoryNetworkForeignBus.main(bad)
        then: thrown(RuntimeException)
    }

    @Ignore
    def "Test Main with no config file"() {
        ProviderInventoryNetworkForeignBus.main(args_)
        expect: true
    }
}
