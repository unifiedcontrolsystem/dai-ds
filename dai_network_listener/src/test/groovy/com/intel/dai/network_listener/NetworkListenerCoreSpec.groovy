package com.intel.dai.network_listener

import com.intel.dai.dsapi.DataStoreFactory
import com.intel.logging.Logger
import com.intel.networking.sink.NetworkDataSink
import com.intel.perflogging.BenchmarkHelper
import spock.lang.Specification

class NetworkListenerCoreSpec extends Specification {
    def underTest_
    void setup() {
        NetworkListenerCore.STABILIZATION_VALUE = 10L
        underTest_ = new NetworkListenerCore(Mock(Logger), Mock(NetworkListenerConfig), Mock(DataStoreFactory), Mock(BenchmarkHelper))
    }

    void cleanup() {
        NetworkListenerCore.STABILIZATION_VALUE = 1500L
    }

    def "Test backgroundContinueConnections"() {
        NetworkDataSink mock1 = Mock(NetworkDataSink)
        NetworkDataSink mock2 = Mock(NetworkDataSink)
        mock1.isListening() >>> [false, false, true]
        mock2.isListening() >>> [true]
        List<NetworkDataSink> list = new ArrayList<>()
        list.add(mock1)
        list.add(mock2)
        underTest_.backgroundContinueConnections(list)
        expect: true
    }
}
