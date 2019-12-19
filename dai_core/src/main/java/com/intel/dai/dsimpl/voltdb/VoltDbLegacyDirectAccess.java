// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.LegacyVoltDbDirectAccess;
import org.voltdb.client.Client;

/**
 * Description of class VoltDbLegacyAccess.
 */
public class VoltDbLegacyDirectAccess implements LegacyVoltDbDirectAccess {
    public VoltDbLegacyDirectAccess(String[] servers) {
        if(servers != null && servers.length > 0)
            servers_ = servers;
        else
            servers_ = new String[] { "localhost" };
    }

    @Override
    public Client getVoltDbClient() {
        VoltDbClient.initializeVoltDbClient(servers_);
        return VoltDbClient.getVoltClientInstance();
    }

    private final String[] servers_;
}
