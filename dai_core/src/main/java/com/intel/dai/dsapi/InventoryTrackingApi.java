// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsapi;

import java.time.Instant;

import com.intel.dai.exceptions.DataStoreException;
import com.intel.properties.PropertyMap;

public interface InventoryTrackingApi {
    void removeComputeNode(String location, Instant timestamp, String reqAdapterType, long reqWorkItemId)
            throws DataStoreException;
    void replaceComputeNode(String location, String serialNumber, String fruType, PropertyMap inventoryInfo,
                            Instant timestamp, String reqAdapterType, long reqWorkItemId) throws DataStoreException;

    void removeServiceNode(String location, Instant timestamp, String reqAdapterType, long reqWorkItemId)
            throws DataStoreException;
    void replaceServiceNode(String location, String serialNumber, String fruType, PropertyMap inventoryInfo,
                            Instant timestamp, String reqAdapterType, long reqWorkItemId) throws DataStoreException;
}