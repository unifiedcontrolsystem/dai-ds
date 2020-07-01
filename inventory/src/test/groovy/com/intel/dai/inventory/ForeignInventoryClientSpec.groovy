package com.intel.dai.inventory

import com.intel.dai.dsapi.HWInvDbApi
import com.intel.dai.inventory.api.HWInvDiscovery
import com.intel.logging.Logger
import org.apache.commons.lang3.tuple.ImmutablePair
import spock.lang.Specification

class ForeignInventoryClientSpec extends Specification {
    ForeignInventoryClient ts;
    void setup() {
        ts = new ForeignInventoryClient(Mock(Logger))
    }

    def "lastHWInventoryHistoryUpdate"() {
        expect: ts.lastHWInventoryHistoryUpdate(Mock(HWInvDbApi)) == null
    }
    def "toCanonicalHWInvJson"() {
        expect: ts.toCanonicalHWInvJson(null) == null
    }

    def "getForeignHWInvJson"() {
        expect: ts.getForeignHWInvJson(null) == null
    }

    def "getCanonicalHWInvJson"() {
        expect: ts.getCanonicalHWInvJson(null) == null
    }

    def "toCanonicalHWInvHistoryJson"() {
        expect: ts.toCanonicalHWInvHistoryJson(null) == null
    }

    def "getForeignHWInvHistoryJson"() {
        def hwDisc = Mock(HWInvDiscovery)
        hwDisc.queryHWInvHistory(_) >> QueryResult
        ts.hwInvDiscovery_ = hwDisc
        expect: ts.getForeignHWInvHistoryJson(null) == Result

        where:
        QueryResult                                 || Result
        new ImmutablePair<>(0, "History")           || "History"
        new ImmutablePair<>(1, "Doesn't matter")    || null
    }

    def "getCanonicalHWInvHistoryJson"() {
        def hwDisc = Mock(HWInvDiscovery)
        hwDisc.queryHWInvHistory(_) >> QueryResult
        ts.hwInvDiscovery_ = hwDisc
        expect: ts.getCanonicalHWInvHistoryJson(null) == null

        where:
        QueryResult                                 || Result
        new ImmutablePair<>(0, "{}")                || "{}"
        new ImmutablePair<>(1, "Doesn't matter")    || null
    }
}
