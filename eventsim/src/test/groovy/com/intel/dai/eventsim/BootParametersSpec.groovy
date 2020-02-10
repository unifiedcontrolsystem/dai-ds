package com.intel.dai.eventsim


import com.intel.properties.PropertyArray
import com.intel.properties.PropertyMap
import spock.lang.Specification

class BootParametersSpec extends Specification {
    def "Read boot params config file, Process null data"() {
        BootParameters bootParametersTest = new BootParameters()
        when:
            bootParametersTest.processData(null)
        then:
            def e = thrown(SimulatorException)
            e.message == "No boot-images data."
    }

    def "Read boot params config file, Process non PropertyMap data format"() {
        BootParameters bootParametersTest = new BootParameters()
        when:
            bootParametersTest.processData(new PropertyArray())
        then:
            def e = thrown(SimulatorException)
            e.message == "No boot-images data."
    }

    def "Read boot params config file, Process empty PropertyMap data format"() {
        BootParameters bootParametersTest = new BootParameters()
        when:
            bootParametersTest.processData(new PropertyMap())
        then:
            def e = thrown(SimulatorException)
            e.message == "No boot-images data."
    }

    def "Read boot params config file, Process PropertyMap with missing 'boot-images' key"() {
        BootParameters bootParametersTest = new BootParameters()

        //Generate non empty data of type PropertyMap with missing boot-image key
        PropertyMap data = new PropertyMap();
        data.put("key", "value")
        when:
            bootParametersTest.processData(data)
        then:
            def e = thrown(SimulatorException)
            e.message == "No boot-images data."
    }
}
