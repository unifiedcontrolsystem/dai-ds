// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;
import org.voltdb.client.ClientFactory;
import org.voltdb.client.ClientResponse;


import java.io.IOException;

final class VoltDbClient {
    private static final long MAX_CONNECTION_RETRY_SECONDS = 300; // 5 minutes
    private static final long CONNECTION_RESPONSE_TIMEOUT_SECONDS = 60; // 1 minute
    private static final long PROCEDURE_CALL_TIMEOUT_SECONDS = CONNECTION_RESPONSE_TIMEOUT_SECONDS * 3;
    private static boolean hookInstalled = false;
    static Client voltClient = null;

    private VoltDbClient() {}// Singleton style. Cannot construct!

    static void initializeVoltDbClient(String[] servers) {
        if(!hookInstalled) {
            Runtime.getRuntime().addShutdownHook(new Thread(VoltDbClient::shutdownHook));
            hookInstalled = true;
        }
        if(voltClient != null) {
            return;
        }
        if(servers != null) {
            connectToVoltDb(servers);
        }
        else {
            String[] localhost = { "localhost" };
            connectToVoltDb(localhost);
        }
    }

    // Close resource on normal shutdown...
    private static void shutdownHook() {
        if (voltClient != null) {
            try {
                voltClient.close();
            } catch (InterruptedException e) { /* Should not happen in practice. */ }
        }
    }

    static void failedConnection() {
        try {
            voltClient.close();
        } catch(InterruptedException e) { /* Ignore possible error */ }
        voltClient = null;
    }

    //-----------------------------------------------------------------
    // Connect to the VoltDB database servers/nodes
    // Parms:
    //      sListOfVoltDbServers - is a string containing a comma separated list of VoltDB servers
    //-----------------------------------------------------------------
    private static void connectToVoltDb(String[] servers) {
        ClientConfig config = VoltClientConfiguration.getVoltClientConfiguration();
        config.setReconnectOnConnectionLoss(true);
        config.setMaxConnectionRetryInterval(MAX_CONNECTION_RETRY_SECONDS * 1000L);
        config.setInitialConnectionRetryInterval(MAX_CONNECTION_RETRY_SECONDS * 1000L);
        config.setConnectionResponseTimeout(CONNECTION_RESPONSE_TIMEOUT_SECONDS * 1000L);
        config.setProcedureCallTimeout(PROCEDURE_CALL_TIMEOUT_SECONDS * 1000L);
        voltClient = ClientFactory.createClient(config);
        // Connect to all the VoltDB servers
        int connections = servers.length;
        for (String server: servers) {
            try {
                voltClient.createConnection(server, Client.VOLTDB_SERVER_PORT);
            }
            catch (IOException ie) {
                connections--;
            }
        }
        if(connections == 0)
            failedConnection();
    }   // End connectToVoltDb(String sListOfVoltDbServers)

    static Client getVoltClientInstance() {
        if(voltClient == null) {
            RuntimeException e = new RuntimeException("Must call initializeVoltDbClient before calling " +
                    "getVoltClientInstance!");
            throw e;
        }
        return voltClient;
    }

    static String statusByteAsString(byte bStatus) {
        String sStatusByteAsString = null;
        if (bStatus == ClientResponse.USER_ABORT)               { sStatusByteAsString = "USER_ABORT"; }
        else if (bStatus == ClientResponse.CONNECTION_LOST)     { sStatusByteAsString = "CONNECTION_LOST"; }
        else if (bStatus == ClientResponse.CONNECTION_TIMEOUT)  { sStatusByteAsString = "CONNECTION_TIMEOUT"; }
        else if (bStatus == ClientResponse.GRACEFUL_FAILURE)    { sStatusByteAsString = "GRACEFUL_FAILURE"; }
        else if (bStatus == ClientResponse.RESPONSE_UNKNOWN)    { sStatusByteAsString = "RESPONSE_UNKNOWN"; }
        else if (bStatus == ClientResponse.UNEXPECTED_FAILURE)  { sStatusByteAsString = "UNEXPECTED_FAILURE"; }
        else if (bStatus == ClientResponse.SUCCESS)             { sStatusByteAsString = "SUCCESS"; }
        else    { sStatusByteAsString = Byte.toString( bStatus ); }
        return sStatusByteAsString;
    }   // End statusByteAsString(byte bStatus)
}
