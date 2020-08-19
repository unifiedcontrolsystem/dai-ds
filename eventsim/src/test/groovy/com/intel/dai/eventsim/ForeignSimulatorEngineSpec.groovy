package com.intel.dai.eventsim

import com.intel.config_io.ConfigIO
import com.intel.config_io.ConfigIOFactory
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.NodeInformation
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class ForeignSimulatorEngineSpec extends Specification {

    private DataStoreFactory factoryMock_
    private ConfigIO parser_

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    def setup() {
        parser_ = ConfigIOFactory.getInstance("json")

        File eventTypeTemplate = createAndLoadDataToFile("EventTypeTemplate.json", EVENT_TEMPLATE)
        PropertyMap eventsTemplate = parser_.fromString(EVENTS_CONFIG).getAsMap()
        eventsTemplate.getMap("event-types").getMap("fabric").put("template", eventTypeTemplate.getAbsolutePath())

        File eventsTemplateConfigFile = createAndLoadDataToFile("TemplateConfig.json", parser_.toString(eventsTemplate))

        PropertyMap serverConfig = parser_.fromString(FOREIGN_SERVER_CONFIG).getAsMap()
        serverConfig.getMap("events-simulator-config").put("events-template-config", eventsTemplateConfigFile.getAbsolutePath())
        foreignServerConfigFile_ = createAndLoadDataToFile("ServerConfig.json", parser_.toString(serverConfig))

        logMock_ = Mock(Logger.class)

        dataLoader_ = new DataLoader(foreignServerConfigFile_.getAbsolutePath() , "voltdb-server", logMock_)
        nodeInfoMock_ = Mock(NodeInformation.class)
        factoryMock_ = Mock(DataStoreFactory.class)
        factoryMock_.createNodeInformation() >> nodeInfoMock_
        dataLoader_.factory_ = factoryMock_

        locations_ = loadLocations()
        parameters_ = loadParameters()
        hostnames_ = loadHostnames()

        nodeInfoMock_.getNodeLocations() >> locations_
        nodeInfoMock_.getComputeHostnameFromLocationMap() >> hostnames_

        dataLoader_.initialize()
    }

    def "generate ras events" () {
        sourceMock_ = Mock(NetworkObject.class)
        sourceMock_.send(any() as String, any() as String) >> {}
        ForeignSimulatorEngine foreignSimEngineTest = new ForeignSimulatorEngine(dataLoader_, sourceMock_, logMock_);
        foreignSimEngineTest.initialize()

        Map<String, String> parameters = new HashMap<>()
        parameters.put("locations", "TEST-01")
        parameters.put("count", "1")

        when:
        parameters.put("count", count.toString())
        foreignSimEngineTest.generateRasEvents(parameters)

        then:
        foreignSimEngineTest.publishedEvents_ == publishedEvents

        where:
        count  |  publishedEvents
         2     |      2
         3     |      3
         5     |      5
        12     |     12
    }

    def loadParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("locations", "TEST-01")
        parameters.put("count", "1")
        return parameters
    }

    def loadLocations() {
        List<String> locations = new ArrayList<>()
        locations.add("TEST-01")
        locations.add("TEST-02")
        return locations
    }

    def loadHostnames() {
        Map<String, String> hostnames = new HashMap<>()
        hostnames.put("test-location", "test-hostname")
        return hostnames
    }

    private File createAndLoadDataToFile(String filename, String data) throws Exception {
        final File newFile = tempFolder.newFile(filename)
        FileUtils.writeStringToFile(newFile, data)
        return newFile
    }

    private NodeInformation nodeInfoMock_
    private Logger logMock_
    private NetworkObject sourceMock_

    private PropertyMap eventsConfig_
    private DataLoader dataLoader_

    private List<String> locations_ = new ArrayList<>()

    private Map<String, String> parameters_ = new HashMap<>()
    private Map<String, String> hostnames_ = new HashMap<>()

    private File foreignServerConfigFile_;

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
            "    \"count\": 10,\n" +
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
            "}"

    private final String EVENTS_CONFIG = "{\n" +
            "  \"event\": {\n" +
            "    \"fabric\" : {\n" +
            "      \"default\" : \"fabric\",\n" +
            "      \"types\" :[\n" +
            "        \"fabric\"\n" +
            "      ]\n" +
            "    }\n" +
            "  } ,\n" +
            "  \"event-types\": {\n" +
            "    \"fabric\": {\n" +
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
            "      \"timestamp\" : \"metrics/messages[?]/Events[?]/Oem/Sensors[?]/Timestamp\"\n" +
            "    }\n" +
            "  }\n" +
            "}"

    private final String EVENT_TEMPLATE = "{\n" +
            "  \"metrics\": {\n" +
            "    \"messages\": [\n" +
            "      {\n" +
            "        \"Events\": [\n" +
            "          {\n" +
            "            \"EventTimestamp\": \"2020-06-03T22:40:14.059Z\" ,\n" +
            "            \"MessageId\": \"CrayFabricPerfTelemetry.RFC3635\" ,\n" +
            "            \"Oem\": {\n" +
            "              \"Sensors\": [\n" +
            "                {\n" +
            "                  \"Timestamp\": \"2020-06-03T22:40:14.059Z\" ,\n" +
            "                  \"Location\": \"ros-0040a683cda1l12\" ,\n" +
            "                  \"ParentalIndex\": 0 ,\n" +
            "                  \"PhysicalContext\": \"IfInOctets\" ,\n" +
            "                  \"Index\": 0 ,\n" +
            "                  \"DeviceSpecificContext\": \"edge\" ,\n" +
            "                  \"SubIndex\": 12 ,\n" +
            "                  \"Value\": \"14576\"\n" +
            "                } ,\n" +
            "                {\n" +
            "                  \"Timestamp\": \"2020-06-03T22:40:14.059Z\" ,\n" +
            "                  \"Location\": \"ros-0040a683cda1l12\" ,\n" +
            "                  \"ParentalIndex\": 0 ,\n" +
            "                  \"PhysicalContext\": \"IfInUcastPkts\" ,\n" +
            "                  \"Index\": 0 ,\n" +
            "                  \"DeviceSpecificContext\": \"edge\" ,\n" +
            "                  \"SubIndex\": 12 ,\n" +
            "                  \"Value\": \"20\"\n" +
            "                }\n" +
            "              ] ,\n" +
            "              \"TelemetrySource\": \"FabricPerf\"\n" +
            "            }\n" +
            "          }\n" +
            "        ]\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}"
}
