// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import com.intel.dai.inventory.api.pojo.rqst.InventoryInfoRequester;
import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClient;
import org.apache.commons.lang3.tuple.ImmutablePair;

/**
 * This is interface uses rest API to interacts with the foreign inventory server.
 */
public interface ForeignServerInventoryRest {
    /**
     * Initializes logger, requester and client.
     * @param logger logs diagnostic traces
     * @param config contains details on rest api calls used to interact with the foreign server
     * @param restClient rest client object used for making rest api calls
     */
    void initialize(Logger logger, InventoryInfoRequester config, RESTClient restClient);

    /**
     * Initiates asynchronous inventory discovery at the specified foreign location.
     * @param foreignLocName foreign name of the root of a subtree in the HPC inventory hierarchy
     * @return 0 if discovery is started successfully, otherwise 1
     */
    int initiateDiscovery(String foreignLocName);

    /**
     * Polls the foreign server for the completion of the inventory discovery process.
     * @return 0 if all discovery processes have completed successfully, otherwise 1
     */
    int getDiscoveryStatus();

    /**
     * Gets the entire HW inventory.
     * @return status 0 and json containing the inventory if successful; otherwise status is 1
     */
    ImmutablePair<Integer, String> getHwInventory();

    /**
     * Gets the entire HW inventory at the specified location.
     * @param locationName DAI name of the root of a subtree in the HPC inventory hierarchy
     * @return status 0 and json containing the inventory if successful; otherwise status is 1
     */
    ImmutablePair<Integer, String> getHwInventory(String locationName);

    /**
     * Gets the hardware inventory history at the specified start start to the present.
     * @param startTime start time of the requested inventory history
     * @return status 0 and json containing the inventory history if successful; otherwise status is 1
     */
    ImmutablePair<Integer, String> getHWInventoryHistory(String startTime);
}
