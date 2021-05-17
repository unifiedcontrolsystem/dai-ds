// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsapi;

import com.intel.dai.exceptions.DataStoreException;

import java.util.List;
import java.util.Map;

/**
 * Description of interface DbStatusApi.
 */
public interface NodeInformation {
    /**
     * Get state of a compute node
     *
     * @param lctn location of node to get state from
     * @return String with node state
     */
    String getComputeNodeState(String lctn) throws DataStoreException;

    Map<String, String> getComputeHostnameFromLocationMap() throws DataStoreException;
    Map<String, String> getServiceHostnameFromLocationMap() throws DataStoreException;
    Map<String, String> getComputeNodeLocationFromHostnameMap() throws DataStoreException;
    Map<String, String> getServiceNodeLocationFromHostnameMap() throws DataStoreException;

    Map<String, Long> getComputeNodeSequenceNumberFromLocationMap() throws DataStoreException;
    Map<String, String> getAggregatorLocationFromNodeLocationMap() throws DataStoreException;
    Map<String, NodeIpAndBmcIp> getNodeAndBmcIPsFromLocationMap() throws DataStoreException;
    Map<String, NodeIpAndBmcIp> getComputeNodeAndBmcIPsFromLocationMap() throws DataStoreException;

    boolean isComputeNodeLocation(String location) throws DataStoreException;
    boolean isComputeNodeHostname(String location) throws DataStoreException;
    boolean isServiceNodeLocation(String location) throws DataStoreException;
    boolean isServiceNodeHostname(String location) throws DataStoreException;
    String getNodesBmcIpAddress(String location) throws DataStoreException;
    String getNodesIpAddress(String location) throws DataStoreException;
    String getComputeNodeLocationFromHostname(String location) throws DataStoreException;
    String getServiceNodeLocationFromHostname(String location) throws DataStoreException;

    List<String> getComputeNodeLocations() throws DataStoreException;
    List<String> getServiceNodeLocations() throws DataStoreException;
    List<String> getNodeLocations() throws DataStoreException;
}
