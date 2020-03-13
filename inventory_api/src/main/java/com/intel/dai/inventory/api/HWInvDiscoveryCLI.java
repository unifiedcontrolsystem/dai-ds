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

    HWInvDiscoveryCLI(HWInvDiscovery hwInvDiscovery) {
        hwInvDiscovery_ = hwInvDiscovery;
    }

    public static void main(String[] args) {
        log_.initialize();
        int status = new HWInvDiscoveryCLI(new HWInvDiscovery(log_)).run(args);

        // Note that the return code is only from 0 to 255
        if (status != 0) {
            System.exit(1);
        }
    }

    private int run(String[] args) {
        try {
            hwInvDiscovery_.initialize();
            log_.info("rest client created");
            getOptions(args);
            log_.info("outputFilePath:%s, discoveryForeignName:%s, queryForeignName:%s",
                    outputFilePath, discoveryForeignName, queryForeignName);
            return run();
        } catch (ParseException e) {
            log_.error("ParseException: %s", e.getMessage());
        } catch (RESTClientException e) {
            log_.error("Fail to create REST client: %s", e.getMessage());
        }

        return 1;
    }

    private int run() {
        if (mode != null) {
            switch (mode) {
                case "p":
                    return hwInvDiscovery_.pollForDiscoveryProgress();

                case "s":
                    ImmutablePair<Integer, String> snapshot = hwInvDiscovery_.queryHWInvTree();
                    return toFile(snapshot, outputFilePath);

                case "d":
                    return hwInvDiscovery_.initiateDiscovery(discoveryForeignName);

                case "q":
                    ImmutablePair<Integer, String> update = hwInvDiscovery_.queryHWInvTree(queryForeignName);
                    return toFile(update, outputFilePath);

                default:
                    log_.error("An acceptable action option must be set: %s", mode);
                    return invalidHTTPCode;
            }
        }

        return 1;
    }

    private int toFile(ImmutablePair<Integer, String> result, Path outputFile) {
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

    private void getOptions(String[] args) throws ParseException {
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
            discoveryForeignName = cliUtil.getOptionValue(cmd, "d", discoveryForeignName);
            queryForeignName = cliUtil.getOptionValue(cmd, "q", queryForeignName);
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

    private static Logger log_ = LoggerFactory.getInstance("CLI", "HWInvDiscovery", "console");

    private static final int invalidHTTPCode = 1;
    private String mode = "";
    private Path outputFilePath = Paths.get("output.json");
    private String discoveryForeignName;
    private String queryForeignName;
    private HWInvDiscovery hwInvDiscovery_;
}
