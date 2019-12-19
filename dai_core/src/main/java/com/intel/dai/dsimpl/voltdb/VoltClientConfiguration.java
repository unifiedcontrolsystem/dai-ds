// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import org.voltdb.client.ClientConfig;

class VoltClientConfiguration {

    private static ClientConfig config;

    private VoltClientConfiguration() { }

    static ClientConfig getVoltClientConfiguration() {
        if(config == null) {
            config = new ClientConfig("", "", null);
            config.setReconnectOnConnectionLoss(true);
        }
        return config;
    }
}
