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
        eventSimApiTest.jsonParser_.fromString(eventSimApiTest.getBootParameters(parameters)).getAsArray().getMap(0).containsKey("hosts")
    }

    def "Read EventSim config file, fetch boot parameters for a known location" () {
        Logger log = Mock(Logger)
        final File bootPrametersConfigFileLoation = tempFolder.newFile("BootParameters.json")
        loadDataIntoFile(bootPrametersConfigFileLoation, bootParametersConfig)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json")
        eventSimApiTest.simEngineDataLoader = Mock(DataLoaderEngine.class)
        eventSimApiTest.simEngineDataLoader.getBootParamsFileLocation() >> bootPrametersConfigFileLoation.getAbsolutePath()
        eventSimApiTest.bootParamsApi_ = new BootParameters();
        Map<String, String> parameters = new HashMap<>()
        parameters.put("hosts", "x0")
        expect :
        eventSimApiTest.jsonParser_.fromString(eventSimApiTest.getBootParameters(parameters)).getAsArray().getMap(0).containsKey("hosts")
        eventSimApiTest.jsonParser_.fromString(eventSimApiTest.getBootParameters(parameters)).getAsArray().getMap(0).getArrayOrDefault("hosts", null).get(0).equals("x0")
    }

    def "Read EventSim config file, fetch boot parameters for a default location" () {
        Logger log = Mock(Logger)
        final File bootPrametersConfigFileLoation = tempFolder.newFile("BootParameters.json")
        loadDataIntoFile(bootPrametersConfigFileLoation, bootParametersConfig)
        EventSimApp eventSimApiTest = new EventSimApp(log)
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json")
        eventSimApiTest.simEngineDataLoader = Mock(DataLoaderEngine.class)
        eventSimApiTest.simEngineDataLoader.getBootParamsFileLocation() >> bootPrametersConfigFileLoation.getAbsolutePath()
        eventSimApiTest.bootParamsApi_ = new BootParameters();
        Map<String, String> parameters = new HashMap<>()
        parameters.put("hosts", "s0")
        expect :
        eventSimApiTest.jsonParser_.fromString(eventSimApiTest.getBootParameters(parameters)).getAsArray().getMap(0).containsKey("hosts")
        eventSimApiTest.jsonParser_.fromString(eventSimApiTest.getBootParameters(parameters)).getAsArray().getMap(0).getArrayOrDefault("hosts", null).get(0).equals("Default")
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

    private final String bootParametersConfig = "[\n" +
            "  {\n" +
            "    \"hosts\": [\n" +
            "      \"Default\"\n" +
            "    ],\n" +
            "    \"initrd\": \"initird\",\n" +
            "    \"kernel\": \"kernel\",\n" +
            "    \"params\": \"params-data\",\n" +
            "    \"id\": \"boot-image-id\",\n" +
            "    \"description\": \"boot-image-description\",\n" +
            "    \"BootImageFile\": \"boot-image-file\",\n" +
            "    \"BootImageChecksum\": \"boot-image-checksum\",\n" +
            "    \"BootOptions\": null,\n" +
            "    \"KernelArgs\": null,\n" +
            "    \"BootStrapImageFile\": \"bootstrap-file\",\n" +
            "    \"BootStrapImageChecksum\": \"bootstrap-checksum\"\n" +
            "  },\n" +
            "  {\n" +
            "    \"hosts\": [\n" +
            "      \"x0\"\n" +
            "    ],\n" +
            "    \"initrd\": \"initird.img\",\n" +
            "    \"kernel\": \"kernel\",\n" +
            "    \"params\": \"parms-data\",\n" +
            "    \"id\": \"boot-image-id-1\",\n" +
            "    \"description\": \"bootimage-description\",\n" +
            "    \"BootImageFile\": \"boot-image-file-1\",\n" +
            "    \"BootImageChecksum\": \"boot-image-checksum\",\n" +
            "    \"BootOptions\": null,\n" +
            "    \"KernelArgs\": null,\n" +
            "    \"BootStrapImageFile\": \"bootstrap-file\",\n" +
            "    \"BootStrapImageChecksum\": \"bootstrap-checksum\"\n" +
            "  }\n" +
            "]"

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
