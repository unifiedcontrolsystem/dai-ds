// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.IAdapter;
import com.intel.dai.dsapi.*;
import com.intel.dai.exceptions.BadInputException;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.exceptions.ProviderException;
import com.intel.dai.locations.Location;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.anyMap;

class AdapterUIRestMock extends AdapterUIRestMock2 {

    AdapterUIRestMock(String[] args) throws IOException {
        super(args);
    }

    String result = "success";
}

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

class AdapterUIRestMock3 extends AdapterUIRestMock2 {
    AdapterUIRestMock3(String[] args) throws IOException {
        super(args);
    }

    public class AdapterUIRestTest {

        @Test
        public void main() {

        }

        @Test
        public void queryCmds() {
        }

        @Test
        public void cli_cmds() {
        }

        @Test
        public void retrieveSystemInformation() throws DataStoreException {
            try {
                AdapterUIRestMock2 obj = new AdapterUIRestMock2(new String[]{"localhost"});

                ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
                obj.responseCreator.setParser(jsonParser_);

                PropertyArray racks = new PropertyArray();
                PropertyMap rackInfo = new PropertyMap();
                rackInfo.put("LCTN", "R1");
                racks.add(rackInfo);


                PropertyArray serviceNodes = new PropertyArray();
                PropertyMap serviceNodeInfo = new PropertyMap();
                serviceNodeInfo.put("LCTN", "R1-SM1");
                serviceNodes.add(serviceNodeInfo);

                PropertyArray computeNodes = new PropertyArray();
                PropertyMap computeNodeInfo = new PropertyMap();
                computeNodeInfo.put("LCTN", "R1-CH0-N1");
                computeNodes.add(computeNodeInfo);

                when(obj.configMgr.getRackConfiguration()).thenReturn(racks);
                when(obj.configMgr.getServiceNodeConfiguration()).thenReturn(serviceNodes);
                when(obj.configMgr.getComputeNodeConfiguration()).thenReturn(computeNodes);
                assertEquals("{\"Status\":\"F\"," + "\"Result\":\"{\\\"compute\\\":" + "{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\",\\\"data\\\":\\\"LCTN\\\"," + "\\\"heading\\\":\\\"LCTN\\\"}],\\\"result-data-lines\\\":1," + "\\\"result-status-code\\\":0,\\\"data\\\":[[\\\"R1-CH0-N1\\\"]]," + "\\\"result-data-columns\\\":1}," + "\\\"service\\\":{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\"," + "\\\"data\\\":\\\"LCTN\\\",\\\"heading\\\":\\\"LCTN\\\"}]," + "\\\"result-data-lines\\\":1,\\\"result-status-code\\\":0," + "\\\"data\\\":[[\\\"R1-SM1\\\"]],\\\"result-data-columns\\\":1}}\"}", obj.retrieveSystemInformation(obj));
            } catch (IOException e) {
                Assert.fail("Exception " + e);
            }
        }

        @Test
        public void concatJsonResponses() {
            try {

                AdapterUIRestMock2 obj = new AdapterUIRestMock(new String[]{"localhost"});
                ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
                obj.responseCreator.setParser(jsonParser_);
                String jsonString1 = " {" + "   \"R1-CH01-CN1\": {" + "       \"test\": {" + "           \"sensors/os/kernel_version\": {" + "               \"value\": 4.14" + "           }" + "       }" + "   }," + "   \"R1-CH01-N2\": {" + "       \"test\": {" + "           \"sensors/coretemp/package0/input\": {" + "               \"value\": 100.1," + "                   \"units\": \"C\"" + "           }" + "       }" + "   }" + " }";
                String jsonString2 = " {" + "   \"R1-CH02-CN1\": {" + "       \"test\": {" + "           \"sensors/os/kernel_version\": {" + "               \"value\": 4.14" + "           }" + "       }" + "   }," + "   \"R1-CH02-N2\": {" + "       \"test\": {" + "           \"sensors/coretemp/package0/input\": {" + "               \"value\": 102.1," + "                   \"units\": \"C\"" + "           }" + "       }" + "   }" + " }";
                assertEquals("{\"R1-CH02-N2\":{" + "\"test\":{" + "\"sensors\\/coretemp\\/package0\\/input\":{" + "\"units\":\"C\",\"value\":102.1}}}," + "\"R1-CH01-N2\":{" + "\"test\":{" + "\"sensors\\/coretemp\\/package0\\/input\":{" + "\"units\":\"C\",\"value\":100.1}}}," + "\"R1-CH01-CN1\":{" + "\"test\":{" + "\"sensors\\/os\\/kernel_version\":{" + "\"value\":4.14}}}," + "\"R1-CH02-CN1\":{" + "\"test\":{" + "\"sensors\\/os\\/kernel_version\":{" + "\"value\":4.14" + "}}}}",
                        obj.responseCreator.concatControlJsonResponses(new ArrayList<String>(Arrays.asList(jsonString1, jsonString2))));

            } catch (IOException | ConfigIOParseException e) {
                Assert.fail("Exception " + e);
            }
        }

        @Test
        public void addDevicesToGroup() throws ProviderException, DataStoreException, IOException {

            AdapterUIRestMock2 obj = new AdapterUIRestMock2(new String[]{"localhost"});

            ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
            obj.responseCreator.setParser(jsonParser_);

            when(obj.groupsMgr.addDevicesToGroup(anyString(), anySet())).thenReturn("success");
            assertEquals("{\"Status\":\"F\",\"Result\":\"success\"}", obj.addDevicesToGroup("g1", "R1-CH01-N1"));
        }

        @Test
        public void addDevicesToGroupError() throws ProviderException, DataStoreException, IOException {

            AdapterUIRestMock2 obj = new AdapterUIRestMock2(new String[]{"localhost"});

            ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
            obj.responseCreator.setParser(jsonParser_);

            doThrow(DataStoreException.class).when(obj.groupsMgr).addDevicesToGroup(anyString(), anySet());
            assertEquals("{\"Status\":\"E\",\"Result\":null}", obj.addDevicesToGroup("g1", "R1-CH01-N1"));
        }

        @Test
        public void deleteDevicesFromGroup() throws ProviderException, DataStoreException, IOException {

            AdapterUIRestMock2 obj = new AdapterUIRestMock2(new String[]{"localhost"});

            ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
            obj.responseCreator.setParser(jsonParser_);

            when(obj.groupsMgr.deleteDevicesFromGroup(anyString(), anySet())).thenReturn("success");
            assertEquals("{\"Status\":\"F\",\"Result\":\"success\"}", obj.deleteDevicesFromGroup("g1", "R1-CH01-N1"));
        }

        @Test
        public void deleteDevicesFromGroupError() throws ProviderException, DataStoreException, IOException {

            AdapterUIRestMock2 obj = new AdapterUIRestMock2(new String[]{"localhost"});

            ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
            obj.responseCreator.setParser(jsonParser_);

            doThrow(DataStoreException.class).when(obj.groupsMgr).deleteDevicesFromGroup(anyString(), anySet());
            assertEquals("{\"Status\":\"E\",\"Result\":null}", obj.deleteDevicesFromGroup("g1", "R1-CH01-N1"));
        }

        @Test
        public void getDevicesFromGroup() throws ProviderException, DataStoreException, IOException {

            AdapterUIRestMock2 obj = new AdapterUIRestMock2(new String[]{"localhost"});

            ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
            obj.responseCreator.setParser(jsonParser_);

            Set<String> devices = new HashSet<>();
            devices.add("R1-CH01-N1");
            devices.add("R1-CH02-N1");

            when(obj.groupsMgr.getDevicesFromGroup("g1")).thenReturn(devices);
            assertEquals("{\"Status\":\"F\",\"Result\":\"R1-CH01-N1,R1-CH02-N1\"}", obj.getDevicesFromGroup("g1"));
        }

        @Test
        public void getDevicesFromGroupError() throws ProviderException, DataStoreException, IOException {

            AdapterUIRestMock2 obj = new AdapterUIRestMock2(new String[]{"localhost"});

            ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
            obj.responseCreator.setParser(jsonParser_);

            doThrow(DataStoreException.class).when(obj.groupsMgr).getDevicesFromGroup("g1");
            assertEquals("{\"Status\":\"E\",\"Result\":null}", obj.getDevicesFromGroup("g1"));
        }

        @Test
        public void listGroups() throws ProviderException, DataStoreException, IOException {

            AdapterUIRestMock2 obj = new AdapterUIRestMock2(new String[]{"localhost"});

            ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
            obj.responseCreator.setParser(jsonParser_);

            Set<String> groups = new HashSet<>();
            groups.add("g1");
            groups.add("g2");

            when(obj.groupsMgr.listGroups()).thenReturn(groups);
            assertEquals("{\"Status\":\"F\",\"Result\":\"g1,g2\"}", obj.listGroups());
        }

        @Test
        public void listGroupsError() throws ProviderException, DataStoreException, IOException {

            AdapterUIRestMock2 obj = new AdapterUIRestMock2(new String[]{"localhost"});

            ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
            obj.responseCreator.setParser(jsonParser_);

            doThrow(DataStoreException.class).when(obj.groupsMgr).listGroups();
            assertEquals("{\"Status\":\"E\",\"Result\":null}", obj.listGroups());
        }

        @Test
        public void getLocations() throws ProviderException, IOException {
            AdapterUIRestMock2 obj = new AdapterUIRestMock2(new String[]{"localhost"});

            ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
            obj.responseCreator.setParser(jsonParser_);

            assertEquals("{\"system\":\"mock\",\"nodes\":{\"node1\":\"location1\"}}", obj.getLocations());
        }

    }
}

