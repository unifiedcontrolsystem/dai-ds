// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import lombok.ToString;

@ToString
public class Requester {
    RestRequest initiateDiscovery;
    RestRequest getDiscoveryStatus;
    RestRequest getHwInventorySnapshot;
    RestRequest getHWInventoryUpdate;
    RestRequest getHWInventoryHistory;

    public Requester() {
        initiateDiscovery = new RestRequest();
        getDiscoveryStatus = new RestRequest();
        getHwInventorySnapshot = new RestRequest();
        getHWInventoryUpdate = new RestRequest();
        getHWInventoryHistory = new RestRequest();
    }
}
