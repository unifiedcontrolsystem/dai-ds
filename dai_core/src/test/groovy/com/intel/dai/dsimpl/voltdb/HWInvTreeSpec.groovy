// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb

import com.intel.dai.dsapi.HWInvLoc
import com.intel.dai.dsapi.HWInvTree

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
