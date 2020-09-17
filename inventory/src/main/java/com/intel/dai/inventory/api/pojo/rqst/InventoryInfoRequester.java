// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.rqst;

import lombok.ToString;

@ToString
public class InventoryInfoRequester {
    public RestMethod initiateDiscovery;
    public RestMethod getDiscoveryStatus;
    public RestMethod getHwInventorySnapshot;
    public RestMethod getHWInventoryUpdate;
    public RestMethod getHWInventoryHistory;

    public InventoryInfoRequester() {
        initiateDiscovery = new RestMethod();
        getDiscoveryStatus = new RestMethod();
        getHwInventorySnapshot = new RestMethod();
        getHWInventoryUpdate = new RestMethod();
        getHWInventoryHistory = new RestMethod();
    }
}
