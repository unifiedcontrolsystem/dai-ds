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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.*;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

class AdapterUIRestMock extends AdapterUIRestMock2 {

    AdapterUIRestMock(String[] args) throws IOException {
        super(args);
    }

    @Override
    String workItemBlocking(String adapterType, Map<String, String> params, String workWantDone) {
        return result;
    }

    String result = "success";
}

class AdapterUIRestMock2 extends AdapterUIRest {

    AdapterUIRestMock2(String[] args) throws IOException {
        super(args, mock(Logger.class));
        nodeLocation = mock(Location.class);
        location = mock(com.intel.dai.dsapi.Location.class);
        PropertyMap map = new PropertyMap();
        PropertyMap nodeMap = new PropertyMap();
        nodeMap.put("node1", "location1");
        map.put("system", "mock");
        map.put("nodes", nodeMap);
        when(location.getSystemLocations()).thenReturn(map);
    }

    @Override
    void initialize(String sThisAdaptersAdapterType, String sAdapterName, String[] args) {
        adapter = mock(IAdapter.class);
        setupFactoryObjects(args, adapter);
    }

    @Override
    Location createLocationObject(Set<String> nodes) {
        return nodeLocation;
    }

    @Override
    void setupFactoryObjects(String[] args, IAdapter adapter) {
        //Create WorkQueue from the Factory
        workQueue = mock(WorkQueue.class);
        configMgr = mock(Configuration.class);
        groupsMgr = mock(Groups.class);
        raseventapi = mock(RasEventApi.class);
        bootImage = mock(BootImage.class);
        serviceInfo = mock(ServiceInformation.class);
        try {
            when(serviceInfo.getServiceOperationInfo(anyString())).thenReturn(new HashMap<>(){{
                put("one", new Object());
            }});
        } catch (IOException | ProcCallException e) {/* Cannot happen with 'when' */}
    }

    Location nodeLocation;
}

public class AdapterUIRestTest {
    AdapterUIRestMock obj;
    AdapterUIRestMock2 obj2;
    ConfigIO jsonParser_;

    @Before
    public void setUp() throws IOException {
        String[] args = new String[] { "localhost" };
        jsonParser_ = ConfigIOFactory.getInstance("json");
        obj = new AdapterUIRestMock(args);
        obj.initialize("test", "test", args);
        obj.setParser(jsonParser_);
        obj2 = new AdapterUIRestMock2(args);
        obj2.initialize("test", "test", args);
        obj2.setParser(jsonParser_);
    }

    @Test
    public void main() {

    }

    @Test
    public void powerCommands() {
        try {
            assertEquals(obj.power_commands(null, "on"), "success");
            assertEquals(obj.power_commands(null, "off"), "success");
            assertEquals(obj.power_commands(null, "cycle"), "success");
            assertEquals(obj.power_commands(null, "default"), "{\"Status\":\"E\",\"Result\":\"Command not found!\"}");
        } catch (IOException | InterruptedException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void diagnosticsInband() {
    }

    @Test
    public void biosUpdate() {
        try {
            assertEquals(obj.bios_update(null), "success");
        } catch (IOException | InterruptedException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void biosToggles() {
        try {
            assertEquals(obj.bios_toggles(null, "get-option"), "success");
            assertEquals(obj.bios_toggles(null, "set-option"), "success");
            assertEquals(obj.bios_toggles(null, "list-options"), "success");
            assertEquals(obj.bios_toggles(null, "default"), "{\"Status\":\"E\",\"Result\":\"Command not found!\"}");
        } catch (IOException | InterruptedException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void queryCmds() {
    }

    @Test
    public void cli_cmds() {
    }

    @Test
    public void bios_version() {
        try {
            assertEquals(obj.bios_version(null), "success");
        } catch (IOException | InterruptedException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void getHardwareInfo() {
        try {
            assertEquals(obj.get_hardware_info(null), "success");
        } catch (IOException | InterruptedException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void getOsInfo() {
        try {
            assertEquals(obj.get_os_info(null), "success");
        } catch (IOException | InterruptedException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void getInventorySnapshot() {
        try {
            assertEquals(obj.get_inventory_snapshot(null), "success");
        } catch (IOException | InterruptedException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void resourceCommands() throws DataStoreException {
        try {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("locations", "testLocation");
            Set<String> devicesInGroup = new HashSet<>();
            when(obj.groupsMgr.getDevicesFromGroup(anyString())).thenReturn(devicesInGroup);
            assertEquals(obj.resource_commands(parameters, "add"), "success");
            assertEquals(obj.resource_commands(parameters, "remove"), "success");
            assertEquals(obj.resource_commands(parameters, "check"), "success");
            assertEquals(obj.resource_commands(parameters, "default"), "{\"Status\":\"E\",\"Result\":\"Command not found!\"}");
        } catch (IOException | InterruptedException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void serviceCommands() {
        try {
            Map<String, String> parameters = new HashMap<String, String>();
            parameters.put("device","testlocation");
            assertEquals(obj.service_commands(parameters, "repair-start"), "success");
            assertEquals(obj.service_commands(parameters, "repair-end"), "success");
            assertEquals(obj.service_commands(parameters, "default"), "{\"Status\":\"E\",\"Result\":\"Command not found!\"}");
        }catch(IOException | InterruptedException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void jobLaunchCommands() {
        try {
            assertEquals(obj.job_launch_commands(null, "launch"), "success");
            assertEquals(obj.job_launch_commands(null, "check"), "success");
            assertEquals(obj.job_launch_commands(null, "cancel"), "success");
            assertEquals(obj.job_launch_commands(null, "default"), "{\"Status\":\"E\",\"Result\":\"Command not found!\"}");
        } catch (IOException | InterruptedException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void sensorCommands() {
        try {
            assertEquals(obj.sensor_commands(null, "get"), "success");
            assertEquals(obj.sensor_commands(null, "get_over_time"), "success");
            assertEquals(obj.sensor_commands(null, "default"), "{\"Status\":\"E\",\"Result\":\"Command not found!\"}");
        } catch (IOException | InterruptedException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void discoverCommands() {
        try {
            assertEquals(obj.discover_commands(null, "Discover"), "success");
        } catch (IOException | InterruptedException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void retrieveSystemInformation() throws DataStoreException {
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

        when(obj2.configMgr.getRackConfiguration()).thenReturn(racks);
        when(obj2.configMgr.getServiceNodeConfiguration()).thenReturn(serviceNodes);
        when(obj2.configMgr.getComputeNodeConfiguration()).thenReturn(computeNodes);
        assertEquals("{\"Status\":\"F\"," + "\"Result\":\"{\\\"compute\\\":" + "{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\",\\\"data\\\":\\\"LCTN\\\"," + "\\\"heading\\\":\\\"LCTN\\\"}],\\\"result-data-lines\\\":1," + "\\\"result-status-code\\\":0,\\\"data\\\":[[\\\"R1-CH0-N1\\\"]]," + "\\\"result-data-columns\\\":1}," + "\\\"service\\\":{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\"," + "\\\"data\\\":\\\"LCTN\\\",\\\"heading\\\":\\\"LCTN\\\"}]," + "\\\"result-data-lines\\\":1,\\\"result-status-code\\\":0," + "\\\"data\\\":[[\\\"R1-SM1\\\"]],\\\"result-data-columns\\\":1}}\"}", obj2.retrieveSystemInformation(obj2));
    }

    @Test
    public void concatJsonStrings() {
        String jsonString1 = " {" + "   \"R1-CH01-CN1\": {" + "       \"test\": {" + "           \"sensors/os/kernel_version\": {" + "               \"value\": 4.14" + "           }" + "       }" + "   }," + "   \"R1-CH01-N2\": {" + "       \"test\": {" + "           \"sensors/coretemp/package0/input\": {" + "               \"value\": 100.1," + "                   \"units\": \"C\"" + "           }" + "       }" + "   }" + " }";
        String jsonString2 = " {" + "   \"R1-CH02-CN1\": {" + "       \"test\": {" + "           \"sensors/os/kernel_version\": {" + "               \"value\": 4.14" + "           }" + "       }" + "   }," + "   \"R1-CH02-N2\": {" + "       \"test\": {" + "           \"sensors/coretemp/package0/input\": {" + "               \"value\": 102.1," + "                   \"units\": \"C\"" + "           }" + "       }" + "   }" + " }";
        assertEquals("{\"R1-CH02-N2\":{" + "\"test\":{" + "\"sensors\\/coretemp\\/package0\\/input\":{" + "\"units\":\"C\",\"value\":102.1}}}," + "\"R1-CH01-N2\":{" + "\"test\":{" + "\"sensors\\/coretemp\\/package0\\/input\":{" + "\"units\":\"C\",\"value\":100.1}}}," + "\"R1-CH01-CN1\":{" + "\"test\":{" + "\"sensors\\/os\\/kernel_version\":{" + "\"value\":4.14}}}," + "\"R1-CH02-CN1\":{" + "\"test\":{" + "\"sensors\\/os\\/kernel_version\":{" + "\"value\":4.14" + "}}}}", obj2.concatJsonStrings(jsonString1, jsonString2));
    }

    @Test
    public void flattenMapToString() {
        Map<String, String> testMap = new HashMap<>();
        testMap.put("key1", "value1");
        testMap.put("key2", "value2");
        assertEquals("key1=value1\nkey2=value2\n", AdapterUIRestMock2.flattenMapToString(testMap));
    }

    @Test
    public void createRasEventWithNolocation() throws ProviderException, ConfigIOParseException {
        Map<String, String> param = new HashMap<>();
        param.put("eventtype", "100005");
        doNothing().when(obj.raseventapi).createRasEvent(any());
        String result = obj.generateRasEvents(param);
        PropertyMap finalData = jsonParser_.fromString(result).getAsMap();
        String actual = finalData.get("Result").toString();
        String expectedStatus = finalData.get("Status").toString();
        assertEquals("F", expectedStatus);
        assertEquals("Success", actual);
    }

    @Test
    public void createRasEventWithlocation() throws ProviderException {
        try {
            Map<String, String> param = new HashMap<>();
            param.put("eventtype", "100005");
            param.put("location", "node00");
            doNothing().when(obj2.raseventapi).createRasEvent(any());
            Map<String, String> data = new HashMap<>();
            data.put("node00", "node00");
            when(obj2.nodeLocation.getLocation()).thenReturn(data);
            String result = obj2.generateRasEvents(param);
            PropertyMap finalData = jsonParser_.fromString(result).getAsMap();
            String actual = finalData.get("Result").toString();
            String expectedStatus = finalData.get("Status").toString();
            assertEquals("F", expectedStatus);
            assertEquals("Success", actual);
        } catch (ConfigIOParseException | BadInputException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void errorCreateRasEvents() throws ProviderException {
        try {
            doThrow(new ProviderException("Error in creating rasevents")).when(obj2.raseventapi).createRasEvent(any());
            Map<String, String> param = new HashMap<>();
            param.put("eventtype", "100005");
            String result = obj2.generateRasEvents(param);
            PropertyMap finalData = jsonParser_.fromString(result).getAsMap();
            String actual = (String) finalData.get("Result");
            String expectedStatus = finalData.get("Status").toString();
            assertEquals("E", expectedStatus);
            assertEquals("Error: Error in creating rasevents", actual);
        } catch (ConfigIOParseException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void getRasEventTypes() throws ProviderException {
        try {
            PropertyMap data = new PropertyMap();
            data.put("data", "test");
            Map<String, String> param = new HashMap<>();
            param.put("eventtype", "100005");
            when(obj2.raseventapi.getRasEventTypes(param)).thenReturn(data);
            String result = obj2.listRasEventTypes(param);
            PropertyMap finalData = jsonParser_.fromString(result).getAsMap();
            String actual = (String) finalData.get("Result");
            String expectedStatus = finalData.get("Status").toString();
            assertEquals("F", expectedStatus);
            assertEquals("{\"data\":\"test\"}", actual);
        } catch (ConfigIOParseException e) {
            Assert.fail("Exception " + e);
        }
    }

    @Test
    public void workItemBlockingForNonRackAdapters() throws IOException, InterruptedException {
        Map<String, String> params = new HashMap<>();
        String[] result = new String[2];
        result[0] = "F";
        result[1] = "Success";
        params.put("location", "R1-CH01-N1");
        when(obj2.adapter.adapterType()).thenReturn("resource_manager");
        when(obj2.workQueue.baseWorkItemId()).thenReturn(1L);
        when(obj2.workQueue.queueWorkItem("WLM", null, "UseNode", params, true, "resource_manager", 1L)).thenReturn(2L);
        when(obj2.workQueue.waitForWorkItemToFinishAndMarkDone("UseNode", "WLM", 2L, "resource_manager", 1L)).thenReturn(result);
        assertEquals("{\"Status\":\"F\",\"Result\":\"Success\"}", obj2.workItemBlocking("WLM", params, "UseNode"));
    }

    @Test
    public void workItemBlockingForRackAdaptersWithUnknownLocation() throws IOException, InterruptedException, BadInputException {
        Map<String, String> params = new HashMap<>();
        params.put("device", "R1-CH01-N1");
        doThrow(BadInputException.class).when(obj2.nodeLocation).getAggregatorNodeLocations();
        assertEquals("{\"Status\":\"E\",\"Result\":\"Bad input.\\nnull\"}", obj2.workItemBlocking("CONTROL", params, "PowerOnNode"));
    }

    @Test
    public void workItemBlockingForRackAdaptersWithknownLocation() throws IOException, InterruptedException, BadInputException {
        Map<String, String> params = new HashMap<>();
        params.put("device", "R1-CH01-N1");

        Map<String, Set<String>> aggregatorToNodes = new HashMap<>();
        Set<String> nodes = new HashSet<>();
        nodes.add("R1-CH01-N1");
        aggregatorToNodes.put("SN1-SM1", nodes);

        String[] result = new String[2];
        result[0] = "F";
        result[1] = " {" +
                "   \"R1-CH01-N1\": {" +
                "       \"test\": {"  +
                "           \"power\": {" +
                "               \"value\": true" +
                "           }" +
                "       }" +
                "   }" +
                "}";

        ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
        obj.setParser(jsonParser_);

        when(obj2.nodeLocation.getAggregatorNodeLocations()).thenReturn(aggregatorToNodes);

        when(obj2.adapter.adapterType()).thenReturn("control");
        when(obj2.workQueue.baseWorkItemId()).thenReturn(1L);
        when(obj2.workQueue.queueWorkItem("CONTROL", "SN1-SM1", "PowerOnNode", params, true, "control", 1L)).thenReturn(2L);
        when(obj2.workQueue.waitForWorkItemToFinishAndMarkDone("PowerOnNode", "CONTROL", 2L, "control", 1L)).thenReturn(result);

        assertEquals("{\"Status\":\"F\",\"Result\":\" {   \\\"R1-CH01-N1\\\": {       " + "\\\"test\\\": {           \\\"power\\\": {               " + "\\\"value\\\": true           }       }   }}\"}", obj2.workItemBlocking("CONTROL", params, "PowerOnNode"));
    }

    @Test
    public void workItemBlockingForRackAdaptersWithmultipleLocations() throws IOException, InterruptedException, BadInputException {
        Map<String, String> params = new HashMap<>();
        params.put("device", "R1-CH01-N1,R1-CH02-N1");

        Map<String, Set<String>> aggregatorToNodes = new HashMap<>();
        Set<String> nodes1 = new HashSet<>();
        nodes1.add("R1-CH01-N1");
        aggregatorToNodes.put("SN1-SM1", nodes1);
        Set<String> nodes2 = new HashSet<>();
        nodes2.add("R1-CH02-N1");
        aggregatorToNodes.put("SN1-SM2", nodes2);

        String[] result1 = new String[2];
        result1[0] = "F";
        result1[1] = " {" + "   \"R1-CH01-N1\": {" + "       \"test\": {" + "           \"power\": {" + "               \"value\": true" + "           }" + "       }" + "   }" + "}";

        String[] result2 = new String[2];
        result2[0] = "F";
        result2[1] = " {" + "   \"R1-CH02-N1\": {" + "       \"test\": {" + "           \"power\": {" + "               \"value\": true" + "           }" + "       }" + "   }" + "}";

        when(obj2.nodeLocation.getAggregatorNodeLocations()).thenReturn(aggregatorToNodes);

        when(obj2.adapter.adapterType()).thenReturn("control");
        when(obj2.workQueue.baseWorkItemId()).thenReturn(1L);

        when(obj2.workQueue.queueWorkItem("CONTROL", "SN1-SM1", "PowerOnNode", params, true, "control", 1L)).thenReturn(2L);

        when(obj2.workQueue.queueWorkItem("CONTROL", "SN1-SM2", "PowerOnNode", params, true, "control", 1L)).thenReturn(3L);

        when(obj2.workQueue.waitForWorkItemToFinishAndMarkDone("PowerOnNode", "CONTROL", 2L, "control", 1L)).thenReturn(result1);

        when(obj2.workQueue.waitForWorkItemToFinishAndMarkDone("PowerOnNode", "CONTROL", 3L, "control", 1L)).thenReturn(result2);

        assertEquals("{\"Status\":\"F\",\"Result\":\"{\\\"R1-CH01-N1\\\":{\\\"test\\\":{\\\"power\\\":" + "{\\\"value\\\":true}}},\\\"R1-CH02-N1\\\":{\\\"test\\\":{\\\"power\\\":{\\\"value\\\":true}}}}\"}", obj2.workItemBlocking("CONTROL", params, "PowerOnNode"));
    }

    @Test
    public void addDevicesToGroup() throws DataStoreException {
        when(obj2.groupsMgr.addDevicesToGroup(anyString(), anySet())).thenReturn("success");
        assertEquals("{\"Status\":\"F\",\"Result\":\"success\"}", obj2.addDevicesToGroup("g1", "R1-CH01-N1"));
    }

    @Test
    public void addDevicesToGroupError() throws DataStoreException {
        doThrow(DataStoreException.class).when(obj2.groupsMgr).addDevicesToGroup(anyString(), anySet());
        assertEquals("{\"Status\":\"E\",\"Result\":null}", obj2.addDevicesToGroup("g1", "R1-CH01-N1"));
    }

    @Test
    public void deleteDevicesFromGroup() throws DataStoreException {
        when(obj2.groupsMgr.deleteDevicesFromGroup(anyString(), anySet())).thenReturn("success");
        assertEquals("{\"Status\":\"F\",\"Result\":\"success\"}", obj2.deleteDevicesFromGroup("g1", "R1-CH01-N1"));
    }

    @Test
    public void deleteDevicesFromGroupError() throws DataStoreException {
        doThrow(DataStoreException.class).when(obj2.groupsMgr).deleteDevicesFromGroup(anyString(), anySet());
        assertEquals("{\"Status\":\"E\",\"Result\":null}", obj2.deleteDevicesFromGroup("g1", "R1-CH01-N1"));
    }

    @Test
    public void getDevicesFromGroup() throws DataStoreException {
        Set<String> devices = new HashSet<>();
        devices.add("R1-CH01-N1");
        devices.add("R1-CH02-N1");

        when(obj2.groupsMgr.getDevicesFromGroup("g1")).thenReturn(devices);
        assertEquals("{\"Status\":\"F\",\"Result\":\"R1-CH01-N1,R1-CH02-N1\"}", obj2.getDevicesFromGroup("g1"));
    }

    @Test
    public void getDevicesFromGroupError() throws DataStoreException {
        doThrow(DataStoreException.class).when(obj2.groupsMgr).getDevicesFromGroup("g1");
        assertEquals("{\"Status\":\"E\",\"Result\":null}", obj2.getDevicesFromGroup("g1"));
    }

    @Test
    public void listGroups() throws DataStoreException {
        Set<String> groups = new HashSet<>();
        groups.add("g1");
        groups.add("g2");

        when(obj2.groupsMgr.listGroups()).thenReturn(groups);
        assertEquals("{\"Status\":\"F\",\"Result\":\"g1,g2\"}", obj2.listGroups());
    }

    @Test
    public void listGroupsError() throws DataStoreException {
        doThrow(DataStoreException.class).when(obj2.groupsMgr).listGroups();
        assertEquals("{\"Status\":\"E\",\"Result\":null}", obj2.listGroups());
    }

    @Test
    public void addProvisioningProfile() throws DataStoreException {
        Map<String, String> profile = new HashMap<>();

        when(obj2.bootImage.addBootImageProfile(profile)).thenReturn("success");
        assertEquals("{\"Status\":\"F\"," + "\"Result\":\"{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\"," + "\\\"data\\\":\\\"Message\\\",\\\"heading\\\":\\\"Message\\\"}]," + "\\\"result-data-lines\\\":1,\\\"result-status-code\\\":0," + "\\\"data\\\":[[\\\"success\\\"]],\\\"result-data-columns\\\":1}\"}", obj2.addProvisioningProfile(profile));
    }

    @Test
    public void addProvisioningProfileError() throws DataStoreException {
        Map<String, String> profile = new HashMap<>();

        doThrow(DataStoreException.class).when(obj2.bootImage).addBootImageProfile(profile);
        assertEquals("{\"Status\":\"FE\"," + "\"Result\":\"{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\",\\\"data\\\":\\\"Message\\\"," + "\\\"heading\\\":\\\"Message\\\"}]," + "\\\"result-data-lines\\\":1,\\\"result-status-code\\\":1,\\\"result-data-columns\\\":1," + "\\\"error\\\":[[null]]}\"}", obj2.addProvisioningProfile(profile));
    }

    @Test
    public void editProvisioningProfile() throws DataStoreException {
        Map<String, String> profile = new HashMap<>();

        when(obj2.bootImage.editBootImageProfile(profile)).thenReturn("success");
        assertEquals("{\"Status\":\"F\"," + "\"Result\":\"{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\"," + "\\\"data\\\":\\\"Message\\\",\\\"heading\\\":\\\"Message\\\"}]," + "\\\"result-data-lines\\\":1,\\\"result-status-code\\\":0," + "\\\"data\\\":[[\\\"success\\\"]],\\\"result-data-columns\\\":1}\"}", obj2.editProvisioningProfile(profile));
    }

    @Test
    public void editProvisioningProfileError() throws DataStoreException {
        Map<String, String> profile = new HashMap<>();

        doThrow(DataStoreException.class).when(obj2.bootImage).editBootImageProfile(profile);
        assertEquals("{\"Status\":\"FE\"," + "\"Result\":\"{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\",\\\"data\\\":\\\"Message\\\"," + "\\\"heading\\\":\\\"Message\\\"}]," + "\\\"result-data-lines\\\":1,\\\"result-status-code\\\":1,\\\"result-data-columns\\\":1," + "\\\"error\\\":[[null]]}\"}", obj2.editProvisioningProfile(profile));
    }

    @Test
    public void deleteProvisioningProfile() throws DataStoreException {
        List<String> profiles = new ArrayList<>();
        profiles.add("CNOS");
        profiles.add("SLES");

        when(obj2.bootImage.listBootImageProfiles()).thenReturn(profiles);
        when(obj2.bootImage.deleteBootImageProfile("CNOS")).thenReturn("SUCCESS");
        assertEquals("{\"Status\":\"F\",\"Result\":\"{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\"," + "\\\"data\\\":\\\"Message\\\",\\\"heading\\\":\\\"Message\\\"}]," + "\\\"result-data-lines\\\":1,\\\"result-status-code\\\":0," + "\\\"data\\\":[[\\\"SUCCESS\\\"]],\\\"result-data-columns\\\":1}\"}", obj2.deleteProvisioningProfile("CNOS"));
    }

    @Test
    public void deleteProvisioningProfileProfileMissing() throws DataStoreException {
        List<String> profiles = new ArrayList<>();
        profiles.add("CNOS");
        profiles.add("SLES");

        when(obj2.bootImage.listBootImageProfiles()).thenReturn(profiles);
        assertEquals("{\"Status\":\"FE\"," + "\"Result\":\"{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\",\\\"data\\\":\\\"Message\\\"," + "\\\"heading\\\":\\\"Message\\\"}]," + "\\\"result-data-lines\\\":1,\\\"result-status-code\\\":1,\\\"result-data-columns\\\":1," + "\\\"error\\\":[[\\\"Profile Id DIAG doesn't exist\\\"]]}\"}", obj2.deleteProvisioningProfile("DIAG"));
    }

    @Test
    public void deleteProvisioningProfileError() throws DataStoreException {
        List<String> profiles = new ArrayList<>();
        profiles.add("CNOS");
        profiles.add("SLES");

        when(obj2.bootImage.listBootImageProfiles()).thenReturn(profiles);
        doThrow(DataStoreException.class).when(obj2.bootImage).deleteBootImageProfile("CNOS");
        assertEquals("{\"Status\":\"FE\"," + "\"Result\":\"{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\",\\\"data\\\":\\\"Message\\\"," + "\\\"heading\\\":\\\"Message\\\"}]," + "\\\"result-data-lines\\\":1,\\\"result-status-code\\\":1,\\\"result-data-columns\\\":1," + "\\\"error\\\":[[null]]}\"}", obj2.deleteProvisioningProfile("CNOS"));
    }

    @Test
    public void listProvisioningProfiles() throws DataStoreException {
        List<String> profiles = new ArrayList<>();
        profiles.add("CNOS");
        profiles.add("SLES");

        when(obj2.bootImage.listBootImageProfiles()).thenReturn(profiles);
        assertEquals("{\"Status\":\"F\"," + "\"Result\":\"{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\"," + "\\\"data\\\":\\\"Profiles\\\",\\\"heading\\\":\\\"Profiles\\\"}]," + "\\\"result-data-lines\\\":2,\\\"result-status-code\\\":0," + "\\\"data\\\":[[\\\"CNOS\\\"],[\\\"SLES\\\"]],\\\"result-data-columns\\\":1}\"}", obj2.listProvisioningProfiles());
    }

    @Test
    public void listProvisioningProfilesError() throws DataStoreException {
        doThrow(DataStoreException.class).when(obj2.bootImage).listBootImageProfiles();
        assertEquals("{\"Status\":\"FE\"," + "\"Result\":\"{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\",\\\"data\\\":\\\"Message\\\"," + "\\\"heading\\\":\\\"Message\\\"}]," + "\\\"result-data-lines\\\":1,\\\"result-status-code\\\":1,\\\"result-data-columns\\\":1," + "\\\"error\\\":[[null]]}\"}", obj2.listProvisioningProfiles());
    }

    @Test
    public void getProvisioningProfilesInfo() throws DataStoreException {
        Map<String, String> bootImg = new HashMap<>();
        bootImg.put("id", "CNOS");
        bootImg.put("description", "compute Node OS");
        bootImg.put("vnfs", "compute");
        bootImg.put("vnfs_checksum", "0123456123");
        bootImg.put("bootstrapimage", "4.11-mos-06-19-2019");
        bootImg.put("bootstrapimage_checksum", "234561234567");
        bootImg.put("kernelargs", "kenel=1, args=2");

        when(obj2.bootImage.retrieveBootImageProfile("CNOS")).thenReturn(bootImg);
        assertEquals("{\"Status\":\"F\"," + "\"Result\":\"{\\\"CNOS\\\":{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\"," + "\\\"data\\\":\\\"key\\\",\\\"heading\\\":\\\"key\\\"}," + "{\\\"unit\\\":\\\"string\\\",\\\"data\\\":\\\"value\\\"," + "\\\"heading\\\":\\\"value\\\"}],\\\"result-data-lines\\\":7," + "\\\"result-status-code\\\":0,\\\"data\\\":[[\\\"vnfs\\\",\\\"compute\\\"]," + "[\\\"kernelargs\\\",\\\"kenel=1, args=2\\\"]," + "[\\\"bootstrapimage\\\",\\\"4.11-mos-06-19-2019\\\"]," + "[\\\"description\\\",\\\"compute Node OS\\\"]," + "[\\\"bootstrapimage_checksum\\\",\\\"234561234567\\\"]," + "[\\\"vnfs_checksum\\\",\\\"0123456123\\\"]," + "[\\\"id\\\",\\\"CNOS\\\"]],\\\"result-data-columns\\\":2}}\"}", obj2.getProvisioningProfilesInfo(new String[]{"CNOS"}));
    }

    @Test
    public void getProvisioningProfilesInfoError() throws DataStoreException {
        doThrow(DataStoreException.class).when(obj2.bootImage).retrieveBootImageProfile("CNOS");
        assertEquals("{\"Status\":\"FE\"," + "\"Result\":\"{\\\"schema\\\":[{\\\"unit\\\":\\\"string\\\",\\\"data\\\":\\\"Message\\\"," + "\\\"heading\\\":\\\"Message\\\"}]," + "\\\"result-data-lines\\\":1,\\\"result-status-code\\\":1,\\\"result-data-columns\\\":1," + "\\\"error\\\":[[null]]}\"}", obj2.getProvisioningProfilesInfo(new String[]{"CNOS"}));
    }

    @Test
    public void getLocations() {
        assertEquals("{\"system\":\"mock\",\"nodes\":{\"node1\":\"location1\"}}", obj2.getLocations());
    }

    @Test
    public void commandAllowed() throws Exception {
        when(obj.adapter.getRasEventType(anyString())).thenReturn("0101010101");
        assertTrue(obj.commandAllowed("service","Exclusive","me", "me","R0-CH0-CN0"));
        assertFalse(obj.commandAllowed("service","Exclusive","you", "me","R0-CH0-CN0"));
        assertTrue(obj.commandAllowed("service","","you", "me","R0-CH0-CN0"));
        assertFalse(obj.commandAllowed("not service","","you", "me","R0-CH0-CN0"));
    }

    @Test
    public void commandAllowedWLM() throws Exception {
        when(obj.adapter.getRasEventType(anyString())).thenReturn("0101010101");
        assertTrue(obj.commandAllowedWLM("command", "me", "R0-CH0-CN0", "me"));
        assertFalse(obj.commandAllowedWLM("command", "me", "R0-CH0-CN0", "you"));
        assertTrue(obj.commandAllowedWLM("command", "root", "R0-CH0-CN0", "you"));
    }
}

