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
import com.intel.dai.dsapi.NodeInformation;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public abstract class AdapterUI {
    IAdapter adapter;
    WorkQueue workQueue;
    Configuration configMgr;
    Groups groupsMgr;
    BootImage bootImage;
    Logger log_;
    NodeInformation nodeinfo;
    RasEventApi raseventapi;
    EventsLog eventlog;
    Location location;
    ServiceInformation serviceInfo;
    WLMInformation wlmInfo;

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
            workQueue = adapter.setUpAdapter(DbServers, SnLctn);

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
        bootImage = factory.createBootImageApi(adapter);
        InventorySnapshot inv = factory.createInventorySnapshotApi();
        nodeinfo = factory.createNodeInformation();
        eventlog = factory.createEventsLog(adapter.adapterName(), adapter.adapterType());
        raseventapi = new RasEventApi(eventlog);
        location = factory.createLocation();
        serviceInfo = factory.createServiceInformation();
        wlmInfo = factory.createWLMInformation();
    }

    IAdapter getAdapter() { return adapter; }
    /**
     * cmd_args are: 'power', 'on', 'device'
     */
    public abstract String power_commands(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException;
    public abstract String bios_update(Map<String, String> parameters) throws IOException, InterruptedException;
    public abstract String bios_version(Map<String, String> parameters) throws IOException, InterruptedException;
    public abstract String bios_toggles(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException;
    public abstract String get_hardware_info(Map<String, String> parameters) throws IOException, InterruptedException;
    public abstract String get_os_info(Map<String, String> parameters) throws IOException, InterruptedException;
    public abstract String get_inventory_snapshot(Map<String, String> parameters) throws IOException, InterruptedException;
    public abstract String diagnostics_inband(String nodes, String image, String test, String result) throws IOException, InterruptedException;
    public abstract String resource_commands(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException;
    public abstract String job_launch_commands(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException;
    public abstract String sensor_commands(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException;
    public abstract String query_cmds(String cmd, HashMap<String, String> params) throws IOException, InterruptedException;
    public abstract String discover_commands(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException;
    public abstract String canned_cmds(String cmd, Map<String, String> params) throws IOException, InterruptedException;
    public abstract String service_commands(Map<String, String> parameters, String sub_cmd) throws IOException, InterruptedException;
    public abstract String addDevicesToGroup(String groupName, String devices);
    public abstract String deleteDevicesFromGroup(String groupName, String devices);
    public abstract String getDevicesFromGroup(String groupName);
    public abstract String listGroups();
    public abstract String addProvisioningProfile(Map<String, String> parameters);
    public abstract String editProvisioningProfile(Map<String, String> parameters);
    public abstract String deleteProvisioningProfile(String profileIdToDelete);
    public abstract String listProvisioningProfiles();
    public abstract String getProvisioningProfilesInfo(String[] profileIds);
    public abstract String fanSpeedControl(Map<String, String> parameters) throws IOException, InterruptedException;
    public abstract String flashLedControl(Map<String, String> parameters) throws IOException, InterruptedException;
}
