package com.intel.dai.eventsim


import com.intel.properties.PropertyArray
import com.intel.properties.PropertyMap
import spock.lang.Specification

class BootParametersSpec extends Specification {

    def setup() {
        data.put("key", "value")
    }

    def "Read or process boot parameters configuration file"() {
        BootParameters bootParametersTest = new BootParameters()
        when:
        bootParametersTest.processData(input)
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output)
        where:
        input       |   output
        null        |   "No boot-images data."
        data        |   "No boot-images data."
        EMPTY_MAP   |   "No boot-images data."
        EMPTY_ARRAY |   "No boot-images data."
    }

    def "Set boot parameters configuration file as empty or null"() {
        BootParameters bootParametersTest = new BootParameters()
        when:
        bootParametersTest.setBootParamsConfigFile(file)
        then:
        def e = thrown(SimulatorException)
        e.message.equals(output)
        where:
        file      |   output
        null      |   "Invalid or null boot parameters config file."
        ""        |   "Invalid or null boot parameters config file."
    }

    private static PropertyMap data = new PropertyMap()
    private static PropertyMap EMPTY_MAP = new PropertyMap()
    private static PropertyArray EMPTY_ARRAY = new PropertyArray()
}
