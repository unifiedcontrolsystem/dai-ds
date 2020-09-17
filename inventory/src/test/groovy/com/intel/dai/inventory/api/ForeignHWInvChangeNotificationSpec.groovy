// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api

import com.intel.dai.inventory.api.pojo.scn.ForeignHWInvChangeNotification
import spock.lang.Specification

class ForeignHWInvChangeNotificationSpec extends Specification {
    def "ToString"() {
        def notif0 = new ForeignHWInvChangeNotification()
        def notif1 = new ForeignHWInvChangeNotification()

        notif0.Components = new ArrayList<>()
        notif1.Components = new ArrayList<>()

        expect:
        notif0.toString() == notif0.toString()
        notif1.toString() == notif1.toString()
        notif0.toString() == notif1.toString()
    }
}
