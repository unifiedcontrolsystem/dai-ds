// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import com.intel.config_io.*;
import com.intel.properties.*;

import java.util.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayDeque;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;

public class ComponentTest {
    Component componentUT;
    PropertyMap systemManifestJson;
    PropertyMap systemManifestMissingViewsJson;
    PropertyMap systemManifestMissingViewsFullJson;
    PropertyMap systemManifestMissingViewsFullDefinitionsJson;
    PropertyMap systemManifestMissingViewsFullDefinitionsSpecificJson;
    ArrayDeque<PublishData> events;
    private ConfigIO parser_;
    int ratio_ = 2;

    String systemManifest = "{\n" +
            "\t\"sysname\": \"Development\",\n" +
            "\t\"views\": {\n" +
            "\t\t\"Full\": {\n" +
            "\t\t\t\"view\": \"Full\",\n" +
            "\t\t\t\"view-description\": \"Full Floor Layout\",\n" +
            "\t\t\t\"initzoom\": 0,\n" +
            "\t\t\t\"zoomscales\": [4, 6, 8],\n" +
            "\t\t\t\"floor\" : {\n" +
            "\t\t\t\t\"description\": \"The full floor map\",\n" +
            "\t\t\t\t\"width\" : 100, \"height\" : 200,\n" +
            "\t\t\t\t\"content\" : [\n" +
            "\t\t\t\t\t{ \"name\": \"R0\",  \"definition\": \"compute-rack\", \"x\": 55, \"y\": 1 }\n" +
            "\t\t\t\t]\n" +
            "\t\t\t},\n" +
            "\t\t\t\"definitions\": {\n" +
            "\t\t\t\t\"compute-rack\" : {\n" +
            "\t\t\t\t  \"description\": \"Compute Rack\",\n" +
            "\t\t\t\t  \"width\": 38, \"height\": 102, \"obscured\":true, \"type\": \"Rack\",\n" +
            "\t\t\t\t  \"content\" : [\n" +
            "\t\t\t\t\t{\"name\": \"CH00\", \"definition\": \"compute-chassis\", \t\"x\":0,  \"y\":4 },\n" +
            "\t\t\t\t\t{\"name\": \"CH01\", \"definition\": \"compute-chassis\", \t\"x\":0,  \"y\":4 }\n" +
            "\t\t\t\t  ]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t\"compute-chassis\": {\n" +
            "\t\t\t\t  \"description\": \"Compute Chassis\",\n" +
            "\t\t\t\t  \"width\": 38, \"height\": 4, \"type\": \"Chassis\",\n" +
            "\t\t\t\t  \"content\" : [\n" +
            "\t\t\t\t\t{\"name\": \"CN0\", \"definition\": \"compute-node\", \"x\":2,  \"y\":0},\n" +
            "\t\t\t\t\t{\"name\": \"CN1\", \"definition\": \"compute-node\", \"x\":2,  \"y\":0}\n" +
            "\t\t\t\t  ]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t\"compute-node\": {\n" +
            "\t\t\t\t  \"description\": \"Compute node\",\n" +
            "\t\t\t\t  \"width\": 18, \"height\": 2, \"type\": \"ComputeNode\",\n" +
            "\t\t\t\t  \"content\" : []\n" +
            "\t\t\t\t}\n" +
            "\n" +
            "\t\t\t},\n" +
            "\n" +
            "\t\t\t\"boot-images\" : {\n" +
            "\t\t\t\t\"content\" : [\n" +
            "\t\t\t\t\t{\"id\": \"centos7.3-slurm\",\n" +
            "\t\t\t\t\t \"description\": \"Centos 7.3 w/ Slurm VNFS\",\n" +
            "\t\t\t\t\t \"BootImageFile\": \"centos7.3-1611-slurm\",\n" +
            "\t\t\t\t\t \"BootImageChecksum\": \"7427dbf6ec4e028f22d595195fe72563\",\n" +
            "\t\t\t\t\t \"BootOptions\": \"\",\n" +
            "\t\t\t\t\t \"BootStrapImageFile\": \"3.10.0-514.16.1.el7.x86_64\",\n" +
            "\t\t\t\t\t \"BootStrapImageChecksum\": \"93a94d8985aa3b10e38122d2bd8bbba1\"\n" +
            "\t\t\t\t\t}\n" +
            "\t\t\t\t]\n" +
            "\t\t\t}\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";

    String systemManifestMissingViews = "{\n" +
            "\t\"sysname\": \"Development\",\n" +
            "}";

    String systemManifestMissingViewsFull = "{\n" +
            "\t\"sysname\": \"Development\",\n" +
            "\t\"views\": {\n" +
            "\t}\n" +
            "}";


    String systemManifestMissingViewsFullDefinitions = "{\n" +
            "\t\"sysname\": \"Development\",\n" +
            "\t\"views\": {\n" +
            "\t\t\"Full\": {\n" +
            "\t\t\t\"view\": \"Full\",\n" +
            "\t\t\t\"view-description\": \"Full Floor Layout\",\n" +
            "\t\t\t\"initzoom\": 0,\n" +
            "\t\t\t\"zoomscales\": [4, 6, 8],\n" +
            "\t\t\t\"floor\" : {\n" +
            "\t\t\t\t\"description\": \"The full floor map\",\n" +
            "\t\t\t\t\"width\" : 100, \"height\" : 200,\n" +
            "\t\t\t\t\"content\" : [\n" +
            "\t\t\t\t\t{ \"name\": \"R0\",  \"definition\": \"compute-rack\", \"x\": 55, \"y\": 1 }\n" +
            "\t\t\t\t]\n" +
            "\t\t\t},\n" +
            "\t\t\t\"boot-images\" : {\n" +
            "\t\t\t\t\"content\" : [\n" +
            "\t\t\t\t\t{\"id\": \"centos7.3-slurm\",\n" +
            "\t\t\t\t\t \"description\": \"Centos 7.3 w/ Slurm VNFS\",\n" +
            "\t\t\t\t\t \"BootImageFile\": \"centos7.3-1611-slurm\",\n" +
            "\t\t\t\t\t \"BootImageChecksum\": \"7427dbf6ec4e028f22d595195fe72563\",\n" +
            "\t\t\t\t\t \"BootOptions\": \"\",\n" +
            "\t\t\t\t\t \"BootStrapImageFile\": \"3.10.0-514.16.1.el7.x86_64\",\n" +
            "\t\t\t\t\t \"BootStrapImageChecksum\": \"93a94d8985aa3b10e38122d2bd8bbba1\"\n" +
            "\t\t\t\t\t}\n" +
            "\t\t\t\t]\n" +
            "\t\t\t}\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";

    String systemManifestMissingViewsFullDefinitionsSpecific = "{\n" +
            "\t\"sysname\": \"Development\",\n" +
            "\t\"views\": {\n" +
            "\t\t\"Full\": {\n" +
            "\t\t\t\"view\": \"Full\",\n" +
            "\t\t\t\"view-description\": \"Full Floor Layout\",\n" +
            "\t\t\t\"initzoom\": 0,\n" +
            "\t\t\t\"zoomscales\": [4, 6, 8],\n" +
            "\t\t\t\"floor\" : {\n" +
            "\t\t\t\t\"description\": \"The full floor map\",\n" +
            "\t\t\t\t\"width\" : 100, \"height\" : 200,\n" +
            "\t\t\t\t\"content\" : [\n" +
            "\t\t\t\t\t{ \"name\": \"R0\",  \"definition\": \"compute-rack\", \"x\": 55, \"y\": 1 }\n" +
            "\t\t\t\t]\n" +
            "\t\t\t},\n" +
            "\t\t\t\"definitions\": {\n" +
            "\t\t\t\t\"compute-rack\" : {\n" +
            "\t\t\t\t  \"description\": \"Compute Rack\",\n" +
            "\t\t\t\t  \"width\": 38, \"height\": 102, \"obscured\":true, \"type\": \"Rack\",\n" +
            "\t\t\t\t  \"content\" : [\n" +
            "\t\t\t\t\t{\"name\": \"CH00\", \"definition\": \"compute-chassis\", \t\"x\":0,  \"y\":4 },\n" +
            "\t\t\t\t\t{\"name\": \"CH01\", \"definition\": \"compute-chassis\", \t\"x\":0,  \"y\":4 }\n" +
            "\t\t\t\t  ]\n" +
            "\t\t\t\t},\n" +

            "\t\t\t\t\"compute-node\": {\n" +
            "\t\t\t\t  \"description\": \"Compute node\",\n" +
            "\t\t\t\t  \"width\": 18, \"height\": 2, \"type\": \"ComputeNode\",\n" +
            "\t\t\t\t  \"content\" : []\n" +
            "\t\t\t\t}\n" +
            "\n" +
            "\t\t\t},\n" +
            "\n" +
            "\t\t\t\"boot-images\" : {\n" +
            "\t\t\t\t\"content\" : [\n" +
            "\t\t\t\t\t{\"id\": \"centos7.3-slurm\",\n" +
            "\t\t\t\t\t \"description\": \"Centos 7.3 w/ Slurm VNFS\",\n" +
            "\t\t\t\t\t \"BootImageFile\": \"centos7.3-1611-slurm\",\n" +
            "\t\t\t\t\t \"BootImageChecksum\": \"7427dbf6ec4e028f22d595195fe72563\",\n" +
            "\t\t\t\t\t \"BootOptions\": \"\",\n" +
            "\t\t\t\t\t \"BootStrapImageFile\": \"3.10.0-514.16.1.el7.x86_64\",\n" +
            "\t\t\t\t\t \"BootStrapImageChecksum\": \"93a94d8985aa3b10e38122d2bd8bbba1\"\n" +
            "\t\t\t\t\t}\n" +
            "\t\t\t\t]\n" +
            "\t\t\t}\n" +
            "\t\t}\n" +
            "\t}\n" +
            "}";

    String mockSensorMetadataJSON_ = "{\n" +
            "  \"Rack\": [\n" +
            "    {\n" +
            "      \"unit\": \"V\",\n" +
            "      \"description\": \"CC_V_VDD_1_8V\",\n" +
            "      \"id\": \"1000\",\n" +
            "      \"type\": \"VoltageIn\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Chassis\": [\n" +
            "    {\n" +
            "      \"unit\": \"degC\",\n" +
            "      \"description\": \"BC_T_NODE2_PCIE_3V3VRD_TEMP\",\n" +
            "      \"id\": \"2306\",\n" +
            "      \"type\": \"Temp\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"ComputeNode\": [\n" +
            "    {\n" +
            "      \"unit\": \"degC\",\n" +
            "      \"description\": \"BC_T_NODE3_CPU0_CH2_DRAM\",\n" +
            "      \"id\": \"1462\",\n" +
            "      \"type\": \"Temp\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    PropertyMap mockSensorMetadata_;
    List<String> mockRasMetadata;
    Map<String,String> details;

    @Before
    public void SetUp() throws PropertyNotExpectedType, ConfigIOParseException, IOException {
        events = new ArrayDeque<>();
        parser_ = ConfigIOFactory.getInstance("json");
        systemManifestJson = parser_.readConfig(new ByteArrayInputStream(systemManifest.getBytes())).getAsMap();
        systemManifestMissingViewsJson = parser_.readConfig(new ByteArrayInputStream(systemManifestMissingViews.getBytes())).getAsMap();
        systemManifestMissingViewsFullJson = parser_.readConfig(new ByteArrayInputStream(systemManifestMissingViewsFull.getBytes())).getAsMap();
        systemManifestMissingViewsFullDefinitionsJson = parser_.readConfig(new ByteArrayInputStream(systemManifestMissingViewsFullDefinitions.getBytes())).getAsMap();
        systemManifestMissingViewsFullDefinitionsSpecificJson = parser_.readConfig(new ByteArrayInputStream(systemManifestMissingViewsFullDefinitionsSpecific.getBytes())).getAsMap();
        mockSensorMetadata_ = parser_.readConfig(new ByteArrayInputStream(mockSensorMetadataJSON_.getBytes())).getAsMap();;

        mockRasMetadata = new LinkedList<>();
        mockRasMetadata.add("ec_boot");
        mockRasMetadata.add("ec_node_standby");
        mockRasMetadata.add("ec_node_available");
        details = new HashMap<String, String>();
        details.put("name", "CH00");
        details.put("definition", "compute-chassis");
        details.put("parentLocation", "R0");
        details.put("parentForeignLocation", "c0-0");
        details.put("randomizerSeed", "1");
        details.put("rowCount", "5");
        details.put("startTime", String.valueOf(System.currentTimeMillis()));
    }
    @Test
    public void ComponentNameSetCorrectly() throws PropertyNotExpectedType {
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
        assertEquals(componentUT.getName(), "CH00");
    }

    @Test (expected = RuntimeException.class)
    public void ComponentNameMissingFails() throws PropertyNotExpectedType {
        details.remove("name");
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
    }

    @Test (expected = RuntimeException.class)
    public void ComponentDefinitionMissingFails() throws PropertyNotExpectedType {
        details.remove("definition");
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
    }

    @Test (expected = RuntimeException.class)
    public void ComponentParentLocationMissingFails() throws PropertyNotExpectedType {
        details.remove("parentLocation");
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
    }

    @Test (expected = RuntimeException.class)
    public void SystemManifestMissingViews() throws PropertyNotExpectedType {
        componentUT = new Component(details, systemManifestMissingViewsJson, events, ratio_, mockSensorMetadata_,
                mockRasMetadata, mock(Logger.class));
    }

    @Test (expected = RuntimeException.class)
    public void SystemManifestMissingViewsFull() throws PropertyNotExpectedType {
        componentUT = new Component(details, systemManifestMissingViewsFullJson, events, ratio_, mockSensorMetadata_,
                mockRasMetadata, mock(Logger.class));
    }

    @Test (expected = RuntimeException.class)
    public void SystemManifestMissingViewsFullDefinitions() throws PropertyNotExpectedType {
        componentUT = new Component(details, systemManifestMissingViewsFullDefinitionsJson, events, ratio_,
                mockSensorMetadata_, mockRasMetadata, mock(Logger.class));
    }

    @Test (expected = RuntimeException.class)
    public void SystemManifestMissingViewsFullDefinitionsSpecific() throws PropertyNotExpectedType {
        componentUT = new Component(details, systemManifestMissingViewsFullDefinitionsSpecificJson, events, ratio_,
                mockSensorMetadata_, mockRasMetadata, mock(Logger.class));
    }

    @Test
    public void ComponentLoadsCorrectNumberOfSubcomponents() throws PropertyNotExpectedType {
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
        assertEquals(2, componentUT.getSubcomponents().size());
        assertEquals(2,componentUT.getTotalSubcomponentsCount());
        assertEquals(5, componentUT.rowCount_);
    }

    @Test
    public void ComponentPrintsHierarchy() throws PropertyNotExpectedType {
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
        String componentList = componentUT.getHierarchy();
        System.out.println(componentList);
    }

    @Test
    public void ComponentCanStoreItsLocation() throws PropertyNotExpectedType {
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
        System.out.println(componentUT.getLocation());
    }

    @Test
    public void ComponentGetRegexMatchedLocations() throws PropertyNotExpectedType, InterruptedException {
        ArrayList<String> test = new ArrayList<>();
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
        int count = componentUT.getRegexMatchedComponentsCount(".*",test);
        assertEquals(3, count);
    }

    @Test
    public void ComponentGetRegexUnMatchedLocations() throws PropertyNotExpectedType, InterruptedException {
        ArrayList<String> test = new ArrayList<>();
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
        int count = componentUT.getRegexMatchedComponentsCount("AB.*",test);
        assertEquals(0, count);
    }

    @Test
    public void publishBootingEventsForLocationForNotSelectedRandom() throws PropertyNotExpectedType{
        ArrayList<String> regexMatchedLocationList = null;
        if(regexMatchedLocationList == null){
            regexMatchedLocationList = new ArrayList<>();
            regexMatchedLocationList.add("R0-VG");
        }
        ArrayList<String> failuredRegexMatchedNodes = new ArrayList<>();
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
        int count = componentUT.publishBootingEventsForLocation(".*", regexMatchedLocationList);
        assertEquals(0, count);
    }

    @Test
    public void publishBootingEventsForLocationForNullRegexMatchedList() throws PropertyNotExpectedType{
        ArrayList<String> regexMatchedLocationList = null;
        if(regexMatchedLocationList == null){
            regexMatchedLocationList = new ArrayList<>();
            regexMatchedLocationList.add("R0");
        }
        ArrayList<String> failedRegexMatchedNodes;
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
        int count = componentUT.publishBootingEventsForLocation("RQ", regexMatchedLocationList);
        assertEquals(0, count);
    }

    @Test
    public void publishBootingEventsForLocationForNullRegexMatchedList_2() throws PropertyNotExpectedType{
        ArrayList<String> regexMatchedLocationList = null;
        if(regexMatchedLocationList == null){
            regexMatchedLocationList = new ArrayList<>();
            regexMatchedLocationList.add("OM");
            regexMatchedLocationList.add("OI");
        }
        int intProbabilityValue = 1;
        ArrayList<String> failedRegexMatchedNodes = null;
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
        int count = componentUT.publishBootingEventsForLocation(".*", regexMatchedLocationList);
        assertEquals(0, count);
    }

    @Test
    public void publishBootingEventsForLocationForNullRegexMatchedList_toHitBootingEvents()
            throws PropertyNotExpectedType {
        ArrayList<String> regexMatchedLocationList = null;
        if(regexMatchedLocationList == null){
            regexMatchedLocationList = new ArrayList<>();
            regexMatchedLocationList.add("OM");
            regexMatchedLocationList.add("OI");
            regexMatchedLocationList.add("OII");
        }
        ArrayList<String> failedRegexMatchedNodes = null;
        componentUT = new Component(details, systemManifestJson, events, ratio_, mockSensorMetadata_, mockRasMetadata,
                mock(Logger.class));
        int count = componentUT.publishBootingEventsForLocationWithBF(".*", regexMatchedLocationList,2);
        assertEquals(0, count);
    }

}
