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
import java.io.IOException;

public class HWInvTranslatorCLI {
    private static Logger logger = LoggerFactory.getInstance("CLI", "HWInvTranslater", "console");

    private static String mode;
    private static String inputFileName;
    private static String outputFileName;

    public static void main(String[] args) {
        int status = new HWInvTranslatorCLI().run(args);

        // Note that the return code is only from 0 to 255
        if (status != 0) {
            System.exit(1);
        }
    }
    int run(String[] args) {
        try {
            getOptions(args);
            return run();
        } catch (ParseException e) {
            logger.error("ParseException: %s", e.getMessage());
        } catch (IOException e) {
            logger.error("IOException: %s", e.getMessage());
        }
        return 1;
    }
    private int run() throws IOException {
        HWInvTranslator tr = new HWInvTranslator(inputFileName, outputFileName, new HWInvUtilImpl());
        logger.info("Translate %s to %s", inputFileName, outputFileName);
        return tr.foreignToCanonical();
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
            formatter.printHelp("Main", options);
            throw e;
        }
    }
}
