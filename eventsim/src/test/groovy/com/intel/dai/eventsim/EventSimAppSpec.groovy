package com.intel.dai.eventsim

import com.intel.logging.Logger
import com.intel.properties.PropertyArray
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class EventSimAppSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    def setup() {
        logMock_ = Mock(Logger.class)

        eventSimApp_ = new EventSimApp("voltdb-server", "server-config-file", logMock_)
        eventSimApp_.log_ = logMock_
        eventSimApp_.dataLoader_ = Mock(DataLoader.class)
        eventSimApp_.dataLoader_.initialize() >> null
        eventSimApp_.dataLoader_.getNetworkConfigurationData() >> loadNetworkDetails()

        eventSimApp_.initialiseInstances()

        eventSimApp_.network_ = Mock(NetworkObject.class)
        eventSimApp_.network_.startServer() >> {}
        eventSimApp_.network_.stopServer() >> {}
        eventSimApp_.network_.serverStatus() >> {}
        eventSimApp_.foreignSimulatorEngine_ = Mock(ForeignSimulatorEngine.class)
        eventSimApp_.bootParamsApi_ = Mock(BootParameters.class)
        eventSimApp_.hwInvApi_ = Mock(HardwareInventory.class)
        eventSimApp_.wlmApi_ = Mock(WlmApi.class)
        eventSimApp_.apiReq_ = Mock(ApiReqData.class)

        eventSimApp_.run(eventSimApp_)
    }

    def "server status"() {
        eventSimApp_.startServer()
        expect:
        eventSimApp_.isServerUp_
        eventSimApp_.stopServer()
        !eventSimApp_.isServerUp_
        !eventSimApp_.serverStatus()
    }

    def "generate boot events"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("locations", "test")
        parameters.put("count", "1")

        eventSimApp_.foreignSimulatorEngine_.generateBootEvents(parameters) >> {}

        expect:
        eventSimApp_.generateBootEvents(parameters).contains("Success")
    }

    def "generate ras events"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("locations", "test")
        parameters.put("count", "1")

        eventSimApp_.foreignSimulatorEngine_.generateRasEvents(parameters) >> {}

        expect:
        eventSimApp_.generateRasEvents(parameters).contains("Success")
    }

    def "generate sensor events"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("locations", "test")
        parameters.put("count", "1")

        eventSimApp_.foreignSimulatorEngine_.generateSensorEvents(parameters) >> {}

        expect:
        eventSimApp_.generateSensorEvents(parameters).contains("Success")
    }

    def "generate scenario events"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("locations", "test")
        parameters.put("file", "/tmp/scenario.json")

        eventSimApp_.foreignSimulatorEngine_.generateSensorEvents(parameters) >> {}

        expect:
        eventSimApp_.generateSensorEvents(parameters).contains("Success")
    }

    def "fetch random seed" () {
        Map<String, String> parameters = new HashMap<>()
        eventSimApp_.foreignSimulatorEngine_.getRandomizationSeed() >> "123"
        expect :
        eventSimApp_.getRandomizationSeed(parameters).contains("{\"Status\":\"F\",\"Result\":\"123\"}")
    }

    def "fetch available locations" () {
        Map<String, String> parameters = new HashMap<>()
        PropertyArray locations = new PropertyArray();
        locations.add("location-0")
        locations.add("location-1")
        eventSimApp_.foreignSimulatorEngine_.getAllAvailableLocations() >> locations
        expect :
        eventSimApp_.getAllAvailableLocations(parameters).contains("{\"Status\":\"F\",\"Result\":\"[\\\"location-0\\\",\\\"location-1\\\"]\"}")
    }

    def "generate boot/ras/sensor/scenario events with exception"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("locations", "test")
        parameters.put("count", "1")

        eventSimApp_.foreignSimulatorEngine_.generateBootEvents(parameters) >>
                {throw new SimulatorException("test exception")}
        eventSimApp_.foreignSimulatorEngine_.generateRasEvents(parameters) >>
                {throw new SimulatorException("test exception")}
        eventSimApp_.foreignSimulatorEngine_.generateSensorEvents(parameters) >>
                {throw new SimulatorException("test exception")}
        eventSimApp_.foreignSimulatorEngine_.generateEventsForScenario(parameters) >>
                {throw new SimulatorException("test exception")}

        expect:
        eventSimApp_.generateBootEvents(parameters).contains("test exception")
        eventSimApp_.generateRasEvents(parameters).contains("test exception")
        eventSimApp_.generateSensorEvents(parameters).contains("test exception")
        eventSimApp_.generateEventsForScenario(parameters).contains("test exception")
    }

    def "fetch boot parameters details"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("name", "host0")

        eventSimApp_.dataLoader_.getBootParamsFileLocation() >> BOOT_PARAMETERS_CONFIG
        eventSimApp_.bootParamsApi_ = new BootParameters()

        expect:
        eventSimApp_.parser_.fromString(eventSimApp_.getBootParameters(parameters)).getAsArray().getMap(0)
                .containsKey("hosts")
        eventSimApp_.parser_.fromString(eventSimApp_.getBootParameters(parameters)).getAsArray().getMap(0)
                .getArray("hosts").get(0).equals("host0")
    }

    def "fetch boot images details"() {
        Map<String, String> parameters = new HashMap<>()

        eventSimApp_.dataLoader_.getBootImagesFileLocation() >> BOOT_IMAGES_CONFIG
        eventSimApp_.bootImagesApi_ = new BootImages()

        expect:
        eventSimApp_.parser_.fromString(eventSimApp_.getBootImages(parameters)).getAsArray().getMap(0)
                .containsKey("id")
        eventSimApp_.parser_.fromString(eventSimApp_.getBootImages(parameters)).getAsArray().getMap(0)
                .getString("id").equals("boot-image-id-0")
    }

    def "Read tmp file, fetch boot images details"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("sub_component", "boot-image-id-0")

        when:
        File testConfig = createAndLoadDataToFile("test.json", file_data)
        eventSimApp_.dataLoader_.getBootImagesFileLocation() >> testConfig.getAbsolutePath()
        then:
        eventSimApp_.parser_.fromString(eventSimApp_.getBootImages(parameters)).getAsArray().size() == result0
        eventSimApp_.parser_.fromString(eventSimApp_.getBootImageForImageId(parameters)).getAsMap().size() == result1
        where:
        file_data                          | result0 |  result1
        "[]"                               |   0     |     0
        "[{\"id\" : \"boot-image-id-0\"}]" |   1     |     1
    }

    def "fetch boot images details for a given boot image id"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("sub_component", "boot-image-id-0")

        eventSimApp_.dataLoader_.getBootImagesFileLocation() >> BOOT_IMAGES_CONFIG

        expect:
        eventSimApp_.parser_.fromString(eventSimApp_.getBootImageForImageId(parameters)).getAsMap().containsKey("id")
        eventSimApp_.parser_.fromString(eventSimApp_.getBootImageForImageId(parameters)).getAsMap().getString("id")
                .equals("boot-image-id-0")
        parameters.put("sub_component", "")
        eventSimApp_.parser_.fromString(eventSimApp_.getBootImageForImageId(parameters)).getAsArray().size() == 0
    }

    def "fetch node state details"() {
        Map<String, String> parameters = new HashMap<>()

        eventSimApp_.dataLoader_.getNodeStateFileLocation() >> NODE_STATE_CONFIG
        eventSimApp_.nodeStatesApi_ = new NodeState()

        expect:
        eventSimApp_.parser_.fromString(eventSimApp_.getNodeStates(parameters)).getAsMap().containsKey("Components")
        eventSimApp_.parser_.fromString(eventSimApp_.getNodeStates(parameters)).getAsMap().getArray("Components")
                .getMap(0).containsKey("ID")
        eventSimApp_.parser_.fromString(eventSimApp_.getNodeStates(parameters)).getAsMap().getArray("Components")
                .getMap(0).getString("ID").equals("xname-0")
    }

    def "Read tmp file, fetch node state details"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("sub_component", "xname-0")

        when:
        File testConfig = createAndLoadDataToFile("test.json", file_data)
        eventSimApp_.dataLoader_.getNodeStateFileLocation() >> testConfig.getAbsolutePath()
        then:
        eventSimApp_.parser_.fromString(eventSimApp_.getNodeStates(parameters)).getAsMap().size() == result0
        eventSimApp_.parser_.fromString(eventSimApp_.getNodeStatesForLocation(parameters)).getAsMap().size() == result1
        where:
        file_data                                   | result0 |  result1
        "{\"Components\": [{}]}"                    |   1     |     0
        "{\"Components\": [{\"ID\": \"xname-0\"}]}" |   1     |     1
    }

    def "fetch node states details for a given location"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("sub_component", "xname-0")

        eventSimApp_.dataLoader_.getNodeStateFileLocation() >> NODE_STATE_CONFIG

        expect:
        eventSimApp_.parser_.fromString(eventSimApp_.getNodeStatesForLocation(parameters)).getAsMap().containsKey("ID")
        eventSimApp_.parser_.fromString(eventSimApp_.getNodeStatesForLocation(parameters)).getAsMap().getString("ID")
                .equals("xname-0")
        parameters.put("sub_component", "")
        eventSimApp_.parser_.fromString(eventSimApp_.getNodeStatesForLocation(parameters)).getAsMap().size() == 0
    }

    def "Initiate inventory discovery and observe discovery status"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("xnames", "test")

        eventSimApp_.dataLoader_.getHwInventoryDiscStatusUrl() >> "/Inventory/DiscoveryStatus"
        eventSimApp_.network_.getAddress() >> "localhost"
        eventSimApp_.network_.getPort() >> 1234
        expect:
        eventSimApp_.initiateInventoryDiscovery(parameters).contains("[{\"URI\":\"http://localhost:1234/Inventory/DiscoveryStatus/0\"}]")
        eventSimApp_.getAllInventoryDiscoveryStatus(parameters).contains("{\"Status\":\"Complete\",\"Details\":null,\"LastUpdateTime\":")
        when:
        parameters.put("xnames", null)
        eventSimApp_.initiateInventoryDiscovery(parameters)
        eventSimApp_.getAllInventoryDiscoveryStatus(parameters).contains("")
        then:
        def e = thrown(ResultOutputException)
        e.getMessage() == "404::One or more requested RedfishEndpoint foreign IDs was not found."
    }

    def "Fetch hardware inventory and query details"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("sub_component", "test_location")

        File input = createAndLoadDataToFile("test_location.json", HARDWARE_INVENTORY_CONFIG)

        eventSimApp_.dataLoader_.getHwInventoryFileLocation() >> input.getAbsolutePath()
        eventSimApp_.dataLoader_.getHwInventoryFileLocationPath() >> input.getParent()
        eventSimApp_.dataLoader_.getHwInventoryQueryLocationPath() >> input.getParent()
        eventSimApp_.hwInvApi_ = new HardwareInventory()

        expect:
        eventSimApp_.getInventoryHardware(parameters).contains("processor-node-id-1")
        eventSimApp_.getInventoryHardwareForLocation(parameters).contains("processor-node-id-1")
        eventSimApp_.getInventoryHardwareQueryForLocation(parameters).contains("processor-node-id-1")
    }

    def "createReservation" () {
        eventSimApp_.wlmApi_ = new WlmApi(logMock_)
        eventSimApp_.wlmApi_.bgschedPath = "./build/tmp/bgsched.log"
        Map<String, String> parameters = new HashMap<>()
        parameters.put("name", "testres")
        parameters.put("users", "root")
        parameters.put("nodes", "node01 node02")
        parameters.put("starttime", "2019-02-14 02:15:58")
        parameters.put("duration", "3600000")

        expect :
        eventSimApp_.createReservation(parameters).contains("{\"Status\":\"F\"")
    }

    def "createReservation Exception" () {
        eventSimApp_.wlmApi_ = new WlmApi(logMock_)
        eventSimApp_.wlmApi_.bgschedPath = "./build/tmp/bgsched.log"
        Map<String, String> parameters = null

        expect :
        eventSimApp_.createReservation(parameters).contains("{\"Status\":\"E\"")
    }

    def "modifyReservation" () {
        eventSimApp_.wlmApi_ = new WlmApi(logMock_)
        eventSimApp_.wlmApi_.bgschedPath = "./build/tmp/bgsched.log"
        Map<String, String> parameters = new HashMap<>()
        parameters.put("name", "testres")
        parameters.put("users", "root")
        parameters.put("nodes", "node01 node02")
        parameters.put("starttime", "2019-02-14 02:15:58")
        expect :
        eventSimApp_.modifyReservation(parameters).contains("{\"Status\":\"F\"")
    }

    def "modifyReservation Exception" () {
        eventSimApp_.wlmApi_ = new WlmApi(logMock_)
        eventSimApp_.wlmApi_.bgschedPath = "./build/tmp/bgsched.log"
        Map<String, String> parameters = null

        expect :
        eventSimApp_.modifyReservation(parameters).contains("{\"Status\":\"E\"")
    }

    def "deleteReservation" () {
        eventSimApp_.wlmApi_ = new WlmApi(logMock_)
        eventSimApp_.wlmApi_.bgschedPath = "./build/tmp/bgsched.log"
        Map<String, String> parameters = new HashMap<>()
        parameters.put("name", "testres")

        expect :
        eventSimApp_.deleteReservation(parameters).contains("{\"Status\":\"F\"")
    }

    def "deleteReservation Exception" () {
        eventSimApp_.wlmApi_ = new WlmApi(logMock_)
        eventSimApp_.wlmApi_.bgschedPath = "./build/tmp/bgsched.log"
        Map<String, String> parameters = null

        expect :
        eventSimApp_.deleteReservation(parameters).contains("{\"Status\":\"E\"")
    }

    def "startJob" () {
        eventSimApp_.wlmApi_ = new WlmApi(logMock_)
        eventSimApp_.wlmApi_.cqmPath = "./build/tmp/cqm.log"
        Map<String, String> parameters = new HashMap<>()
        parameters.put("jobid", "10")
        parameters.put("name", "testjob")
        parameters.put("users", "root")
        parameters.put("nodes", "node01 node02")
        parameters.put("starttime", "2019-02-14 02:15:58")
        parameters.put("workdir", "/home")

        expect :
        eventSimApp_.startJob(parameters).contains("{\"Status\":\"F\"")
    }

    def "startJob Exception" () {
        eventSimApp_.wlmApi_ = new WlmApi(logMock_)
        eventSimApp_.wlmApi_.cqmPath = "./build/tmp/cqm.log"
        Map<String, String> parameters = null

        expect :
        eventSimApp_.startJob(parameters).contains("{\"Status\":\"E\"")
    }

    def "terminateJob" () {
        eventSimApp_.wlmApi_ = new WlmApi(logMock_)
        eventSimApp_.wlmApi_.cqmPath = "./build/tmp/cqm.log"
        Map<String, String> parameters = new HashMap<>()
        parameters.put("jobid", "10")
        parameters.put("name", "testjob")
        parameters.put("users", "root")
        parameters.put("nodes", "node01 node02")
        parameters.put("starttime", "2019-02-14 02:15:58")
        parameters.put("workdir", "/home")
        parameters.put("exitstatus", "0")

        expect :
        eventSimApp_.terminateJob(parameters).contains("{\"Status\":\"F\"")
    }

    def "terminateJob Exception" () {
        eventSimApp_.wlmApi_ = new WlmApi(logMock_)
        eventSimApp_.wlmApi_.cqmPath = "./build/tmp/cqm.log"
        Map<String, String> parameters = null

        expect :
        eventSimApp_.terminateJob(parameters).contains("{\"Status\":\"E\"")
    }

    def "simulateWlm Exception" () {
        eventSimApp_.wlmApi_ = new WlmApi(logMock_)
        eventSimApp_.wlmApi_.cqmPath = "./build/tmp/cqm.log"
        Map<String, String> parameters = null

        expect :
        eventSimApp_.simulateWlm(parameters).contains("{\"Status\":\"E\"")
    }

    def loadNetworkDetails() {
        return eventSimApp_.parser_.fromString(NETWORK_CONFIG).getAsMap()
    }

    private File createAndLoadDataToFile(String filename, String data) throws Exception {
        final File newFile = tempFolder.newFile(filename)
        FileUtils.writeStringToFile(newFile, data)
        return newFile
    }

    private Logger logMock_
    private EventSimApp eventSimApp_
    private final String BOOT_IMAGES_CONFIG = "/resources/test-config-files/TestBootImages.json"
    private final String BOOT_PARAMETERS_CONFIG = "/resources/test-config-files/TestBootParameters.json"
    private final String NODE_STATE_CONFIG = "/resources/test-config-files/TestNodeState.json"

    private final String NETWORK_CONFIG = "{\n" +
            "  \"network\" : \"sse\",\n" +
            "  \"sse\": {\n" +
            "    \"server-address\": \"local\" ,\n" +
            "    \"server-port\": \"1234\" ,\n" +
            "    \"urls\": {\n" +
            "      \"/url-1\": [\n" +
            "        \"stream-1\"\n" +
            "      ]\n" +
            "    }\n" +
            "  } ,\n" +
            "  \"rabbitmq\": {\n" +
            "    \"exchangeName\": \"simulator\",\n" +
            "    \"uri\": \"amqp://127.0.0.1\"\n" +
            "  }\n" +
            "}"

    private final String HARDWARE_INVENTORY_CONFIG = "[{\n" +
            "  \"Nodes\": [\n" +
            "    {\n" +
            "      \"ID\": \"node-id\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Processors\": [\n" +
            "    {\n" +
            "      \"ID\": \"processor-node-id-1\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Memory\": [\n" +
            "    {\n" +
            "      \"ID\": \"memory-node-id\"\n" +
            "    }\n" +
            "  ]\n" +
            "}]"
}