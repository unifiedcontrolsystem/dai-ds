// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api


import com.intel.logging.Logger

class HWInvDiscoverySpec extends spock.lang.Specification {
    def "createTokenProvider" () {
        given: HWInvDiscovery.tokenProvider_ = null
        when: HWInvDiscovery.createTokenProvider(null, null)
        then: HWInvDiscovery.tokenProvider_ == null
    }
    def "createRequester" () {
        given: HWInvDiscovery.requester_ = null
        when: HWInvDiscovery.createRequester(null, null, null)
        then: HWInvDiscovery.requester_ == null
    }
    def "initiateDiscovery" () {
        given: HWInvDiscovery.requester_ = null
        expect: HWInvDiscovery.initiateDiscovery() == 1
    }
    def "pollForDiscoveryProgress" () {
        given: HWInvDiscovery.requester_ = null
        expect: HWInvDiscovery.pollForDiscoveryProgress() == 1
    }
    def "queryHWInvTree - entire tree" () {
        HWInvDiscovery.log = Mock(Logger)
        given: HWInvDiscovery.requester_ = null
        expect: HWInvDiscovery.queryHWInvTree().getLeft() == 1
    }
    def "queryHWInvTree - partial tree" () {
        HWInvDiscovery.log = Mock(Logger)
        given: HWInvDiscovery.requester_ = null
        expect: HWInvDiscovery.queryHWInvTree(null).getLeft() == 1
    }
}

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
}
