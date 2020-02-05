// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener

import com.intel.dai.AdapterInformation
import com.intel.dai.dsapi.BootState
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.logging.Logger
import spock.lang.Specification

class BenchmarkingSystemActionsSpec extends Specification {
    def underTest_
    void setup() {
        underTest_ = new BenchmarkingSystemActions(Mock(Logger), Mock(DataStoreFactory), Mock(AdapterInformation),
                Mock(NetworkListenerConfig))
    }

    def "Initialize"() {
        underTest_.initialize()
        expect: true
    }

    def "StoreNormalizedData"() {
        underTest_.storeNormalizedData("", "", 0L, 0.0)
        expect: true
    }

    def "StoreAggregatedData"() {
        underTest_.storeAggregatedData("", "", 0L, 0.0, 0.0, 0.0)
        expect: true
    }

    def "StoreRasEvent"() {
        for(int i = 0; i < 1002; i++)
            underTest_.storeRasEvent("","","",0L)
        expect: true
    }

    def "PublishNormalizedData"() {
        underTest_.publishNormalizedData("", "", "", 0L, 0.0)
        expect: true
    }

    def "PublishAggregatedData"() {
        underTest_.publishAggregatedData("", "", "", 0L, 0.0, 0.0, 0.0)
        expect: true
    }

    def "PublishRasEvent"() {
        underTest_.publishRasEvent("", "", "", "", 0L)
        expect: true
    }

    def "PublishBootEvent"() {
        underTest_.publishBootEvent("", BootState.NODE_OFFLINE, "", 0L)
        expect: true
    }

    def "ChangeNodeStateTo"() {
        underTest_.changeNodeStateTo(BootState.NODE_OFFLINE, "", 0L, false)
        expect: true
    }

    def "ChangeNodeBootImageId"() {
        underTest_.changeNodeBootImageId("", "")
        expect: true
    }

    def "UpsertBootImages"() {
        underTest_.upsertBootImages(null)
        expect: true
    }

    def "LogFailedToUpdateNodeBootImageId"() {
        underTest_.logFailedToUpdateNodeBootImageId("", "")
        expect: true
    }

    def "LogFailedToUpdateBootImageInfo"() {
        underTest_.logFailedToUpdateBootImageInfo("")
        expect: true
    }

    def "upsertHWInventory"() {
        underTest_.upsertHWInventory("")
        expect: true
    }

    def "Close"() {
        underTest_.close()
        expect: true
    }
}
