// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class HWInvTranslatorCLI {
    private static Logger logger = LoggerFactory.getInstance("CLI", "HWInvTranslater", "console");

    private static String inputFileName;
    private static String outputFileName;

    public static void main(String[] args) {
        logger.initialize();
        int status = new HWInvTranslatorCLI().run(args);
        // Note that the return code is only from 0 to 255
        if (status != 0) {
            System.exit(1);
        }
    }
    private int run(String[] args) {
        try {
            getOptions(args);
            return run();
        } catch (ParseException e) {
            logger.error("ParseException: %s", e.getMessage());
        }
        return 1;
    }
    private int run() {
        HWInvTranslator tr = new HWInvTranslator(inputFileName, outputFileName, new HWInvUtilImpl());
        String loc = tr.foreignToCanonical();
        logger.info("Translate %s to %s for %s", inputFileName, outputFileName, loc);
        return loc == null ? 1 : 0;
    }
    private static void getOptions(String[] args) throws ParseException {
        final Options options = new Options();
        try {
            final Option inputFileOption = Option.builder("i")
                    .required()
                    .longOpt("input")
                    .hasArg()
                    .desc("input file name")
                    .build();
            final Option outputFileOption = Option.builder("o")
                    .required()
                    .longOpt("output")
                    .hasArg()
                    .desc("output file name")
                    .build();

            options.addOption(inputFileOption)
                    .addOption(outputFileOption);

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            inputFileName = cmd.getOptionValue("i");
            outputFileName = cmd.getOptionValue("o");
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("HWInvTranslatorCLI", options);
            throw e;
        }
    }
}
