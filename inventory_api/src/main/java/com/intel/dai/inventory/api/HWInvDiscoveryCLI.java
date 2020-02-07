// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import com.intel.dai.dsapi.HWInvUtil;
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.restclient.RESTClientException;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class HWInvDiscoveryCLI {
    private static Logger log = LoggerFactory.getInstance("CLI", "HWInvDiscovery", "console");

    private static final int invalidHTTPCode = 1;

    private static String mode = "";
    private static Path outputFilePath = Paths.get("output.json");
    private static String discoveryXname;
    private static String queryXname;

    public static void main(String[] args) {
        log.initialize();
        try {
            HWInvDiscovery.initialize(log);
            log.info("rest client created");
        } catch (RESTClientException e) {
            log.fatal("Fail to create REST client: %s", e.getMessage());
            System.exit(1);
        }

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
        if (mode != null) {
            switch (mode) {
                case "p":
                    return HWInvDiscovery.pollForDiscoveryProgress();

                case "s":
                    ImmutablePair<Integer, String> snapshot = HWInvDiscovery.queryHWInvTree();
                    return toFile(snapshot, outputFilePath);

                case "d":
                    return HWInvDiscovery.initiateDiscovery(discoveryXname);

                case "q":
                    ImmutablePair<Integer, String> update = HWInvDiscovery.queryHWInvTree(queryXname);
                    return toFile(update, outputFilePath);

                default:
                    log.error("An acceptable action option must be set: %s", mode);
                    return invalidHTTPCode;
            }
        }

        return 1;
    }

    private static int toFile(ImmutablePair<Integer, String> result, Path outputFile) {
        int status = result.getLeft();
        if (status == 0) {
            HWInvUtil cliUtil = new HWInvUtilImpl();
            try {
                cliUtil.fromStringToFile(result.getRight(), outputFile.toString());
            } catch (IOException | NullPointerException e) {
                return 1;
            }
        }
        return status;
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
}
