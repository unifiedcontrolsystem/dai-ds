// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api

import com.intel.dai.dsapi.HWInvUtil
import com.intel.logging.Logger

class HWInvDiscoveryCLISpec extends spock.lang.Specification {
    HWInvDiscoveryCLI app

    def setup() {
        HWInvDiscoveryCLI.log = Mock(Logger)
        app = new HWInvDiscoveryCLI()
    }

    def "run"() {
        expect: app.run("") == 1
    }
    def "run - negative"() {
        app.mode = "notAMode"
        expect: app.run() == 1
    }
    def "createTokenProvider" () {
        given: app.tokenProvider_ = null
        when: app.createTokenProvider(null, null)
        then: app.tokenProvider_ == null
    }
    def "createRequester" () {
        given: app.requester_ = null
        when: app.createRequester(null, null, null)
        then: app.requester_ == null
    }
    def "initiateDiscovery" () {
        given: app.requester_ = null
        expect: app.initiateDiscovery() == 1
    }
    def "pollForDiscoveryProgress" () {
        given: app.requester_ = null
        expect: app.pollForDiscoveryProgress() == 1
    }
    def "queryHWInvTree" () {
        given: app.requester_ = null
        expect: app.queryHWInvTree() == 1
    }
//    def
}
