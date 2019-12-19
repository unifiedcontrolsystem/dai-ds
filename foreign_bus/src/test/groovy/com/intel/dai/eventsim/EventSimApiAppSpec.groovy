package com.intel.dai.eventsim

import com.intel.config_io.ConfigIO
import com.intel.config_io.ConfigIOFactory
import com.intel.logging.Logger
import com.intel.networking.NetworkException
import com.intel.networking.restserver.RESTServerException
import com.intel.properties.PropertyDocument
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.*

class EventSimApiAppSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    def "InventoryDiscoveryStatus" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        params.put("xnames", "test1")
        expect : test.getAllInventoryDiscoverStatus(params).contains("{\"Status\":\"Complete\",\"Details\":null,\"LastUpdateTime\":")
    }

    def "InitiateInventoryDiscover" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.source_ = Mock(NetworkSource.class)
        test.source_.getAddress() >> "localhost"
        test.source_.getPort() >> 1234
        Map<String, String> params = new HashMap<>()
        params.put("xnames", "test1")
        expect : test.initiateInventoryDiscover(params).contains("[{\"URI\":\"http://localhost:1234/Inventory/DiscoveryStatus/0\"}]")
    }

    def "MissingXnamesWhenInitiateInventoryDiscover" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.source_ = Mock(NetworkSource.class)
        test.source_.getAddress() >> "localhost"
        test.source_.getPort() >> 1234
        Map<String, String> params = new HashMap<>()
        when :
            test.initiateInventoryDiscover(params)
        then :
            def e = thrown(RESTServerException)
            e.message == "Error: xnames details is required."
    }

    def "TestInitializedInstances" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        final File systemManifestConfigFile = tempFolder.newFile("SystemManifest.json")
        loadDataIntoFile(systemManifestConfigFile, systemManifestConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        expect :
            test.source_ != null
            test.bootParamApi_ != null
            test.inventoryApi_ != null
            test.eventSimEngine != null
    }

    def "StartEventsimEngine" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        final File systemManifestConfigFile = tempFolder.newFile("SystemManifest.json")
        loadDataIntoFile(systemManifestConfigFile, systemManifestConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.eventSimEngine.systemManifestLocation_ = systemManifestConfigFile.getAbsolutePath()
        test.startEngine()
        expect :
            test.eventSimEngine.systemManifestJSON_.containsKey("sysname") == true
    }

    def "StopServer" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        final File systemManifestConfigFile = tempFolder.newFile("SystemManifest.json")
        loadDataIntoFile(systemManifestConfigFile, systemManifestConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.stopServer()
        expect :
            test.source_.getServerStatus() == false
    }

    def "RunEventsimServer" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        final File systemManifestConfigFile = tempFolder.newFile("SystemManifest.json")
        loadDataIntoFile(systemManifestConfigFile, systemManifestConfig)
        ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
        PropertyDocument data = jsonParser_.readConfig(eventsimConfigFile.getAbsolutePath())
        data.getAsMap().getMap("eventsimConfig").put("SystemManifest", systemManifestConfigFile.getAbsolutePath())
        loadDataIntoFile(eventsimConfigFile, jsonParser_.toString(data))
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.run(test)
        test.stopServer()
        expect :
            test.source_.getServerStatus() == false
    }

    def "RunEventsimServerException" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        final File systemManifestConfigFile = tempFolder.newFile("SystemManifest.json")
        loadDataIntoFile(systemManifestConfigFile, systemManifestConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        when :
            test.run(test)
            test.stopServer()
        then :
        thrown Exception
    }

    def "ExceptionGenerateRasEvents" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        params.put("xnames", "test1")
        expect :
            test.generatRasEvents(params).contains("{\"Status\":\"E\"")
    }

    def "ExceptionGenerateSensorEvents" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        params.put("xnames", "test1")
        expect :
            test.generateEnvEvents(params).contains("{\"Status\":\"E\"")
    }

    def "ExceptionGenerateBootEvents" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        params.put("xnames", "test1")
        expect :
            test.generateBootEvents(params).contains("{\"Status\":\"E\"")
    }

    def "SingleSubscriptions" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        test.unsubscribeAllStateChangeNotifications(params)
        params.put("Subscriber", "test1")
        params.put("Url", "http://test1.com")
        expect :
        test.subscribeStateChangeNotifications(params).equals("{\"Subscriber\":\"test1\",\"ID\":1,\"Url\":\"http:\\/\\/test1.com\"}")
    }

    def "DuplicateSCSubscriptions" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        test.unsubscribeAllStateChangeNotifications(params)
        params.put("Subscriber", "test1")
        params.put("Url", "http://test1.com")
        test.subscribeStateChangeNotifications(params)
        when :
            test.subscribeStateChangeNotifications(params)
        then :
            def e = thrown(NetworkException)
            e.message == "409::The subscription already exists for the specified subscriber and URL"
    }

    def "MissingSubscriberSCNotifications" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        test.unsubscribeAllStateChangeNotifications(params)
        params.put("Url", "http://test1.com")
        when :
            test.subscribeStateChangeNotifications(params)
        then :
            def e = thrown(NetworkException)
            e.message == "Insufficient data to subscribe a connection."
    }

    def "MissingUrlSCNotifications" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        test.unsubscribeAllStateChangeNotifications(params)
        params.put("Subscriber", "test1")
        when :
            test.subscribeStateChangeNotifications(params)
        then :
            def e = thrown(NetworkException)
            e.message == "Insufficient data to subscribe a connection."
    }

    def "MissingSubscribeAndUrlSCNotifications" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        test.unsubscribeAllStateChangeNotifications(params)
        when :
            test.subscribeStateChangeNotifications(params)
        then :
            def e = thrown(NetworkException)
            e.message == "Insufficient data to subscribe a connection."
    }

    def "UnSubscribeAllSCNotifications" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        params.put("Subscriber", "test1")
        params.put("Url", "http://test1.com")
        test.unsubscribeAllStateChangeNotifications(params)
        test.subscribeStateChangeNotifications(params)
        expect :
        test.unsubscribeAllStateChangeNotifications(params).equals("")
    }

    def "UnSubscribeSCNotifications" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params1 = new HashMap<>()
        params1.put("Subscriber", "test1")
        params1.put("Url", "http://test1.com")
        Map<String, String> params2 = new HashMap<>()
        params2.put("Subscriber", "test2")
        params2.put("Url", "http://test2.com")
        test.unsubscribeAllStateChangeNotifications(params1)
        test.subscribeStateChangeNotifications(params1)
        test.subscribeStateChangeNotifications(params2)
        params1.put("sub_cmd", "1")
        expect :
        test.unsubscribeStateChangeNotifications(params1).equals("")
        test.getAllSubscriptionDetails(params1).equals("{\"SubscriptionList\":[{\"Subscriber\":\"test2\",\"ID\":2,\"Url\":\"http:\\/\\/test2.com\"}]}")
    }

    def "UnSubscribeSCNotificationsInvalidId" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params1 = new HashMap<>()
        params1.put("Subscriber", "test1")
        params1.put("Url", "http://test1.com")
        Map<String, String> params2 = new HashMap<>()
        params2.put("Subscriber", "test2")
        params2.put("Url", "http://test2.com")
        test.unsubscribeAllStateChangeNotifications(params1)
        test.subscribeStateChangeNotifications(params1)
        test.subscribeStateChangeNotifications(params2)
        params1.put("sub_cmd", "3")
        when:
            test.unsubscribeStateChangeNotifications(params1)
        then:
            def e = thrown(Exception)
            e.message == "400::deletion of requested subscription doesn't exists."
    }

    def "SubscriptionDetailsForId" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        params.put("Subscriber", "test1")
        params.put("Url", "http://test1.com")
        params.put("sub_cmd", "1")
        test.unsubscribeAllStateChangeNotifications(params)
        test.subscribeStateChangeNotifications(params)
        expect :
        test.getSubscriptionDetailForId(params).equals("{\"Subscriber\":\"test1\",\"Url\":\"http:\\/\\/test1.com\",\"sub_cmd\":\"1\"}")
    }

    def "SubscriptionDetailsForInvalidId" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        params.put("Subscriber", "test1")
        params.put("Url", "http://test1.com")
        params.put("sub_cmd", "2")
        test.unsubscribeAllStateChangeNotifications(params)
        test.subscribeStateChangeNotifications(params)
        when:
            test.getSubscriptionDetailForId(params)
        then:
            def e = thrown(Exception)
            e.message == "Subscription details for requested id does not exists."
    }

    def "MissingIdToGetSubscriptionData" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params = new HashMap<>()
        params.put("Subscriber", "test1")
        params.put("Url", "http://test1.com")
        test.unsubscribeAllStateChangeNotifications(params)
        test.subscribeStateChangeNotifications(params)
        when :
            test.getSubscriptionDetailForId(params)
        then :
            def e = thrown(Exception)
            e.message == "Insufficient data to get subscription details."
    }

    def "MissingIdUnSubscribeSCNotifications" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params1 = new HashMap<>()
        params1.put("Subscriber", "test1")
        params1.put("Url", "http://test1.com")
        Map<String, String> params2 = new HashMap<>()
        params2.put("Subscriber", "test2")
        params2.put("Url", "http://test2.com")
        test.unsubscribeAllStateChangeNotifications(params1)
        test.subscribeStateChangeNotifications(params1)
        test.subscribeStateChangeNotifications(params2)
        when :
            test.unsubscribeStateChangeNotifications(params1)
        then :
            def e = thrown(NetworkException)
            e.message == "400::Insufficient data to unsubscribe a connection."

    }

    def "ZeroSubscriptions" () {
        Logger log = Mock(Logger)
        final File eventsimConfigFile = tempFolder.newFile("EventSim.json")
        loadDataIntoFile(eventsimConfigFile, eventsimConfig)
        EventSimApiApp test = new EventSimApiApp(eventsimConfigFile.getAbsolutePath(), log)
        test.initialize()
        test.jsonParser_ = ConfigIOFactory.getInstance("json");
        Map<String, String> params1 = new HashMap<>()
        params1.put("Subscriber", "test1")
        params1.put("Url", "http://test1.com")
        Map<String, String> params2 = new HashMap<>()
        params2.put("Subscriber", "test2")
        params2.put("Url", "http://test2.com")
        test.unsubscribeAllStateChangeNotifications(params1)
        when :
            test.getAllSubscriptionDetails(params1)
        then :
            def e = thrown(Exception)
            e.message == "No subscriptions exists."

    }


    private void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.writeStringToFile(file, data);
    }

    private String eventsimConfig = "{\n" +
            "    \"network\" : \"sse\",\n" +
            "    \"eventsimConfig\" : {\n" +
            "        \"SystemManifest\": \"/opt/ucs/SystemManifest.json\",\n" +
            "        \"SensorMetadata\": \"/resources/ForeignSensorMetaData.json\",\n" +
            "        \"RASMetadata\": \"/resources/ForeignEventMetaData.json\",\n" +
            "        \"eventCount\": 10,\n" +
            "        \"timeDelayMus\": 1,\n" +
            "        \"eventRatioSensorToRas\": 1,\n" +
            "        \"randomizerSeed\": \"234\"\n" +
            "    },\n" +
            "    \"sseConfig\" : {\n" +
            "        \"serverAddress\": \"10.54.134.150\",\n" +
            "        \"serverPort\": \"5678\",\n" +
            "        \"urls\" : {\n" +
            "            \"/streams/nodeTelemetry\" : [\"telemetry\"],\n" +
            "            \"/streams/nodeBootEvents\" : [\"stateChanges\"],\n" +
            "            \"/streams/nodeRasEvents\" : [\"events\"]\n" +
            "        }\n" +
            "    },\n" +
            "    \"rabbitmq\" : {\n" +
            "        \"exchangeName\" : \"simulator\",\n" +
            "        \"uri\"          : \"amqp://127.0.0.1\"\n" +
            "    }\n" +
            "}"

    private String systemManifestConfig = "{\n" +
            "    \"sysname\": \"Big\",\n" +
            "    \"views\": {\n" +
            "        \"Full\": {\n" +
            "            \"view\": \"Full\",\n" +
            "            \"view-description\": \"Full Floor Layout\",\n" +
            "            \"floor\" : {\n" +
            "                \"description\": \"The full floor map\",\n" +
            "                \"width\" : 1000, \"height\" : 760,\n" +
            "                \"content\" : [\n" +
            "                    { \"name\": \"R0-00\",  \"definition\": \"dense-rack\", \"x\":   1, \"y\":   1 }\n" +
            "                ]\n" +
            "            },\n" +
            "            \"definitions\": {\n" +
            "                \"dense-rack\" : {\n" +
            "                  \"description\": \"Dense compute rack\",\n" +
            "                  \"width\": 37, \"height\": 64, \"obscured\":true, \"type\": \"Rack\",\n" +
            "                  \"content\" : [\n" +
            "                    {\"name\": \"CH00\", \"definition\": \"dense-chassis-left\",  \"x\": 0, \"y\": 0},\n" +
            "                  ]\n" +
            "                },\n" +
            "                \"dense-chassis-left\": {\n" +
            "                  \"description\": \"Dense compute chassis\",\n" +
            "                  \"width\": 18, \"height\": 15, \"type\": \"Chassis\",\n" +
            "                  \"content\" : [\n" +
            "                    {\"name\": \"CN0\", \"definition\": \"dense-compute-node\", \"x\":2 , \"y\":1},\n" +
            "                  ]\n" +
            "                },\n" +
            "                \"dense-compute-node\": {\n" +
            "                  \"description\": \"Dense compute node (supernode)\",\n" +
            "                  \"width\": 2, \"height\": 13, \"type\": \"ComputeNode\",\n" +
            "                  \"content\" : []\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}"
}
