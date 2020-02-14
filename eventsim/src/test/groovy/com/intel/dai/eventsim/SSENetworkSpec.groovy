package com.intel.dai.eventsim

import com.intel.logging.Logger
import com.intel.networking.HttpMethod
import com.intel.networking.restserver.RESTServer
import com.intel.networking.restserver.RESTServerException
import com.intel.networking.restserver.RESTServerHandler
import com.intel.networking.restserver.Request
import com.intel.properties.PropertyArray
import com.intel.properties.PropertyMap
import com.intel.properties.PropertyNotExpectedType
import org.apache.commons.io.FileUtils
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean

class SSENetworkSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    def "initialise sse instance" () {
        loadData()
        SSENetwork sseNetworkTest = new SSENetwork(config.getMap("sseConfig"), Mock(Logger))
        sseNetworkTest.initialize()
        sseNetworkTest.server_ = Mock(RESTServer)
        sseNetworkTest.server_.setAddress("localhost") >> ""
        sseNetworkTest.server_.setPort(1234) >> ""
        sseNetworkTest.server_.addSSEHandler(any(), any()) >> ""
        sseNetworkTest.server_.running_ = Mock(AtomicBoolean)
        sseNetworkTest.server_.running_.get() >> false
        sseNetworkTest.server_.routes_ = Mock(TreeMap)
        sseNetworkTest.server_.routes_.containsKey(any()) >> false
        sseNetworkTest.initialize()
        expect :
            sseNetworkTest.server_ != null
    }

    def "Start server" () {
        loadData()
        SSENetwork sseNetworkTest = new SSENetwork(config.getMap("sseConfig"), Mock(Logger))
        sseNetworkTest.server_ = Mock(RESTServer)
        sseNetworkTest.server_.running_ = Mock(AtomicBoolean)
        sseNetworkTest.server_.running_.set(true)
        sseNetworkTest.startServer()
        expect:
        sseNetworkTest.serverStatus() == true
    }

    def "Stop server" () {
        loadData()
        SSENetwork sseNetworkTest = new SSENetwork(config.getMap("sseConfig"), Mock(Logger))
        sseNetworkTest.server_ = Mock(RESTServer)
        sseNetworkTest.server_.running_ = Mock(AtomicBoolean)
        sseNetworkTest.server_.running_.set(false)
        sseNetworkTest.stopServer()
        expect:
        sseNetworkTest.serverStatus() == false
    }

    def "Get server address" () {
        loadData()
        SSENetwork sseNetworkTest = new SSENetwork(config.getMap("sseConfig"), Mock(Logger))
        sseNetworkTest.server_ = Mock(RESTServer)
        sseNetworkTest.server_.running_ = Mock(AtomicBoolean)
        sseNetworkTest.server_.running_.set(false)
        sseNetworkTest.server_.setAddress("localhost")
        expect:
        sseNetworkTest.getAddress() == "localhost"
    }

    def "Get server port" () {
        loadData()
        SSENetwork sseNetworkTest = new SSENetwork(config.getMap("sseConfig"), Mock(Logger))
        sseNetworkTest.server_ = Mock(RESTServer)
        sseNetworkTest.server_.running_ = Mock(AtomicBoolean)
        sseNetworkTest.server_.running_.set(false)
        sseNetworkTest.server_.setPort(1234)
        expect:
        sseNetworkTest.getPort() == "1234"
    }

    def "Publish data to sse network in constant mode" () {
        loadData()
        SSENetwork sseNetworkTest = new SSENetwork(config.getMap("sseConfig"), Mock(Logger))
        sseNetworkTest.server_ = Mock(RESTServer)
        sseNetworkTest.server_.running_ = Mock(AtomicBoolean)
        sseNetworkTest.server_.running_.set(true)
        List<String> eventMessages = new ArrayList<>()
        eventMessages.add("message")
        expect:
        sseNetworkTest.publish("telemetry", eventMessages, true, 1)
    }

    def "Publish data to sse network in burst mode" () {
        loadData()
        SSENetwork sseNetworkTest = new SSENetwork(config.getMap("sseConfig"), Mock(Logger))
        sseNetworkTest.server_ = Mock(RESTServer)
        sseNetworkTest.server_.running_ = Mock(AtomicBoolean)
        sseNetworkTest.server_.running_.set(true)
        List<String> eventMessages = new ArrayList<>()
        eventMessages.add("message")
        expect:
        sseNetworkTest.publish("telemetry", eventMessages, false, 1)
    }

    def "Test logging occured error message while publishing data" () {
        loadData()
        SSENetwork sseNetworkTest = new SSENetwork(config.getMap("sseConfig"), Mock(Logger))
        sseNetworkTest.server_ = Mock(RESTServer)
        sseNetworkTest.server_.running_ = Mock(AtomicBoolean)
        sseNetworkTest.server_.running_.set(true)
        List<String> eventMessages = new ArrayList<>()
        eventMessages.add("message")
        sseNetworkTest.server_.ssePublish("message", "message", null) >> { throw new RESTServerException() }
        expect:
        sseNetworkTest.publish("message", eventMessages, true, 1)
    }

    def "Test register to sse network with url as null value" () {
        loadData()
        SSENetwork sseNetworkTest = new SSENetwork(config.getMap("sseConfig"), Mock(Logger))
        sseNetworkTest.server_ = Mock(RESTServer)
        when:
        sseNetworkTest.register(null, "GET", Mock(RESTServerHandler))
        then:
        def e = thrown(RESTServerException)
        e.getMessage() == "Could not register URL or HttpMethod or call back method : NULL value(s)"
    }

    def "Test register to sse network with http method as null value" () {
        loadData()
        SSENetwork sseNetworkTest = new SSENetwork(config.getMap("sseConfig"), Mock(Logger))
        sseNetworkTest.server_ = Mock(RESTServer)
        when:
        sseNetworkTest.register("http://test.com", null, Mock(RESTServerHandler))
        then:
        def e = thrown(RESTServerException)
        e.getMessage() == "Could not register URL or HttpMethod or call back method : NULL value(s)"
    }

    def "Test register to sse network with valid details" () {
        loadData()
        SSENetwork sseNetworkTest = new SSENetwork(config.getMap("sseConfig"), Mock(Logger))
        sseNetworkTest.server_ = Mock(RESTServer)
        sseNetworkTest.server_.running_ = Mock(AtomicBoolean)
        sseNetworkTest.server_.running_.get() >> false
        sseNetworkTest.server_.routes_ = Mock(TreeMap)
        sseNetworkTest.server_.routes_.containsKey(any()) >> false
        sseNetworkTest.server_.log_ = Mock(Logger)
        expect:
        sseNetworkTest.register("http://test.com", "GET", Mock(RESTServerHandler))
    }

    void loadData() {
        final File sseConfigFile = tempFolder.newFile("SSEConfig.json")
        loadDataIntoFile(sseConfigFile, sseConfig)
        config = LoadFileLocation.fromFileLocation(sseConfigFile.getAbsolutePath())
    }

    private void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.writeStringToFile(file, data);
    }

    String sseConfig = "{\n" +
            "  \"sseConfig\": {\n" +
            "    \"serverAddress\": \"localhost\" ,\n" +
            "    \"serverPort\": \"1234\" ,\n" +
            "    \"urls\": {\n" +
            "      \"/v1/stream/cray-telemetry-fan\": [\n" +
            "        \"telemetry\"\n" +
            "      ] ,\n" +
            "      \"/streams/nodeBootEvents\": [\n" +
            "        \"stateChanges\"\n" +
            "      ] ,\n" +
            "      \"/v1/stream/cray-dmtf-resource-event\": [\n" +
            "        \"events\"\n" +
            "      ]\n" +
            "    }\n" +
            "  }\n" +
            "}"

    PropertyMap config
}
