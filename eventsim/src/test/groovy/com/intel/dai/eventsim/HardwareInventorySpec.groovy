package com.intel.dai.eventsim


import com.intel.properties.PropertyArray
import com.intel.properties.PropertyMap
import spock.lang.Specification

class HardwareInventorySpec extends Specification {
    def "Read hardware_inventory query/inventory_for_location config file, Process null data"() {
        HardwareInventory hardwareInventoryTest = new HardwareInventory()
        when:
        hardwareInventoryTest.processData(null)
        then:
        def e = thrown(SimulatorException)
        e.message == "Error while loading hardware inventory data for a location"
    }

    def "Read hardware_inventory config file, Process null data"() {
        HardwareInventory hardwareInventoryTest = new HardwareInventory()
        when:
        hardwareInventoryTest.processDataAsArray(null)
        then:
        def e = thrown(SimulatorException)
        e.message == "Error while loading hardware inventory data"
    }

    def "Read hardware_inventory config file, Process non PropertyArray data format"() {
        HardwareInventory hardwareInventoryTest = new HardwareInventory()
        when:
        hardwareInventoryTest.processDataAsArray(new PropertyMap())
        then:
        def e = thrown(SimulatorException)
        e.message == "Error while loading hardware inventory data"
    }

    def "Read hardware_inventory config file, Process empty PropertyArray data format"() {
        HardwareInventory hardwareInventoryTest = new HardwareInventory()
        when:
        hardwareInventoryTest.processDataAsArray(new PropertyArray())
        then:
        def e = thrown(SimulatorException)
        e.message == "Error while loading hardware inventory data"
    }
}