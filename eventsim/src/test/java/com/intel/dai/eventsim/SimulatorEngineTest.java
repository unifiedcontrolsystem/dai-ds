package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.NodeInformation;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restserver.RESTServerException;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SimulatorEngineTest {

    private DataStoreFactory dsfactory_;

    public class EventSimTestMock extends EventSim {

        public EventSimTestMock(String[] args_, Logger log_) throws SimulatorException, RESTClientException, RESTServerException, PropertyNotExpectedType {
            super(args_, log_);
            dsfactory_ = mock(DataStoreFactory.class);
        }

        @Override
        DataStoreFactory createDataStoreFactory(String[] args) {
            return dsfactory_;
        }
    }

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void testHwInventoryFileLocation() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        assertEquals("/opt/ucs/etc/HWInventory.json", eventSimTestMock.simEngineDataLoader.getHwInventoryFileLocation());
    }

    @Test
    public void testHwInventoryDiscStatusUrl() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        assertEquals("/Inventory/DiscoveryStatus", eventSimTestMock.simEngineDataLoader.getHwInventoryDiscStatusUrl());
    }

    @Test
    public void testHwInventoryFileLocationPath() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        assertEquals("/opt/ucs/etc", eventSimTestMock.simEngineDataLoader.getHwInventoryFileLocationPath());
    }

    @Test
    public void testHwInventoryQueryLocationPath() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        assertEquals("/opt/ucs/etc", eventSimTestMock.simEngineDataLoader.getHwInventoryQueryLocationPath());
    }

    @Test(expected = SimulatorException.class)
    public void testErrorLoadDataFromDb() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        doThrow(new DataStoreException("test exception")).when(nodeInfoMock).getNodeLocations();
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
    }

    @Test(expected = SimulatorException.class)
    public void missingEventSimServerConfiguration() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, "");
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
    }

    @Test(expected = SimulatorException.class)
    public void missingEventSimServerConfigurationFile() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", ""};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
    }

    @Test(expected = SimulatorException.class)
    public void invalidEventSimConfiguration() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, invalidEventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
    }


    @Test(expected = SimulatorException.class)
    public void missingNetworkConfigEntry() throws Exception {
        ConfigIO parser_ = ConfigIOFactory.getInstance("json");
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        PropertyMap data = LoadFileLocation.fromFileLocation(eventSimConfigFile.getAbsolutePath()).getAsMap();
        data.remove("networkConfig");
        loadDataIntoFile(eventSimConfigFile, parser_.toString(data));
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
    }

    @Test(expected = SimulatorException.class)
    public void missingEventSimConfigEntry() throws Exception {
        ConfigIO parser_ = ConfigIOFactory.getInstance("json");
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        PropertyMap data = LoadFileLocation.fromFileLocation(eventSimConfigFile.getAbsolutePath()).getAsMap();
        data.remove("eventsimConfig");
        loadDataIntoFile(eventSimConfigFile, parser_.toString(data));
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
    }

    @Test(expected = SimulatorException.class)
    public void missingSensorMetadataConfigEntry() throws Exception {
        ConfigIO parser_ = ConfigIOFactory.getInstance("json");
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        PropertyMap data = LoadFileLocation.fromFileLocation(eventSimConfigFile.getAbsolutePath()).getAsMap();
        data.getMapOrDefault("eventsimConfig", null).remove("SensorMetadata");
        loadDataIntoFile(eventSimConfigFile, parser_.toString(data));
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        eventSimTestMock.initialise(args);
    }

    @Test(expected = SimulatorException.class)
    public void missingRASMetadataConfigEntry() throws Exception {
        ConfigIO parser_ = ConfigIOFactory.getInstance("json");
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        PropertyMap data = LoadFileLocation.fromFileLocation(eventSimConfigFile.getAbsolutePath()).getAsMap();
        data.getMapOrDefault("eventsimConfig", null).remove("RASMetadata");
        loadDataIntoFile(eventSimConfigFile, parser_.toString(data));
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        eventSimTestMock.initialise(args);
    }

    @Test(expected = SimulatorException.class)
    public void missingHWInventoryDiscStatUrlEntry() throws Exception {
        ConfigIO parser_ = ConfigIOFactory.getInstance("json");
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        PropertyMap data = LoadFileLocation.fromFileLocation(eventSimConfigFile.getAbsolutePath()).getAsMap();
        data.getMapOrDefault("eventsimConfig", null).remove("HWInventoryDiscStatUrl");
        loadDataIntoFile(eventSimConfigFile, parser_.toString(data));
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        eventSimTestMock.initialise(args);
    }

    @Test
    public void testRasEvents() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishRasEvents("R0.*", ".*", "19", "false");
        assertEquals(19, simulatorEngineTest.getNumberOfEventsPublished());
    }

    @Test
    public void testRasEvents_NullValues() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishRasEvents(null, null, null, null);
        assertEquals(10, simulatorEngineTest.getNumberOfEventsPublished());
    }

    @Test(expected = SimulatorException.class)
    public void testRasEvents_MismatchRegex() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishRasEvents("GT.*", null, null, null);
    }

    @Test(expected = SimulatorException.class)
    public void testRasEvents_MismatchRegexLabel() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishRasEvents(".*", "GT.*", null, null);
    }

    @Test
    public void testSensorEvents() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishSensorEvents("R0.*", ".*", "19", "false");
        assertEquals(19, simulatorEngineTest.getNumberOfEventsPublished());
    }

    @Test
    public void testSensorEvents_NullValues() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishSensorEvents(null, null, null, null);
        assertEquals(10, simulatorEngineTest.getNumberOfEventsPublished());
    }

    @Test(expected = SimulatorException.class)
    public void testSensorEvents_MismatchRegex() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishSensorEvents("GT.*", null, null, null);
    }

    @Test(expected = SimulatorException.class)
    public void testSensorEvents_MismatchRegexLabel() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishSensorEvents(".*", "GT.*", null, null);
    }

    @Test
    public void testBootEvents() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootEvents("R0.*", "0", "false");
        assertEquals(3, simulatorEngineTest.getNumberOfEventsPublished());
    }

    @Test
    public void testBootEvents_NullValues() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootEvents(null, null, null);
        assertEquals(3, simulatorEngineTest.getNumberOfEventsPublished());
    }

    @Test(expected = SimulatorException.class)
    public void testBootEvents_MismatchRegex() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootEvents("GT.*", null, null);
    }

    @Test
    public void testBootFailureEvents() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootEvents("R0.*", "100", "false");
        assertEquals(3, simulatorEngineTest.getNumberOfEventsPublished());
    }

    @Test
    public void testBootFailureEvents_60_bf() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList();
        locations.add("R0");
        locations.add("SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootEvents(".*", "50", "false");
        assertEquals(6, simulatorEngineTest.getNumberOfEventsPublished());
    }

    private void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.writeStringToFile(file, data);
    }

    String eventSimConfig = "{\n" +
            "    \"eventsimConfig\" : {\n" +
            "        \"SensorMetadata\": \"/resources/ForeignSensorMetaData.json\",\n" +
            "        \"RASMetadata\": \"/resources/ForeignEventMetaData.json\",\n" +
            "        \"BootParameters\" : \"/opt/ucs/etc/BootParameters.json\",\n" +
            "        \"HWInventory\" : \"/opt/ucs/etc/HWInventory.json\",\n" +
            "        \"HWInventoryPath\" : \"/opt/ucs/etc\",\n" +
            "        \"HWInventoryQueryPath\" : \"/opt/ucs/etc\",\n" +
            "        \"HWInventoryDiscStatUrl\" : \"/Inventory/DiscoveryStatus\",\n" +
            "        \"eventCount\": 10,\n" +
            "        \"timeDelayMus\": 1,\n" +
            "        \"eventRatioSensorToRas\": 1,\n" +
            "        \"randomizerSeed\": \"234\"\n" +
            "    },\n" +
            "    \"networkConfig\" : {\n" +
            "        \"network\" : \"sse\",\n" +
            "        \"sseConfig\": {\n" +
            "            \"serverAddress\": \"*\" ,\n" +
            "            \"serverPort\": \"5678\" ,\n" +
            "            \"urls\": {\n" +
            "                \"/v1/stream/cray-telemetry-fan\": [\n" +
            "                    \"telemetry\"\n" +
            "                ] ,\n" +
            "                \"/streams/nodeBootEvents\": [\n" +
            "                    \"stateChanges\"\n" +
            "                ] ,\n" +
            "                \"/v1/stream/cray-dmtf-resource-event\": [\n" +
            "                    \"events\"\n" +
            "                ]\n" +
            "            }\n" +
            "        } ,\n" +
            "        \"rabbitmq\": {\n" +
            "            \"exchangeName\": \"simulator\" ,\n" +
            "            \"uri\": \"amqp://127.0.0.1\"\n" +
            "        }\n" +
            "    }\n" +
            "}\n" +
            "\n";

    String invalidEventSimConfig = "{\n" +
            "    \"eventsimConfig\" : {\n" +
            "        \"SensorMetadata\": \"/resources/ForeignSensorMetaData.json\",\n" +
            "        \"RASMetadata\": \"/resources/ForeignEventMetaData.json\",\n" +
            "        \"BootParameters\" : \"/opt/ucs/etc/BootParameters.json\",\n" +
            "        \"HWInventory\" : \"/opt/ucs/etc/HWInventory.json\",\n" +
            "        \"HWInventoryPath\" : \"/opt/ucs/etc\",\n" +
            "        \"HWInventoryQueryPath\" : \"/opt/ucs/etc\",\n" +
            "        \"HWInventoryDiscStatUrl\" : \"/Inventory/DiscoveryStatus\",\n" +
            "        \"eventCount\": 10,\n" +
            "        \"timeDelayMus\": 1,\n" +
            "        \"eventRatioSensorToRas\": 1,\n" +
            "        \"randomizerSeed\": \"234\"\n" +
            "    },\n" +
            "    \"networkConfig\" : \n" + //add { before \n to make valid json
            "        \"network\" : \"sse\",\n" +
            "        \"sseConfig\": {\n" +
            "            \"serverAddress\": \"*\" ,\n" +
            "            \"serverPort\": \"5678\" ,\n" +
            "            \"urls\": {\n" +
            "                \"/v1/stream/cray-telemetry-fan\": [\n" +
            "                    \"telemetry\"\n" +
            "                ] ,\n" +
            "                \"/streams/nodeBootEvents\": [\n" +
            "                    \"stateChanges\"\n" +
            "                ] ,\n" +
            "                \"/v1/stream/cray-dmtf-resource-event\": [\n" +
            "                    \"events\"\n" +
            "                ]\n" +
            "            }\n" +
            "        } ,\n" +
            "        \"rabbitmq\": {\n" +
            "            \"exchangeName\": \"simulator\" ,\n" +
            "            \"uri\": \"amqp://127.0.0.1\"\n" +
            "        }\n" +
            "    }\n" +
            "}\n" +
            "\n";

    String emptyEventSimConfig = "";
}


