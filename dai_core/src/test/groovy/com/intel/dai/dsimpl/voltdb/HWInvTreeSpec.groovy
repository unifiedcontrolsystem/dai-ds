// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi

class HWInvTreeSpec extends spock.lang.Specification {
    def "Test HWInvTree toString" () {
        def t0 = new HWInvTree()
        def t1 = new HWInvTree()

        expect: t0.toString() == t1.toString()
    }
    def "Test HWInvLoc toString" () {
        def s0 = new HWInvLoc()
        def s1 = new HWInvLoc()
        expect: s0.toString() == s1.toString()
    }
}

class HWInvHistorySpec extends spock.lang.Specification {
    def "Test HWInvHistory toString" () {
        def t0 = new HWInvHistory()
        def t1 = new HWInvHistory()

        expect: t0.toString() == t1.toString()
    }
    def "Test HWInvHistoryEvent toString" () {
        def s0 = new HWInvHistoryEvent()
        def s1 = new HWInvHistoryEvent()
        expect: s0.toString() == s1.toString()
    }
}
