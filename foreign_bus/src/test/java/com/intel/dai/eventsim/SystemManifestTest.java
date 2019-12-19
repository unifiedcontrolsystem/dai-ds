// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.intel.config_io.*;
import com.intel.properties.*;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.mockito.Mockito.mock;

public class SystemManifestTest {

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
            "\t\t\t\t\t{ \"name\": \"R0\",  \"definition\": \"compute-rack\", \"x\": 55, \"y\": 1 },\n" +
            "\t\t\t\t\t{ \"name\": \"R1-2\",  \"definition\": \"compute-rack\", \"x\": 55, \"y\": 1 }\n" +
            "\t\t\t\t]\n" +
            "\t\t\t},\n" +
            "\t\t\t\"definitions\": {\n" +
            "\t\t\t\t\"compute-rack\" : {\n" +
            "\t\t\t\t  \"description\": \"Compute Rack\",\n" +
            "\t\t\t\t  \"width\": 38, \"height\": 102, \"obscured\":true, \"type\": \"Rack\",\n" +
            "\t\t\t\t  \"content\" : [\n" +
            "\t\t\t\t\t{\"name\": \"CH00\", \"definition\": \"compute-chassis\", \t\"x\":0,  \"y\":4 }\n" +
            "\t\t\t\t  ]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t\"compute-chassis\": {\n" +
            "\t\t\t\t  \"description\": \"Compute Chassis\",\n" +
            "\t\t\t\t  \"width\": 38, \"height\": 4, \"type\": \"Chassis\",\n" +
            "\t\t\t\t  \"content\" : [\n" +
            "\t\t\t\t\t{\"name\": \"CN0\", \"definition\": \"compute-node\", \"x\":2,  \"y\":0}\n" +
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

    String systemManifestLarge = "{\n" +
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
            "\t\t\t\t\t{ \"name\": \"R0\",  \"definition\": \"compute-rack\", \"x\": 55, \"y\": 1 },\n" +
            "\t\t\t\t\t{ \"name\": \"R1\",  \"definition\": \"compute-rack\", \"x\": 55, \"y\": 1 },\n" +
            "\t\t\t\t\t{ \"name\": \"R2\",  \"definition\": \"compute-rack\", \"x\": 55, \"y\": 1 },\n" +
            "\t\t\t\t\t{ \"name\": \"R3\",  \"definition\": \"compute-rack\", \"x\": 55, \"y\": 1 }\n" +
            "\t\t\t\t]\n" +
            "\t\t\t},\n" +
            "\t\t\t\"definitions\": {\n" +
            "\t\t\t\t\"compute-rack\" : {\n" +
            "\t\t\t\t  \"description\": \"Compute Rack\",\n" +
            "\t\t\t\t  \"width\": 38, \"height\": 102, \"obscured\":true, \"type\": \"Rack\",\n" +
            "\t\t\t\t  \"content\" : [\n" +
            "\t\t\t\t\t{\"name\": \"CH00\", \"definition\": \"compute-chassis\", \t\"x\":0,  \"y\":4 },\n" +
            "\t\t\t\t\t{\"name\": \"CH01\", \"definition\": \"compute-chassis\", \t\"x\":0,  \"y\":4 },\n" +
            "\t\t\t\t\t{\"name\": \"CH02\", \"definition\": \"compute-chassis\", \t\"x\":0,  \"y\":4 },\n" +
            "\t\t\t\t\t{\"name\": \"CH03\", \"definition\": \"compute-chassis\", \t\"x\":0,  \"y\":4 }\n" +
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

    String systemManifestWithServiceNode = "{\n" +
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
            "\t\t\t\t\t{ \"name\": \"R0\",  \"definition\": \"service-rack\", \"x\": 55, \"y\": 1 },\n" +
            "\t\t\t\t\t{ \"name\": \"R1-2\",  \"definition\": \"service-rack\", \"x\": 55, \"y\": 1 }\n" +
            "\t\t\t\t]\n" +
            "\t\t\t},\n" +
            "\t\t\t\"definitions\": {\n" +
            "\t\t\t\t\"service-rack\" : {\n" +
            "\t\t\t\t  \"description\": \"Service Rack\",\n" +
            "\t\t\t\t  \"width\": 38, \"height\": 102, \"obscured\":true, \"type\": \"Rack\",\n" +
            "\t\t\t\t  \"content\" : [\n" +
            "\t\t\t\t\t{\"name\": \"CH00\", \"definition\": \"service-chassis\", \t\"x\":0,  \"y\":4 }\n" +
            "\t\t\t\t  ]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t\"service-chassis\": {\n" +
            "\t\t\t\t  \"description\": \"Service Chassis\",\n" +
            "\t\t\t\t  \"width\": 38, \"height\": 4, \"type\": \"Chassis\",\n" +
            "\t\t\t\t  \"content\" : [\n" +
            "\t\t\t\t\t{\"name\": \"CB0\", \"definition\": \"service-blade\", \"x\":2,  \"y\":0}\n" +
            "\t\t\t\t  ]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t\"service-blade\": {\n" +
            "\t\t\t\t  \"description\": \"Service Blade\",\n" +
            "\t\t\t\t  \"width\": 18, \"height\": 2, \"type\": \"Blade\",\n" +
            "\t\t\t\t  \"content\" : [" +
            "\t\t\t\t\t{\"name\": \"CN0\", \"definition\": \"service-node\", \"x\":2,  \"y\":0}\n" +
            "\t\t\t\t\t]\n" +
            "\t\t\t\t},\n" +
            "\t\t\t\t\"service-node\": {\n" +
            "\t\t\t\t  \"description\": \"Service node\",\n" +
            "\t\t\t\t  \"width\": 18, \"height\": 2, \"type\": \"ServiceNode\",\n" +
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
            "  ],\n" +
            "  \"Blade\": [],\n" +
            "  \"ServiceNode\": []\n" +
            "}";

    String mockSensorMetadataJSON_WithBladeServiceOnly = "{\n" +
            "  \"Rack\": [],\n" +
            "  \"Chassis\": [],\n" +
            "  \"ComputeNode\": [],\n" +
            "  \"Blade\": [\n" +
            "    {\n" +
            "      \"unit\": \"V\",\n" +
            "      \"description\": \"CC_V_VDD_1_8V\",\n" +
            "      \"id\": \"1000\",\n" +
            "      \"type\": \"VoltageIn\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"ServiceNode\": [\n" +
            "    {\n" +
            "      \"unit\": \"degC\",\n" +
            "      \"description\": \"BC_T_NODE2_PCIE_3V3VRD_TEMP\",\n" +
            "      \"id\": \"2306\",\n" +
            "      \"type\": \"Temp\"\n" +
            "    }\n" +
            "  ]\n" +
            "}";

    PropertyMap systemManifestJson;
    PropertyMap systemManifestJson_ServiceBladeOnly;
    PropertyMap systemManifestLargeJson;
    PropertyMap systemManifestJsonWithServiceNode;
    PropertyMap systemManifestMissingViewsJson;
    PropertyMap systemManifestMissingViewsFullJson;
    SystemManifest manifest_;
    ConcurrentLinkedQueue<PublishData> eventQueue_;
    private ConfigIO parser_;
    int eventRatio = 2;
    PropertyMap mockSensorMetadata_;
    List<String> mockRasMetadata;

    @Before
    public void setUp() throws ConfigIOParseException, PropertyNotExpectedType, IOException {
        parser_ = ConfigIOFactory.getInstance("json");
        manifest_ = new SystemManifest(mock(Logger.class));
        systemManifestJson = parser_.readConfig(new ByteArrayInputStream(systemManifest.getBytes())).getAsMap();
        //systemManifestJson_ServiceBladeOnly = parser_.readConfig(new ByteArrayInputStream(mockSensorMetadataJSON_WithBladeServiceOnly.getBytes())).getAsMap();
        systemManifestLargeJson = parser_.readConfig(new ByteArrayInputStream(systemManifestLarge.getBytes())).getAsMap();
        systemManifestJsonWithServiceNode = parser_.readConfig(new ByteArrayInputStream(systemManifestWithServiceNode.getBytes())).getAsMap();
        systemManifestMissingViewsJson = parser_.readConfig(new ByteArrayInputStream(systemManifestMissingViews.getBytes())).getAsMap();
        systemManifestMissingViewsFullJson = parser_.readConfig(new ByteArrayInputStream(systemManifestMissingViewsFull.getBytes())).getAsMap();
        eventQueue_ = new ConcurrentLinkedQueue<>();
        mockSensorMetadata_ = parser_.readConfig(new ByteArrayInputStream(mockSensorMetadataJSON_.getBytes())).getAsMap();;
        systemManifestJson_ServiceBladeOnly = parser_.readConfig(new ByteArrayInputStream(mockSensorMetadataJSON_WithBladeServiceOnly.getBytes())).getAsMap();;

        mockRasMetadata = new LinkedList<>();
        mockRasMetadata.add("ec_boot");
        mockRasMetadata.add("ec_node_standby");
        mockRasMetadata.add("ec_node_available");
    }

    @Test
    public void SystemManifestCanParseSystemName() throws PropertyNotExpectedType {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        assertEquals(manifest_.getSystemName(), "Development");
    }


    @Test (expected = RuntimeException.class)
    public void SystemManifestCannotParseSystemMissingViews() throws PropertyNotExpectedType {
        manifest_.generateSystem(systemManifestMissingViewsJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        assertEquals(manifest_.getSystemName(), "Development");
    }

    @Test (expected = RuntimeException.class)
    public void SystemManifestCannotParseSystemMissingViewsFull() throws PropertyNotExpectedType {
        manifest_.generateSystem(systemManifestMissingViewsFullJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        assertEquals(manifest_.getSystemName(), "Development");
    }

    @Test
    public void SystemManifestCanPrintHierarchy() throws PropertyNotExpectedType {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        manifest_.getHierarchy();
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void SystemManifestCanPublishCorrectEventCountValidRegexLabel() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "a") > 0)
            manifest_.publishEventsFromLocation(18,".*", ".*");
        assertEquals(18, eventQueue_.size());
        int rasCount = 0;
        int sensorCount =0;

        while(eventQueue_.size() > 0) {
            PublishData  ev = eventQueue_.poll();
            if (ev.subject_.compareTo("events") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("telemetry") == 0) {
                sensorCount++;
            }
        }
        assertEquals(12,sensorCount);
        assertEquals(6, rasCount);
    }

    @Test
    public void SystemManifestCanPublishCorrectEventCountInvalidLabel() throws PropertyNotExpectedType, InterruptedException, Exception {
        exception.expect(Exception.class);
        exception.expectMessage("No Matched RAS/Sensor Data to generate events.");
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "a") > 0)
            manifest_.publishEventsFromLocation(18,".*", "AS.*");
    }

    @Test
    public void SystemManifestCanPublishCorrectEventCount_CaseZeroCount() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "a") > 0)
            manifest_.publishEventsFromLocation(0,".*",".*");
        assertEquals(0, eventQueue_.size());
        int rasCount = 0;
        int sensorCount = 0;

        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("evt") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("env") == 0) {
                sensorCount++;
            }
        }
        assertEquals(0,sensorCount);
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCanPublishCorrectEventCount_CaseZeroCount_InvalidLabel() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "a") > 0)
            manifest_.publishEventsFromLocation(0,".*", "AS.*");
        assertEquals(0, eventQueue_.size());
        int rasCount = 0;
        int sensorCount = 0;

        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("evt") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("env") == 0) {
                sensorCount++;
            }
        }
        assertEquals(0,sensorCount);
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCanPublishCorrectEventCountUnevenlyDivided() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "a") > 0)
            manifest_.publishEventsFromLocation(19,".*", ".*");
        assertEquals(19, eventQueue_.size());
        int rasCount = 0;
        int sensorCount =0;

        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("events") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("telemetry") == 0) {
                sensorCount++;
            }
        }
        assertEquals(13,sensorCount);
        assertEquals(6, rasCount);
    }

    @Test
    public void SystemManifestCanPublishCorrectEventCountUnevenlyDivided_InvalidLabel() throws PropertyNotExpectedType, InterruptedException, Exception {
        exception.expect(Exception.class);
        exception.expectMessage("No Matched RAS/Sensor Data to generate events.");
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "a") > 0)
            manifest_.publishEventsFromLocation(19,".*", "AS.*");
    }

    @Test
    public void SystemManifestCannotPublishCorrectEventCount() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount("AB.*", "a") > 0)
            manifest_.publishEventsFromLocation(18,"AB.*", ".*");
        assertEquals(0, eventQueue_.size());
        int rasCount = 0;
        int sensorCount = 0;

        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("events") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("telemetry") == 0) {
                sensorCount++;
            }
        }
        assertEquals(0,sensorCount);
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCannotPublishCorrectEventCount_InvalidLabel() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount("AB.*", "a") > 0)
            manifest_.publishEventsFromLocation(18,"AB.*", "AS.*");
        assertEquals(0, eventQueue_.size());
        int rasCount = 0;
        int sensorCount = 0;

        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("events") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("telemetry") == 0) {
                sensorCount++;
            }
        }
        assertEquals(0,sensorCount);
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCanPublishBootSequenceWithComputeNode() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","b") > 0)
            manifest_.publishBootSequenceFromLocation(".*",100);
        int rasCount = 0;
        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("stateChanges") == 0){
                rasCount++;
            }
        }
        assertEquals(6, rasCount);
    }

    @Test
    public void SystemManifestCanPublishBootSequenceWithComputeNode_100() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","b") > 0)
            manifest_.publishBootSequenceFromLocation(".*",0);
        int rasCount = 0;
        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("stateChanges") == 0){
                rasCount++;
            }
        }
        assertEquals(6, rasCount);
    }

    @Test
    public void SystemManifestCanPublishBootSequenceWithComputeNode_50() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","b") > 0)
            manifest_.publishBootSequenceFromLocation(".*",50);
        int rasCount = 0;
        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("stateChanges") == 0){
                rasCount++;
            }
        }
        assertNotEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCanPublishBootSequenceWithComputeNode_50_Large() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestLargeJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","b") > 0)
            manifest_.publishBootSequenceFromLocation(".*",99);
        int rasCount = 0;
        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("stateChanges") == 0){
                rasCount++;
            }
        }
        assertNotEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCanPublishBootSequenceWithServiceNode() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestJsonWithServiceNode, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "b") > 0)
            manifest_.publishBootSequenceFromLocation(".*",0);
        int rasCount = 0;
        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("stateChanges") == 0){
                rasCount++;
            }
        }
        assertEquals(6, rasCount);
    }

    @Test
    public void SystemManifestCanPublishBootSequenceWithServiceNode_100() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestJsonWithServiceNode, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","b") > 0)
            manifest_.publishBootSequenceFromLocation(".*",100);
        int rasCount = 0;
        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("stateChanges") == 0){
                rasCount++;
            }
        }
        assertEquals(6, rasCount);
    }

    @Test
    public void SystemManifestCanPublishBootSequenceWithServiceNode_50() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestJsonWithServiceNode, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","b") > 0)
            manifest_.publishBootSequenceFromLocation(".*",50);
        int rasCount = 0;
        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("stateChanges") == 0){
                rasCount++;
            }
        }
        assertNotEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCannotPublishBootSequenceWithComputeNode_0() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","b") > 0)
            manifest_.publishBootSequenceFromLocation("AB.*",0);
        int rasCount = 0;
        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("stateChanges") == 0){
                rasCount++;
            }
        }
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCannotPublishBootSequenceWithComputeNode_100() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","b") > 0)
            manifest_.publishBootSequenceFromLocation("AB.*",100);
        int rasCount = 0;
        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("stateChanges") == 0){
                rasCount++;
            }
        }
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCannotPublishBootSequenceWithServiceNode_0() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestJsonWithServiceNode, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","b") > 0)
            manifest_.publishBootSequenceFromLocation("AB.*",0);
        int rasCount = 0;
        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("stateChanges") == 0){
                rasCount++;
            }
        }
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCannotPublishBootSequenceWithServiceNode_100() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestJsonWithServiceNode, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","b") > 0)
            manifest_.publishBootSequenceFromLocation("AB.*",100);
        int rasCount = 0;
        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("stateChanges") == 0){
                rasCount++;
            }
        }
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCanPublishRASEventsOnly() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","r") > 0)
            manifest_.publishRASEventsFromLocation(20,".*", ".*");
        int rasCount = 0;
        int sensorCount = 0;

        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("events") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("telemetry") == 0) {
                sensorCount++;
            }
        }
        assertEquals(0, sensorCount);
        assertEquals(20, rasCount);
    }

    @Test
    public void SystemManifestCanPublishRASEventsOnly_CountZeroCase() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","r") > 0)
            manifest_.publishRASEventsFromLocation(0,".*", ".*");
        int rasCount = 0;
        int sensorCount =0;

        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("events") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("telemetry") == 0) {
                sensorCount++;
            }
        }
        assertEquals(0, sensorCount);
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCanPublishRASEventsOnly_CountZeroCase_InvalidRegex() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*","r") > 0)
            manifest_.publishRASEventsFromLocation(0,".*", "AS.*");
        int rasCount = 0;
        int sensorCount =0;

        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("events") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("telemetry") == 0) {
                sensorCount++;
            }
        }
        assertEquals(0, sensorCount);
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCannotPublishRASEventsOnly() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "r") > 0)
            manifest_.publishRASEventsFromLocation(20,"AB.*", ".*");
        int rasCount = 0;
        int sensorCount =0;

        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("events") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("telemetry") == 0) {
                sensorCount++;
            }
        }
        assertEquals(0, sensorCount);
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCannotPublishRASEventsOnly_InvalidRegex() throws PropertyNotExpectedType, InterruptedException, Exception {
        exception.expect(Exception.class);
        exception.expectMessage("No Matched RAS Data to generate events.");
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "r") > 0)
            manifest_.publishRASEventsFromLocation(20,"AB.*", "AS.*");
    }

    @Test
    public void SystemManifestPublishSensorEventsOnly_InvalidRegex() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "r") > 0)
            manifest_.publishSensorEventsFromLocation(20,".*", ".*");
        int rasCount = 0;
        int sensorCount = 0;

        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("events") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("telemetry") == 0) {
                sensorCount++;
            }
        }
        assertEquals(20, sensorCount);
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCanPublishSensorEventsOnly() throws PropertyNotExpectedType, InterruptedException, Exception {
        exception.expect(Exception.class);
        exception.expectMessage("No Matched Sensor Data to generate events.");
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "r") > 0)
            manifest_.publishSensorEventsFromLocation(20,".*", "AS.*");
    }

    @Test
    public void SystemManifestCanPublishSensorEventsOnly_ServiceBladeOnly() throws PropertyNotExpectedType, InterruptedException, Exception {
        exception.expect(Exception.class);
        exception.expectMessage("No Matched Sensor Data to generate events.");
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", systemManifestJson_ServiceBladeOnly, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "r") > 0)
            manifest_.publishSensorEventsFromLocation(20,".*", "AS.*");
    }

    @Test
    public void SystemManifestCanPublishSensorEventsOnly_CountZeroCase() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount(".*", "s") > 0)
            manifest_.publishSensorEventsFromLocation(0,".*", "AS.*");
        int rasCount = 0;
        int sensorCount = 0;

        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("events") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("telemetry") == 0) {
                sensorCount++;
            }
        }
        assertEquals(0, sensorCount);
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestCannotPublishSensorEventsOnly() throws PropertyNotExpectedType, InterruptedException, Exception {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        if(manifest_.getRegexMatchedComponentsCount("AB.*", "s") > 0)
            manifest_.publishSensorEventsFromLocation(20,"AB.*", "AS.*");
        int rasCount = 0;
        int sensorCount = 0;

        while(eventQueue_.size() > 0) {
            PublishData ev = eventQueue_.poll();
            if (ev.subject_.compareTo("events") == 0){
                rasCount++;
            } else if(ev.subject_.compareTo("telemetry") == 0) {
                sensorCount++;
            }
        }
        assertEquals(0, sensorCount);
        assertEquals(0, rasCount);
    }

    @Test
    public void SystemManifestGetRegexMatchedLocations() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        int count = manifest_.getRegexMatchedComponentsCount(".*","r");
        assertEquals(6, count);
    }

    @Test
    public void SystemManifestGetRegexUnMatchedLocations() throws PropertyNotExpectedType, InterruptedException {
        manifest_.generateSystem(systemManifestJson, eventRatio, "1", "1", mockSensorMetadata_, mockRasMetadata, eventQueue_);
        int count = manifest_.getRegexMatchedComponentsCount("AB.*","b");
        assertEquals(0, count);
    }
}
