// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api

import com.intel.dai.inventory.api.pojo.cfg.HWDiscoverySession
import com.intel.logging.Logger
import com.intel.networking.restclient.BlockingResult
import com.intel.networking.restclient.RESTClient
import com.intel.networking.restclient.RequestInfo
import com.intel.networking.restclient.ResponseCallback
import com.intel.networking.restclient.SSEEvent
import spock.lang.Specification

class FakeRestClient extends RESTClient {
    FakeRestClient(Logger logger) {super(logger)}
    void doSSERequest(RequestInfo request, ResponseCallback callback, SSEEvent eventsCallback) {}
    BlockingResult doRESTRequest(RequestInfo request) { return null; }
}

class HWInvDiscoverySpec extends Specification {
    def "createTokenProvider"() {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        given: HWInvDiscovery.tokenProvider_ = null
        when: hwInvDiscovery.createTokenProvider(null, null)
        then: HWInvDiscovery.tokenProvider_ == null
    }
    
    def "createRequester"() {
        HWDiscoverySession sess = new HWDiscoverySession()
        sess.providerClassMap.requester = null

        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))

        when: hwInvDiscovery.createRequester(sess, restClient)
        then: HWInvDiscovery.requester_ == result

        where:
        requesterClassName                                          | restClient                        || result
        null                                                        | new FakeRestClient(Mock(Logger))  || null
        "com.intel.dai.inventory.api.ForeignHwInventoryRequester"   | null                              || null
    }

    def "initiateDiscovery"() {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        given: HWInvDiscovery.requester_ = null
        expect: hwInvDiscovery.initiateDiscovery() == 1
    }
    
    def "pollForDiscoveryProgress"() {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        given: HWInvDiscovery.requester_ = null
        expect: hwInvDiscovery.pollForDiscoveryProgress() == 1
    }
    
    def "queryHWInvTree - entire tree"() {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        given: HWInvDiscovery.requester_ = null
        expect: hwInvDiscovery.queryHWInvTree().left == 1
    }
    
    def "queryHWInvTree - partial tree"() {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        given: HWInvDiscovery.requester_ = null
        expect: hwInvDiscovery.queryHWInvTree(null).left == 1
    }

    def "createRestClient()"() {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        when: hwInvDiscovery.createRestClient()
        then: thrown Exception
    }
    
    def "initialize()"() {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        when: hwInvDiscovery.initialize()
        then: thrown Exception
    }
    
    def "toHWDiscoverySession()"() {
        HWInvDiscovery hwInvDiscovery = new HWInvDiscovery(Mock(Logger))
        when: hwInvDiscovery.toHWDiscoverySession(null)
        then: thrown Exception
    }
}
