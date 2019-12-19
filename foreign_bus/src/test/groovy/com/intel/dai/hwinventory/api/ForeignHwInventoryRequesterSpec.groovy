package com.intel.dai.hwinventory.api

import com.intel.logging.Logger
import com.intel.networking.restclient.BlockingResult
import com.intel.networking.restclient.RESTClient
import spock.lang.Specification

class ForeignHwInventoryRequesterSpec extends Specification {
    ForeignHwInventoryRequester invRequester;

    def log = Mock(Logger)
    def config = new Requester()
    def restClient = Mock(RESTClient)

    def setup() {
        invRequester = new ForeignHwInventoryRequester();
    }

    def "initialize"() {
        invRequester.initialize(log, config, restClient)

        expect:
        invRequester.logger == log
        invRequester.config == config
        invRequester.restClient == restClient
    }

    def "initiateDiscovery"() {
        setup:
        invRequester.initialize(log, config, restClient)

        when:
        def res = invRequester.initiateDiscovery()

        then:
        res == 1
    }

    def "getDiscoveryStatus"() {
        setup:
        invRequester.initialize(log, config, restClient)

        when:
        def res = invRequester.getDiscoveryStatus()

        then:
        res == 1
    }

    def "getHwInventory"() {
        setup:
        invRequester.initialize(log, config, restClient)

        when:
        def res = invRequester.getHwInventory("anyfile")

        then:
        res == 1
    }

    def "makeUri" () {
        expect: invRequester.makeUri("http://localhost:5678", "/Inventory/Hardware").toString() ==
                new URI("http://localhost:5678/Inventory/Hardware").toString()
    }

    def "makeUri - sub resource" () {
        expect: invRequester.makeUri("http://localhost:5678", "/Inventory/Hardware/Query/",
                "x0c0s0b0n0").toString() ==
                new URI("http://localhost:5678/Inventory/Hardware/Query/x0c0s0b0n0").toString()
    }
}
