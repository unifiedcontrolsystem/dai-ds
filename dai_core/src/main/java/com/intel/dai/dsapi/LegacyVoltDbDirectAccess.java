// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import org.voltdb.client.Client;

/**
 * Description of interface LegacyVoltDbAccess.
 */
public interface LegacyVoltDbDirectAccess {
    Client getVoltDbClient();
}
