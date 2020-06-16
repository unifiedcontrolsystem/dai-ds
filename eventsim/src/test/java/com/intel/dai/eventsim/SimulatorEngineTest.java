package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.NodeInformation;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

public class SimulatorEngineTest {

    private DataStoreFactory dsfactory_;

    public class EventSimTestMock extends EventSim {

        EventSimTestMock(String[] args_, Logger log_) throws SimulatorException {
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
        List<String> locations = new ArrayList<>();
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
        List<String> locations = new ArrayList<>();
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
        List<String> locations = new ArrayList<>();
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
        List<String> locations = new ArrayList<>();
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
        new EventSimTestMock(args, mock(Logger.class));
    }

    @Test(expected = SimulatorException.class)
    public void missingEventSimServerConfigurationWithEmptyData() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, "{}");
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        new EventSimTestMock(args, mock(Logger.class));
    }

    @Test(expected = SimulatorException.class)
    public void missingEventSimServerConfigurationFile() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", ""};
        new EventSimTestMock(args, mock(Logger.class));
    }

    @Test(expected = SimulatorException.class)
    public void invalidEventSimConfiguration() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, invalidEventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        new EventSimTestMock(args, mock(Logger.class));
    }


    @Test(expected = SimulatorException.class)
    public void missingNetworkConfigEntry() throws Exception {
        ConfigIO parser_ = ConfigIOFactory.getInstance("json");
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        PropertyMap data = LoadFileLocation.fromFileLocation(eventSimConfigFile.getAbsolutePath()).getAsMap();
        data.remove("networkConfig");
        assertNotNull(parser_);
        loadDataIntoFile(eventSimConfigFile, parser_.toString(data));
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        new EventSimTestMock(args, mock(Logger.class));
    }

    @Test(expected = SimulatorException.class)
    public void missingEventSimConfigEntry() throws Exception {
        ConfigIO parser_ = ConfigIOFactory.getInstance("json");
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        PropertyMap data = LoadFileLocation.fromFileLocation(eventSimConfigFile.getAbsolutePath()).getAsMap();
        data.remove("eventsimConfig");
        assertNotNull(parser_);
        loadDataIntoFile(eventSimConfigFile, parser_.toString(data));
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        new EventSimTestMock(args, mock(Logger.class));
    }

    @Test(expected = SimulatorException.class)
    public void missingSensorMetadataConfigEntry() throws Exception {
        ConfigIO parser_ = ConfigIOFactory.getInstance("json");
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        PropertyMap data = LoadFileLocation.fromFileLocation(eventSimConfigFile.getAbsolutePath()).getAsMap();
        data.getMapOrDefault("eventsimConfig", null).remove("SensorMetadata");
        assertNotNull(parser_);
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
        assertNotNull(parser_);
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
        assertNotNull(parser_);
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
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        locations.add("R0-SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishRasEvents("R0.*", ".*", "false", "0", null, "5", null);
        assertEquals(5, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testRasEvents_DefaultValues() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        locations.add("R0-SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishRasEvents(".*", ".*", "true", "0", "123", null, null);
        assertEquals(3, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test(expected = SimulatorException.class)
    public void testRasEvents_MismatchLocationRegex() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        locations.add("R0-SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishRasEvents("GT.*", ".*", "true", null, null, null, null);
    }

    @Test(expected = SimulatorException.class)
    public void testRasEvents_MismatchLabelRegex() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        locations.add("R0-SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishRasEvents(".*", "GT.*", "true", null, null, null, null);
    }

    @Test
    public void testSensorEvents() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        locations.add("R0-SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishSensorEvents("R0.*", ".*", "true", "0", "123", "5", "false");
        assertEquals(5, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testSensorEvents_DefaultValues() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        ArrayList<String> locations = new ArrayList<String>();
        locations.add("R0");
        locations.add("R0-SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishSensorEvents(".*", ".*", "true", null, null, "3", null);
        assertEquals(3, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test(expected = SimulatorException.class)
    public void testSensorEvents_MismatchRegexLocation() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        locations.add("R0-SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishSensorEvents("GT.*", ".*", "true", null, null, null, null);
    }

    @Test(expected = SimulatorException.class)
    public void testSensorEvents_MismatchRegexLabel() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        locations.add("R0-SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishSensorEvents(".*", "GT.*", "true", null, null, null, null);
    }

    @Test
    public void testJobEvents() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        locations.add("R0-SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishJobEvents("R0.*", ".*", "true", "0", "123", "5", "false");
        assertEquals(5, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testJobEvents_DefaultValues() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        ArrayList<String> locations = new ArrayList<String>();
        locations.add("R0");
        locations.add("R0-SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishJobEvents(".*", ".*", "true", null, null, "3", null);
        assertEquals(3, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test(expected = SimulatorException.class)
    public void testJobEvents_MismatchRegexLocation() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        locations.add("R0-SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishJobEvents("GT.*", ".*", "true", null, null, null, null);
    }

    @Test(expected = SimulatorException.class)
    public void testJobEvents_MismatchRegexLabel() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        locations.add("R0-SMS");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishJobEvents(".*", "GT.*", "true", null, null, null, null);
    }

    @Test
    public void testBootEvents() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootEvents("R0.*", "0", "false", "0", "123", null);
        assertEquals(3, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testBootOffEvents() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootOffEvents("R0.*", "false", "0", null, null);
        assertEquals(1, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testBootOnEvents() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        locations.add("R0-SNS1");
        locations.add("R0-SNS2");
        locations.add("R0-SNS3");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootOnEvents("R0.*", "0", "false", "0", "123", null);
        assertEquals(4, simulatorEngineTest.getPublishedEventsCount());
        simulatorEngineTest.publishBootOnEvents("R0.*", "1", "false", "0", "123", null);
        assertEquals(4, simulatorEngineTest.getPublishedEventsCount());
        simulatorEngineTest.publishBootOnEvents(".*", "0.6", "false", "0", "123", null);
        assertEquals(4, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testBootReadyEvents() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootReadyEvents("R0.*", "false", "0", "123", null);
        assertEquals(1, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testBootOffOnReadyDefaultEvents_NullValues() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootEvents(".*", "0", "true", "0", null, null);
        assertEquals(3, simulatorEngineTest.getPublishedEventsCount());
        simulatorEngineTest.publishBootOffEvents(".*", "true", null, "0", null);
        assertEquals(1, simulatorEngineTest.getPublishedEventsCount());
        simulatorEngineTest.publishBootOnEvents(".*", "0", "true", "0", null, null);
        assertEquals(1, simulatorEngineTest.getPublishedEventsCount());
        simulatorEngineTest.publishBootReadyEvents(".*", "true", null, null, null);
        assertEquals(1, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test(expected = SimulatorException.class)
    public void testBootEvents_MismatchRegex() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootEvents("GT.*", "0", "true", null, null, null);
    }

    @Test
    public void testBootFailureEvents() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootEvents("R0.*", "1", "false", null, null, null);
        assertEquals(3, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testBootFailureEvents_60_bf() throws Exception {
        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishBootEvents(".*", "0.5", "false", null, null, null);
        assertEquals(3, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testScenario_Burst() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "burst", ".*", ".*", ".*", "0", "true", "0", "123", "1", null, null, null);
        assertEquals(67, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testScenario_GroupBurst() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "group-burst", ".*", ".*", ".*", "0", "true", "0", "123", null, "1", null, null);
        assertEquals(10, simulatorEngineTest.getPublishedEventsCount());
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), null, ".*", ".*", ".*", "0", "true", null, null, null, null, null, null);
        assertEquals(67, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testScenario_Repeat_Counter() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "repeat", ".*", ".*", ".*", "0", "true", "0", "123", null, "0", null, null);
        assertEquals(0, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testScenario_Repeat_StartTime() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        locations.add("R0-SNS1");
        locations.add("R0-SNS2");
        locations.add("R0-SNS3");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "repeat", ".*", ".*", ".*", "0.6", "true", "0", "123", null, null, null, null);
        assertEquals(67, simulatorEngineTest.getPublishedEventsCount());
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "repeat", ".*", ".*", ".*", "1", "true", "0", "123", null, null, null, null);
        assertEquals(67, simulatorEngineTest.getPublishedEventsCount());
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "repeat", ".*", ".*", ".*", "0", "true", "0", "123", null, null, null, null);
        assertEquals(67, simulatorEngineTest.getPublishedEventsCount());
        assertEquals("123", simulatorEngineTest.getRandomizationSeed());
    }

    @Test
    public void testScenario_Repeat_Duration() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "repeat", ".*", ".*", ".*", "0", "true", "0", "123", "1", null, null, null);
        assertEquals(67, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test(expected = SimulatorException.class)
    public void testScenario_ValidateParameters_RegexLoc_Null() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "group-burst", null, ".*", ".*", "0", "true", "0", "123", "1", null, null, null);
    }

    @Test(expected = SimulatorException.class)
    public void testScenario_MismatchRegexLocation() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "group-burst", "GT.*", ".*", ".*", "0", "true", "0", "123", "1", null, null, null);
    }

    @Test(expected = SimulatorException.class)
    public void testScenario_MismatchRegexLabel() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        final File eventSimConfigFile = tempFolder.newFile("EventSim.json");
        loadDataIntoFile(eventSimConfigFile, eventSimConfig);
        String[] args = new String[]{"localhost", eventSimConfigFile.getAbsolutePath()};
        EventSimTestMock eventSimTestMock = new EventSimTestMock(args, mock(Logger.class));
        NodeInformation nodeInfoMock = mock(NodeInformation.class);
        List<String> locations = new ArrayList<>();
        locations.add("R0");
        when(dsfactory_.createNodeInformation()).thenReturn(nodeInfoMock);
        when(nodeInfoMock.getNodeLocations()).thenReturn(locations);
        eventSimTestMock.initialise(args);
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(eventSimTestMock.simEngineDataLoader, mock(NetworkObject.class), mock(Logger.class));
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "group-burst", ".*", "GT.*", "GT.*", "0", "true", "0", "123", "1", null, null, null);
    }

    private void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.writeStringToFile(file, data, StandardCharsets.UTF_8);
    }

    private String eventSimConfig = "{\n" +
            "    \"eventsimConfig\" : {\n" +
            "        \"SensorMetadata\": \"/resources/ForeignSensorMetaData.json\",\n" +
            "        \"RASMetadata\": \"/resources/ForeignEventMetaData.json\",\n" +
            "        \"JobsMetadata\": \"/resources/ForeignJobsMetaData.json\",\n" +
            "        \"BootParameters\" : \"/opt/ucs/etc/BootParameters.json\",\n" +
            "        \"HWInventory\" : \"/opt/ucs/etc/HWInventory.json\",\n" +
            "        \"HWInventoryPath\" : \"/opt/ucs/etc\",\n" +
            "        \"HWInventoryQueryPath\" : \"/opt/ucs/etc\",\n" +
            "        \"HWInventoryDiscStatUrl\" : \"/Inventory/DiscoveryStatus\",\n" +
            "        \"eventCount\": 3,\n" +
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

    private String invalidEventSimConfig = "{\n" +
            "    \"eventsimConfig\" : {\n" +
            "        \"SensorMetadata\": \"/resources/ForeignSensorMetaData.json\",\n" +
            "        \"RASMetadata\": \"/resources/ForeignEventMetaData.json\",\n" +
            "        \"JobsMetadata\": \"/resources/ForeignJobsMetaData.json\",\n" +
            "        \"BootParameters\" : \"/opt/ucs/etc/BootParameters.json\",\n" +
            "        \"HWInventory\" : \"/opt/ucs/etc/HWInventory.json\",\n" +
            "        \"HWInventoryPath\" : \"/opt/ucs/etc\",\n" +
            "        \"HWInventoryQueryPath\" : \"/opt/ucs/etc\",\n" +
            "        \"HWInventoryDiscStatUrl\" : \"/Inventory/DiscoveryStatus\",\n" +
            "        \"eventCount\": 3,\n" +
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

    private String scenarioConfig = "{\n" +
            "  \"mode\" : \"repeat\",\n" +
            "  \"group-burst\" : {\n" +
            "    \"totalRas\" : \"7\",\n" +
            "    \"totalSensor\" : \"3\",\n" +
            "    \"totalBootOn\" : \"0\",\n" +
            "    \"totalBootOff\" : \"0\",\n" +
            "    \"totalBootReady\" : \"0\",\n" +
            "    \"ras\" : \"5\",\n" +
            "    \"sensor\" : \"1\",\n" +
            "    \"boot-on\" : \"19\",\n" +
            "    \"boot-off\" : \"19\",\n" +
            "    \"boot-ready\" : \"19\",\n" +
            "    \"seed\" : \"123\"\n" +
            "  },\n" +
            "  \"burst\" : {\n" +
            "    \"ras\" : \"6\",\n" +
            "    \"sensor\" : \"4\",\n" +
            "    \"boot-on\" : \"19\",\n" +
            "    \"boot-off\" : \"19\",\n" +
            "    \"boot-ready\" : \"19\",\n" +
            "    \"rate\" : \"5\",\n" +
            "    \"seed\" : \"123\"\n" +
            "  },\n" +
            "  \"repeat\" : {\n" +
            "    \"mode\" : \"burst\",\n" +
            "    \"clock-mode\" : \"counter\",\n" +
            "    \"duration\" : \"1\",\n" +
            "    \"counter\" : \"1\",\n" +
            "    \"start-time\" : \"2020-05-27 16:34:50.607Z\"\n" +
            "  },\n" +
            "  \"delay\" : \"0\"\n" +
            "}";
}