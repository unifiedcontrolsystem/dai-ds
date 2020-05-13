// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api


import com.intel.logging.Logger
import spock.lang.Specification

class HWInvDiscoverySpec extends Specification {
    def "createTokenProvider" () {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        given: HWInvDiscovery.tokenProvider_ = null
        when: hwInvDiscovery.createTokenProvider(null, null)
        then: HWInvDiscovery.tokenProvider_ == null
    }
    def "createRequester" () {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        given: HWInvDiscovery.requester_ = null
        when: hwInvDiscovery.createRequester(null, null, null)
        then: HWInvDiscovery.requester_ == null
    }
    def "initiateDiscovery" () {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        given: HWInvDiscovery.requester_ = null
        expect: hwInvDiscovery.initiateDiscovery() == 1
    }
    def "pollForDiscoveryProgress" () {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        given: HWInvDiscovery.requester_ = null
        expect: hwInvDiscovery.pollForDiscoveryProgress() == 1
    }
    def "queryHWInvTree - entire tree" () {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        given: HWInvDiscovery.requester_ = null
        expect: hwInvDiscovery.queryHWInvTree().getLeft() == 1
    }
    def "queryHWInvTree - partial tree" () {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        given: HWInvDiscovery.requester_ = null
        expect: hwInvDiscovery.queryHWInvTree(null).getLeft() == 1
    }
}
