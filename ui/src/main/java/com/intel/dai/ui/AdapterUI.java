// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;

import com.intel.dai.AdapterSingletonFactory;
import com.intel.dai.dsapi.*;
import com.intel.dai.exceptions.ProviderException;
import com.intel.logging.*;
import com.intel.dai.Adapter;
import com.intel.dai.IAdapter;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public abstract class AdapterUI {
    IAdapter adapter;
    WorkQueue workQueue;
    Configuration configMgr;
    Groups groupsMgr;
    Logger log_;
    Location locationMgr;
    LocationApi locationApi;

    AdapterUI(String[] args, Logger logger) throws ProviderException, IOException, TimeoutException {
        log_ = logger;
        initialize(args);
    }

    void initialize(String[] args) throws ProviderException, IOException, TimeoutException {
        adapter = AdapterSingletonFactory.getAdapter();
        setupFactoryObjects(args, adapter);

    }

    void setupFactoryObjects(String[] args, IAdapter adapter) throws ProviderException {
        //Create WorkQueue from the Factory
        DataStoreFactory factory = new DataStoreFactoryImpl(args, log_);
        configMgr = factory.createConfiguration();
        groupsMgr = factory.createGroups();
        locationMgr = factory.createLocation();
        locationApi = new LocationApi(groupsMgr, locationMgr);
    }

    IAdapter getAdapter() { return adapter; }

    public abstract String query_cmds(String cmd, HashMap<String, String> params) throws ProviderException;
    public abstract String canned_cmds(String cmd, Map<String, String> params) throws ProviderException;
    public abstract String addDevicesToGroup(String groupName, String devices);
    public abstract String deleteDevicesFromGroup(String groupName, String devices);
    public abstract String getDevicesFromGroup(String groupName);
    public abstract String listGroups();
    public abstract void receiveDataFromUsers();
    public void mainProcessingFlow(String[] args) throws IOException, TimeoutException {
        try {
            log_.info("starting");

            // Get list of VoltDb servers, location of service node this adapter is running on, and service node's hostname.
            final String DbServers = (args.length >= 1) ? args[0] : "localhost";  // e.g., voltdbserver1,voltdbserver2,10.11.12.13
            final String SnLctn = (args.length >= 2) ? args[1] : "UnknownLctn";
            final String SnHostname = (args.length >= 3) ? args[2] : "UnknownHostName";
            log_.info("this adapter instance is running on lctn=%s, hostname=%s, pid=%d", SnLctn, SnHostname, adapter.pid());
            // Set up the adapter instance.
            workQueue = adapter.setUpAdapter(DbServers, SnLctn);

            //-----------------------------------------------------------------
            // Main processing loop
            //-----------------------------------------------------------------
            while (adapter.adapterShuttingDown() == false) {
                // Handle any work items that have been queued for this type of adapter.
                boolean bGotWorkItem = workQueue.grabNextAvailWorkItem();
                if (bGotWorkItem == true) {
                    // did get a work item
                    //processClientParams(workQueue.getClientParameters(","));
                    switch (workQueue.workToBeDone()) {
                        case "HandleInputFromUsers":
                            receiveDataFromUsers();
                            break;

                        default:
                            workQueue.handleProcessingWhenUnexpectedWorkItem();
                            break;
                    }   // end of switch - workToBeDone()
                }   // did get a work item


                // Sleep for a little bit if no work items are queued for this adapter type.
                if (workQueue.amtTimeToWait() > 0 && !adapter.adapterShuttingDown())
                    Thread.sleep(Math.min(workQueue.amtTimeToWait(), 5) * 100);
            }   // End while loop - handle any work items that have been queued for this type of adapter.

            log_.info("Signaling shutdown process is complete...");
            log_.info("Exiting main processing loop...");
        }
        catch (Exception e) {
            adapter.handleMainlineAdapterException(e);
        }
    }   // End mainProcessingFlow(String[] args)

}
