package com.intel.dai.eventsim

import spock.lang.Specification

class LoadFileLocationSpec extends Specification {

    def "No resources found to read data" () {
        LoadFileLocation fileLocationSpec = new LoadFileLocation()
        LoadFileLocation.class.getResourceAsStream("test_location") >> null
        when:
        fileLocationSpec.fromResources("test_location")
        then:
        def e = thrown(FileNotFoundException)
        e.getMessage() == "Resource not found: test_location"
    }

    def "null/empty data to write into file" () {
        when:
        LoadFileLocation.writeFile(null, "/tmp/output.txt")
        then:
        def e = thrown(IOException)
        e.getMessage() == "data or file path is null or empty."
    }

}
