package com.intel.dai.hwinventory.api;


import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.logging.Logger;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

class ForeignHwInventoryRequester implements RestRequester {
    public ForeignHwInventoryRequester() {}

    @Override
    public void initialize(Logger logger, Requester config, RESTClient restClient) {
        this.logger = logger;
        this.config = config;
        this.restClient = restClient;
    }

    @Override
    public int initiateDiscovery(String xname) {
        try {
            URI uri = makeUri(config.initiateDiscovery.endpoint, config.initiateDiscovery.resource);
            logger.info("uri: %s", uri.toString());
            String payload = String.format("{\"xnames\": [\"%s\"], \"force\": false}", xname);
            BlockingResult result = restClient.postRESTRequestBlocking(uri, payload);
            return interpretedInitiateDiscoveryServerResult(uri, result);
        } catch (URISyntaxException e) {
            logger.fatal("Cannot create URI to initiate discovery");
        } catch (RESTClientException e) {
            logger.fatal("postRESTRequestBlocking failure");
        }
        return 1;
    }
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
    @Override
    public int getHwInventory(String outputFile) {
        try {
            URI uri = makeUri(config.getHwInventorySnapshot.endpoint, config.getHwInventorySnapshot.resource);
            logger.info("uri: %s", uri.toString());
            BlockingResult result = restClient.getRESTRequestBlocking(uri);
            return interpreteQueryHWInvTreeResult(uri, result, outputFile);
        } catch (URISyntaxException e) {
            logger.fatal("Cannot create URI to initiate discovery");
        } catch (RESTClientException e) {
            logger.fatal("getRESTRequestBlocking failure");
        }
        return 1;
    }
    @Override
    public int getHwInventory(String xname, String outputFile) {
        try {
            URI uri = makeUri(config.getHWInventoryUpdate.endpoint, config.getHWInventoryUpdate.resource, xname);
            logger.info("uri: %s", uri.toString());
            BlockingResult result = restClient.getRESTRequestBlocking(uri);
            return interpreteQueryHWInvTreeResult(uri, result, outputFile);
        } catch (URISyntaxException e) {
            logger.fatal("Cannot create URI to initiate discovery");
        } catch (RESTClientException e) {
            logger.fatal("getRESTRequestBlocking failure");
        }
        return 1;
    }
    private URI makeUri(String endpoint, String resource) throws URISyntaxException {
        return new URI(String.format("%s%s", endpoint, resource));
    }
    private URI makeUri(String endpoint, String resource, String subResource) throws URISyntaxException {
        return new URI(String.format("%s%s%s", endpoint, resource, subResource));
    }

    private int interpreteQueryHWInvTreeResult(URI uri, BlockingResult result, String outputFile) {
        if (result == null) {
            return 1;
        }
        if (result.code == 200) {
            if (result.responseDocument == null) {
                logger.error("responseDocument is null");
                return 1;
            }
            HWInvUtilImpl cliUtil = new HWInvUtilImpl();
            try {
                cliUtil.fromStringToFile(result.responseDocument, outputFile);
            } catch (IOException e) {
                logger.error("Cannot write to %s", outputFile);
                return 1;
            }
            return 0;
        }
        logger.error("%s => %d", uri.toString(), result.code);
        return result.code;
    }
    private int interpretePollForDiscoveryProgressResult(URI uri, BlockingResult result) {
        if (result == null) {
            return 1;
        }
        if (result.code == 200) {
            if (result.responseDocument == null) {
                logger.error("json is null");
                return 1;
            }
            logger.info(result.responseDocument);
            if (!result.responseDocument.contains("Complete")) {
                return 1;
            }
            String[] notComplete = {"NotStarted", "Pending", "InProgress"};
            for (String nc : notComplete) {
                if (result.responseDocument.contains(nc)) {
                    return 1;
                }
            }
            return 0;
        }
        // Requires interpretation of discovery progress by walking the status array
        logger.error("%s => %d", uri.toString(), result.code);
        return result.code;
    }
    private int interpretedInitiateDiscoveryServerResult(URI uri, BlockingResult result) {
        if (result == null) {
            return 1;
        }
        if (result.code == 200) {
            if (result.responseDocument == null) {
                logger.error("interpretedInitiateDiscoveryServerResult:json is null");
                return 1;
            }
            logger.info(result.responseDocument);
            return 0;
        }
        logger.error("%s => %d", uri.toString(), result.code);
        return result.code;
    }

    private Logger logger;
    private Requester config;
    private RESTClient restClient;
}
