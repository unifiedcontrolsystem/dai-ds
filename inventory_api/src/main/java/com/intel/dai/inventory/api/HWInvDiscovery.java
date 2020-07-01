// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intel.authentication.TokenAuthentication;
import com.intel.authentication.TokenAuthenticationException;
import com.intel.dai.inventory.api.pojo.cfg.HWDiscoverySession;
import com.intel.dai.inventory.api.pojo.rqst.InventoryInfoRequester;
import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restclient.RESTClientFactory;
import com.intel.xdg.XdgConfigFile;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * This class uses the foreign server inventory rest api to gather inventory information from the foreign
 * inventory server.
 *
 * It configures the logger used to capture diagnostic trace, and the rest client that communicates with the foreign
 * server.  It also determines the configuration of the rest methods required for the communication.
 *
 * The logger, rest client and the rest methods are are used to configure an object that implements the
 * ForeignServerInventoryRest interface.  Most of the business logic required to gather inventory information from
 * the foreign server reside in this object.
 */
public class HWInvDiscovery {

    /**
     * Constructs the HWInvDiscovery object by initializing the logger and the GSON object.
     * @param logger logs diagnostic traces
     */
    public HWInvDiscovery(Logger logger) {
        log = logger;

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();
    }

    /**
     * Initializes the rest client used to communicate with the foreign inventory server.
     * @throws RESTClientException if the rest client cannot be created
     */
    public void initialize() throws RESTClientException {
        createRestClient();
    }

    /**
     * Initiates asynchronous inventory discovery at the specified foreign location.
     * @param foreignLocName foreign name of the root of a subtree in the HPC inventory hierarchy
     * @return 0 if discovery is started successfully, otherwise 1
     */
    public int initiateDiscovery(String foreignLocName) {
        if (requester_ == null) {
            return 1;
        }
        return requester_.initiateDiscovery(foreignLocName);
    }

    /**
     * Polls the foreign server for the completion of the inventory discovery process.
     * @return 0 if all discovery processes have completed successfully, otherwise 1
     */
    public int pollForDiscoveryProgress() {
        if (requester_ == null) {
            return 1;
        }
        return requester_.getDiscoveryStatus();
    }

    /**
     * Gets the entire HW inventory at the specified location.
     * @param foreignLocName foreign name of the root of a subtree in the HPC inventory hierarchy
     * @return status 0 and json containing the inventory if successful; otherwise status is 1
     */
    public ImmutablePair<Integer, String> queryHWInvTree(String foreignLocName) {
        if (requester_ == null) {
            log.error("requester_ is null");
            return new ImmutablePair<>(1, "");
        }
        return requester_.getHwInventory(foreignLocName);
    }

    /**
     * Gets the entire HW inventory.
     * @return status 0 and json containing the inventory if successful; otherwise status is 1
     */
    public ImmutablePair<Integer, String> queryHWInvTree() {
        if (requester_ == null) {
            log.error("requester_ is null");
            return new ImmutablePair<>(1, "");
        }
        return requester_.getHwInventory();
    }

    /**
     * Gets the hardware inventory history at the specified start start to the present.
     * @param startTime start time of the requested inventory history
     * @return status 0 and json containing the inventory history if successful; otherwise status is 1
     */
    public ImmutablePair<Integer, String> queryHWInvHistory(String startTime) {
        if (requester_ == null) {
            log.error("requester_ is null");
            return new ImmutablePair<>(1, "");
        }
        return requester_.getHWInventoryHistory(startTime);
    }

    /**
     * Creates/Initializes a rest client.
     */
    private void createRestClient() throws RESTClientException {
        HWDiscoverySession sess;

        XdgConfigFile xdg = new XdgConfigFile("ucs");
        String configPath = xdg.FindFile("HWInvDiscoveryConfig.json");
        if(configPath == null)
            throw new NullPointerException("Was unable to find the configuration file: HWInvDiscoveryConfig.json");

        sess = toHWDiscoverySession(configPath);
        log.info("config:%n%s", sess.toString());

        RESTClient restClient = RESTClientFactory.getInstance("jdk11", log);

        String requesterClass = sess.providerClassMap.requester;
        String tokenAuthProvider = sess.providerClassMap.tokenAuthProvider;

        if (tokenAuthProvider != null && !tokenAuthProvider.equals("")) {
            Map<String, String> config = Map.of(
                    "tokenServer", sess.providerConfigurations.tokenAuthProvider.tokenServer,
                    "realm", sess.providerConfigurations.tokenAuthProvider.realm,
                    "clientId", sess.providerConfigurations.tokenAuthProvider.clientId,
                    "clientSecret", sess.providerConfigurations.tokenAuthProvider.clientSecret
            );
            createTokenProvider(tokenAuthProvider, config);
            if (tokenProvider_ == null) {
                throw new RESTClientException("Cannot create token provider");
            }
            if (restClient != null) {
                restClient.setTokenOAuthRetriever(tokenProvider_);
            }
        }

        createRequester(requesterClass, sess.providerConfigurations.inventoryInfoRequester, restClient);
    }

    private HWDiscoverySession toHWDiscoverySession(String inputFileName) throws RESTClientException {
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFileName),
                    StandardCharsets.UTF_8));
            return gson.fromJson(br, HWDiscoverySession.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            String msg = String.format("Fail to determine discovery session providers: %s", e.getMessage());
            log.fatal(msg);
            throw new RESTClientException(msg);
        }
    }

    private void createTokenProvider(String className, Map<String, String> config) {
        if (className == null || config == null) {
            return;
        }
        try {
            Class<?> classObj = Class.forName(className);
            Constructor<?> ctor = classObj.getDeclaredConstructor();
            tokenProvider_ = (TokenAuthentication) ctor.newInstance();
            tokenProvider_.initialize(log, config);
        } catch (ClassNotFoundException e) {
            log.exception(e, String.format("Requires TokenAuthentication implementation '%s'", className));
        } catch (NoSuchMethodException e) {
            log.exception(e, String.format("Requires public constructor for TokenAuthentication implementation '%s'",
                    className));
        } catch (IllegalAccessException e) {
            log.exception(e, String.format("Default constructor for TokenAuthentication implementation " +
                    "'%s' must be public", className));
        } catch (InstantiationException | InvocationTargetException | TokenAuthenticationException e) {
            log.exception(e, String.format("Cannot construct TokenAuthentication implementation '%s'", className));
        }
    }

    private void createRequester(String requester, InventoryInfoRequester config, RESTClient restClient) {
        if (requester == null || config == null || restClient == null) {
            return;
        }
        try {
            Class<?> classObj = Class.forName(requester);
            Constructor<?> ctor = classObj.getDeclaredConstructor();
            requester_ = (ForeignServerInventoryRest) ctor.newInstance();
            requester_.initialize(log, config, restClient);
        } catch (ClassNotFoundException e) {
            log.exception(e, String.format("Missing RestRequester implementation '%s'", requester));
        } catch (NoSuchMethodException e) {
            log.exception(e, String.format("Missing public constructor for RestRequester implementation '%s'",
                    requester));
        } catch (IllegalAccessException e) {
            log.exception(e, String.format("Default constructor for RestRequester implementation " +
                    "'%s' must be public", requester));
        } catch (InstantiationException | InvocationTargetException e) {
            log.exception(e, String.format("Cannot construct RestRequester implementation '%s'", requester));
        }
    }

    private static ForeignServerInventoryRest requester_ = null;
    private static TokenAuthentication tokenProvider_ = null;
    private final Gson gson;
    private final Logger log;
}
