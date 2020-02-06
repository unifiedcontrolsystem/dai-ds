// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.HWInvApi;
import com.intel.dai.dsapi.HWInvLoc;
import com.intel.dai.dsapi.HWInvTree;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.tuple.ImmutablePair;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;

public class HWInvDbClientCLI {
    private static Logger logger = LoggerFactory.getInstance("CLI", "HWInvDbClient", "console");

    private static String mode;
    private static String vendorInputFileName;
    private static String canonicalInputFileName;
    private static String vendorUpdateFileName;
    private static String canonicalOutputFileName;
    private static String debugLevel = "INFO";

    public static void main(String[] args) {
        int status = new HWInvDbClientCLI().run(args);

        // Note that the return code is only from 0 to 255
        if (status != 0) {
            System.exit(1);
        }
    }

    public int run(String[] args) {
        try {
            getOptions(args);
            System.setProperty("daiLoggingLevel", debugLevel);
            logger.initialize();
            String[] servers = new String[1];
            String server = "localhost";
            servers[0] = server;
            DataStoreFactory factory = new DataStoreFactoryImpl(servers, logger);
            HWInvApi client = factory.createHWInvApi();

            // Apparently there is no client.close()
            return run(client);
        } catch (ParseException e) {
            logger.error("ParseException: %s", e.getMessage());
        } catch (InterruptedException e) {
            logger.error("InterruptedException: %s", e.getMessage());
        } catch (IOException e) {
            logger.error("IOException: %s", e.getMessage());
        } catch (DataStoreException e) {
            logger.error("HWInvDataStoreException: %s", e.getMessage());
        }
        return 1;
    }
    private int run(HWInvApi client) throws InterruptedException, IOException, DataStoreException {
        switch (mode) {
            case "v2d":
                logger.info("v2d: vendorInputFileName: %s", vendorInputFileName);
                return ingest(client, vendorInputFileName, false);

            case "c2d":
                client.initialize();
                logger.info("c2d: canonicalInputFileName: %s", canonicalInputFileName);
                return client.ingest(Paths.get(canonicalInputFileName));

            case "upd":
                logger.info("upd: vendorUpdateFileName: %s", vendorUpdateFileName);
                return ingest(client, vendorUpdateFileName, true);

            case "out":
                logger.info("out: canonicalOutputFileName: %s", canonicalOutputFileName);
                return dump(client, canonicalOutputFileName);

            default:
                logger.error("Unsupported mode: %s", mode);
                return 1;
        }
    }

    private int ingest(HWInvApi client, String inputFile, boolean update) throws
            InterruptedException, IOException, DataStoreException {
//        final String canonicalTreeFileName = inputFile + ".tr";
        HWInvUtilImpl util = new HWInvUtilImpl();
        HWInvTranslator tr = new HWInvTranslator(util);
        ImmutablePair<String, String> res = tr.foreignToCanonical(Paths.get(inputFile));
        String locationName = res.getKey();
        String canonicalJson = res.getValue();
        if (locationName != null) {
            client.initialize();
            HWInvTree before = client.allLocationsAt(locationName, null);
            if (update) {
                logger.info("delete %s", locationName);
                client.delete(locationName);
            }
            int status = client.ingest(canonicalJson);
            HWInvTree after = client.allLocationsAt(locationName, null);
            List<HWInvLoc> delList = util.subtract(before.locs, after.locs);
            for (HWInvLoc s : delList) {
                logger.info("Deleted: %s", s.ID);
            }
            List<HWInvLoc> addList = util.subtract(after.locs, before.locs);
            for (HWInvLoc s : addList) {
                logger.info("Added: %s", s.ID);
            }
            logger.info("ingest %s: %d", inputFile, status);
            return status;
        }
        return 1;
    }

    int dump(HWInvApi client, String outputFileName) throws IOException, DataStoreException {
        return client.allLocationsAt("", outputFileName) != null ? 0 : 1;
    }

    private static void getOptions(String[] args) throws ParseException {
        final Options options = new Options();
        try {
            final Option vendorFileOption = Option.builder("v")
                    .longOpt("vendor")
                    .hasArg()
                    .desc("vendor HW Inventory file name")
                    .build();

            final Option canonicalFileOption = Option.builder("c")
                    .longOpt("canonical")
                    .hasArg()
                    .desc("canonical HW Inventory file name")
                    .build();

            final Option vendorUpdateFileOption = Option.builder("u")
                    .longOpt("update")
                    .hasArg()
                    .desc("vendor update file name")
                    .build();

            final Option dumpFileOption = Option.builder("o")
                    .longOpt("output")
                    .hasArg()
                    .desc("canonical json DB dump file name")
                    .build();

            final Option debugOption = Option.builder("d")
                    .longOpt("debug")
                    .hasArg()
                    .desc("debug mode: DEBUG/INFO/WARN/ERROR/FATAL")
                    .build();

            options.addOption(vendorFileOption);
            options.addOption(canonicalFileOption);
            options.addOption(vendorUpdateFileOption);
            options.addOption(dumpFileOption);
            options.addOption(debugOption);

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            vendorInputFileName = cmd.getOptionValue("v");
            canonicalInputFileName = cmd.getOptionValue("c");
            vendorUpdateFileName = cmd.getOptionValue("u");
            canonicalOutputFileName = cmd.getOptionValue("o");
            determineMode();

            debugLevel = cmd.getOptionValue("d", "ERROR");
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("HWInvDbClientCLI", options);
            throw e;
        }
    }

    private static void determineMode() {
        mode = "undefined";
        if (vendorInputFileName != null) {
            mode = "v2d";
            logger.info("vendorInputFileName: %s", vendorInputFileName);
        } else if (canonicalInputFileName != null) {
            mode = "c2d";
            logger.info("canonicalInputFileName: %s", canonicalInputFileName);
        } else if (vendorUpdateFileName != null) {
            mode = "upd";
            logger.info("vendorUpdateFileName: %s", vendorUpdateFileName);
        } else if (canonicalOutputFileName != null) {
            mode = "out";
            logger.info("canonicalOutputFileName: %s", canonicalOutputFileName);
        }
        logger.info("mode: %s", mode);
    }
}
