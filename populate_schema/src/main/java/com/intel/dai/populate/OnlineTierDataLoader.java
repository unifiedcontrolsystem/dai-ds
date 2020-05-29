// Copyright (C) 2017-2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.populate;

import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.DataLoaderApi;
import com.intel.dai.dsapi.DbStatusApi;
import com.intel.dai.dsapi.DbStatusEnum;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;

import java.io.IOException;
import java.time.Instant;

public class OnlineTierDataLoader {
    private static final long LOOP_SLEEP = 2000; // 2 seconds in ms.

    public OnlineTierDataLoader(DataStoreFactory dsFactory, String servers, String manifestFile,
                                String machineConfigFile, String rasMetaDataFile, Logger log) {
        this.dsFactory = dsFactory;
        this.servers = servers;
        this.manifestFile = manifestFile;
        this.machineConfigFile = machineConfigFile;
        this.rasMetaDataFile = rasMetaDataFile;
        this.log = log;
        defaultLoader = new DefaultOnlineTierDataLoader(log, dsFactory);
    }

    public int populateOnlineTier() {
        int returnCode = 0;

        long timeouts = Long.parseLong(System.getProperty("com.intel.dai.populate.OnlineTierDataLoader.timeout", "500"));
        connectToVoltDb(timeouts); // 300 seconds; 5 minute timeout
        if(client == null) {
            log.error("Failed to connect to voltdb within %d seconds", timeouts);
            return 1;
        }
        DbStatusApi statusApi = null;
        try {
            statusApi = dsFactory.createDbStatusApi(client);
            if(statusApi.getStatus() == DbStatusEnum.POPULATE_DATA_COMPLETED) {
                client.close();
                return 0;
            }
        } catch(DataStoreException | InterruptedException e) {
            log.exception(e, "Unable to initialize Online Data Store API for retrieving DB status");
            returnCode = 1;
        }
        long targetTime = Instant.now().getEpochSecond() + timeouts; // 5 minute timeout
        while(!checkInitialStatusOfOnlineTier() && Instant.now().getEpochSecond() < targetTime)
            try { Thread.sleep(LOOP_SLEEP); } catch (InterruptedException e) { /* Ignore */ }
        if(!checkInitialStatusOfOnlineTier())
            return 1;

        if (statusApi != null) {
            try {
                statusApi.setDataPopulationStarting();
                // Load tier 1 from configuration
                returnCode = populateOnlineTierFromConfig();
                statusApi.setDataPopulationComplete("OK");
            } catch (DataStoreException ex) {
                log.exception(ex, "Unable to check Nearline tier status or set Online tier DB status");
                returnCode = 1;
                try {
                    statusApi.setDataPopulationFailed(makeExceptionDetailsText(ex));
                } catch(DataStoreException e) {
                    log.exception(e, "Failed to set populate data error state");
                }
            }
        }

        return returnCode;
    }

    boolean checkInitialStatusOfOnlineTier() {
        boolean ok = true;
        try {
            DbStatusApi statusApi = dsFactory.createDbStatusApi(client);
            try {
                DbStatusEnum status = statusApi.getStatus();
                switch (status) {
                    case SCHEMA_LOADING:
                        log.warn("The Schema is currently loading and the schema cannot be populated");
                        ok = false;
                        break;
                    case SCHEMA_ERROR:
                        log.warn("The Schema is currently in an error state and cannot be populated");
                        ok = false;
                        break;
                    case POPULATE_DATA_STARTED:
                        log.warn("The Schema is currently being populated and the schema cannot be populated concurrently");
                        ok = false;
                        break;
                    case POPULATE_DATA_ERROR:
                        log.warn("The Schema failed to populate last time and is in a n unknown state");
                        ok = false;
                        break;
                    case POPULATE_DATA_COMPLETED:
                        log.info("The Schema is currently populated, exiting with success...");
                        break;
                    case SCHEMA_LOADED:
                        break;
                    default:
                        log.error("The Schema is not loaded in the online tier (i.e. no tables or procedures)");
                        ok = false;
                }
            } catch (DataStoreException e) {
                log.warn("Failed to get status of the Online DB");
                ok = false;
            }
        } catch(DataStoreException e) {
            log.exception(e, "Unable to initialize Online Data Store API for retrieving DB status");
            ok = false;
        }
        return ok;
    }

    protected void connectToVoltDb(long timeout) {
        ClientConfig config;
        Client voltClient;
        long targetTime = Instant.now().getEpochSecond() + timeout;
        while(client == null && Instant.now().getEpochSecond() < targetTime) {
            config = new ClientConfig("", "", null);
            config.setReconnectOnConnectionLoss(true);
            voltClient = ClientFactory.createClient(config);
            // Connect to all the VoltDB servers
            for (String server : servers.split(",")) {
                try {
                    voltClient.createConnection(server, Client.VOLTDB_SERVER_PORT);
                    client = voltClient;
                } catch (IOException ie) {
                    log.warn("Failed to connect to VoltDB server...");
                    try { Thread.sleep(LOOP_SLEEP); } catch(InterruptedException e) { /* Ignore */ }
                }
            }
        }
        if(client == null) {
            config = new ClientConfig("", "", null);
            config.setReconnectOnConnectionLoss(true);
            voltClient = ClientFactory.createClient(config);
            // Connect to all the VoltDB servers
            for (String server : servers.split(",")) {
                try {
                    voltClient.createConnection(server, Client.VOLTDB_SERVER_PORT);
                    client = voltClient;
                } catch (IOException ie) {
                    log.exception(ie);
                }
            }
        }
    }

    static String makeExceptionDetailsText(DataStoreException ex) {
        StringBuilder builder = new StringBuilder();
        builder.append(ex.getMessage()).append("\n");
        Throwable current = ex;
        while(current != null) {
            for (StackTraceElement element : ex.getStackTrace())
                builder.append(element.toString()).append("\n");
            current = current.getCause();
            if(current != null)
                builder.append("Caused By: ").append(current.getMessage()).append("\n");
        }
        return builder.toString();
    }

    private int populateOnlineTierFromConfig() {
        return defaultLoader.doPopulate(servers, manifestFile, machineConfigFile, rasMetaDataFile);
    }

    // Convenience method for unit tests
    void setDefaultLoader(DefaultOnlineTierDataLoader defaultLoader) {
        this.defaultLoader = defaultLoader;
    }

    private DataStoreFactory dsFactory;
    private DefaultOnlineTierDataLoader defaultLoader;
    private String servers;
    private String manifestFile;
    private String machineConfigFile;
    private String rasMetaDataFile;
    private Logger log;
            Client client = null;
}