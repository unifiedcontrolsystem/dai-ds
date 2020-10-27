package com.intel.dai.eventsim

import com.intel.config_io.ConfigIO
import com.intel.config_io.ConfigIOFactory
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.NodeInformation
import com.intel.logging.Logger
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class DataLoaderSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    def setup() {
        parser_ = ConfigIOFactory.getInstance("json")
        nodeInfoMock_ = Mock(NodeInformation.class)
        factoryMock_ = Mock(DataStoreFactory.class)
        logMock_ = Mock(Logger.class)

        locations_ = load_locations()
        hostnames_ = load_hostnames()

        nodeInfoMock_.getNodeLocations() >> locations_
        nodeInfoMock_.getComputeHostnameFromLocationMap() >> hostnames_

        File foreignServerConfigFile = create_and_load_data_to_file("Test.json", FOREIGN_SIM_CONFIG)
        dataLoader = new DataLoader(foreignServerConfigFile.getAbsolutePath(), "voltdb-server", logMock_)
        factoryMock_.createNodeInformation() >> nodeInfoMock_
        dataLoader.factory_ = factoryMock_
    }

    def "load foreign server configuration details"() {
        dataLoader.initialize()
        expect:
        dataLoader.getEventsTemplateConfigurationFile() == "/tmp/template.json"
        dataLoader.getEventsConfigutaion("count", "-1") == "10"
        dataLoader.getEventsConfigutaion("unknown", "default-value") == "default-value"
        dataLoader.getNodeLocations().containsAll(locations_)
        dataLoader.getNodeLocationsHostnames().containsAll(hostnames_.values())
        dataLoader.getAllLocations().containsAll(locations_)
    }

    def "All locations should have a matching foreign name"() {
        locations_.add("UNKNOWN-LOCATION")
        when:
        dataLoader.initialize()
        then:
        def e = thrown(SimulatorException)
        e.message == "Not all locations in database has the mapping foreign name"
    }

    def load_locations() {
        List<String> locations = new ArrayList<>()
        locations.add("TEST-01")
        locations.add("TEST-02")
        return locations
    }

    def load_hostnames() {
        Map<String, String> hostnames = new HashMap<>()
        hostnames.put("TEST-01", "TEST-01-Host")
        return hostnames
    }

    def create_and_load_data_to_file(String filename, String data) throws Exception {
        final File newFile = tempFolder.newFile(filename)
        FileUtils.writeStringToFile(newFile, data)
        return newFile
    }

    private ConfigIO parser_
    private NodeInformation nodeInfoMock_
    private Logger logMock_
    private DataStoreFactory factoryMock_
    private DataLoader dataLoader

    private List<String> locations_ = new ArrayList<>()

    private Map<String, String> hostnames_ = new HashMap<>()

    private final String FOREIGN_SIM_CONFIG = "{\n" +
            "  \"api-simulator-config\" : {\n" +
            "      \"boot-parameters\" : \"/tmp/boot_parameters.json\",\n" +
            "      \"boot-images\" : \"/tmp/boot_images.json\",\n" +
            "      \"hw-inventory\" : \"/tmp/hw_inventory.json\",\n" +
            "      \"hw-inventory-path\" : \"/tmp\",\n" +
            "      \"hw-inventory-query-path\" : \"/tmp\",\n" +
            "      \"hw-inv-discover-status-url\" : \"/url/discovery_status\",\n" +
            "      \"sensor-metadata\": \"/tmp/sensor.json\",\n" +
            "      \"ras-metadata\": \"/tmp/ras.json\",\n" +
            "      \"jobs-metadata\" : \"/tmp/jobs.json\",\n" +
            "      \"node-state\" : \"/tmp/nodestate.json\"\n" +
            "  },\n" +
            "  \"events-simulator-config\" : {\n" +
            "    \"count\": 10,\n" +
            "    \"seed\": \"1234\",\n" +
            "    \"events-template-config\" : \"/tmp/template.json\",\n" +
            "    \"time-delay-mus\": 1\n" +
            "  },\n" +
            "  \"network-config\" : {\n" +
            "      \"network\" : \"sse\",\n" +
            "      \"sse\": {\n" +
            "          \"server-address\": \"local\" ,\n" +
            "          \"server-port\": \"1235\" ,\n" +
            "          \"urls\": {\n" +
            "          }\n" +
            "      } ,\n" +
            "      \"rabbitmq\": {\n" +
            "          \"exchangeName\": \"simulator\",\n" +
            "          \"uri\": \"1234\"\n" +
            "      }\n" +
            "  }\n" +
            "}"
}
