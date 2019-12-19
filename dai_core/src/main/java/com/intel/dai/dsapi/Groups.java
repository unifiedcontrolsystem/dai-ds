// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsapi;


import com.intel.dai.exceptions.DataStoreException;

import java.util.Set;

public interface Groups {
    String addDevicesToGroup(String groupName, Set<String> device) throws DataStoreException;

    String deleteDevicesFromGroup(String groupName, Set<String> device) throws DataStoreException;

    Set<String> getDevicesFromGroup(String groupName) throws DataStoreException;

    Set<String> listGroups() throws DataStoreException;
}
