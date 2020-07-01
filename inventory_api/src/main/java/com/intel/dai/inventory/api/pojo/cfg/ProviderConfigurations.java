// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.cfg;

import com.intel.dai.inventory.api.pojo.rqst.InventoryInfoRequester;
import lombok.ToString;

@ToString
public class ProviderConfigurations {
    public TokenAuthProvider tokenAuthProvider;
    public InventoryInfoRequester inventoryInfoRequester;

    public ProviderConfigurations() {
        tokenAuthProvider = new TokenAuthProvider();
        inventoryInfoRequester = new InventoryInfoRequester();
    }
}
