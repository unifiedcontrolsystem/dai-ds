// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api

import com.intel.dai.inventory.api.pojo.cfg.HWDiscoverySession
import com.intel.dai.inventory.api.pojo.cfg.ProviderClassMap
import com.intel.dai.inventory.api.pojo.cfg.ProviderConfigurations
import com.intel.dai.inventory.api.pojo.cfg.TokenAuthProvider
import com.intel.dai.inventory.api.pojo.rqst.InventoryInfoRequester
import com.intel.dai.inventory.api.pojo.rqst.RestMethod
import spock.lang.Specification

class HWDiscoverySessionSpec extends Specification {
    def "ToString" () {
        expect:
        new HWDiscoverySession().toString() == new HWDiscoverySession().toString()
        new ProviderClassMap().toString() == new ProviderClassMap().toString()
        new TokenAuthProvider().toString() == new TokenAuthProvider().toString()
        new ProviderConfigurations().toString() == new ProviderConfigurations().toString()
        new InventoryInfoRequester().toString() == new InventoryInfoRequester().toString()
        new RestMethod().toString() == new RestMethod().toString()
    }
}
