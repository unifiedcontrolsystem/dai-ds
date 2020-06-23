// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring

import com.intel.dai.AdapterInformation
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.logging.Logger
import spock.lang.Specification

class ProviderMonitoringNetworkForeignBusSpec extends Specification {
    def "Test ctor"() {
        new ProviderMonitoringNetworkForeignBus(Mock(Logger), Mock(DataStoreFactory), Mock(AdapterInformation),
                "./build/tmp/bmf.txt", 100L)
        expect: true
    }
}
