// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClient;
import org.apache.commons.lang3.tuple.ImmutablePair;

public interface RestRequester {
    void initialize(Logger logger, Requester config, RESTClient restClient);
    int initiateDiscovery(String xname);
    int getDiscoveryStatus();
    ImmutablePair<Integer, String> getHwInventory();
    ImmutablePair<Integer, String> getHwInventory(String xname);
}
