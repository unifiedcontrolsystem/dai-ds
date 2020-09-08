// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory

import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.HWInvDbApi
import com.intel.dai.dsapi.HWInvHistoryEvent
import com.intel.dai.dsapi.HWInvUtil
import com.intel.logging.Logger
import spock.lang.Specification

class DatabaseSynchronizerSpec extends Specification {
    def ts = new DatabaseSynchronizer(Mock(Logger), Mock(DataStoreFactory))

    def "initializeDependencies"() {
        when: ts.initializeDependencies()
        then:
        ts.util_ != null
        ts.foreignInventoryDatabaseClient_ != null
    }

    def "extractChangedNodeLocations -- empty input"() {
        expect:
        ts.extractChangedNodeLocations([]) == [:]
    }

    def "extractChangedNodeLocations"() {
        def evt = new HWInvHistoryEvent()
        evt.ID = ID
        evt.Timestamp = Timestamp
        def input = new ArrayList<HWInvHistoryEvent>()
        input.add(evt)

        expect: ts.extractChangedNodeLocations(input) == ExpectedResult

        where:
        ID                  | Timestamp || ExpectedResult
        'cow'               | 'ts0'     || [:]
        'R0-CB0-CN0'        | 'ts0'     || ['R0-CB0-CN0': 'ts0']
        'R0-CB0-CN0-DIMM0'  | 'ts0'     || ['R0-CB0-CN0': 'ts0']
        'x0b0n0'            | 'ts1'     || ['x0b0n0': 'ts1']
        'x0b0n0d0'          | 'ts1'     || ['x0b0n0': 'ts1']
    }

    def "ingestChangedNodeLocationSnapshots -- empty"() {
        expect: ts.ingestChangedNodeLocationSnapshots([]) == 0
    }

    def "ingestChangedNodeLocationSnapshots"() {
        ts.foreignInventoryDatabaseClient_ = Mock(ForeignInventoryClient)
        ts.foreignInventoryDatabaseClient_.getCanonicalHWInvJson(_) >> null // guarantees that expect result is always 0
        ts.util_ = Mock(HWInvUtil)
        ts.util_.head(*_) >> ""

        def evt = new HWInvHistoryEvent()
        evt.ID = ID
        evt.Timestamp = Timestamp
        def input = new ArrayList<HWInvHistoryEvent>()
        input.add(evt)

        expect: ts.ingestChangedNodeLocationSnapshots(input) == ExpectedResult

        where:
        ID                  | Timestamp || ExpectedResult
        'cow'               | 'ts0'     || 0
        'R0-CB0-CN0'        | 'ts0'     || 0
        'R0-CB0-CN0-DIMM0'  | 'ts0'     || 0
        'x0b0n0'            | 'ts1'     || 0
        'x0b0n0d0'          | 'ts1'     || 0
    }

    def "ingestCookedNodes"() {
        ts.onlineInventoryDatabaseClient_ = Mock(HWInvDbApi)
        ts.onlineInventoryDatabaseClient_.getCanonicalHWInvJson(_) >> 0 // guarantees that expect result is always 0

        expect:
        ts.ingestCookedNodes([:]) == 0
    }
}
