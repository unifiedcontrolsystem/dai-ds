package com.intel.dai.inventory

import com.intel.dai.inventory.utilities.*
import com.intel.dai.dsimpl.DataStoreFactoryImpl
import com.intel.logging.Logger
import spock.lang.Specification

// Use the following in place of Mock(Logger) get traces on stdout
// def logger = LoggerFactory.getInstance(AdapterInventoryNetworkBase.ADAPTER_TYPE, "inventory", "console")

class InventoryUpdateThreadITSpec extends Specification {
    def setup() {
        println Helper.testStartMessage(specificationContext)
    }

    def cleanup() {
        println Helper.testEndMessage(specificationContext)
    }

    def "run - near line server unavailable"() {
        def ts = new InventoryUpdateThread(Mock(Logger))
        when:
        ts.run()
        then:
        notThrown Exception
    }
}

class DatabaseSynchronizerITSpec extends Specification {
    def setup() {
        println Helper.testStartMessage(specificationContext)
    }

    def cleanup() {
        println Helper.testEndMessage(specificationContext)
    }

//    def "updateDaiInventoryTables - near line server unavailable"() {
//        def dsFactory = new DataStoreFactoryImpl(null as String, Mock(Logger))
//        def ts = Spy(DatabaseSynchronizer, constructorArgs: [Mock(Logger), dsFactory])
//        ts.getLastHWInventoryHistoryUpdate() >> null    //near line server unavailable
//        when:
//        ts.updateDaiInventoryTables()
//        then:
//        notThrown Exception
//    }
//
//    def "updateDaiInventoryTables - initial loading"() {
//        def dsFactory = new DataStoreFactoryImpl(null as String, Mock(Logger))
//        def ts = Spy(DatabaseSynchronizer, constructorArgs: [Mock(Logger), dsFactory])
//        ts.getLastHWInventoryHistoryUpdate() >> ''  // initial loading
//        when:
//        ts.updateDaiInventoryTables()
//        then:
//        notThrown Exception
//    }
//
//    def "updateDaiInventoryTables - patching"() {
//        def dsFactory = new DataStoreFactoryImpl(null as String, Mock(Logger))
//        def ts = Spy(DatabaseSynchronizer, constructorArgs: [Mock(Logger), dsFactory])
//        ts.getLastHWInventoryHistoryUpdate() >> '2020-07-27T21:10:49.745223Z'  // patching
//        when:
//        ts.updateDaiInventoryTables()
//        then:
//        notThrown Exception
//    }
//
//    def "updateDaiInventoryTables - history unavailable"() {
//        def dsFactory = new DataStoreFactoryImpl(null as String, Mock(Logger))
//        def ts = Spy(DatabaseSynchronizer, constructorArgs: [Mock(Logger), dsFactory])
//        ts.getLastHWInventoryHistoryUpdate() >> '2222-07-27T21:10:49.745223Z'  // future; history unavailable
//        when:
//        ts.updateDaiInventoryTables()
//        then:
//        notThrown Exception
//    }
//
//    def "updateDaiInventoryTables - not a timestamp"() {
//        def dsFactory = new DataStoreFactoryImpl(null as String, Mock(Logger))
//        def ts = Spy(DatabaseSynchronizer, constructorArgs: [Mock(Logger), dsFactory])
//        ts.getLastHWInventoryHistoryUpdate() >> 'notATimeStamp' // not a timestamp
//        when:
//        ts.updateDaiInventoryTables()
//        then:
//        notThrown Exception
//    }
}
