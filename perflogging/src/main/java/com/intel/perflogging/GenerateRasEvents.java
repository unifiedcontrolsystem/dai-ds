// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.perflogging;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.*;
import org.voltdb.client.Client;

/**
 * Generate RAS events directly into the VoltDb.
 *
 *  java -jar perflogging.jar servers
 *                            [--count[=| ]<count_of_events>]
 *                            [--seed[=| ]<random_seed>]
 *                            [--percent-jobs[=| ]<percent_needing_job_ids>]
 *                            [--percent-ctrl[=| ]<percent_needing_control_ops>]
 *
 *      servers        Required in position 1: VoltDb servers list (comma separated).
 *      --count        The total number of RAS events to insert (Long 1+ w/ default = 100,000).
 *      --seed         The seed for the PRNG so that reproducible results are possible
 *                     (Long w/ default = Instant.now().toEpochMillis)
 *      --percent-jobs Percent of total that need job ID association (Long 0-100, w/ default = 0).
 *      --percent-ctrl Percent of total that need control operations (Long 0-100, w/ default = 0).
 *
 *      Note: The statistical overlap of job association and control operations is 25% if both are set to 50%.
 */
public class GenerateRasEvents {
    /**
     * Application entry point.
     *
     * @param args The commandline arguments.
     * @throws IOException when VoltDb fails.
     * @throws ProcCallException when VoltDb fails.
     * @throws InterruptedException when a Java interrupt is sent to the application.
     * @throws RuntimeException when the commandline cannot be parsed.
     */
    public static void main(String[] args) throws IOException, ProcCallException, InterruptedException {
        new GenerateRasEvents(args).run();
    }

    /**
     * Create the single use object that generates RAS Events directly into VoltDb with percentages for
     * job ID association and control operations.
     *
     * @param args The command line arguments described above.
     * @throws RuntimeException when the commandline cannot be parsed. 
     */
    public GenerateRasEvents(String[] args) {
        assert args.length >= 1 : "Must have at least the VoltDb Server in the arguments list!";
        Instant ts = Instant.now();
        servers_ = args[0];
        timestampBase_ = (ts.getEpochSecond() * 1_000_000L) + (ts.getNano() / 1_000L);
        options_.put("seed", timestampBase_);
        parseArgs(args);
    }

    // Parse commandline minus the VoltDb servers value(s).
    private void parseArgs(String[] args) {
        int count = 1;
        while(count < args.length) {
            String option;
            String value;
            if(!args[count].startsWith("--"))
                throw new RuntimeException("Next argument was not an option: " + args[count]);
            if(args[count].contains("=")) {
                String[] parts = args[count++].split("=");
                option = parts[0].substring(2);
                value = stripQuotes(parts[1].trim());
            } else {
                option = args[count++].substring(2);
                value = stripQuotes(args[count++].trim());
            }
            setOptionValue(option, value);
        }
    }

    // Set a named option value or if invalid throw a RuntimeException.
    private void setOptionValue(String name, String value) {
        if(options_.containsKey(name))
            options_.put(name, Long.parseLong(value));
        else
            throw new RuntimeException("Unknown option: " + name);
    }

    // Remove matching quotes from a trimmed string.
    private String stripQuotes(String trim) {
        if((trim.charAt(0) == '"' && trim.charAt(trim.length() - 1) == '"') ||
                (trim.charAt(0) == '\'' && trim.charAt(trim.length() - 1) == '\'')) {
            return trim.substring(1, trim.length() - 1);
        }
        return trim;
    }

    // Startup the VoltDb client.
    private void setupVolt() throws IOException {
        if(client_ == null) {
            ClientConfig config = new ClientConfig("", "", null);
            config.setReconnectOnConnectionLoss(true);
            client_ = ClientFactory.createClient(config);

            String[] servers = servers_.split(",");

            // Connect to all the VoltDb servers
            for (String server : servers)
                client_.createConnection(server, Client.VOLTDB_SERVER_PORT);
        }
    }

    // Get the nodes list from the VoltDb server(s)
    private void populateNodes() throws IOException, ProcCallException {
        ClientResponse response = client_.callProcedure("@AdHoc",
                "SELECT Lctn FROM ComputeNode ORDER BY Lctn;");
        if(response.getStatus() != ClientResponse.SUCCESS || response.getResults()[0].getRowCount() == 0)
            throw new RuntimeException("Failed to get nodes list from VoltDb: " +
                    statusString_.get(response.getStatus()));
        VoltTable table = response.getResults()[0];
        for(int i = 0; i < table.getRowCount(); i++) {
            table.advanceRow();
            nodes_.add((String)table.get("Lctn", VoltType.STRING));
        }
    }

    // Objects entry point.
    private void run() throws InterruptedException, IOException, ProcCallException {
        setupVolt();
        populateNodes();
        long seed = options_.getOrDefault("seed", timestampBase_);
        random_ = new Random(seed);
        System.out.println(String.format("*** Seed Value                      = %d",   seed));
        System.out.println(String.format("*** Event Count                     = %d",   options_.get("count")));
        System.out.println(String.format("*** Percent Events With Job Assoc   = %d%%", options_.get("percent-jobs")));
        System.out.println(String.format("*** Percent Events With Control Ops = %d%%", options_.get("percent-ctrl")));
        long iterations = options_.get("count");
        doEventCreationLoop(iterations);
        if(errors_ > 0)
            System.out.println(String.format("%n--- Failed Writes Count             = %d", errors_));
        else
            System.out.println(String.format("%n+++ Succeeded"));

        Thread.sleep(2_000);
        client_.close();
        client_ = null;
    }

    // Insert the specified number of RAS events.
    private void doEventCreationLoop(long iterations) {
        for(int i = 0; i < iterations; i++) {
            long updateTime = timestampBase_ + (i * 1_000L); // one ms between iterations.
            try {
                client_.callProcedure(this::writeCallback, "RASEVENT.Insert", i, "0000000000",
                        getLocation(i), null, getJobId(), 0, getControlOperation(), "N", "InstanceData", updateTime,
                        updateTime, "INITIALIZATION", -1);
            } catch (IOException e) {
                errors_++;
            }
            if((i % 1_000) == 0 && i != 0) {
                try {
                    System.out.write(".".getBytes(StandardCharsets.UTF_8));
                    System.out.flush();
                } catch(IOException e) {
                    /* A write to default stdout should not fail! Ignore this. */
                }
            }
        }
    }

    // Callback from writing the RAS event asynchronously.
    private void writeCallback(ClientResponse clientResponse) {
        if(clientResponse.getStatus() != ClientResponse.SUCCESS)
            errors_++;
    }

    // Return the control operation or null based on "percent-ctrl".
    private String getControlOperation() {
        if(options_.get("percent-ctrl") <= 0L)
            return null;
        else if(options_.get("percent-ctrl") >= 100)
            return "ErrorOnNode";
        return (random_.nextInt(99) + 1) <= options_.get("percent-ctrl") ? "ErrorOnNode" : null;
    }

    // Return the job ID of '?' or null based on "percent-jobs".
    private String getJobId() {
        if(options_.get("percent-jobs") <= 0L)
            return null;
        else if(options_.get("percent-jobs") >= 100)
            return "?";
        return (random_.nextInt(99) + 1) <= options_.get("percent-jobs") ? "?" : null;
    }

    // Return the location of the RAS event based on the zero based iteration and nodes list (Round Robin)
    private String getLocation(int i) {
        return nodes_.get(i % nodes_.size());
    }

    private int errors_ = 0;                                // Errors count writing RAS events during the run().
    private final List<String> nodes_ = new ArrayList<>();  // Nodes list from VoltDb
    @SuppressWarnings("serial")
    private Map<String, Long> options_ = new HashMap<String, Long>() {{ // Allowed commandline options with defaults.
        put("seed", 0L);
        put("count", 100_000L);
        put("percent-jobs", 0L);
        put("percent-ctrl", 0L);
    }};
    private long timestampBase_; // Start time for this run, Each RAS event will be 1ms different.
    private String servers_;     // String list of VoltDb servers (comma separated).
    private Random random_;      // PRNG used for deciding when job IDs and/or control operations are needed.

    private static Client client_ = null; // VoltDb client
    @SuppressWarnings("serial")
    private static final Map<Byte,String> statusString_ = new HashMap<Byte,String>() {{ // Convert status to string map.
        put(ClientResponse.USER_ABORT,         "USER_ABORT");
        put(ClientResponse.CONNECTION_LOST,    "CONNECTION_LOST");
        put(ClientResponse.CONNECTION_TIMEOUT, "CONNECTION_TIMEOUT");
        put(ClientResponse.GRACEFUL_FAILURE,   "GRACEFUL_FAILURE");
        put(ClientResponse.RESPONSE_UNKNOWN,   "RESPONSE_UNKNOWN");
        put(ClientResponse.UNEXPECTED_FAILURE, "UNEXPECTED_FAILURE");
        put(ClientResponse.SUCCESS,            "SUCCESS");
    }};
}
