package com.intel.dai.dsimpl.voltdb

import com.intel.dai.dsapi.HWInvSlot
import com.intel.dai.dsapi.HWInvTree

class HWInvTreeSpec extends spock.lang.Specification {
    def "Test HWInvTree toString" () {
        def t0 = new HWInvTree()
        def t1 = new HWInvTree()
        expect: t0.toString() == t1.toString()
    }
    def "Test HWInvSlot toString" () {
        def s0 = new HWInvSlot()
        def s1 = new HWInvSlot()
        expect: s0.toString() == s1.toString()
    }
}
