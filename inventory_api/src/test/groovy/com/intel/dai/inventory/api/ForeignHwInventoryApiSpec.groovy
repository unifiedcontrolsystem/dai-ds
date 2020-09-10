// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api

import com.intel.dai.dsapi.HWInvUtil
import com.intel.dai.inventory.api.pojo.fru.ForeignFRU
import com.intel.dai.inventory.api.pojo.fru.info.*
import com.intel.dai.inventory.api.pojo.loc.ForeignHWInvByLoc
import com.intel.dai.inventory.api.pojo.loc.info.*
import com.intel.dai.inventory.api.pojo.rqst.InventoryInfoRequester
import com.intel.logging.Logger
import com.intel.networking.restclient.BlockingResult
import com.intel.networking.restclient.RESTClient
import com.intel.networking.restclient.RequestInfo
import spock.lang.Specification

class ForeignHwInventoryApiSpec extends Specification {
    ForeignHwInventoryApi invRequester;

    def log = Mock(Logger)
    def config = new InventoryInfoRequester()
    def restClient = Mock(RESTClient)

    def setup() {
        invRequester = new ForeignHwInventoryApi();
        invRequester.util = Mock(HWInvUtil)
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
        def res = invRequester.getHwInventory().left

        then:
        res == 1
    }

    def "getHwInventory - xname"() {
        setup:
        invRequester.initialize(log, config, restClient)

        when:
        def res = invRequester.getHwInventory("x0").left

        then:
        res == 1
    }

    def "getHWInventoryHistory"() {
        setup:
        invRequester.initialize(log, config, restClient)

        when:
        def res = invRequester.getHWInventoryHistory("t0").left

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

    def "makeHistoryQuery" () {
        invRequester.logger = log
        invRequester.config = new InventoryInfoRequester();
        invRequester.config.getHWInventoryHistory.endpoint = "http://localhost:5678/"
        invRequester.config.getHWInventoryHistory.resource = "Inventory/Hardware/History"
        expect: invRequester.makeHistoryQuery("2019-01-02T15:04:05Z07:00").toString() ==
                new URI("http://localhost:5678/Inventory/Hardware/History?start_time=2019-01-02T15:04:05Z07:00").
                        toString()
    }

    def "validBlockingResult"() {
        invRequester.logger = log
        def uri = new URI("")
        def blockingResult = new BlockingResult(code, responseDocument, requestInfo)
        expect: invRequester.interpretePollForDiscoveryProgressResult(uri, blockingResult) == status

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
        invRequester.interpreteQueryHWInvQueryResult(uri, blockingResult).left == status

        where:
        code    | responseDocument      | requestInfo                       || status
        200     | "hw inv tree"         | new RequestInfo(null, null, null) || 0
        400     | "hw inv tree"         | new RequestInfo(null, null, null) || 1
    }
}

class LocPojoSpec extends Specification {
    ForeignHWInvByLoc ts

    def setup() {
        ts = new ForeignHWInvByLoc()
    }

    def "info - 0"() {
        ts.MemoryLocationInfo = MemInfo
        ts.ProcessorLocationInfo = ProcInfo
        ts.NodeLocationInfo = NodeInfo

        expect: ts.info() != null

        where:
        MemInfo                     | ProcInfo                          | NodeInfo
        null                        | null                              | null
        new MemoryLocationInfoBlk() | null                              | null
        null                        | new ProcessorLocationInfoBlk()    | null
        null                        | null                              | new NodeLocationInfoBlk()
    }

    def "info - 1"() {
        ts.NodeEnclosureLocationInfo = NodeEnclosureInfo
        ts.HSNBoardLocationInfo = HSNBoardInfo
        ts.DriveLocationInfo = DriveInfo

        expect: ts.info() != null

        where:
        NodeEnclosureInfo                   | HSNBoardInfo                  | DriveInfo
        new NodeEnclosureLocationInfoBlk()  | null                          | null
        null                                | new HSNBoardLocationInfoBlk() | null
        null                                | null                          | new DriveLocationInfoBlk()
    }
}

class FRUPojoSpec extends Specification {
    ForeignFRU ts

    def setup() {
        ts = new ForeignFRU()
    }

    def "info - 0"() {
        ts.MemoryFRUInfo = MemInfo
        ts.ProcessorFRUInfo = ProcInfo
        ts.NodeFRUInfo = NodeInfo

        expect: ts.info() != null

        where:
        MemInfo                 | ProcInfo                  | NodeInfo
        null                    | null                      | null
        new MemoryFRUInfoBlk()  | null                      | null
        null                    | new ProcessorFRUInfoBlk() | null
        null                    | null                      | new NodeFRUInfoBlk()
    }
    def "info - 1"() {
        ts.NodeEnclosureFRUInfo = NodeEnclosureInfo
        ts.HSNBoardFRUInfo = HSNBoardInfo
        ts.DriveFRUInfo = DriveInfo

        expect: ts.info() != null

        where:
        NodeEnclosureInfo               | HSNBoardInfo              | DriveInfo
        new NodeEnclosureFRUInfoBlk()   | null                      | null
        null                            | new HSNBoardFRUInfoBlk()  | null
        null                            | null                      | new DriveFRUInfoBlk()
    }
}
