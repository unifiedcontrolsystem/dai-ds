package com.intel.dai.eventsim

import com.intel.config_io.ConfigIO
import com.intel.config_io.ConfigIOFactory
import com.intel.dai.eventsim.java11.Java11RESTServer
import com.intel.logging.Logger
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
        parser_ = ConfigIOFactory.getInstance("json");
        networkConfig_ = parser_.fromString(networkConfigStr)
        networkObjectTest_ = new NetworkObject(networkConfig_, Mock(Logger), Mock(ApiReqData))
    }

    def "Initialise network object" () {
        networkConfig_.put(networkObjectTest_.SERVER_NETWORK, "sse")
        networkObjectTest_.initialise()
        expect:
        networkObjectTest_.getNetworkName() == "sse"
        networkObjectTest_.getPort() == "8080"
        networkObjectTest_.getAddress() == "localhost"
        networkObjectTest_.networkConnectionObject != null
    }

    def "Initialise network object exception" () {
        networkConfig_.put(networkObjectTest_.SERVER_NETWORK, "other")
        when:
        networkObjectTest_.initialise()
        then:
        def e = thrown(SimulatorException.class)
        e.getMessage() == "Cannot initialise the given network : other"
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
        networkConfig_.put(networkObjectTest_.SERVER_NETWORK, "callback")
        networkObjectTest_.initialise()

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

    def "Test exception occured to start and stop server" () {
        networkObjectTest_.initialise()

        networkObjectTest_.networkConnectionObject = Mock(NetworkConnectionObject)
        networkObjectTest_.networkConnectionObject.startServer() >> {throw new RESTServerException("could not start server")}
        networkObjectTest_.networkConnectionObject.stopServer() >> {throw new RESTServerException("could not stop server")}

        when:
        networkObjectTest_.startServer()
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "could not start server"

        when:
        networkObjectTest_.stopServer()
        then:
        e = thrown(SimulatorException)
        e.getMessage() == "could not stop server"

    }

    def "Exists subscriptions, fetch subscription with url or subscriber as null values" () {
        networkConfig_.put(networkObjectTest_.SERVER_NETWORK, "callback")
        networkObjectTest_.initialise()

        Map<String, String> input = new HashMap<>()
        input.put("url", "http://test.com")
        input.put("subscriber", "test")

        networkObjectTest_.unRegisterAll()
        networkObjectTest_.register("http://test.com", "test", input)

        when:
        networkObjectTest_.getSubscription(null, "test")
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Insufficient details to get subscription: url or subscriber null value(s)"

        when:
        networkObjectTest_.getSubscription("http://test.com", null)
        then:
        e = thrown(SimulatorException)
        e.getMessage() == "Insufficient details to get subscription: url or subscriber null value(s)"
    }

    def "Register network with url or http method as null value" () {
        Map<String, String> input_parameters = new HashMap<String, String>()

        networkObjectTest_.initialise()

        when:
        networkObjectTest_.register(null, "GET", input_parameters)
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Could not add subscription: url or subscriber null value(s)"

        when:
        networkObjectTest_.register("http://test.com", null, input_parameters)
        then:
        e = thrown(SimulatorException)
        e.getMessage() == "Could not add subscription: url or subscriber null value(s)"
    }

    def "Register network with valid url and subscriber" () {
        Map<String, String> input_parameters = new HashMap<String, String>()
        input_parameters.put("url", "http://test.com")
        input_parameters.put("subscriber", "GET")

        networkObjectTest_.initialise()

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

    def "Send data to network" () {
        networkObjectTest_.initialise()
        networkObjectTest_.networkConnectionObject = Mock(NetworkConnectionObject)
        networkObjectTest_.networkConnectionObject.send("telemetry", "message") >> {}

        expect:
        networkObjectTest_.send("telemetry", "message")
    }

    String networkConfigStr = "{\n" +
            "    \"server-network\" : \"sse\",\n" +
            "    \"publisher-network\": \"kafka\",\n" +
            "    \"sse\": {\n" +
            "      \"server-address\": \"localhost\" ,\n" +
            "      \"server-port\": \"8080\" ,\n" +
            "      \"urls\": {\n" +
            "        \"/url/dmtf-resource-event\": \"dmtfEvent\" ,\n" +
            "        \"/url/telemetry-voltage\": \"voltageTelemetry\" ,\n" +
            "        \"/url/telemetry-power\": \"powerTelemetry\" ,\n" +
            "      }\n" +
            "    } ,\n" +
            "    \"rabbitmq\": {\n" +
            "      \"exchangeName\": \"simulator\",\n" +
            "      \"uri\": \"amqp://localhost\"\n" +
            "    } ,\n" +
            "    \"kafka\": {\n" +
            "      \"bootstrap.servers\": \"localhost:9092\",\n" +
            "      \"schema.registry.url\": \"http://localhost:8081\",\n" +
            "      \"acks\": \"all\",\n" +
            "      \"retries\": \"10\"\n" +
            "    }\n" +
            "  }"

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
            "            \"server-port\": \"8080\" ,\n" +
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
            "        \"server-network\": \"other\" ,\n" +
            "        \"publisher-network\": \"other\" ,\n" +
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

    PropertyMap networkConfig_
    ConfigIO parser_
    NetworkObject networkObjectTest_
}
