// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsapi;

import com.intel.dai.exceptions.DataStoreException;

public interface DataLoaderApi {
    // Retrieves the Nearline tier's configuration property indicating if it's in a valid state to use for populating
    // the Online tier.
    boolean isNearlineTierValid() throws DataStoreException;

    // Sets the Nearline tier's configuration property that indicates if it's in a valid state to use for populating
    // the Online tier.
    void setNearlineTierValid(boolean valid) throws DataStoreException;

    // Populates the Online tier with the latest state and configuration available in the Nearline tier.  This is meant
    // to be performed once during UCS startup after the Online tier's schema has been created but is not yet
    // populated.
    void populateOnlineTierFromNearlineTier() throws DataStoreException;

    void dropSnapshotTablesFromNearlineTier();

    void disconnectAll();
}
