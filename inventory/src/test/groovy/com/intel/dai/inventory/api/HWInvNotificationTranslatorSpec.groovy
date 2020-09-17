// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api

import com.intel.logging.Logger;
import spock.lang.Specification

class HWInvNotificationTranslatorSpec extends Specification {
    def ts

    def setup() {
        ts = new HWInvNotificationTranslator(Mock(Logger))
    }

    def "HWInvNotificationTranslator - failures"() {
        expect:
        ts.toPOJO(scnJson) == result

        where:
        scnJson || result
        null    || null
        ""      || null
        '{'     || null
        '['     || null
        '[]'    || null
        "cow"   || null
    }
    def "HWInvNotificationTranslator - edge cases"() {
        expect: ts.toPOJO(scnJson).toString() == result

        where:
        scnJson                                 || result
        "{}"                                    || "ForeignHWInvChangeNotification(Components=[], State=)"
        '{Components: [], State: "On"}'         || "ForeignHWInvChangeNotification(Components=[], State=On)"
        '{Components: [], State: "Cow"}'        || "ForeignHWInvChangeNotification(Components=[], State=Cow)"
        '{Components: [""], State: "On"}'       || "ForeignHWInvChangeNotification(Components=[], State=On)"
        '{Components: ["", ""], State: "On"}'   || "ForeignHWInvChangeNotification(Components=[, ], State=On)"
    }
    def "HWInvNotificationTranslator - common usages"() {
        expect: ts.toPOJO(scnJson).toString() == result

        where:
        scnJson                                     || result
        '{Components: ["n0"], State: "Off"}'        || "ForeignHWInvChangeNotification(Components=[n0], State=Off)"
        '{Components: ["n0", "n1"], State: "On"}'   || "ForeignHWInvChangeNotification(Components=[n0, n1], State=On)"
    }

    // Notice that missing fields do not result in failure, and missing fields are given default values
    def "HWInvNotificationTranslator - negative"() {
        expect: ts.toPOJO(scnJson).toString() == result

        where:
        scnJson                  || result
        '{Cows: ["n0", "n1"]}'   || "ForeignHWInvChangeNotification(Components=[], State=)"
    }
}
