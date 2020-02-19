package com.intel.dai.inventory.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intel.authentication.TokenAuthentication;
import com.intel.authentication.TokenAuthenticationException;
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

public class HWInvDiscovery {
    public static void initialize(Logger logger) throws RESTClientException {
        log = logger;

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();

        HWInvDiscovery.createRestClient();
    }

    public static int initiateDiscovery(String xname) {
        if (requester_ == null) {
            return 1;
        }
        return requester_.initiateDiscovery(xname);
    }
    public static int pollForDiscoveryProgress() {
        if (requester_ == null) {
            return 1;
        }
        return requester_.getDiscoveryStatus();
    }
    public static ImmutablePair<Integer, String> queryHWInvTree(String xname) {
        if (requester_ == null) {
            log.error("requester_ is null");
            return new ImmutablePair<>(1, "");
        }
        return requester_.getHwInventory(xname);
    }

    public static ImmutablePair<Integer, String> queryHWInvTree() {
        if (requester_ == null) {
            log.error("requester_ is null");
            return new ImmutablePair<>(1, "");
        }
        return requester_.getHwInventory();
    }

    private static void createRestClient() throws RESTClientException {
        HWDiscoverySession sess;

        XdgConfigFile xdg = new XdgConfigFile("ucs");
        String configPath = xdg.FindFile("HWInvDiscoveryConfig.json");
        if(configPath == null)
            throw new NullPointerException("Was unable to find the configuration file: HWInvDiscoveryConfig.json");
        sess = toHWDiscoverySession(configPath);
        if(sess == null)
            throw new NullPointerException("Session section of the JSON was empty: HWInvDiscoveryConfig.json");
        log.info("config:%n%s", sess.toString());

        RESTClient restClient = RESTClientFactory.getInstance("jdk11", log);

        String requesterClass = sess.providerClassMap.requester;
        String tokenAuthProvider = sess.providerClassMap.tokenAuthProvider;

        if (tokenAuthProvider != null) {
            // The following dead code is causing spotbug warnings.  So, comment it out for now.
//            Map<String, String> config = Map.of(
//                    "tokenServer", sess.providerConfigurations.tokenAuthProvider.tokenServer,
//                    "realm", sess.providerConfigurations.tokenAuthProvider.realm,
//                    "clientId", sess.providerConfigurations.tokenAuthProvider.clientId,
//                    "clientSecret", sess.providerConfigurations.tokenAuthProvider.clientSecret
//            );
//            createTokenProvider(tokenAuthProvider, config);
//            if (tokenProvider_ == null) {
//                throw new RESTClientException("Cannot create token provider");
//            }
//            if (restClient != null) {
//                restClient.setTokenOAuthRetriever(tokenProvider_);
//            }
        }

        createRequester(requesterClass, sess.providerConfigurations.requester, restClient);
    }

    private static HWDiscoverySession toHWDiscoverySession(String inputFileName) throws RESTClientException {
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
    private static void createTokenProvider(String className, Map<String, String> config) {
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
    private static void createRequester(String requester, Requester config, RESTClient restClient) {
        if (requester == null || config == null || restClient == null) {
            return;
        }
        try {
            Class<?> classObj = Class.forName(requester);
            Constructor<?> ctor = classObj.getDeclaredConstructor();
            requester_ = (RestRequester) ctor.newInstance();
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

    private static RestRequester requester_ = null;
    private static TokenAuthentication tokenProvider_ = null;
    private static Gson gson;
    private static Logger log;
}
