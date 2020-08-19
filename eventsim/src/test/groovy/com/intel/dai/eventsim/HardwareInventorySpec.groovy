package com.intel.dai.eventsim


import com.intel.properties.PropertyArray
import com.intel.properties.PropertyMap
import spock.lang.Specification

class HardwareInventorySpec extends Specification {
    def "Read or process hardware inventory location and query configuration file"() {
        HardwareInventory hardwareInventoryTest = new HardwareInventory()
        when:
        hardwareInventoryTest.processData(null)
        hardwareInventoryTest.processDataAsArray(null)
        hardwareInventoryTest.processDataAsArray(new PropertyMap())
        hardwareInventoryTest.processDataAsArray(new PropertyArray())
        then:
        def e = thrown(SimulatorException)
        e.message == "Error while loading hardware inventory data for a location"
    }

    def "Set hardware inventory query path as empty or null"() {
        HardwareInventory hardwareInventoryTest = new HardwareInventory()
        when:
        hardwareInventoryTest.setInventoryHardwareQueryPath("")
        hardwareInventoryTest.setInventoryHardwareQueryPath(null)
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Invalid or null hardware inventory query path"
    }

    def "Set hardware inventory for a location path as empty or null"() {
        HardwareInventory hardwareInventoryTest = new HardwareInventory()
        when:
        hardwareInventoryTest.setInventoryHardwareConfigPath("")
        hardwareInventoryTest.setInventoryHardwareConfigPath(null)
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Invalid or null hardware inventory config path"
    }

    def "Set hardware inventory file as empty or null"() {
        HardwareInventory hardwareInventoryTest = new HardwareInventory()
        when:
        hardwareInventoryTest.setInventoryHardwareConfigLocation("")
        hardwareInventoryTest.setInventoryHardwareConfigLocation(null)
        then:
        def e = thrown(SimulatorException)
        e.getMessage() == "Invalid or null hardware inventory config file"
    }
}