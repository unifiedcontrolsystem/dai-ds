package com.intel.dai.eventsim

import com.intel.properties.PropertyArray
import com.intel.properties.PropertyMap
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class NodeStateSpec extends Specification {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder()

    def setup() {
        data.put("key", "value")
    }

    def "Read tmp file, fetch node states details"() {
        final File testConfigFile = tempFolder.newFile("test.json")
        NodeState nodeStateTest = new NodeState()

        when:
        loadDataIntoFile(testConfigFile, file_data)
        nodeStateTest.setNodeStatesConfigFile(testConfigFile.absolutePath)
        nodeStateTest.getNodeStates()
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output)
        where:
        file_data             |   output
        "[{\"id\" ,: \"data]" |   "Error in loading node states data."
    }

    def "Read tmp file, fetch node state details for a given location"() {
        final File testConfigFile = tempFolder.newFile("test.json")
        NodeState nodeStateTest = new NodeState()

        when:
        loadDataIntoFile(testConfigFile, file_data)
        nodeStateTest.setNodeStatesConfigFile(testConfigFile.absolutePath)
        nodeStateTest.getNodeStateForLocation("xname-0")
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output)
        where:
        file_data             |   output
        "[{\"id\" ,: \"data]" |   "Error in loading node states data."
    }

    def "Set node state configuration file, fetch node states details for a given location"() {
        NodeState nodeStateTest = new NodeState()
        when:
        nodeStateTest.setNodeStatesConfigFile(file_name)
        nodeStateTest.getNodeStateForLocation("xname-0")
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output)
        where:
        file_name    |   output
        null         |   "Invalid or null node states config file."
        ""           |   "Invalid or null node states config file."
        "/test.json" |   "Given node states config file doesn't exists : /test.json"
    }

    def "Set node state configuration file, fetch node state details"() {
        NodeState nodeStateTest = new NodeState()
        when:
        nodeStateTest.setNodeStatesConfigFile(file)
        nodeStateTest.getNodeStates()
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output)
        where:
        file         |   output
        null         |   "Invalid or null node states config file."
        ""           |   "Invalid or null node states config file."
        "/test.json" |   "Given node states config file doesn't exists : /test.json"
    }

    def "fetch node state details for a given location throwing exceptions"() {
        NodeState nodeStateTest = new NodeState()
        when:
        nodeStateTest.setNodeStatesConfigFile(file)
        nodeStateTest.getNodeStateForLocation("xname-0")
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output)
        where:
        file         |   output
        null         |   "Invalid or null node states config file."
        ""           |   "Invalid or null node states config file."
        "/test.json" |   "Given node states config file doesn't exists : /test.json"
    }

    private static void loadDataIntoFile(File file, String data) throws Exception {
        FileUtils.writeStringToFile(file, data);
    }


    private static PropertyMap data = new PropertyMap()
    private static PropertyMap EMPTY_MAP = new PropertyMap()
    private static PropertyArray EMPTY_ARRAY = new PropertyArray()
}

