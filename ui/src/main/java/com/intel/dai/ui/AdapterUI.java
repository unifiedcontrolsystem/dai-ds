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

public abstract class AdapterUI {
    IAdapter adapter;
    Configuration configMgr;
    Groups groupsMgr;
    Logger log_;
    Location locationMgr;
    LocationApi locationApi;

    AdapterUI(Logger logger) {
        log_ = logger;
    }

    void initialize(String sThisAdaptersAdapterType, String sAdapterName,
                    String[] args) {
        try {
            log_.info("starting");

            adapter = AdapterSingletonFactory.getAdapter();

            // Get list of VoltDb servers, location of service node this adapter is running on, and service node's hostname.
            final String DbServers  = (args.length >= 1) ? args[0] : "localhost";  // e.g., voltdbserver1,voltdbserver2,10.11.12.13
            final String SnLctn     = (args.length >= 2) ? args[1] : "UnknownLctn";
            final String SnHostname = (args.length >= 3) ? args[2] : "UnknownHostName";
            log_.info("this adapter instance is running on lctn=%s, hostname=%s, pid=%d", SnLctn, SnHostname, adapter.pid());
            // Set up the adapter instance.

            setupFactoryObjects(args, adapter);
        }   // End try
        catch (Exception e) {
            log_.exception(e);
            System.exit(-1);
        }
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
}
