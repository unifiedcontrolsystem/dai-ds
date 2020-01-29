// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.ui;

import com.intel.dai.IAdapter;
import com.intel.dai.dsapi.Configuration;
import com.intel.dai.dsapi.Groups;
import com.intel.dai.locations.Location;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import java.io.IOException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Description of class AdapterUIRestMock2.
 */
class AdapterUIRestMock2 extends AdapterUIRest {

    AdapterUIRestMock2(String[] args) throws IOException {
        super(args, mock(Logger.class));
        nodeLocation = mock(Location.class);
        locationMgr = mock(com.intel.dai.dsapi.Location.class);
        PropertyMap map = new PropertyMap();
        PropertyMap nodeMap = new PropertyMap();
        nodeMap.put("node1", "location1");
        map.put("system", "mock");
        map.put("nodes", nodeMap);
        when(locationMgr.getSystemLocations()).thenReturn(map);
        locationApi = mock(LocationApi.class);
        initialize("UI", "TEST", args);
    }

    @Override
    void initialize(String sThisAdaptersAdapterType, String sAdapterName, String[] args) {
        adapter = mock(IAdapter.class);
        setupFactoryObjects(args, adapter);
    }

    @Override
    void setupFactoryObjects(String[] args, IAdapter adapter) {
        //Create WorkQueue from the Factory
        configMgr = mock(Configuration.class);
        groupsMgr = mock(Groups.class);
    }

    Location nodeLocation;
}
