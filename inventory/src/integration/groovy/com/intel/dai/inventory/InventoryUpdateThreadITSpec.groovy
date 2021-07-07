package com.intel.dai.inventory

import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.HWInvUtil
import com.intel.dai.dsimpl.jdbc.InventorySnapshotJdbc
import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl
import com.intel.dai.dsimpl.voltdb.VoltHWInvDbApi
import com.intel.dai.inventory.utilities.*
import com.intel.logging.Logger
import spock.lang.Specification

// Use the following in place of Mock Logger to get traces on stdout
// def logger = LoggerFactory.getInstance(AdapterInventoryNetworkBase.ADAPTER_TYPE, "inventory", "console")

class InventoryUpdateThreadITSpec extends Specification {
    Logger logger = Mock Logger
    HWInvUtil util = new HWInvUtilImpl(logger)
    DataStoreFactory dsClientFactory = Mock DataStoreFactory
    String[] voltDbServers = ['css-centos-8-00.ra.intel.com']

    def setup() {
        println Helper.testStartMessage(specificationContext)
        print "./src/integration/resources/scripts/drop_inventory_data.sh".execute().text
        dsClientFactory.createHWInvApi() >> new VoltHWInvDbApi(logger, util, voltDbServers)
//        dsClientFactory.createInventorySnapshotApi() >> new InventorySnapshotJdbc(logger)
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
    Logger logger = Mock Logger
    HWInvUtil util = new HWInvUtilImpl(logger)
    DataStoreFactory dsClientFactory = Mock DataStoreFactory
    String[] voltDbServers = ['css-centos-8-00.ra.intel.com']

    def setup() {
        println Helper.testStartMessage(specificationContext)
        print "./src/integration/resources/scripts/drop_inventory_data.sh".execute().text
        dsClientFactory.createHWInvApi() >> new VoltHWInvDbApi(logger, util, voltDbServers)
    }

    def cleanup() {
        println Helper.testEndMessage(specificationContext)
    }

    def "updateDaiInventoryTables - near line server unavailable"() {
        def ts = Spy(DatabaseSynchronizer, constructorArgs: [logger, dsClientFactory])
        ts.getLastHWInventoryHistoryUpdate() >> null    //near line server unavailable
        when:
        ts.updateDaiInventoryTables()
        then:
        notThrown Exception
    }

    /**
     * We need to use a Spy because getLastHWInventoryHistoryUpdate() does not work in the
     * absence of Postgres.
     */
    def "updateDaiInventoryTables - initial loading"() {
        def ts = Spy(DatabaseSynchronizer, constructorArgs: [logger, dsClientFactory])
        ts.getLastHWInventoryHistoryUpdate() >> ''  // initial loading
        when:
        ts.updateDaiInventoryTables()
        then:
        ts.totalNumberOfInjectedDocuments == 34 + 243 + 8
    }

    /**
     * We need to use a Spy because getLastHWInventoryHistoryUpdate() does not work in the
     * absence of Postgres.
     */
//    def "updateDaiInventoryTables - patching"() {
//        def ts = Spy(DatabaseSynchronizer, constructorArgs: [logger, dsClientFactory])
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
