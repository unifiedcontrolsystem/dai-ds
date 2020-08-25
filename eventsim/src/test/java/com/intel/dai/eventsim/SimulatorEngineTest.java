package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.NodeInformation;
import com.intel.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class SimulatorEngineTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    private DataLoader dataLoader_;
    private NetworkObject sourceMock_;
    private Logger logMock_;
    private ConfigIO parser_;
    private NodeInformation nodeInfoMock_;
    private DataStoreFactory factoryMock_;

    @Before
    public void setup() throws Exception {
        parser_ = ConfigIOFactory.getInstance("json");
        logMock_ = mock(Logger.class);
        sourceMock_ = mock(NetworkObject.class);

        File foreignServerConfigFile = createAndLoadDataToFile("Test.json",FOREIGN_SERVER_CONFIG);

        dataLoader_ = new DataLoader(foreignServerConfigFile.getAbsolutePath() , "voltdb-server", logMock_);
        nodeInfoMock_ = mock(NodeInformation.class);
        factoryMock_ = mock(DataStoreFactory.class);
        when(factoryMock_.createNodeInformation()).thenReturn(nodeInfoMock_);
        dataLoader_.factory_ = factoryMock_;

        locations_ = loadLocations();
        parameters_ = loadParameters();
        hostnames_ = loadHostnames();

        when(nodeInfoMock_.getNodeLocations()).thenReturn(locations_);
        when(nodeInfoMock_.getComputeHostnameFromLocationMap()).thenReturn(hostnames_);
        dataLoader_.initialize();
    }

    @Test
    public void testJobEvents() throws Exception {
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(dataLoader_, sourceMock_, logMock_);
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishJobEvents("TEST.*", ".*", "true", "0", "123", "5", "/dev/null");
        assertEquals(5, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testJobEvents_DefaultValues() throws Exception {
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(dataLoader_, sourceMock_, logMock_);
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishJobEvents(".*", ".*", "true", null, null, "3", null);
        assertEquals(3, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test(expected = SimulatorException.class)
    public void testJobEvents_MismatchRegexLocation() throws Exception {
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(dataLoader_, sourceMock_, logMock_);
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishJobEvents("GT.*", ".*", "true", null, null, null, null);
    }

    @Test(expected = SimulatorException.class)
    public void testJobEvents_MismatchRegexLabel() throws Exception {
        SimulatorEngine simulatorEngineTest = new SimulatorEngine(dataLoader_, sourceMock_, logMock_);
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishJobEvents(".*", "GT.*", "true", null, null, null, null);
    }

    @Test
    public void testScenario_Burst() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        SimulatorEngine simulatorEngineTest = new SimulatorEngine(dataLoader_, sourceMock_, logMock_);
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "burst", ".*", ".*", ".*", "0", "true", "0", "123", "1", null, null, null);
        assertEquals(67, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testScenario_GroupBurst() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        SimulatorEngine simulatorEngineTest = new SimulatorEngine(dataLoader_, sourceMock_, logMock_);
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

        SimulatorEngine simulatorEngineTest = new SimulatorEngine(dataLoader_, sourceMock_, logMock_);
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "repeat", ".*", ".*", ".*", "0", "true", "0", "123", null, "0", null, null);
        assertEquals(0, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test
    public void testScenario_Repeat_StartTime() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        SimulatorEngine simulatorEngineTest = new SimulatorEngine(dataLoader_, sourceMock_, logMock_);
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

        SimulatorEngine simulatorEngineTest = new SimulatorEngine(dataLoader_, sourceMock_, logMock_);
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "repeat", ".*", ".*", ".*", "0", "true", "0", "123", "1", null, null, null);
        assertEquals(67, simulatorEngineTest.getPublishedEventsCount());
    }

    @Test(expected = SimulatorException.class)
    public void testScenario_ValidateParameters_RegexLoc_Null() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        SimulatorEngine simulatorEngineTest = new SimulatorEngine(dataLoader_, sourceMock_, logMock_);
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "group-burst", null, ".*", ".*", "0", "true", "0", "123", "1", null, null, null);
    }

    @Test(expected = SimulatorException.class)
    public void testScenario_MismatchRegexLocation() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        SimulatorEngine simulatorEngineTest = new SimulatorEngine(dataLoader_, sourceMock_, logMock_);
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "group-burst", "GT.*", ".*", ".*", "0", "true", "0", "123", "1", null, null, null);
    }

    @Test(expected = SimulatorException.class)
    public void testScenario_MismatchRegexLabel() throws Exception {
        final File scenarioConfigFile = tempFolder.newFile("scenario.json");
        loadDataIntoFile(scenarioConfigFile, scenarioConfig);

        SimulatorEngine simulatorEngineTest = new SimulatorEngine(dataLoader_, sourceMock_, logMock_);
        simulatorEngineTest.initialize();
        simulatorEngineTest.publishEventsForScenario(scenarioConfigFile.getAbsolutePath(), "group-burst", ".*", "GT.*", "GT.*", "0", "true", "0", "123", "1", null, null, null);
    }

    Map<String, String> loadParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("locations", "TEST-01");
        parameters.put("count", "1");
        return parameters;
    }

    List<String> loadLocations() {
        List<String> locations = new ArrayList<>();
        locations.add("TEST-01");
        locations.add("TEST-02");
        return locations;
    }

    Map<String, String> loadHostnames() {
        Map<String, String> hostnames = new HashMap<>();
        hostnames.put("test-location", "test-hostname");
        return hostnames;
    }

    private File createAndLoadDataToFile(String filename, String data) throws Exception {
        final File newFile = tempFolder.newFile(filename);
        FileUtils.writeStringToFile(newFile, data);
        return newFile;
    }

    private void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.writeStringToFile(file, data, StandardCharsets.UTF_8);
    }

    private List<String> locations_ = new ArrayList<>();

    private Map<String, String> parameters_ = new HashMap<>();
    private Map<String, String> hostnames_ = new HashMap<>();

    private String FOREIGN_SERVER_CONFIG = "{\n" +
            "  \"api-simulator-config\" : {\n" +
            "      \"boot-parameters\" : \"/opt/ucs/etc/BootParameters.json\",\n" +
            "      \"hw-inventory\" : \"/opt/ucs/etc/HWInventory.json\",\n" +
            "      \"hw-inventory-path\" : \"/opt/ucs/etc\",\n" +
            "      \"hw-inventory-query-path\" : \"/opt/ucs/etc/HwInvQuery_\",\n" +
            "      \"hw-inv-discover-status-url\" : \"/Inventory/DiscoveryStatus\",\n" +
            "      \"sensor-metadata\": \"/resources/ForeignSensorMetaData.json\",\n" +
            "      \"ras-metadata\": \"/resources/ForeignEventMetaData.json\",\n" +
            "      \"jobs-metadata\" : \"/resources/ForeignJobsMetaData.json\"\n" +
            "  },\n" +
            "  \"events-simulator-config\" : {\n" +
            "    \"count\": 3,\n" +
            "    \"seed\": \"1234\",\n" +
            "    \"events-template-config\" : \"/opt/ucs/etc/EventsTemplate.json\",\n" +
            "    \"time-delay-mus\": 1\n" +
            "  },\n" +
            "  \"network-config\" : {\n" +
            "      \"network\" : \"sse\",\n" +
            "      \"sse\": {\n" +
            "          \"server-address\": \"sms01-nmn.local\" ,\n" +
            "          \"server-port\": \"8080\" ,\n" +
            "          \"urls\": {\n" +
            "            \"/v1/stream/cray-telemetry-fan\": [\n" +
            "              \"telemetry\"\n" +
            "            ] ,\n" +
            "            \"/streams/nodeBootEvents\": [\n" +
            "              \"stateChanges\"\n" +
            "            ] ,\n" +
            "            \"/v1/stream/cray-dmtf-resource-event\": [\n" +
            "              \"events\"\n" +
            "            ] ,\n" +
            "            \"/apis/sma-telemetry/v1/stream/cray-fabric-perf-telemetry\": [\n" +
            "              \"fabric\"\n" +
            "            ]\n" +
            "          }\n" +
            "      } ,\n" +
            "      \"rabbitmq\": {\n" +
            "          \"exchangeName\": \"simulator\",\n" +
            "          \"uri\": \"amqp://127.0.0.1\"\n" +
            "      }\n" +
            "  }\n" +
            "}";

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
            "        \"sse\": {\n" +
            "            \"serverAddress\": \"*\" ,\n" +
            "            \"server-port\": \"5678\" ,\n" +
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

    private final String EVENTS_CONFIG = "{\n" +
            "  \"event\": {\n" +
            "    \"fabric\" : {\n" +
            "      \"default\" : \"fabric-perf\",\n" +
            "      \"types\" :[\n" +
            "        \"fabric-perf\"\n" +
            "      ]\n" +
            "    }\n" +
            "  } ,\n" +
            "  \"event-types\": {\n" +
            "    \"fabric-perf\": {\n" +
            "      \"template\": \"/resources/templates/fabric-perf-telemetry.json\" ,\n" +
            "      \"stream-type\": \"fabric\" ,\n" +
            "      \"single-template\": {\n" +
            "        \"metrics/messages[*]/Events[*]/Oem/Sensors[?]/DeviceSpecificContext\": \".*\" ,\n" +
            "        \"metrics/messages[*]/Events[*]/Oem/Sensors[?]/Location\": \".*\"\n" +
            "      },\n" +
            "      \"single-template-count\": {\n" +
            "        \"metrics/messages[*]\": 1\n" +
            "      },\n" +
            "      \"path-count\" : {\n" +
            "        \"metrics/messages[*]/Events[*]/Oem/Sensors[*]\" : 3,\n" +
            "        \"metrics/messages[*]\" : 3\n" +
            "      },\n" +
            "      \"update-fields\": {\n" +
            "        \"metrics/messages[*]/Events[*]/Oem/Sensors[*]\": {\n" +
            "          \"DeviceSpecificContext\": {\n" +
            "            \"metadata\": \"/resources/metadata/ForeignEventMetaDataArray.json\" ,\n" +
            "            \"metadata-filter\": \".*\"\n" +
            "          } ,\n" +
            "          \"Location\": {\n" +
            "            \"metadata\": \"DB-Locations\" ,\n" +
            "            \"metadata-filter\": \".*\"\n" +
            "          } ,\n" +
            "          \"Value\": {\n" +
            "            \"metadata\": \"Integer\" ,\n" +
            "            \"metadata-filter\": [10, 12]\n" +
            "          }\n" +
            "        }\n" +
            "      },\n" +
            "      \"generate-data-and-overflow-path\" : {\n" +
            "        \"metrics/messages[*]/Events[*]/Oem/Sensors[*]\" : \"metrics/messages[*]\",\n" +
            "        \"metrics/messages[*]\" : \"new\"\n" +
            "      },\n" +
            "      \"timestamp\" : \"metrics/messages[?]/Events[*]/Oem/Sensors[?]/Timestamp\"\n" +
            "    }\n" +
            "  }\n" +
            "}";
}
