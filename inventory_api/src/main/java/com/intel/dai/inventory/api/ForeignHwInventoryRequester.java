// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;


import com.intel.dai.dsapi.HWInvUtil;
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.dai.inventory.api.pojo.rqst.InventoryInfoRequester;
import com.intel.logging.Logger;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * This class implements the rest client API that interacts with the foreign inventory server.
 */
class ForeignHwInventoryRequester implements ForeignServerInventoryRest {
    public ForeignHwInventoryRequester() {}

    /**
     * Initializes logger, requester and client.
     * @param logger logs diagnostic traces
     * @param config contains details on rest api calls used to interact with the foreign server
     * @param restClient rest client object used for making rest api calls
     */
    @Override
    public void initialize(Logger logger, InventoryInfoRequester config, RESTClient restClient) {
        this.logger = logger;
        this.config = config;
        this.restClient = restClient;
        this.util = new HWInvUtilImpl(logger);
    }

    /**
     * Initiates asynchronous inventory discovery at the specified foreign location.
     * @param foreignLocName foreign name of the root of a subtree in the HPC inventory hierarchy
     * @return 0 if discovery is started successfully, otherwise 1
     */
    @Override
    public int initiateDiscovery(String foreignLocName) {
        try {
            URI uri = makeUri(config.initiateDiscovery.endpoint, config.initiateDiscovery.resource);
            logger.info("uri: %s", uri.toString());
            String payload = String.format("{\"xnames\": [\"%s\"], \"force\": false}", foreignLocName);
            BlockingResult result = restClient.postRESTRequestBlocking(uri, payload);
            return interpretedInitiateDiscoveryServerResult(uri, result);
        } catch (URISyntaxException e) {
            logger.fatal("Cannot create URI to initiate discovery");
        } catch (RESTClientException e) {
            logger.fatal("postRESTRequestBlocking failure");
        }
        return 1;
    }

    /**
     * Polls the foreign server for the completion of the inventory discovery process.
     * @return 0 if all discovery processes have completed successfully, otherwise 1
     */
    @Override
    public int getDiscoveryStatus() {
        try {
            URI uri = makeUri(config.getDiscoveryStatus.endpoint, config.getDiscoveryStatus.resource);
            logger.info("uri: %s", uri.toString());
            BlockingResult result = restClient.getRESTRequestBlocking(uri);
            return interpretePollForDiscoveryProgressResult(uri, result);
        } catch (URISyntaxException e) {
            logger.fatal("Cannot create URI to initiate discovery");
        } catch (RESTClientException e) {
            logger.fatal("getRESTRequestBlocking failure");
        }
        return 1;
    }

    /**
     * Gets the entire HW inventory.
     * @return status 0 and json containing the inventory if successful; otherwise status is 1
     */
    @Override
    public ImmutablePair<Integer, String> getHwInventory() {
        try {
            URI uri = makeUri(config.getHwInventorySnapshot.endpoint, config.getHwInventorySnapshot.resource);
            logger.info("HWI:%n  uri: %s", uri.toString());
            BlockingResult result = restClient.getRESTRequestBlocking(uri);
            return interpreteQueryHWInvQueryResult(uri, result);
        } catch (URISyntaxException e) {
            logger.fatal("HWI:%n  Cannot create URI to get HW inventory");
        } catch (RESTClientException e) {
            logger.fatal("HWI:%n  getRESTRequestBlocking failure");
        }
        return new ImmutablePair<>(1, "");
    }

    /**
     * Gets the entire HW inventory at the specified location.
     * @param locationName foreign name of the root of a subtree in the HPC inventory hierarchy
     * @return status 0 and json containing the inventory if successful; otherwise status is 1
     */
    @Override
    public ImmutablePair<Integer, String> getHwInventory(String locationName) {
        try {
            URI uri = makeUri(config.getHWInventoryUpdate.endpoint,
                    config.getHWInventoryUpdate.resource,
                    toForeignLocationName(locationName));
            logger.info("HWI:%n  uri: %s", uri);
            BlockingResult result = restClient.getRESTRequestBlocking(uri);
            return interpreteQueryHWInvQueryResult(uri, result);
        } catch (URISyntaxException e) {
            logger.fatal("Cannot create URI to get HW inventory update");
        } catch (RESTClientException e) {
            logger.fatal("getRESTRequestBlocking failure");
        }
        return new ImmutablePair<>(1, "");
    }

    String toForeignLocationName(String locationName) {
        if (isForeignLocationName(locationName)) {
            logger.warn("HWI:%n  %s is already in foreign namespace", locationName);
            return locationName;
        }
        try {
            return CommonFunctions.convertLocationToForeign(locationName);
        } catch (ConversionException e) {
            logger.error("HWI:%n  %s cannot be mapped into foreign namespace because %s",
                    locationName, e.getMessage());
            return locationName;
        }
    }

    boolean isForeignLocationName(String locationName) {
        return locationName.equals("all") || locationName.startsWith("x");  // case sensitive is important here
    }

    /**
     * Gets the hardware inventory history at the specified start start to the present.
     * @param startTime start time of the requested inventory history
     * @return status 0 and json containing the inventory history if successful; otherwise status is 1
     */
    @Override
    public ImmutablePair<Integer, String> getHWInventoryHistory(String startTime) {
        try {
            URI query = makeHistoryQuery(startTime);
            logger.info("HWI:%n  makeHistoryQuery(startTime=%S) => %s", startTime, query.toString());
            BlockingResult result = restClient.getRESTRequestBlocking(query);
            return interpreteQueryHWInvQueryResult(query, result);
        } catch (URISyntaxException e) {
            logger.fatal("HWI:%n  Cannot create URI to get HW inventory history");
        } catch (RESTClientException e) {
            logger.fatal("HWI:%n  getRESTRequestBlocking failure");
        }
        return new ImmutablePair<>(1, "");
    }

    private URI makeUri(String endpoint, String resource) throws URISyntaxException {
        return new URI(String.format("%s%s", endpoint, resource));
    }

    private URI makeUri(String endpoint, String resource, String subResource) throws URISyntaxException {
        return new URI(String.format("%s%s%s", endpoint, resource, subResource));
    }

    /**
     * Returns the URL for querying for inventory history starting at the given time.  If start time is null, all
     * available inventory history is returned.
     *
     * @param startTime if available; otherwise null
     * @return URI for querying inventory history from the given start time
     * @throws URISyntaxException URI Syntax Exception
     */
    private URI makeHistoryQuery(String startTime) throws URISyntaxException {
        URI uri = makeUri(config.getHWInventoryHistory.endpoint, config.getHWInventoryHistory.resource);
        logger.debug("HWI:%n  makeUri(getHWInventoryHistory.endpoint=%s, getHWInventoryHistory.resource=%s) => %s",
                config.getHWInventoryHistory.endpoint, config.getHWInventoryHistory.resource,
                uri);
        if (startTime == null || startTime.equals("")) {
            return uri; // no start time means entire history
        }
        return new URI(String.format("%s?start_time=%s", uri.toString(), startTime));
    }

    private ImmutablePair<Integer, String> interpreteQueryHWInvQueryResult(URI uri, BlockingResult result) {
        int res = validateBlockingResult(uri, result);
        if (res != 0) {
            logger.error("HWI:%n  validateBlockingResult(uri, result) => %d", res);
            return new ImmutablePair<>(1, "");
        }
        return new ImmutablePair<>(0, result.responseDocument);
    }

    private int interpretePollForDiscoveryProgressResult(URI uri, BlockingResult result) {
       if (validateBlockingResult(uri, result) != 0) {
            return 1;
       }

        // Need at least 1 "Complete"
        if (!result.responseDocument.contains("Complete")) {
            return 1;
        }

        // All discovery must be "Complete"
        String[] notComplete = {"NotStarted", "Pending", "InProgress"};
        for (String nc : notComplete) {
            if (result.responseDocument.contains(nc)) {
                return 1;
            }
        }
        return 0;
    }

    private int interpretedInitiateDiscoveryServerResult(URI uri, BlockingResult result) {
       if (validateBlockingResult(uri, result) != 0) {
            return 1;
       }

        // Success as long as the result from the blocking call is valid
        return 0;
    }

    private int validateBlockingResult(URI uri, BlockingResult result) {
        if (result == null) {
            return 1;
        }
        if (result.code != 200) {
            logger.error("HWI:%n  %s => %d", uri.toString(), result.code);
            return result.code;
        }
        if (result.requestInfo == null) {
            logger.error("HWI:%n  requestInfo is null");
            return 1;
        }
        if (result.responseDocument == null) {
            logger.error("HWI:%n  responseDocument is null");
            return 1;
        }
        logger.debug("HWI:%n  result.responseDocument:%s",
                util.head(result.responseDocument, 240));
        return 0;
    }

    private HWInvUtil util;
    private Logger logger;
    private InventoryInfoRequester config;
    private RESTClient restClient;
}
