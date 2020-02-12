// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.exceptions.AdapterException;
import com.intel.logging.*;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.dsapi.WorkQueue;
import org.voltdb.client.*;
import java.lang.*;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * AdapterNearlineTier for the VoltDB database.
 *
 * Parms:
 *  List of the db node names, so that this client connects to each of them.
 *  E.g., dbServer1, dbServer2, ...
 *
 * Example invocation:
 *      java AdapterNearlineTier 10.254.89.8  (or java AdapterNearlineTier  - this will default to using localhost)
 */
public abstract class AdapterNearlineTier {
    IAdapter adapter;
    Logger log_;
    WorkQueue workQueue;
    SyncAdapterShutdownHandler shutdownHandler;
    String rabbitMQ = "localhost";
    protected static final long MAX_SHUTDOWN_TIME_SEC = 10L;
    protected static final String CONSUMER_TAG = "DataReceiver";
    protected static final long SHUTDOWN_CHECK_INTERVAL_MS = 1000L;

    // Constructor
    AdapterNearlineTier(Logger logger) throws IOException, TimeoutException {
        log_ = logger;
        shutdownHandler = new SyncAdapterShutdownHandler(log_, MAX_SHUTDOWN_TIME_SEC);
    }   // ctor

    public void initializeAdapter() throws IOException, TimeoutException, DataStoreException {
        adapter = AdapterSingletonFactory.getAdapter();
    }


    //--------------------------------------------------------------------------
    // This method handles receipt of the historical data that is being moved from Tier1 to Tier2.
    //--------------------------------------------------------------------------
    public abstract long receiveDataFromDataMover() throws InterruptedException, IOException, ProcCallException, TimeoutException, DataStoreException;

    //--------------------------------------------------------------------------
    // This method handles the general processing flow for NearlineTier adapters (regardless of specific implementation, e.g. VoltDB).
    //--------------------------------------------------------------------------
    public void mainProcessingFlow(String[] args) throws IOException, TimeoutException {
        try {
            log_.info("starting");

            // Get list of VoltDb servers, location of service node this adapter is running on, and service node's hostname.
            final String DbServers  = (args.length >= 1) ? args[0] : "localhost";  // e.g., voltdbserver1,voltdbserver2,10.11.12.13
            final String SnLctn     = (args.length >= 2) ? args[1] : "UnknownLctn";
            final String SnHostname = (args.length >= 3) ? args[2] : "UnknownHostName";
            log_.info("this adapter instance is running on lctn=%s, hostname=%s, pid=%d", SnLctn, SnHostname, adapter.pid());
            // Set up the adapter instance.
            workQueue = adapter.setUpAdapter(DbServers, SnLctn, shutdownHandler);

            //-----------------------------------------------------------------
            // Main processing loop
            //-----------------------------------------------------------------
            while(!adapter.adapterShuttingDown()) {
                // Handle any work items that have been queued for this type of adapter.
                boolean bGotWorkItem = workQueue.grabNextAvailWorkItem();
                if (bGotWorkItem) {
                    // did get a work item
                    processClientParams(workQueue.getClientParameters(","));
                    switch(workQueue.workToBeDone()) {
                        case "DataReceiver":
                            long rc = receiveDataFromDataMover();
                            break;

                        default:
                            workQueue.handleProcessingWhenUnexpectedWorkItem();
                            break;
                    }   // end of switch - workToBeDone()
                }   // did get a work item


                // Sleep for a little bit if no work items are queued for this adapter type.
                if (workQueue.amtTimeToWait() > 0 && !adapter.adapterShuttingDown())
                    Thread.sleep( Math.min(workQueue.amtTimeToWait(), 5) * 100);
            }   // End while loop - handle any work items that have been queued for this type of adapter.

            log_.info("Signaling shutdown process is complete...");
            shutdownHandler.signalShutdownComplete();
            log_.info("Exiting main processing loop...");

            //-----------------------------------------------------------------
            // Clean up adapter table, base work item, and close connections to db.
            //-----------------------------------------------------------------
            adapter.handleMainlineAdapterCleanup(adapter.adapterAbnormalShutdown());
        }   // End try
        catch (RuntimeException | InterruptedException | AdapterException | ProcCallException | DataStoreException e) {
            adapter.handleMainlineAdapterException(e);
        }
    }   // End mainProcessingFlow(String[] args)

    private void processClientParams(String[] clientParameters) {
        for(String nameValue: clientParameters) {
            if(nameValue.startsWith("RabbitMQHost="))
                rabbitMQ = nameValue.substring(nameValue.indexOf("=")+1).trim();
        }
    }

}   // End class AdapterNearlineTier