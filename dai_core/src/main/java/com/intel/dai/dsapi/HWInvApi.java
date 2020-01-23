// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import com.intel.dai.exceptions.DataStoreException;

import java.io.IOException;

public interface HWInvApi {
    void initialize();
    int ingest(String inputJsonFileName) throws IOException, InterruptedException, DataStoreException;
    void delete(String locationName) throws IOException, DataStoreException;
    HWInvTree allLocationsAt(String rootLocationName, String outputJsonFileName) throws IOException, DataStoreException;
}
