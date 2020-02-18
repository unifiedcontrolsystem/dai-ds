// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.config_io.*;
import com.intel.networking.HttpMethod;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RequestInfo;
import com.intel.properties.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashSet;
import java.util.Set;
import java.util.HashMap;
import java.util.Map;

public class EventSimEngineTest {

    class MockEventSimEngine extends EventSimEngine {

        public MockEventSimEngine(NetworkSource source, ConnectionManager connMan) throws PropertyNotExpectedType, IOException, ConfigIOParseException {
            super(source, connMan, mock(Logger.class));
            initialize();
            source_ = source;
        }

        @Override
        public PropertyMap loadSystemManifestFromJSON() throws IOException, ConfigIOParseException {
            PropertyMap sysManifest = parser_.readConfig(new ByteArrayInputStream(systemManifest.getBytes())).getAsMap();
            return sysManifest;
        }

        @Override
        public PropertyMap loadSensorMetadataFromJSON() throws IOException, ConfigIOParseException {
            PropertyMap sensorMetadata = parser_.readConfig(new ByteArrayInputStream(mockSensorMetadata_.getBytes())).getAsMap();
            return sensorMetadata;
        }

        @Override
        public PropertyMap loadRASMetadataFromJSON() throws IOException, ConfigIOParseException {
            PropertyMap rasMetadata = parser_.readConfig(new ByteArrayInputStream(mockRasMetadata_.getBytes())).getAsMap();
            return rasMetadata;
        }
    }

    @Before
    public void Setup() {
        parser_ = ConfigIOFactory.getInstance("json");
        assert parser_ != null: "Failed to create a JSON parser!";
        loadTestConfigDetails();
    }

    private void loadTestConfigDetails() {
        goodConfigJSON = new PropertyMap();
        goodConfigJSON.put("SystemManifest", "val1");
        goodConfigJSON.put("SensorMetadata", "SensorMeatadata.json");
        goodConfigJSON.put("RASMetadata", "SensorMeatadata.json");
        goodConfigJSON.put("eventCount", "5");
        goodConfigJSON.put("timeDelayMus", "1");
        goodConfigJSON.put("eventRatioSensorToRas", "5");
        goodConfigJSON.put("randomizerSeed", "1");
        goodConfigJSON.put("serverAddress", "127.0.0.1");
        goodConfigJSON.put("serverPort", "7979");
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    public void testSystemManifestMissing() throws PropertyNotExpectedType {
        goodConfigJSON.remove("SystemManifest");
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        try {
            appUnderTest = new MockEventSimEngine(source_, connMan_);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("EventSim Configuration file doesn't contain SystemManifest location",e.getMessage());
        }
    }

    @Test
    public void testSensorMetadataMissing() throws PropertyNotExpectedType {
        goodConfigJSON.remove("SensorMetadata");
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        try {
            appUnderTest = new MockEventSimEngine(source_, connMan_);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("EventSim Configuration file doesn't contain SensorMetadata location",e.getMessage());
        }
    }

    @Test
    public void testRASMetadataMissing() throws PropertyNotExpectedType {
        goodConfigJSON.remove("RASMetadata");
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        try {
            appUnderTest = new MockEventSimEngine(source_, connMan_);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("EventSim Configuration file doesn't contain RASMetadata location",e.getMessage());
        }
    }

    @Test
    public void testEventCountMissing() throws PropertyNotExpectedType {
        goodConfigJSON.remove("eventCount");
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        try {
            appUnderTest = new MockEventSimEngine(source_, connMan_);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("EventSim Configuration file doesn't contain 'eventCount' entry",e.getMessage());
        }
    }

    @Test
    public void testTimeDelayMusMissing() throws PropertyNotExpectedType {
        goodConfigJSON.remove("timeDelayMus");
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        try {
            appUnderTest = new MockEventSimEngine(source_, connMan_);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("EventSim Configuration file doesn't contain 'timeDelayMus' entry",e.getMessage());
        }
    }

    @Test
    public void testRandomizerSeedMissing() throws PropertyNotExpectedType {
        goodConfigJSON.remove("randomizerSeed");
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        try {
            appUnderTest = new MockEventSimEngine(source_, connMan_);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("EventSim Configuration file doesn't contain 'randomizerSeed' entry",e.getMessage());
        }
    }

    @Test
    public void testEventRatioSensorToRasMissing() throws PropertyNotExpectedType {
        goodConfigJSON.remove("eventRatioSensorToRas");
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        try {
            appUnderTest = new MockEventSimEngine(source_, connMan_);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("EventSim Configuration file doesn't contain 'eventRatioSensorToRas' entry",e.getMessage());
        }
    }

    @Test
    public void testServerAddressMissing() throws PropertyNotExpectedType {
        goodConfigJSON.remove("serverAddress");
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        try {
            appUnderTest = new MockEventSimEngine(source_, connMan_);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("EventSim Configuration file doesn't contain 'serverAddress' entry",e.getMessage());
        }
    }

    @Test
    public void testServerPortMissing() throws PropertyNotExpectedType {
        goodConfigJSON.remove("serverPort");
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        try {
            appUnderTest = new MockEventSimEngine(source_, connMan_);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("EventSim Configuration file doesn't contain 'serverPort' entry",e.getMessage());
        }
    }

    @Test
    public void testEventsPublishedValidRegex() throws Exception {
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        new Thread(appUnderTest).start();
        while (!appUnderTest.running_.get()) {
            System.out.println("Waiting for thread to start");
            while(!appUnderTest.running_.get()) {} //waiting for thread to start
        }
        appUnderTest.publishBatchForLocation(".*",".*", "5");
        Thread.sleep(50);

        while (appUnderTest.active) {
            //Do Nothing
        }
        appUnderTest.stopPublishing();
        assertEquals(0, appUnderTest.getOutstandingEventCount());
        assertEquals(5, appUnderTest.getPublishedEventCount());
    }

    @Test
    public void testNoEventsInValidRegex() {
        try {
            Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
            appUnderTest = new MockEventSimEngine(source_, connMan_);
            appUnderTest.publishBatchForLocation("as.*",".*", "5");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("No Matched Regex Locations to generate RAS+SEnsor Events.",e.getMessage());
        }
    }

    @Test
    public void testNoEventsPublishedInValidLabel() {
        try {
            Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
            appUnderTest = new MockEventSimEngine(source_, connMan_);
            appUnderTest.publishBatchForLocation(".*","as.*", "5");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("No Matched RAS/Sensor Data to generate events.",e.getMessage());
        }
    }

    @Test
    public void testNoEventsPublished() {
        try {
            Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
            appUnderTest = new MockEventSimEngine(source_, connMan_);
            appUnderTest.publishBatchForLocation("as.*","as.*", "5");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("No Matched Regex Locations to generate RAS+SEnsor Events.",e.getMessage());
        }
    }

    @Test
    public void testRasEventsPublishedValidRegex() throws Exception {
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        new Thread(appUnderTest).start();
        while (!appUnderTest.running_.get()) {
            System.out.println("Waiting for thread to start");
            while(!appUnderTest.running_.get()) {} //waiting for thread to start
        }
        appUnderTest.publishRasEvents(".*",".*", "5", "false");
        Thread.sleep(50);

        while (appUnderTest.active) {
            //Do Nothing
        }
        appUnderTest.stopPublishing();
        assertEquals(0, appUnderTest.getOutstandingEventCount());
        assertEquals(5, appUnderTest.getPublishedEventCount());
    }

    @Test
    public void testNoRasEventsInValidRegex() {
        try {
            Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
            appUnderTest = new MockEventSimEngine(source_, connMan_);
            appUnderTest.publishRasEvents("as.*",".*", "5", "false");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("No Matched Regex Locations to generate RAS Events.",e.getMessage());
        }
    }

    @Test
    public void testNoRasEventsPublishedInValidLabel() {
        try {
            Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
            appUnderTest = new MockEventSimEngine(source_, connMan_);
            appUnderTest.publishRasEvents(".*","as.*", "5", "true");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("No Matched RAS Data to generate events.",e.getMessage());
        }
    }

    @Test
    public void testNoRasEventsPublished() {
        try {
            Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
            appUnderTest = new MockEventSimEngine(source_, connMan_);
            appUnderTest.publishRasEvents("as.*","as.*", "5", "true");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("No Matched Regex Locations to generate RAS Events.",e.getMessage());
        }
    }

    @Test
    public void testSensorEventsPublishedValidRegex() throws Exception {
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        new Thread(appUnderTest).start();
        while (!appUnderTest.running_.get()) {
            System.out.println("Waiting for thread to start");
            while(!appUnderTest.running_.get()) {} //waiting for thread to start
        }
        appUnderTest.publishSensorEvents(".*",".*", "5", "false");
        Thread.sleep(50);

        while (appUnderTest.active) {
            //Do Nothing
        }
        appUnderTest.stopPublishing();
        assertEquals(0, appUnderTest.getOutstandingEventCount());
        assertEquals(5, appUnderTest.getPublishedEventCount());
    }

    @Test
    public void testNoSensorEventsInValidRegex() {
        try {
            Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
            appUnderTest = new MockEventSimEngine(source_, connMan_);
            appUnderTest.publishSensorEvents("as.*",".*", "5", "false");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("No Matched Regex Locations to generate Sensor Events.",e.getMessage());
        }
    }

    @Test
    public void testNoSensorEventsPublishedInValidLabel() {
        try {
            Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
            appUnderTest = new MockEventSimEngine(source_, connMan_);
            appUnderTest.publishSensorEvents(".*","as.*", "5", "true");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("No Matched Sensor Data to generate events.",e.getMessage());
        }
    }

    @Test
    public void testNoSensorEventsPublished() {
        try {
            Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
            appUnderTest = new MockEventSimEngine(source_, connMan_);
            appUnderTest.publishSensorEvents("as.*","as.*", "5", "true");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("No Matched Regex Locations to generate Sensor Events.",e.getMessage());
        }
    }

    @Test
    public void testBootEventsPublishedValidRegex() throws Exception {
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        ConnectionObject conn = new ConnectionObject("url1", "sub1", mock(Logger.class));
        conn.restClient_ = mock(RESTClient.class);
        Set<ConnectionObject> setConn = new HashSet<>();
        setConn.add(conn);
        Mockito.when(connMan_.getConnections()).thenReturn(setConn);
        BlockingResult result = new BlockingResult(1, "response", new RequestInfo(HttpMethod.POST, new URI("url"),"BODY"));
        Mockito.when(conn.restClient_.postRESTRequestBlocking(new URI("url1"), anyString())).thenReturn(result);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        new Thread(appUnderTest).start();
        while (!appUnderTest.running_.get()) {
            System.out.println("Waiting for thread to start");
            while(!appUnderTest.running_.get()) {} //waiting for thread to start
        }
        appUnderTest.publishBootEvents(".*","0", "true");
        Thread.sleep(50);

        while (appUnderTest.active) {
            //Do Nothing
        }
        appUnderTest.stopPublishing();
        assertEquals(-12, appUnderTest.getOutstandingEventCount());
        assertEquals(12, appUnderTest.getPublishedEventCount());
    }

    @Test
    public void testNoBootEventsInValidRegex() {
        try {
            Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
            appUnderTest = new MockEventSimEngine(source_, connMan_);
            appUnderTest.publishBootEvents("as.*","0", "true");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("No Matched Regex Locations to start Boot Sequence.",e.getMessage());
        }
    }

    @Test
    public void testNoBootEventsPublishedFailures() throws Exception {
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        new Thread(appUnderTest).start();
        while (!appUnderTest.running_.get()) {
            System.out.println("Waiting for thread to start");
            while(!appUnderTest.running_.get()) {} //waiting for thread to start
        }
        appUnderTest.publishBootEvents(".*","100", "true");
        Thread.sleep(50);

        while (appUnderTest.active) {
            //Do Nothing
        }
        appUnderTest.stopPublishing();
        assertEquals(-12, appUnderTest.getOutstandingEventCount());
        assertEquals(12, appUnderTest.getPublishedEventCount());
    }

    @Test
    public void testNoBootEventsPublished() {
        try {
            Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
            MockEventSimEngine appUnderTest = new MockEventSimEngine(source_, connMan_);
            appUnderTest.publishBootEvents("as.*","100", "true");
        } catch (Exception e) {
            System.out.println(e.getMessage());
            assertEquals("No Matched Regex Locations to start Boot Sequence.",e.getMessage());
        }
    }

    @Test
    public void testEventsPublishedDefaults() throws Exception {
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        new Thread(appUnderTest).start();

        while (!appUnderTest.running_.get()) {
            System.out.println("Waiting for thread to start");
            while(!appUnderTest.running_.get()) {} //waiting for thread to start
        }
        appUnderTest.publishBatchForLocation(null, null, null);
        Thread.sleep(50);

        while (appUnderTest.active) {
            //Do Nothing
        }
        appUnderTest.stopPublishing();
        assertEquals(0, appUnderTest.getOutstandingEventCount());
        assertEquals(5, appUnderTest.getPublishedEventCount());
    }

    @Test
    public void testRasEventsPublishedDefaults() throws Exception {
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        new Thread(appUnderTest).start();
        while (!appUnderTest.running_.get()) {
            System.out.println("Waiting for thread to start");
            while(!appUnderTest.running_.get()) {} //waiting for thread to start
        }
        appUnderTest.publishRasEvents(null, null, null, null);
        Thread.sleep(50);

        while (appUnderTest.active) {
            //Do Nothing
        }
        appUnderTest.stopPublishing();
        assertEquals(0, appUnderTest.getOutstandingEventCount());
        assertEquals(5, appUnderTest.getPublishedEventCount());
    }

    @Test
    public void testSensorEventsPublishedDefaults() throws Exception {
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        new Thread(appUnderTest).start();
        while (!appUnderTest.running_.get()) {
            System.out.println("Waiting for thread to start");
            while(!appUnderTest.running_.get()) {} //waiting for thread to start
        }
        appUnderTest.publishSensorEvents(null, null, null, null);
        Thread.sleep(50);

        while (appUnderTest.active) {
            //Do Nothing
        }
        appUnderTest.stopPublishing();
        assertEquals(0, appUnderTest.getOutstandingEventCount());
        assertEquals(5, appUnderTest.getPublishedEventCount());
    }

    @Test
    public void testBootEventsPublishedDefaults() throws Exception {
        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        new Thread(appUnderTest).start();
        while (!appUnderTest.running_.get()) {
            System.out.println("Waiting for thread to start");
            while(!appUnderTest.running_.get()) {} //waiting for thread to start
        }
        appUnderTest.publishBootEvents(null,null,null);
        Thread.sleep(50);
        while (appUnderTest.active) {
            //Do Nothing
        }
        appUnderTest.stopPublishing();
        assertEquals(-12, appUnderTest.getOutstandingEventCount());
        assertEquals(12, appUnderTest.getPublishedEventCount());
    }

    @Test
    public void testCreateReservation() throws Exception {

        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        Map<String, String> params = new HashMap<>();
        params.put("name", "testres");
        params.put("users", "root");
        params.put("nodes", "node01 node02");
        params.put("starttime", "2019-02-14 02:15:58");
        params.put("duration", "3600000");

        appUnderTest.createReservation(params);
    }

    @Test
    public void testCreateReservationRandomNodes() throws Exception {

        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        Map<String, String> params = new HashMap<>();
        params.put("name", "testres");
        params.put("users", "root");
        params.put("nodes", "random");
        params.put("starttime", "2019-02-14 02:15:58");
        params.put("duration", "3600000");

        appUnderTest.createReservation(params);
    }

    @Test
    public void testModifyReservation() throws Exception {

        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        Map<String, String> params = new HashMap<>();
        params.put("name", "testres");
        params.put("users", "root");
        params.put("nodes", "node01 node02");
        params.put("starttime", "2019-02-14 02:15:58");

        appUnderTest.modifyReservation(params);
    }

    @Test
    public void testDeleteReservation() throws Exception {

        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        Map<String, String> params = new HashMap<>();
        params.put("name", "testres");

        appUnderTest.deleteReservation(params);
    }

    @Test
    public void testStartJob() throws Exception {

        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        Map<String, String> params = new HashMap<>();
        params.put("jobid", "10");
        params.put("name", "testjob");
        params.put("users", "root");
        params.put("nodes", "node01 node02");
        params.put("starttime", "2019-02-14 02:15:58");
        params.put("workdir", "/home");

        appUnderTest.startJob(params);
    }

    @Test
    public void testTerminateJob() throws Exception {

        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
        appUnderTest = new MockEventSimEngine(source_, connMan_);
        Map<String, String> params = new HashMap<>();
        params.put("jobid", "10");
        params.put("name", "testjob");
        params.put("users", "root");
        params.put("nodes", "node01 node02");
        params.put("starttime", "2019-02-14 02:15:58");
        params.put("workdir", "/home");
        params.put("exitstatus", "0");

        appUnderTest.terminateJob(params);
    }

//    @Test
//    public void testSimulateWlm() throws Exception {
//
//        Mockito.when(source_.getAppConfiguration()).thenReturn(goodConfigJSON);
//        Mockito.when(source_.sendMessage(Mockito.any(), Mockito.anyString() )).thenReturn(true);
//        appUnderTest = new MockEventSimEngine(source_, connMan_);
//        Map<String, String> params = new HashMap<>();
//        params.put("reservations", "10");
//
//        appUnderTest.simulateWlm(params);
//    }

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

    String mockSensorMetadata_ = "{\n" +
            "    \"988\": {\"description\": \"CC_F_RECT_FAN_SETPOINT_PEAK\", \"unit\": \"%\", \"type\": \"unknown\" },\n" +
            "    \"989\": {\"description\": \"CC_H_CAB_HEALTH_MAIN\", \"unit\": \"status\", \"type\": \"unknown\" },\n" +
            "    \"990\": {\"description\": \"CC_H_CAB_HEALTH_MAIN_LATCHED\", \"unit\": \"status\", \"type\": \"unknown\" },\n" +
            "    \"991\": {\"description\": \"CC_T_MCU_TEMP\", \"unit\": \"degC\", \"type\": \"Temp\" },\n" +
            "    \"992\": {\"description\": \"CC_T_PCB_TEMP\", \"unit\": \"degC\", \"type\": \"Temp\" },\n" +
            "    \"1528\": {\"description\": \"BC_P_NODE3_CPU0_PCKG_ACC\", \"unit\": \"J\", \"type\": \"Energy\" },\n" +
            "    \"1529\": {\"description\": \"BC_P_NODE3_CPU1_PCKG_ACC\", \"unit\": \"J\", \"type\": \"Energy\" },\n" +
            "    \"1530\": {\"description\": \"BC_P_NODE3_CPU2_PCKG_ACC\", \"unit\": \"J\", \"type\": \"Energy\" },\n" +
            "    \"1531\": {\"description\": \"BC_P_NODE3_CPU3_PCKG_ACC\", \"unit\": \"J\", \"type\": \"Energy\" },\n" +
            "    \"1532\": {\"description\": \"BC_T_NODE0_KNC_EAST_TEMP\", \"unit\": \"degC\", \"type\": \"Temp\" },\n"+
            "    \"1868\": {\"description\": \"BC_L_NODE0_MEM_CH_CUPS\", \"unit\": \"\", \"type\": \"unknown\" },\n" +
            "    \"1869\": {\"description\": \"BC_L_NODE1_MEM_CH_CUPS\", \"unit\": \"\", \"type\": \"unknown\" },\n" +
            "    \"1870\": {\"description\": \"BC_L_NODE2_MEM_CH_CUPS\", \"unit\": \"\", \"type\": \"unknown\" },\n" +
            "    \"1871\": {\"description\": \"BC_L_NODE3_MEM_CH_CUPS\", \"unit\": \"\", \"type\": \"unknown\" },\n" +
            "    \"1240\": {\"description\": \"BC_V_AOC0_RX_VCC_12V\", \"unit\": \"mV\", \"type\": \"Voltage\" }\n" +
            "}\n";

    String mockRasMetadata_ = "{\n" +
            "    \"ec_boot\": \"RasMntrForeignNodeBoot\",\n" +
            "    \"ec_node_standby\": \"RasMntrForeignNodeStandby\",\n" +
            "    \"ec_service_started\": \"RasMntrForeignNodeSrvStarted\",\n" +
            "    \"ec_node_available\": \"RasMntrForeignNodeAvailable\",\n" +
            "    \"ec_node_failed\": \"RasMntrForeignNodeFailed\"\n" +
            "}\n";

    PropertyMap goodConfigJSON;
    PropertyMap args;
    private ConfigIO parser_;
    MockEventSimEngine appUnderTest;
    NetworkSource source_ = mock(NetworkSource.class);
    ConnectionManager connMan_ = mock(ConnectionManager.class);
}
