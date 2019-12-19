// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsapi;

import com.intel.dai.exceptions.DataStoreException;
import com.intel.properties.PropertyArray;

import java.util.Set;
import java.util.Map;

public interface Configuration {
    PropertyArray getComputeNodeConfiguration() throws DataStoreException;
    PropertyArray getServiceNodeConfiguration() throws DataStoreException;
    PropertyArray getRackConfiguration() throws DataStoreException;
}
