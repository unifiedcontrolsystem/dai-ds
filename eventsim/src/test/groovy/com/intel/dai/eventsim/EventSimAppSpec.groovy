package com.intel.dai.eventsim

import com.intel.logging.Logger
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

        eventSimApp_.source_ = Mock(NetworkObject.class)
        eventSimApp_.source_.startServer() >> {}
        eventSimApp_.source_.stopServer() >> {}
        eventSimApp_.source_.serverStatus() >> {}
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
        parameters.put("hosts", "x0")

        File input = createAndLoadDataToFile("test.json", BOOT_PARAM_CONFIG)
        eventSimApp_.dataLoader_.getBootParamsFileLocation() >> input.getAbsolutePath()
        eventSimApp_.bootParamsApi_ = new BootParameters()

        expect:
        eventSimApp_.parser_.fromString(eventSimApp_.getBootParameters(parameters)).getAsArray().getMap(0)
                .containsKey("hosts")
        eventSimApp_.parser_.fromString(eventSimApp_.getBootParameters(parameters)).getAsArray().getMap(0)
                .getArray("hosts").get(0).equals("x0")
        parameters.put("hosts", "x1")
        eventSimApp_.parser_.fromString(eventSimApp_.getBootParameters(parameters)).getAsArray().getMap(0)
                .containsKey("hosts")
        eventSimApp_.parser_.fromString(eventSimApp_.getBootParameters(parameters)).getAsArray().getMap(0)
                .getArray("hosts").get(0).equals("Default")
        parameters.put("hosts", null)
        eventSimApp_.parser_.fromString(eventSimApp_.getBootParameters(parameters)).getAsArray().getMap(0)
                .containsKey("hosts")
        eventSimApp_.parser_.fromString(eventSimApp_.getBootParameters(parameters)).getAsArray().getMap(0)
                .getArray("hosts").get(0).equals("Default")
    }

    def "Initiate inventory discovery and observe discovery status"() {
        Map<String, String> parameters = new HashMap<>()
        parameters.put("xnames", "test")

        eventSimApp_.dataLoader_.getHwInventoryDiscStatusUrl() >> "/Inventory/DiscoveryStatus"
        eventSimApp_.source_.getAddress() >> "localhost"
        eventSimApp_.source_.getPort() >> 1234
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

    private final String BOOT_PARAM_CONFIG = "[\n" +
            "  {\n" +
            "    \"hosts\": [\n" +
            "      \"Default\"\n" +
            "    ],\n" +
            "    \"id\": \"default-boot-image-id\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"hosts\": [\n" +
            "      \"x0\"\n" +
            "    ],\n" +
            "    \"id\": \"boot-image-id-x0\"\n" +
            "  }\n" +
            "]"

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