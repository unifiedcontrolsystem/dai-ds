package com.intel.dai.inventory.api;

import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.HWInvApi;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl;
import com.intel.dai.exceptions.DataStoreException;
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

public class HWInvDbClientCLI {
    private static Logger logger = LoggerFactory.getInstance("CLI", "HWInvDbClient", "console");

    private static String mode;
    private static String inputFileName;
    private static String server = "localhost";
    private static int retries = 0;

    public static void main(String[] args) {
        int status = new HWInvDbClientCLI().run(args);

        // Note that the return code is only from 0 to 255
        if (status != 0) {
            System.exit(1);
        }
    }

    private int run(String[] args) {
        try {
            getOptions(args);
            String[] servers = new String[1];
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
                String canonicalTreeFileName = inputFileName + ".tr";
                HWInvTranslator tr = new HWInvTranslator(inputFileName, canonicalTreeFileName,
                        new HWInvUtilImpl());
                if (tr.foreignToCanonical() == 0) {
                    client.initialize();
                    int status = client.ingest(canonicalTreeFileName);
                    logger.info("ingest %s: %d", canonicalTreeFileName, status);
                    return status;
                }
                return 1;

            case "c2d":
                client.initialize();
                return client.ingest(inputFileName);

            default:
                logger.error("Unsupported mode: %s", mode);
                return 1;
        }
    }
    private static void getOptions(String[] args) throws ParseException {
        final Options options = new Options();
        try {
            final Option modeOption = Option.builder("m")
                    .required()
                    .hasArg()
                    .longOpt("mode")
                    .desc("v2d/c2d")
                    .build();
            final Option inputFileOption = Option.builder("i")
                    .required()
                    .longOpt("input")
                    .hasArg()
                    .desc("input file name")
                    .build();

            options.addOption(modeOption);
            options.addOption(inputFileOption);

            CommandLineParser parser = new DefaultParser();
            CommandLine cmd = parser.parse(options, args);

            mode = cmd.getOptionValue("m");
            inputFileName = cmd.getOptionValue("i");
            retries = new CommonCliUtil().getOptionValue(cmd, "r", retries);
        } catch (ParseException e) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp("HWInvDbClientCLI", options);
            throw e;
        }
    }
}
