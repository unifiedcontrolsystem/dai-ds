package com.intel.dai.eventsim

import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.NodeInformation
import com.intel.logging.Logger
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
        sourceMock_ = Mock(NetworkObject.class)
        sourceMock_.send(any() as String, any() as String) >> {}
        ForeignSimulatorEngine foreignSimEngineTest = new ForeignSimulatorEngine(dataLoader_, sourceMock_, logMock_);
        foreignSimEngineTest.initialize()

        Map<String, String> parameters = new HashMap<>()
        parameters.put("locations", "TEST-01")
        parameters.put("count", "1")
        parameters.put("type", "old-ras")

        when:
        parameters.put("count", count.toString())
        foreignSimEngineTest.generateRasEvents(parameters)

        then:
        foreignSimEngineTest.publishedEvents_ == publishedEvents

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

        where:
        type            |  counter  |  duration | start_time  |   publishedEvents
        "burst"         |   null    |   null    |    null     |         11
        "group-burst"   |   null    |   null    |    null     |         11
        "repeat"        |    "2"    |   null    |    null     |         11
        "repeat"        |    "2"    |   null    |    ""       |         11
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

    private DataStoreFactory factoryMock_

}
