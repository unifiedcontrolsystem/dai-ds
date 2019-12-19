// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsapi;

import java.time.Instant;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.properties.PropertyMap;

public interface InventorySnapshot {
    void storeInventorySnapshot(String location, Instant timestamp, String info) throws DataStoreException;
    PropertyMap retrieveRefSnapshot(String location) throws DataStoreException;
    PropertyMap retrieveSnapshot(long id) throws DataStoreException;
    void setReferenceSnapshot(int id) throws DataStoreException;
}
