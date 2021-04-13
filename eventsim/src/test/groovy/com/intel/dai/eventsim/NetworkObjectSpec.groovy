package com.intel.dai.eventsim

import com.intel.config_io.ConfigIO
import com.intel.config_io.ConfigIOFactory
import com.intel.dai.eventsim.java11.Java11RESTServer
import com.intel.logging.Logger
import com.intel.networking.NetworkException
import com.intel.networking.restclient.RESTClientException
import com.intel.networking.restserver.RESTServerException
import com.intel.networking.restserver.RESTServerFactory
import com.intel.properties.PropertyMap
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class NetworkObjectSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    def setupSpec() {
        RESTServerFactory.addImplementation("jdk11", Java11RESTServer.class)
    }

    def cleanupSpec() {
        RESTServerFactory.removeImplementation("jdk11")
    }

    void setup() {
        parser_ = ConfigIOFactory.getInstance("json")
        networkConfig_ = parser_.fromString(sseConfig).getAsMap().getMap("networkConfig")
        networkObjectTest_ = new NetworkObject(networkConfig_, Mock(Logger), Mock(ApiReqData))
    }

    def "Initialise sse network object" () {
        networkConfig_.put(networkObjectTest_.NETWORK_TYPE, "sse")
        networkObjectTest_.initialise()
        expect:
        networkObjectTest_.getNetworkName() == "sse"
        networkObjectTest_.getPort() == "5678"
        networkObjectTest_.getAddress() == "localhost"
        networkObjectTest_.networkConnectionObject != null
    }

    def "Initialise callback network object" () {
        networkConfig_.put(networkObjectTest_.NETWORK_TYPE, "callback")
        networkObjectTest_.initialise()
        expect:
        networkObjectTest_.getNetworkName() == "callback"
        networkObjectTest_.networkConnectionObject != null
    }

    def "Initialise network object exception" () {
        networkConfig_.put(networkObjectTest_.NETWORK_TYPE, "other")
        when:
        networkObjectTest_.initialise()
        then:
        thrown(SimulatorException.class)
        networkObjectTest_.getNetworkName() == "other"
        networkObjectTest_.networkConnectionObject == null
    }

    def "Start and stop server" () {
        networkObjectTest_.initialise()
        expect:
        networkObjectTest_.startServer()
        networkObjectTest_.serverStatus()
        networkObjectTest_.stopServer()
        !networkObjectTest_.serverStatus()
    }

    def "No prior subscriptions, create, fetch and delete subscription for a url and subscriber" () {
        networkObjectTest_.initialise()
        networkObjectTest_.networkConnectionObject = new CallBackNetwork(Mock(Logger))
        networkObjectTest_.networkConnectionObject.initialize()

        Map<String, String> input = new HashMap<>()
        input.put("url", "http://test.com")
        input.put("subscriber", "test")

        networkObjectTest_.unRegisterAll()
        networkObjectTest_.register("http://test.com", "test", input)
        expect:
        networkObjectTest_.getAllSubscriptions().isMap()
        networkObjectTest_.getAllSubscriptions().getAsMap().size() == 1
        networkObjectTest_.getSubscription("http://test.com", "test").get("ID") == 1
        networkObjectTest_.getSubscriptionForId(1).getAsMap().get("url") == "http://test.com"
        networkObjectTest_.unRegisterId(1)
        networkObjectTest_.getSubscription("http://test.com", "test").size() == 0
    }

    def "Test exceptions for invalid config file missing serverAddress details" () {
        networkConfig_.getMap("sse").remove("server-address")
        when:
        networkObjectTest_.initialise()
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "SSE server configuration is missing required entry, entry =  server-address"
    }

    def "Test exceptions for invalid config file missing serverPort details" () {
        networkConfig_.getMap("sse").remove("server-port")
        when:
        networkObjectTest_.initialise()
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "SSE server configuration is missing required entry, entry =  server-port"
    }

    def "Test exceptions for invalid config file missing urls details" () {
        networkConfig_.getMap("sse").remove("urls")
        when:
        networkObjectTest_.initialise()
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "SSE server configuration is missing required entry, entry =  urls"
    }

    def "Test exception occured to start server" () {
        networkObjectTest_.initialise()
        networkObjectTest_.networkConnectionObject = Mock(NetworkConnectionObject)
        networkObjectTest_.networkConnectionObject.startServer() >> {throw new RESTServerException()}
        when:
        networkObjectTest_.startServer()
        then:
        thrown(SimulatorException)
    }

    def "Test exception occured to stop server" () {
        networkObjectTest_.initialise()
        networkObjectTest_.networkConnectionObject = Mock(NetworkConnectionObject)
        networkObjectTest_.networkConnectionObject.stopServer() >> {throw new RESTServerException()}
        when:
        networkObjectTest_.stopServer()
        then:
        thrown(SimulatorException)
    }

    def "Test exception to fetch zero subscriptions" () {
        networkObjectTest_.initialise()
        networkObjectTest_.networkConnectionObject = Mock(NetworkConnectionObject)
        networkObjectTest_.networkConnectionObject.stopServer() >> {throw new RESTServerException()}
        when:
        networkObjectTest_.stopServer()
        then:
        thrown(SimulatorException)
    }

    def "Exists subscriptions, fetch subscription with url as null values" () {
        networkObjectTest_.initialise()
        networkObjectTest_.networkConnectionObject = new CallBackNetwork(Mock(Logger))
        networkObjectTest_.networkConnectionObject.initialize()

        Map<String, String> input = new HashMap<>()
        input.put("url", "http://test.com")
        input.put("subscriber", "test")

        networkObjectTest_.unRegisterAll()
        networkObjectTest_.register("http://test.com", "test", input)
        when:
        networkObjectTest_.getSubscription(null, "test")
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Could not find details with url or subscriber 'NULL' value(s)"
    }

    def "Exists subscriptions, fetch subscription with subscriber as null values" () {
        networkObjectTest_.initialise()
        networkObjectTest_.networkConnectionObject = new CallBackNetwork(Mock(Logger))
        networkObjectTest_.networkConnectionObject.initialize()

        Map<String, String> input = new HashMap<>()
        input.put("url", "http://test.com")
        input.put("subscriber", "test")

        networkObjectTest_.unRegisterAll()
        networkObjectTest_.register("http://test.com", "test", input)
        when:
        networkObjectTest_.getSubscription("http://test.com", null)
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Could not find details with url or subscriber 'NULL' value(s)"
    }

    def "Exists subscription, exception occured to get subscription" () {
        networkObjectTest_.initialise()
        networkObjectTest_.networkConnectionObject = Mock(NetworkConnectionObject)
        networkObjectTest_.networkConnectionObject.getSubscription("http://test.com", "test") >> {throw new RESTClientException("Cannot fetch subscription")}
        when:
        networkObjectTest_.getSubscription("http://test.com", "test")
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Cannot fetch subscription"
    }

    def "Register network with url as null value" () {
        networkObjectTest_.initialise()
        networkObjectTest_.networkConnectionObject = Mock(NetworkConnectionObject)
        Map<String, String> input_parameters = new HashMap<String, String>()
        when:
        networkObjectTest_.register(null, "GET", input_parameters)
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Could not register URL or HttpMethod or input params : NULL value(s)"
    }

    def "Register network with http method as null value" () {
        networkObjectTest_.initialise()
        networkObjectTest_.networkConnectionObject = Mock(NetworkConnectionObject)
        Map<String, String> input_parameters = new HashMap<String, String>()
        when:
        networkObjectTest_.register("http://test.com", null, input_parameters)
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Could not register URL or HttpMethod or input params : NULL value(s)"
    }

    def "Register network with valid url and subscriber" () {
        networkObjectTest_.initialise()
        Map<String, String> input_parameters = new HashMap<String, String>()
        input_parameters.put("url", "http://test.com")
        input_parameters.put("subscriber", "GET")
        networkObjectTest_.unRegisterAll()
        networkObjectTest_.register("http://test.com", "GET", input_parameters)
        expect:
        networkObjectTest_.getAllSubscriptions().getAsMap().size() == 1
        networkObjectTest_.getAllSubscriptions().getAsMap().getArray("SubscriptionList").getMap(0).getString("ID") == "1"
        networkObjectTest_.getSubscription("http://test.com", "GET").toString() == "[subscriber:GET, ID:1, url:http://test.com]"
        networkObjectTest_.getSubscriptionForId(1).toString() == "[subscriber:GET, url:http://test.com]"
        networkObjectTest_.unRegisterId(1)
        networkObjectTest_.unRegisterAll()
        networkObjectTest_.getAllSubscriptions().isEmpty()
    }

    def "Register network with valid url and httpmethod" () {
        networkObjectTest_.initialise()
        networkObjectTest_.register("http://test.com", "GET", Mock(NetworkSimulator))
        expect:
        networkObjectTest_.getAllSubscriptions().getAsMap().size() == 0
    }

    def "Register url and subscriber throws exception" () {
        networkObjectTest_.networkConnectionObject = Mock(NetworkConnectionObject)
        networkObjectTest_.networkConnectionObject.register("test", "test", new HashMap<String, String>()) >> {throw new RESTClientException("unable to initialise")}
        when:
        networkObjectTest_.register("test", "test", new HashMap<String, String>())
        then:
        def e = thrown(SimulatorException.class)
        e.getMessage() == "unable to initialise"
        networkObjectTest_.networkConnectionObject != null
    }

    def "set publisher to send data"() {
        when:
        networkConfig_.put(networkObjectTest_.PUBLISHER_NETWORK_TYPE, publishNetwork)
        networkObjectTest_.initialise()
        then:
        networkObjectTest_.send("subject", "message")
        where:
        publishNetwork | data
        "sse"          |  ""
        "callback"     |  ""
    }

    def "set publisher exception"() {
        networkConfig_.put(networkObjectTest_.PUBLISHER_NETWORK_TYPE, "kafka")
        networkObjectTest_.initialise()
        when:
        networkObjectTest_.send("subject", "message")
        then:
        thrown(NetworkException.class)
    }

    String sseConfig = "{\n" +
            "    \"networkConfig\": {\n" +
            "        \"server-network\": \"sse\" ,\n" +
            "        \"publisher-network\": \"sse\" ,\n" +
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
            "      \"kafka\": {\n" +
            "          \"bootstrap.servers\": \"boot_strap_ip:9092\",\n" +
            "          \"schema.registry.url\": \"http://schema_reg_ip:8081\",\n" +
            "          \"acks\": \"all\",\n" +
            "          \"retries\": \"10\",\n" +
            "          \"is_avro\": \"false\",\n" +
            "          \"additional-publish-property\": {\n" +
            "            \"energyTelemetry\": true,\n" +
            "            \"fabricPerf\": true,\n" +
            "            \"stateChange\": true,\n" +
            "            \"voltageTelemetry\": true,\n" +
            "            \"dmtfEvent\": true,\n" +
            "            \"powerTelemetry\": true,\n" +
            "            \"temperatureTelemetry\": true\n" +
            "          }\n" +
            "      }\n" +
            "    }\n" +
            "}"

    private PropertyMap networkConfig_ = new PropertyMap()
    private ConfigIO parser_
    private NetworkObject networkObjectTest_
}
