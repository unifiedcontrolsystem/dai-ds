// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;


import com.intel.logging.Logger;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.net.URI;
import java.net.URISyntaxException;

class ForeignHwInventoryRequester implements RestRequester {
    public ForeignHwInventoryRequester() {}

    /**
     * This method is used to initialize logger, requestor and client.
     */
    @Override
    public void initialize(Logger logger, Requester config, RESTClient restClient) {
        this.logger = logger;
        this.config = config;
        this.restClient = restClient;
    }

    /**
     * This method is used to initiate and process discovery for a foreign location.
     */
    @Override
    public int initiateDiscovery(String foreignName) {
        try {
            URI uri = makeUri(config.initiateDiscovery.endpoint, config.initiateDiscovery.resource);
            logger.info("uri: %s", uri.toString());
            String payload = String.format("{\"xnames\": [\"%s\"], \"force\": false}", foreignName);
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
     * This method is used to get the status of initiated discovery.
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
     * This method is used to get the hardware inventory data.
     */
    @Override
    public ImmutablePair<Integer, String> getHwInventory() {
        try {
            URI uri = makeUri(config.getHwInventorySnapshot.endpoint, config.getHwInventorySnapshot.resource);
            logger.info("uri: %s", uri.toString());
            BlockingResult result = restClient.getRESTRequestBlocking(uri);
            return interpreteQueryHWInvQueryResult(uri, result);
        } catch (URISyntaxException e) {
            logger.fatal("Cannot create URI to get HW inventory");
        } catch (RESTClientException e) {
            logger.fatal("getRESTRequestBlocking failure");
        }
        return new ImmutablePair<>(1, "");
    }

    /**
     * This method is used to get the hardware inventory data for a location.
     */
    @Override
    public ImmutablePair<Integer, String> getHwInventory(String foreignName) {
        try {
            URI uri = makeUri(config.getHWInventoryUpdate.endpoint, config.getHWInventoryUpdate.resource, foreignName);
            logger.info("uri: %s", uri.toString());
            BlockingResult result = restClient.getRESTRequestBlocking(uri);
            return interpreteQueryHWInvQueryResult(uri, result);
        } catch (URISyntaxException e) {
            logger.fatal("Cannot create URI to get HW inventory update");
        } catch (RESTClientException e) {
            logger.fatal("getRESTRequestBlocking failure");
        }
        return new ImmutablePair<>(1, "");
    }

    /**
     * This method is used to get the hardware inventory history over a time period .
     */
    @Override
    public ImmutablePair<Integer, String> getHWInventoryHistory(String startTime, String endTime) {
        try {
            URI uri = makeUri(config.getHWInventoryHistory.endpoint, config.getHWInventoryHistory.resource);
            URI query = makeQuery(uri, "start_time", startTime, "end_time", endTime);
            logger.info("query: %s", query.toString());
            BlockingResult result = restClient.getRESTRequestBlocking(query);
            return interpreteQueryHWInvQueryResult(query, result);
        } catch (URISyntaxException e) {
            logger.fatal("Cannot create URI to get HW inventory history");
        } catch (RESTClientException e) {
            logger.fatal("getRESTRequestBlocking failure");
        }
        return new ImmutablePair<>(1, "");
    }

    private URI makeUri(String endpoint, String resource) throws URISyntaxException {
        return new URI(String.format("%s%s", endpoint, resource));
    }

    private URI makeUri(String endpoint, String resource, String subResource) throws URISyntaxException {
        return new URI(String.format("%s%s%s", endpoint, resource, subResource));
    }

    private URI makeQuery(URI uri, String param0, String val0, String param1, String val1) throws URISyntaxException {
        return new URI(String.format("%s?%s=%s&%s=%s", uri.toString(), param0, val0, param1, val1));
    }

    private ImmutablePair<Integer, String> interpreteQueryHWInvQueryResult(URI uri, BlockingResult result) {
        if (validBlockingResult(uri, result) != 0) {
            return new ImmutablePair<>(1, "");
        }
        return new ImmutablePair<>(0, result.responseDocument);
    }

    private int interpretePollForDiscoveryProgressResult(URI uri, BlockingResult result) {
       if (validBlockingResult(uri, result) != 0) {
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
       if (validBlockingResult(uri, result) != 0) {
            return 1;
       }

        // Success as long as the result from the blocking call is valid
        return 0;
    }

    private int validBlockingResult(URI uri, BlockingResult result) {
        if (result == null) {
            return 1;
        }
        if (result.code != 200) {
            logger.error("%s => %d", uri.toString(), result.code);
            return result.code;
        }
        if (result.requestInfo == null) {
            logger.error("requestInfo is null");
            return 1;
        }
        if (result.responseDocument == null) {
            logger.error("responseDocument is null");
            return 1;
        }
        logger.info(result.responseDocument);
        return 0;
    }

    private Logger logger;
    private Requester config;
    private RESTClient restClient;
}
