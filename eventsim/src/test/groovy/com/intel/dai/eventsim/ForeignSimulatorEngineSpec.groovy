package com.intel.dai.eventsim

import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.NodeInformation
import com.intel.logging.Logger
import groovy.json.JsonSlurper
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

import java.time.*

class ForeignSimulatorEngineSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    def setup() {
        logMock_ = Mock(Logger.class)

        dataLoader_ = new DataLoader(foreignServerConfigFile_, "voltdb-server", logMock_)
        nodeInfoMock_ = Mock(NodeInformation.class)
        factoryMock_ = Mock(DataStoreFactory.class)
        factoryMock_.createNodeInformation() >> nodeInfoMock_
        dataLoader_.factory_ = factoryMock_

        locations_ = loadLocations()
        parameters_ = loadParameters()
        hostnames_ = loadHostnames()

        nodeInfoMock_.getNodeLocations() >> locations_
        nodeInfoMock_.getComputeHostnameFromLocationMap() >> hostnames_

        dataLoader_.initialize()
    }

    def "generate boot events" () {
        sourceMock_ = Mock(NetworkObject.class)
        sourceMock_.send(any() as String, any() as String) >> {}
        ForeignSimulatorEngine foreignSimEngineTest = new ForeignSimulatorEngine(dataLoader_, sourceMock_, logMock_);
        foreignSimEngineTest.initialize()

        Map<String, String> parameters = new HashMap<>()
        parameters.put("locations", "TEST-01")
        parameters.put("count", "1")
        parameters.put("type", type)

        when:
        parameters.put("count", count.toString())
        foreignSimEngineTest.generateBootEvents(parameters)

        then:
        foreignSimEngineTest.publishedEvents_ == publishedEvents

        where:
        type       |    count    |  publishedEvents
        "off"      |      1      |      1
        "on"       |      1      |      1
        "ready"    |      1      |      1
        "all"      |      3      |      3
    }

    def "generate ras events" () {
        final File outputFile = tempFolder.newFile("output.json")
        sourceMock_ = Mock(NetworkObject.class)
        sourceMock_.send(any() as String, any() as String) >> {}
        ForeignSimulatorEngine foreignSimEngineTest = new ForeignSimulatorEngine(dataLoader_, sourceMock_, logMock_);
        foreignSimEngineTest.initialize()

        Map<String, String> parameters = new HashMap<>()
        parameters.put("locations", "TEST-01")
        parameters.put("count", "1")
        parameters.put("type", "old-ras")
        parameters.put("output", outputFile.getAbsolutePath())

        when:
        parameters.put("count", count.toString())
        foreignSimEngineTest.generateRasEvents(parameters)
        tempFolder.delete()

        then:
        foreignSimEngineTest.publishedEvents_ == publishedEvents
        foreignSimEngineTest.getRandomizationSeed().equals("1234")
        foreignSimEngineTest.getAllAvailableLocations().equals(locations_)

        where:
        count  |  publishedEvents
         2     |      2
         3     |      3
        10     |     10
        20     |     20
    }

    def "generate sensor events" () {
        sourceMock_ = Mock(NetworkObject.class)
        sourceMock_.send(any() as String, any() as String) >> {}
        ForeignSimulatorEngine foreignSimEngineTest = new ForeignSimulatorEngine(dataLoader_, sourceMock_, logMock_);
        foreignSimEngineTest.initialize()

        Map<String, String> parameters = new HashMap<>()
        parameters.put("locations", "TEST-01")
        parameters.put("count", "1")
        parameters.put("type", type)

        when:
        parameters.put("count", count.toString())
        foreignSimEngineTest.generateSensorEvents(parameters)

        then:
        foreignSimEngineTest.publishedEvents_ == publishedEvents
        foreignSimEngineTest.getRandomizationSeed().equals("1234")
        foreignSimEngineTest.getAllAvailableLocations().equals(locations_)

        where:
        type           |    count    |  publishedEvents
        "energy"       |      2      |      2
        "energy"       |      3      |      3
        "energy"       |     10      |     10
        "energy"       |     20      |     20
        "fabric-perf"  |      2      |      2
        "fabric-perf"  |      3      |      3
        "fabric-perf"  |     10      |     10
        "fabric-perf"  |     20      |     20
        "power"        |      2      |      2
        "power"        |      3      |      3
        "power"        |     10      |     10
        "power"        |     20      |     20
        "temperature"  |      2      |      2
        "temperature"  |      3      |      3
        "temperature"  |     10      |     10
        "temperature"  |     20      |     20
        "voltage"      |      2      |      2
        "voltage"      |      3      |      3
        "voltage"      |     10      |     10
        "voltage"      |     20      |     20
    }

    def "generate scenario events" () {
        sourceMock_ = Mock(NetworkObject.class)
        sourceMock_.send(any() as String, any() as String) >> {}
        ForeignSimulatorEngine foreignSimEngineTest = new ForeignSimulatorEngine(dataLoader_, sourceMock_, logMock_);
        foreignSimEngineTest.initialize()

        Map<String, String> parameters = new HashMap<>()
        parameters.put("locations", "TEST-01")
        parameters.put("type", type)
        parameters.put("seed", "1234")

        if(counter != null)
            parameters.put("counter", counter)

        if(duration != null)
            parameters.put("duration", duration)

        if(start_time != null) {
            Instant now = Instant.now()
            Duration fiveMinutes = Duration.ofSeconds( 1)
            Instant fiveMinutesFromNow = now.plus( fiveMinutes )
            ZonedDateTime zdt = fiveMinutesFromNow.atZone(ZoneId.systemDefault())
            parameters.put("start-time", zdt.toString())
        }

        parameters.put("file", foreignScenarioConfigFile_)

        when:
        foreignSimEngineTest.generateEventsForScenario(parameters)

        then:
        foreignSimEngineTest.publishedEvents_ == publishedEvents
        foreignSimEngineTest.getRandomizationSeed().equals("1234")
        foreignSimEngineTest.getAllAvailableLocations().equals(locations_)

        where:
        type            |  counter  |  duration | start_time  |   publishedEvents
        "burst"         |   null    |   null    |    null     |         11
        "group-burst"   |   null    |   null    |    null     |         11
        "repeat"        |    "2"    |   null    |    null     |         11
        "repeat"        |    "2"    |   null    |    ""       |         11
    }

    def "generate echo event" () {

        def jsonSlurper = new JsonSlurper()

        def message_obj
        def filecontent_obj

        sourceMock_ = Mock(NetworkObject.class)
        1 * sourceMock_.isStreamIDValid( *_ ) >> true


        ForeignSimulatorEngine foreignSimEngineTest = new ForeignSimulatorEngine(dataLoader_,sourceMock_, logMock_)
        foreignSimEngineTest.initialize()

        File file = new File(getClass().getResource(foreignEchoTemplateFile_).toURI())

        Map<String, String> parameters = new HashMap<>()
        parameters.put("file", foreignEchoTemplateFile_)
        parameters.put("connection", connection)

        when:
        foreignSimEngineTest.generateEchoEvents(parameters)

        then:
        1 * sourceMock_.send( *_ ) >> { subject, String message ->
            assert subject == connection

            message_obj = jsonSlurper.parse(message.toCharArray())
            filecontent_obj = jsonSlurper.parse(file)

            assert message_obj == filecontent_obj

        }

        where:
        test            | connection
        "ras"           | "events"
        "boot"          | "stateChanges"
        "telemetry"     | "telemetry"

    }

    def "bad echo file" () {

        sourceMock_ = Mock(NetworkObject.class)
        sourceMock_.send(any() as String, any() as String) >> {}
        sourceMock_.isStreamIDValid( *_) >> { return true }

        ForeignSimulatorEngine foreignSimEngineTest = new ForeignSimulatorEngine(dataLoader_,sourceMock_, logMock_)
        foreignSimEngineTest.initialize()

        Map<String, String> parameters = new HashMap<>()
        parameters.put("file", "foo.json")
        parameters.put("connection", "events")

        when:
        foreignSimEngineTest.generateEchoEvents(parameters)

        then:
        def e = thrown(SimulatorException)
        e.getMessage() == new String("Resource not found: foo.json")
        0 * sourceMock_.send()

    }

    def "bad echo connection" () {

        sourceMock_ = Mock(NetworkObject.class)
        sourceMock_.send(any() as String, any() as String) >> {}

        ForeignSimulatorEngine foreignSimEngineTest = new ForeignSimulatorEngine(dataLoader_,sourceMock_, logMock_)
        foreignSimEngineTest.initialize()

        Map<String, String> parameters = new HashMap<>()
        parameters.put("file", foreignEchoTemplateFile_)
        parameters.put("connection", "foo")

        when:
        foreignSimEngineTest.generateEchoEvents(parameters)

        then:
        def e = thrown(SimulatorException)
        e.getMessage() == new String("foo is not a valid streamID.")
        0 * sourceMock_.send()

    }

    def loadParameters() {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("locations", "TEST-01")
        parameters.put("count", "1")
        return parameters
    }

    def loadLocations() {
        List<String> locations = new ArrayList<>()
        locations.add("TEST-01")
        locations.add("TEST-02")
        return locations
    }

    def loadHostnames() {
        Map<String, String> hostnames = new HashMap<>()
        hostnames.put("test-location", "test-hostname")
        return hostnames
    }

    private NodeInformation nodeInfoMock_
    private Logger logMock_
    private NetworkObject sourceMock_
    private DataLoader dataLoader_

    private List<String> locations_ = new ArrayList<>()

    private Map<String, String> parameters_ = new HashMap<>()
    private Map<String, String> hostnames_ = new HashMap<>()

    private String foreignServerConfigFile_ = "/resources/test-config-files/TestConfig.json"
    private String foreignScenarioConfigFile_ = "/resources/test-config-files/TestScenario.json"
    private String foreignEchoTemplateFile_ = "/resources/templates/echo.json"

    private DataStoreFactory factoryMock_

}
