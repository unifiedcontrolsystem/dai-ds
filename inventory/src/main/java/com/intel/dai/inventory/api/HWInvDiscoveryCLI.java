// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import com.intel.authentication.TokenAuthentication;
import com.intel.authentication.TokenAuthenticationException;

import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restclient.RESTClientFactory;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class HWInvDiscoveryCLI {
    private static Logger log = LoggerFactory.getInstance("CLI", "HWInvDiscovery", "console");
    private static Gson gson;
    private static RestRequester requester_ = null;
    private static TokenAuthentication tokenProvider_ = null;


    private static final int invalidHTTPCode = 1;

    private static String mode = "";
    private static Path outputFilePath = Paths.get("output.json");
    private static String discoveryXname;
    private static String queryXname;

    public static void main(String[] args) {
        log.initialize();
        int status = new HWInvDiscoveryCLI().run(args);

        // Note that the return code is only from 0 to 255
        if (status != 0) {
            System.exit(1);
        }
    }
    private int run(String[] args) {
        try {
            getOptions(args);
            log.info("outputFilePath:%s, discoveryXname:%s, queryXname:%s",
                    outputFilePath, discoveryXname, queryXname);
            return run();
        } catch (ParseException e) {
            log.error("ParseException: %s", e.getMessage());
        }

        return 1;
    }
    private int run() {
        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();

        try {
            createRestClient();
        } catch(RESTClientException e) {
            log.fatal("Fail to create REST client");
            return 1;
        }
        log.info("rest client created");

        if (mode != null) {
            switch (mode) {
                case "p":
                    return pollForDiscoveryProgress();

                case "s":
                    return queryHWInvTree();

                case "d":
                    return initiateDiscovery(discoveryXname);

                case "q":
                    return queryHWInvTree(queryXname);

                default:
                    log.error("An acceptable action option must be set: %s", mode);
                    return invalidHTTPCode;
            }
        }

        return 1;
    }

    private void createRestClient() throws RESTClientException {
        HWDiscoverySession sess;
        try {
            String configPath = "/opt/ucs/etc/HWInvDiscoveryCLIConfig.json";
            sess = toHWDiscoverySession(configPath);
            log.info("config:%n%s", sess.toString());
        } catch (Exception e) {
            log.error("Exception: %s", e.getMessage());
            throw new RESTClientException("Cannot determine CLI configuration");
        }
        RESTClient restClient = RESTClientFactory.getInstance("jdk11", log);

        if (sess.providerClassMap.tokenAuthProvider != null) {
            Map<String, String> config = Map.of(
                    "tokenServer", sess.providerConfigurations.tokenAuthProvider.tokenServer,
                    "realm", sess.providerConfigurations.tokenAuthProvider.realm,
                    "clientId", sess.providerConfigurations.tokenAuthProvider.clientId,
                    "clientSecret", sess.providerConfigurations.tokenAuthProvider.clientSecret
            );
            createTokenProvider(sess.providerClassMap.tokenAuthProvider, config);
            if (tokenProvider_ == null) {
                throw new RESTClientException("Cannot create token provider");
            }
            if (restClient != null) {
                restClient.setTokenOAuthRetriever(tokenProvider_);
            }
        }

        createRequester(sess.providerClassMap.requester, sess.providerConfigurations.requester, restClient);
    }

    private static void getOptions(String[] args) throws ParseException {
        final Options options = new Options();
        try {
            final Option discoveryOption = Option.builder("d")
                    .required(false)
                    .hasArg()
                    .longOpt("discovery-initiation")
                    .desc("initiate discovery at {xname}")
                    .build();
            final Option statusOption = Option.builder("p")
                    .required(false)
                    .hasArg(false)
                    .longOpt("discovery-status")
                    .desc("poll for discovery status")
                    .build();
            final Option snapshotOption = Option.builder("s")
                    .required(false)
                    .hasArg(false)
                    .longOpt("entire-hw-inventory-snapshot")
                    .desc("get entire hw inventory snapshot")
                    .build();
            final Option queryOption = Option.builder("q")
                    .required(false)
                    .hasArg()
                    .longOpt("query-hw-inventory")
                    .desc("query hw inventory at {xname}")
                    .build();
            final Option outputFileOption = Option.builder("o")
                    .required(false)
                    .longOpt("output")
                    .hasArg()
                    .desc("output file path for HW inventory JSON")
                    .build();

            options.addOption(discoveryOption).
                    addOption(statusOption).
                    addOption(snapshotOption).
                    addOption(queryOption).
                    addOption(outputFileOption);

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            CommonCliUtil cliUtil = new CommonCliUtil();
            discoveryXname = cliUtil.getOptionValue(cmd, "d", discoveryXname);
            queryXname = cliUtil.getOptionValue(cmd, "q", queryXname);
            outputFilePath = cliUtil.getOptionValue(cmd, "o", outputFilePath);

            // Only first option is accepted
            String[] optList = {"d", "p", "q", "s"};
            for (String opt: optList){
                if (cmd.hasOption(opt)) {
                    mode = opt;
                    break;
                }
            }
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("HWInvDiscoveryCLI", options);
            throw e;
        }
    }
    private int initiateDiscovery(String xname) {
        if (requester_ == null) {
            return 1;
        }
        return requester_.initiateDiscovery(xname);
    }
    private int pollForDiscoveryProgress() {
        if (requester_ == null) {
            return 1;
        }
        return requester_.getDiscoveryStatus();
    }
    private int queryHWInvTree(String xname) {
        if (requester_ == null) {
            return 1;
        }
        return requester_.getHwInventory(xname, outputFilePath.toString());
    }
    private int queryHWInvTree() {
        if (requester_ == null) {
            return 1;
        }
        return requester_.getHwInventory(outputFilePath.toString());
    }
    private static HWDiscoverySession toHWDiscoverySession(String inputFileName) throws
            IOException, JsonIOException, JsonSyntaxException {
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFileName),
                StandardCharsets.UTF_8));
        return gson.fromJson(br, HWDiscoverySession.class);
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
            log.exception(e, String.format("Missing TokenAuthentication implementation '%s'", className));
        } catch (NoSuchMethodException e) {
            log.exception(e, String.format("Missing public constructor for TokenAuthentication implementation '%s'",
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
}
