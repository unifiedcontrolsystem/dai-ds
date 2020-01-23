// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClient;

public interface RestRequester {
    void initialize(Logger logger, Requester config, RESTClient restClient);
    int initiateDiscovery(String xname);
    int getDiscoveryStatus();
    int getHwInventory(String outputFile);
    int getHwInventory(String xname, String outputFile);
}
