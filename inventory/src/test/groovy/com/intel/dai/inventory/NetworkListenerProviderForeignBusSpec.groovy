package com.intel.dai.inventory

import com.intel.dai.network_listener.CommonDataFormat
import com.intel.dai.network_listener.DataType
import com.intel.dai.network_listener.NetworkListenerConfig
import com.intel.dai.network_listener.SystemActions
import com.intel.logging.Logger
import spock.lang.Specification

class NetworkListenerProviderForeignBusSpec extends Specification {
    def underTest_
    void setup() {
        underTest_ = new NetworkListenerProviderForeignBus(Mock(Logger))
    }

    def "Test empty processRawStringData"() {
        expect: underTest_.processRawStringData("data", Mock(NetworkListenerConfig)).size() == 0
    }

    def "Test empty actOnData"() {
        def data = new CommonDataFormat(0L,"location", DataType.InventoryChangeEvent)
        underTest_.actOnData(data, Mock(NetworkListenerConfig), Mock(SystemActions))
        expect: true
    }
}
