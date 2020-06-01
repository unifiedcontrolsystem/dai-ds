// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api

import com.intel.logging.Logger
import com.intel.networking.restclient.BlockingResult
import com.intel.networking.restclient.RESTClient
import com.intel.networking.restclient.RequestInfo
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
        def res = invRequester.getHwInventory().getLeft()

        then:
        res == 1
    }

    def "getHwInventory - xname"() {
        setup:
        invRequester.initialize(log, config, restClient)

        when:
        def res = invRequester.getHwInventory("x0").getLeft()

        then:
        res == 1
    }

    def "getHWInventoryHistory"() {
        setup:
        invRequester.initialize(log, config, restClient)

        when:
        def res = invRequester.getHWInventoryHistory("t0", "t1").getLeft()

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

    def "makeQuery" () {
        def uri = new URI("http://localhost:5678/Inventory/Hardware/History")
        expect: invRequester.makeQuery(uri, "start_time", "2019-01-02T15:04:05Z07:00",
                "end_time", "2019-02-02T15:04:05Z07:00").toString() ==
                new URI("http://localhost:5678/Inventory/Hardware/History?start_time=2019-01-02T15:04:05Z07:00" +
                        "&end_time=2019-02-02T15:04:05Z07:00").toString()
    }

    def "validBlockingResult"() {
        invRequester.logger = log
        def uri = new URI("")
        def blockingResult = new BlockingResult(code, responseDocument, requestInfo)
        expect:
        invRequester.interpretePollForDiscoveryProgressResult(uri, blockingResult) == status

        where:
        code    | responseDocument      | requestInfo                       || status
        200     | "Complete"            | new RequestInfo(null, null, null) || 0
        200     | "Complete InProgress" | new RequestInfo(null, null, null) || 1
        200     | ""                    | new RequestInfo(null, null, null) || 1
        200     | null                  | new RequestInfo(null, null, null) || 1
        200     | "Complete"            | null                              || 1
        200     | null                  | null                              || 1
        500     | "Complete"            | null                              || 1
    }

    def "interpretePollForDiscoveryProgressResult"() {
        invRequester.logger = log
        def uri = new URI("")
        def blockingResult = new BlockingResult(code, responseDocument, requestInfo)
        expect:
        invRequester.interpretePollForDiscoveryProgressResult(uri, blockingResult) == status

        where:
        code    | responseDocument      | requestInfo                       || status
        200     | "Complete"            | new RequestInfo(null, null, null) || 0
        200     | "Complete InProgress" | new RequestInfo(null, null, null) || 1
        200     | ""                    | new RequestInfo(null, null, null) || 1
        500     | "Complete"            | null                              || 1
    }

    def "interpretedInitiateDiscoveryServerResult"() {
        invRequester.logger = log
        def uri = new URI("")
        def blockingResult = new BlockingResult(code, responseDocument, requestInfo)
        expect:
        invRequester.interpretedInitiateDiscoveryServerResult(uri, blockingResult) == status

        where:
        code    | responseDocument  | requestInfo                       || status
        200     | "Whatever"        | new RequestInfo(null, null, null) || 0
        400     | "Whatever"        | new RequestInfo(null, null, null) || 1
    }

    def "interpreteQueryHWInvQueryResult"() {
        invRequester.logger = log
        def uri = new URI("")
        def blockingResult = new BlockingResult(code, responseDocument, requestInfo)
        expect:
        invRequester.interpreteQueryHWInvQueryResult(uri, blockingResult).getLeft() == status

        where:
        code    | responseDocument      | requestInfo                       || status
        200     | "hw inv tree"         | new RequestInfo(null, null, null) || 0
        400     | "hw inv tree"         | new RequestInfo(null, null, null) || 1
    }
}
