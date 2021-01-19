// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsapi;

import com.intel.dai.exceptions.DataStoreException;

import java.util.Map;
import java.util.List;
import java.util.Set;

public interface BootImage {

    String addBootImageProfile(Map<String, String> parameters) throws DataStoreException;
    String editBootImageProfile(Map<String, String> parameters) throws DataStoreException;
    String deleteBootImageProfile(String profileId) throws DataStoreException;
    Map<String, String> retrieveBootImageProfile(String profileId) throws DataStoreException;
    List<String> listBootImageProfiles() throws DataStoreException;
    Map<String, String> getComputeNodesBootImageId(Set<String> computeNodes);
    String updateComputeNodeBootImageId(String lctn, String id, String adapterType) throws DataStoreException;
}
