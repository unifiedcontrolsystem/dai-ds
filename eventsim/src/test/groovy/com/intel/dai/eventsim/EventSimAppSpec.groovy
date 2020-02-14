package com.intel.dai.eventsim

import com.intel.config_io.ConfigIOFactory
import com.intel.logging.Logger
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class EventSimAppSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    def "Read EventSim config file, fetch boot parameters" () {
        Logger log = Mock(Logger)
        final File bootPrametersConfigFileLoation = tempFolder.newFile("BootParameters.json")
        loadDataIntoFile(bootPrametersConfigFileLoation, bootParametersConfig)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json")
        eventSimApiTest.simEngineDataLoader = Mock(DataLoaderEngine.class)
        eventSimApiTest.simEngineDataLoader.getBootParamsFileLocation() >> bootPrametersConfigFileLoation.getAbsolutePath()
        eventSimApiTest.bootParamsApi_ = new BootParameters();
        Map<String, String> parameters = new HashMap<>()
        expect :
            eventSimApiTest.getBootParameters(parameters).contains("boot-image-id")
    }

    def "Fetch boot parameters, occured exception" () {
        Logger log = Mock(Logger)
        final File bootPrametersConfigFileLoation = tempFolder.newFile("BootParameters.json")
        loadDataIntoFile(bootPrametersConfigFileLoation, bootParametersConfig)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json")
        eventSimApiTest.simEngineDataLoader = Mock(DataLoaderEngine.class)
        eventSimApiTest.simEngineDataLoader.getBootParamsFileLocation() >> ""
        eventSimApiTest.bootParamsApi_ = new BootParameters();
        Map<String, String> parameters = new HashMap<>()
        when:
            eventSimApiTest.getBootParameters(parameters).contains("boot-image-id")
        then:
            def e = thrown(SimulatorException)
            e.getMessage() == "Invalid or null boot parameters config file."
    }

    def "Initiate inventory discovery" () {
        Logger log = Mock(Logger)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.simEngineDataLoader = Mock(DataLoaderEngine.class)
        eventSimApiTest.simEngineDataLoader.getHwInventoryDiscStatusUrl() >> "/Inventory/DiscoveryStatus"
        eventSimApiTest.source_ = Mock(NetworkObject.class)
        eventSimApiTest.source_.getAddress() >> "localhost"
        eventSimApiTest.source_.getPort() >> 1234
        Map<String, String> params = new HashMap<>()
        params.put("xnames", "test1")
        expect :
            eventSimApiTest.initiateInventoryDiscovery(params).contains("[{\"URI\":\"http://localhost:1234/Inventory/DiscoveryStatus/0\"}]")
    }

    def "Initiate inventory discovery, for null locations" () {
        Logger log = Mock(Logger)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.simEngineDataLoader = Mock(DataLoaderEngine.class)
        eventSimApiTest.simEngineDataLoader.getHwInventoryDiscStatusUrl() >> "/Inventory/DiscoveryStatus"
        eventSimApiTest.source_ = Mock(NetworkObject.class)
        eventSimApiTest.source_.getAddress() >> "localhost"
        eventSimApiTest.source_.getPort() >> 1234
        Map<String, String> params = new HashMap<>()
        params.put("xnames", null)
        when:
            eventSimApiTest.initiateInventoryDiscovery(params)
        then:
            def e = thrown(ResultOutputException)
            e.getMessage() == "404::One or more requested RedfishEndpoint xname IDs was not found."
    }

    def "Inventory discovery status" () {
        Logger log = Mock(Logger)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        params.put("xnames", "test1")
        expect :
            eventSimApiTest.getAllInventoryDiscoveryStatus(params).contains("{\"Status\":\"Complete\",\"Details\":null,\"LastUpdateTime\":")
    }

    def "Read EventSim config file, fetch inventory hardware" () {
        Logger log = Mock(Logger)
        final File hwInventoryConfigFileLocation = tempFolder.newFile("HWInventory.json")
        loadDataIntoFile(hwInventoryConfigFileLocation, hwInventoryConfig)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json")
        eventSimApiTest.simEngineDataLoader = Mock(DataLoaderEngine.class)
        eventSimApiTest.simEngineDataLoader.getHwInventoryFileLocation() >> hwInventoryConfigFileLocation.getAbsolutePath()
        eventSimApiTest.hwInvApi_ = new HardwareInventory();
        Map<String, String> parameters = new HashMap<>()
        expect :
            eventSimApiTest.getInventoryHardware(parameters).contains("processor-node-id-1")
    }

    def "Fetch inventory hardware, occured exception" () {
        Logger log = Mock(Logger)
        final File hwInventoryConfigFileLocation = tempFolder.newFile("HWInventory.json")
        loadDataIntoFile(hwInventoryConfigFileLocation, hwInventoryConfig)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json")
        eventSimApiTest.simEngineDataLoader = Mock(DataLoaderEngine.class)
        eventSimApiTest.simEngineDataLoader.getHwInventoryFileLocation() >> ""
        eventSimApiTest.hwInvApi_ = new HardwareInventory();
        Map<String, String> parameters = new HashMap<>()
        when :
            eventSimApiTest.getInventoryHardware(parameters)
        then:
            def e = thrown(SimulatorException)
            e.getMessage() == "Invalid or null hardware inventory config file"
    }

    def "Read EventSim config file, fetch inventory hardware for location" () {
        Logger log = Mock(Logger)
        final File hwInventoryLocationConfigFileLocation = tempFolder.newFile("x0c0s0b0n0.json")
        loadDataIntoFile(hwInventoryLocationConfigFileLocation, hwInventoryConfig)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json")
        eventSimApiTest.simEngineDataLoader = Mock(DataLoaderEngine.class)
        eventSimApiTest.simEngineDataLoader.getHwInventoryFileLocationPath() >> hwInventoryLocationConfigFileLocation.getParent()
        eventSimApiTest.hwInvApi_ = new HardwareInventory();
        Map<String, String> parameters = new HashMap<>()
        parameters.put("sub_component", "x0c0s0b0n0")
        expect :
            eventSimApiTest.getInventoryHardwareForLocation(parameters).contains("processor-node-id-1")
    }

    def "Fetch inventory hardware, occured exception for location" () {
        Logger log = Mock(Logger)
        final File hwInventoryLocationConfigFileLocation = tempFolder.newFile("x0c0s0b0n0.json")
        loadDataIntoFile(hwInventoryLocationConfigFileLocation, hwInventoryConfig)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json")
        eventSimApiTest.simEngineDataLoader = Mock(DataLoaderEngine.class)
        eventSimApiTest.simEngineDataLoader.getHwInventoryFileLocationPath() >> ""
        eventSimApiTest.hwInvApi_ = new HardwareInventory();
        Map<String, String> parameters = new HashMap<>()
        parameters.put("sub_component", "x0c0s0b0n0")
        when :
            eventSimApiTest.getInventoryHardwareForLocation(parameters)
        then:
            def e = thrown(SimulatorException)
            e.getMessage() == "Invalid or null hardware inventory config path"
    }

    def "Read EventSim config file, fetch inventory hardware query for location" () {
        Logger log = Mock(Logger)
        final File hwInventoryQueryConfigFileLoation = tempFolder.newFile("x0c0s0b0n0.json")
        loadDataIntoFile(hwInventoryQueryConfigFileLoation, hwInventoryConfig)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json")
        eventSimApiTest.simEngineDataLoader = Mock(DataLoaderEngine.class)
        eventSimApiTest.simEngineDataLoader.getHwInventoryQueryLocationPath() >> hwInventoryQueryConfigFileLoation.getParent()
        eventSimApiTest.hwInvApi_ = new HardwareInventory();
        Map<String, String> parameters = new HashMap<>()
        parameters.put("sub_component", "x0c0s0b0n0")
        expect :
            eventSimApiTest.getInventoryHardwareQueryForLocation(parameters).contains("processor-node-id-1")
    }

    def "Fetch inventory hardware, occured exception query for location" () {
        Logger log = Mock(Logger)
        final File hwInventoryQueryConfigFileLoation = tempFolder.newFile("x0c0s0b0n0.json")
        loadDataIntoFile(hwInventoryQueryConfigFileLoation, hwInventoryConfig)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json")
        eventSimApiTest.simEngineDataLoader = Mock(DataLoaderEngine.class)
        eventSimApiTest.simEngineDataLoader.getHwInventoryQueryLocationPath() >> ""
        eventSimApiTest.hwInvApi_ = new HardwareInventory();
        Map<String, String> parameters = new HashMap<>()
        parameters.put("sub_component", "x0c0s0b0n0")
        when :
            eventSimApiTest.getInventoryHardwareQueryForLocation(parameters)
        then:
            def e = thrown(SimulatorException)
            e.getMessage() == "Invalid or null hardware inventory query path"
    }

    private void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.writeStringToFile(file, data);
    }

    private final String eventSimConfig = "{\n" +
            "    \"eventsimConfig\" : {\n" +
            "        \"SystemManifest\": \"/opt/ucs/etc/SystemManifest.json\",\n" +
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
            "}\n"

    private final String bootParametersConfig = "{\n" +
            "  \"boot-images\": {\n" +
            "    \"content\": [\n" +
            "      {\n" +
            "        \"id\": \"boot-image-id\",\n" +
            "        \"description\": \"boot-image-description\",\n" +
            "        \"BootImageFile\": \"boot-image-file\",\n" +
            "        \"BootImageChecksum\": \"boot-image-checksum\",\n" +
            "        \"BootOptions\": null,\n" +
            "        \"KernelArgs\": null,\n" +
            "        \"BootStrapImageFile\": \"boot-strap-image-file\",\n" +
            "        \"BootStrapImageChecksum\": \"boot-strap-image-checksum\"\n" +
            "      }\n" +
            "    ]\n" +
            "  }\n" +
            "}\n"

    private final String hwInventoryConfig = "[{\n" +
            "  \"Nodes\": [\n" +
            "    {\n" +
            "      \"ID\": \"node-id\"\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Processors\": [\n" +
            "    {\n" +
            "      \"ID\": \"processor-node-id-1\"\n" +
            "    },\n" +
            "    {\n" +
            "      \"ID\": \"processor-node-id-2\",\n" +
            "    }\n" +
            "  ],\n" +
            "  \"Memory\": [\n" +
            "    {\n" +
            "      \"ID\": \"memory-node-id\"\n" +
            "    }\n" +
            "  ]\n" +
            "}]"

}
