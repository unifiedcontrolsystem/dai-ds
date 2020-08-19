package com.intel.dai.eventsim


import com.intel.logging.Logger
import com.intel.networking.restclient.RESTClientException
import com.intel.networking.restserver.RESTServerException
import com.intel.properties.PropertyMap
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class NetworkObjectSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    def "Initialise sse network object" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        expect:
        networkObjectTest.getNetworkName() == "sse"
        networkObjectTest.getPort() == "5678"
        networkObjectTest.getAddress() == "localhost"
        networkObjectTest.networkConnectionObject != null
    }

    def "Initialise sse network object exception" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.networkConnectionObject = Mock(SSENetwork.class)
        networkObjectTest.networkConnectionObject.initialize() >> {throw new RESTServerException("unable to initialise")}
        when:
        networkObjectTest.initialise()
        then:
        def e = thrown(SimulatorException.class)
        e.getMessage() == "unable to initialise"
        networkObjectTest.getNetworkName() == "sse"
    }

    def "Initialise callback network object" () {
        loadData(callbackConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        expect:
        networkObjectTest.getNetworkName() == "callback"
        networkObjectTest.getPort() == null
        networkObjectTest.getAddress() == null
        networkObjectTest.networkConnectionObject != null
    }

    def "Initialise rabbitmq network object" () {
        loadData(rabbitmqConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        when:
        networkObjectTest.initialise()
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Cannot initialise the given network : rabbitmq"
        networkObjectTest.getNetworkName() == "rabbitmq"
        networkObjectTest.networkConnectionObject == null
    }

    def "Initialise other network object" () {
        loadData(otherConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        when:
        networkObjectTest.initialise()
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Cannot initialise the given network : other"
        networkObjectTest.getNetworkName() == "other"
        networkObjectTest.networkConnectionObject == null
    }

    def "Start and stop server" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        expect:
        networkObjectTest.startServer()
        networkObjectTest.serverStatus()
        networkObjectTest.stopServer()
        !networkObjectTest.serverStatus()
    }

    def "No prior subscriptions, create, fetch and delete subscription for a url and subscriber" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        networkObjectTest.networkConnectionObject = new CallBackNetwork(Mock(Logger))
        networkObjectTest.networkConnectionObject.initialize()

        Map<String, String> input = new HashMap<>()
        input.put("url", "http://test.com")
        input.put("subscriber", "test")

        networkObjectTest.unRegisterAll()
        networkObjectTest.register("http://test.com", "test", input)
        expect:
        networkObjectTest.getAllSubscriptions().isMap()
        networkObjectTest.getAllSubscriptions().getAsMap().size() == 1
        networkObjectTest.getSubscription("http://test.com", "test").get("ID") == 1
        networkObjectTest.getSubscriptionForId(1).getAsMap().get("url") == "http://test.com"
        networkObjectTest.unRegisterId(1)
        networkObjectTest.getSubscription("http://test.com", "test").size() == 0
    }

    def "Test exceptions for invalid config file missing serverAddress details" () {
        loadData(networkConfig)
        PropertyMap config = config.getMap("networkConfig")
        config.getMap("sse").remove("server-address")
        when:
        new NetworkObject(config, Mock(Logger), Mock(ApiReqData))
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "EventSim Configuration file doesn't contain 'server-address' entry"
    }

    def "Test exceptions for invalid config file missing serverPort details" () {
        loadData(networkConfig)
        PropertyMap config = config.getMap("networkConfig")
        config.getMap("sse").remove("server-port")
        when:
        new NetworkObject(config, Mock(Logger), Mock(ApiReqData))
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "EventSim Configuration file doesn't contain 'server-port' entry"
    }

    def "Test exceptions for invalid config file missing urls details" () {
        loadData(networkConfig)
        PropertyMap config = config.getMap("networkConfig")
        config.getMap("sse").remove("urls")
        when:
        new NetworkObject(config, Mock(Logger), Mock(ApiReqData))
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "EventSim Configuration file doesn't contain 'urls' entry"
    }

    def "Test exceptions for invalid config file missing exchangeName details" () {
        loadData(networkConfig)
        PropertyMap config = config.getMap("networkConfig")
        config.getMap("rabbitmq").remove("exchangeName")
        when:
        new NetworkObject(config, Mock(Logger), Mock(ApiReqData))
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "EventSim Configuration file doesn't contain 'exchangeName' entry"
    }

    def "Test exceptions for invalid config file missing uri details" () {
        loadData(networkConfig)
        PropertyMap config = config.getMap("networkConfig")
        config.getMap("rabbitmq").remove("uri")
        when:
        new NetworkObject(config, Mock(Logger), Mock(ApiReqData))
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "EventSim Configuration file doesn't contain 'uri' entry"
    }

    def "Test exception occured to start server" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        networkObjectTest.networkConnectionObject = Mock(NetworkConnectionObject)
        networkObjectTest.networkConnectionObject.startServer() >> {throw new RESTServerException("could not start server")}
        when:
        networkObjectTest.startServer()
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "could not start server"
    }

    def "Test exception occured to stop server" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        networkObjectTest.networkConnectionObject = Mock(NetworkConnectionObject)
        networkObjectTest.networkConnectionObject.stopServer() >> {throw new RESTServerException("could not stop server")}
        when:
        networkObjectTest.stopServer()
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "could not stop server"
    }

    def "Test exception to fetch zero subscriptions" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        networkObjectTest.networkConnectionObject = Mock(NetworkConnectionObject)
        networkObjectTest.networkConnectionObject.stopServer() >> {throw new RESTServerException("could not stop server")}
        when:
        networkObjectTest.stopServer()
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "could not stop server"
    }

    def "Exists subscriptions, fetch subscription with url as null values" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        networkObjectTest.networkConnectionObject = new CallBackNetwork(Mock(Logger))
        networkObjectTest.networkConnectionObject.initialize()

        Map<String, String> input = new HashMap<>()
        input.put("url", "http://test.com")
        input.put("subscriber", "test")

        networkObjectTest.unRegisterAll()
        networkObjectTest.register("http://test.com", "test", input)
        when:
        networkObjectTest.getSubscription(null, "test")
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Could not find details with url or subscriber 'NULL' value(s)"
    }

    def "Exists subscriptions, fetch subscription with subscriber as null values" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        networkObjectTest.networkConnectionObject = new CallBackNetwork(Mock(Logger))
        networkObjectTest.networkConnectionObject.initialize()

        Map<String, String> input = new HashMap<>()
        input.put("url", "http://test.com")
        input.put("subscriber", "test")

        networkObjectTest.unRegisterAll()
        networkObjectTest.register("http://test.com", "test", input)
        when:
        networkObjectTest.getSubscription("http://test.com", null)
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Could not find details with url or subscriber 'NULL' value(s)"
    }

    def "Exists subscription, exception occured to get subscription" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.networkConnectionObject = Mock(NetworkConnectionObject)
        networkObjectTest.networkConnectionObject.initialize()
        networkObjectTest.networkConnectionObject.getSubscription("http://test.com", "test") >> {throw new RESTClientException("Cannot fetch subscription")}
        when:
        networkObjectTest.getSubscription("http://test.com", "test")
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Cannot fetch subscription"
    }

    def "Register network with url as null value" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        networkObjectTest.networkConnectionObject = Mock(NetworkConnectionObject)
        Map<String, String> input_parameters = new HashMap<String, String>()
        when:
        networkObjectTest.register(null, "GET", input_parameters)
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Could not register URL or HttpMethod or input params : NULL value(s)"
    }

    def "Register network with http method as null value" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.networkConnectionObject.initialize()
        networkObjectTest.networkConnectionObject = Mock(NetworkConnectionObject)
        Map<String, String> input_parameters = new HashMap<String, String>()
        when:
        networkObjectTest.register("http://test.com", null, input_parameters)
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Could not register URL or HttpMethod or input params : NULL value(s)"
    }

    def "Register network with valid url and subscriber" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        Map<String, String> input_parameters = new HashMap<String, String>()
        input_parameters.put("url", "http://test.com")
        input_parameters.put("subscriber", "GET")
        networkObjectTest.unRegisterAll()
        networkObjectTest.register("http://test.com", "GET", input_parameters)
        expect:
        networkObjectTest.getAllSubscriptions().getAsMap().size() == 1
        networkObjectTest.getAllSubscriptions().getAsMap().getArray("SubscriptionList").getMap(0).getString("ID") == "1"
        networkObjectTest.getSubscription("http://test.com", "GET").toString() == "[subscriber:GET, ID:1, url:http://test.com]"
        networkObjectTest.getSubscriptionForId(1).toString() == "[subscriber:GET, url:http://test.com]"
        networkObjectTest.unRegisterId(1)
        networkObjectTest.unRegisterAll()
        networkObjectTest.getAllSubscriptions().isEmpty()
    }

    def "Register network with valid url and httpmethod" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        networkObjectTest.register("http://test.com", "GET", Mock(NetworkSimulator))
        expect:
        networkObjectTest.getAllSubscriptions().getAsMap().size() == 0
    }

    def "Register url and subscriber throws exception" () {
            loadData(networkConfig)
            NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
            networkObjectTest.networkConnectionObject = Mock(NetworkConnectionObject)
            networkObjectTest.networkConnectionObject.register("test", "test", new HashMap<String, String>()) >> {throw new RESTClientException("unable to initialise")}
            when:
            networkObjectTest.register("test", "test", new HashMap<String, String>())
            then:
            def e = thrown(SimulatorException.class)
            e.getMessage() == "unable to initialise"
            networkObjectTest.networkConnectionObject != null
    }

    def "Send telemetry data to network" () {
        loadData(networkConfig)
        NetworkObject networkObjectTest = new NetworkObject(config.getMap("networkConfig"), Mock(Logger), Mock(ApiReqData))
        networkObjectTest.initialise()
        expect:
        networkObjectTest.send("telemetry", "message")
        networkObjectTest.send("stateChanges", "message")
        networkObjectTest.send("events", "message")
        networkObjectTest.send("other", "message")
    }

    void loadData(String networkConfig) {
        final File networkConfigFile = tempFolder.newFile("NetworkConfig.json")
        loadDataIntoFile(networkConfigFile, networkConfig)
        config = LoadFileLocation.fromFileLocation(networkConfigFile.getAbsolutePath())
    }

    private static void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.writeStringToFile(file, data);
    }

    String networkConfig = "{\n" +
            "    \"networkConfig\": {\n" +
            "        \"network\": \"sse\" ,\n" +
            "        \"sse\": {\n" +
            "            \"server-address\": \"localhost\" ,\n" +
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
            "}"

    String callbackConfig = "{\n" +
            "    \"networkConfig\": {\n" +
            "        \"network\": \"callback\" ,\n" +
            "        \"sse\": {\n" +
            "            \"server-address\": \"localhost\" ,\n" +
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
            "}"

    String rabbitmqConfig = "{\n" +
            "    \"networkConfig\": {\n" +
            "        \"network\": \"rabbitmq\" ,\n" +
            "        \"sse\": {\n" +
            "            \"server-address\": \"localhost\" ,\n" +
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
            "}"

    String otherConfig = "{\n" +
            "    \"networkConfig\": {\n" +
            "        \"network\": \"other\" ,\n" +
            "        \"sse\": {\n" +
            "            \"server-address\": \"localhost\" ,\n" +
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
            "}"

    PropertyMap config
}
