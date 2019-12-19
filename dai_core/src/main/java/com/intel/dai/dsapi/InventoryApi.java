// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import com.intel.dai.exceptions.DataStoreException;

/**
 * Description of interface InventoryApi.
 */
public interface InventoryApi {
    void initialize();
    String getNodesInvInfoFromDb(String sTempLctn) throws DataStoreException;
}
