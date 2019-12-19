package com.intel.dai.dsimpl

import com.intel.dai.dsimpl.voltdb.VoltDbClient
import com.intel.dai.IAdapter
import com.intel.dai.AdapterInformation
import com.intel.dai.exceptions.DataStoreException
import com.intel.logging.Logger
import org.voltdb.VoltTable
import org.voltdb.VoltType
import org.voltdb.client.Client
import org.voltdb.client.ClientResponse
import spock.lang.Specification


class DataStoreFactoryImplSpec extends Specification {
    def "DataStoreFactoryImpl"() {
        def logger = Mock(Logger)
        def ts = new DataStoreFactoryImpl("sServers", logger)
        expect: ts != null
    }
    def "createGroups"() {
        def logger = Mock(Logger)
        def ts = new DataStoreFactoryImpl("sServers", logger)
        VoltDbClient.voltClient = Mock(Client)
        expect: ts.createGroups() != null
    }
    def "generateVoltDBJdbcUrl"() {
        expect: DataStoreFactoryImpl.generateVoltDBJdbcUrl("servers") == "jdbc:voltdb://servers"
    }
    def "createBootImageApi-IAdapter "() {
        def logger = Mock(Logger)
        def ts = new DataStoreFactoryImpl("sServers", logger)
        def adapter = Mock(IAdapter)
        VoltDbClient.voltClient = Mock(Client)
        expect: ts.createBootImageApi(adapter) != null
    }
    def "createBootImageApi-AdapterInformation"() {
        def logger = Mock(Logger)
        def ts = new DataStoreFactoryImpl("sServers", logger)
        def adapterInfo = Mock(AdapterInformation)
        VoltDbClient.voltClient = Mock(Client)
        expect: ts.createBootImageApi(adapterInfo) != null
    }
//    def "createEventsLog"() {
//        def logger = Mock(Logger)
//        def ts = new DataStoreFactoryImpl("sServers", logger)
//        def client = Mock(Client)
//        def clientResponse = Mock(ClientResponse)
//        clientResponse.getStatus() >> ClientResponse.UNEXPECTED_FAILURE
//        VoltTable[] voltdbRes = new VoltTable[1]
//        voltdbRes[0] = new VoltTable(
//                new VoltTable.ColumnInfo("Lctn", VoltType.STRING))
//        clientResponse.getResults() >> voltdbRes
//        client.callProcedure(*_) >> clientResponse
//        VoltDbClient.voltClient = client
//        when: ts.createEventsLog("adapterName", "adapterType")
//        then: thrown RuntimeException
//    }
    def "createInventoryApi"(){
        def logger = Mock(Logger)
        def adapterInfo = Mock(AdapterInformation)
        def ts = new DataStoreFactoryImpl("sServers", logger)

        def client = Mock(Client)
        VoltDbClient.voltClient = client

        expect: ts.createInventoryApi(adapterInfo) != null
    }
//    def "createRasEventLog"(){
//        def logger = Mock(Logger)
//        def adapterInfo = Mock(AdapterInformation)
//        def ts = new DataStoreFactoryImpl("sServers", logger)
//
//        def client = Mock(Client)
//        VoltDbClient.voltClient = client
//
//        def clientResponse = Mock(ClientResponse)
//        clientResponse.getStatus() >> ClientResponse.UNEXPECTED_FAILURE
//        VoltTable[] voltdbRes = new VoltTable[1]
//        voltdbRes[0] = new VoltTable(
//                new VoltTable.ColumnInfo("Lctn", VoltType.STRING))
//        clientResponse.getResults() >> voltdbRes
//        client.callProcedure(*_) >> clientResponse
//
//        when: ts.createRasEventLog(adapterInfo)
//        then: thrown RuntimeException
//    }
    def "createConfiguration"() {
        def logger = Mock(Logger)
        def adapterInfo = Mock(AdapterInformation)
        def ts = new DataStoreFactoryImpl("sServers", logger)

        def client = Mock(Client)
        VoltDbClient.voltClient = client

        expect: ts.createConfiguration() != null
    }
    def "createNodeInformation"() {
        def logger = Mock(Logger)
        def adapterInfo = Mock(AdapterInformation)
        def ts = new DataStoreFactoryImpl("sServers", logger)

        def client = Mock(Client)
        VoltDbClient.voltClient = client

        expect: ts.createNodeInformation() != null
    }
    def "createLocation"() {
        def logger = Mock(Logger)
        def adapterInfo = Mock(AdapterInformation)
        def ts = new DataStoreFactoryImpl("sServers", logger)

        def client = Mock(Client)
        VoltDbClient.voltClient = client

        def clientResponse = Mock(ClientResponse)
        clientResponse.getStatus() >> ClientResponse.UNEXPECTED_FAILURE
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING))
        clientResponse.getResults() >> voltdbRes
        client.callProcedure(*_) >> clientResponse

        expect: ts.createLocation() != null
    }
    def "createServiceInformation"() {
        def logger = Mock(Logger)
        def ts = new DataStoreFactoryImpl("sServers", logger)

        def client = Mock(Client)
        VoltDbClient.voltClient = client

        expect: ts.createServiceInformation() != null
    }
    def "createWLMInformation"() {
        def logger = Mock(Logger)
        def ts = new DataStoreFactoryImpl("sServers", logger)

        def client = Mock(Client)
        VoltDbClient.voltClient = client

        expect: ts.createWLMInformation() != null
    }
    def "createStoreTelemetry"() {
        def logger = Mock(Logger)
        def ts = new DataStoreFactoryImpl("sServers", logger)

        def client = Mock(Client)
        VoltDbClient.voltClient = client

        expect: ts.createStoreTelemetry() != null
    }
    def "createVoltDbLegacyAccess"() {
        def logger = Mock(Logger)
        def ts = new DataStoreFactoryImpl("sServers", logger)

        def client = Mock(Client)
        VoltDbClient.voltClient = client

        expect: ts.createVoltDbLegacyAccess() != null
    }
    def "createJobApi"() {
        def logger = Mock(Logger)
        def ts = new DataStoreFactoryImpl("sServers", logger)

        def client = Mock(Client)
        VoltDbClient.voltClient = client

        expect: ts.createJobApi() != null
    }
    def "createReservationApi"() {
        def logger = Mock(Logger)
        def ts = new DataStoreFactoryImpl("sServers", logger)

        def client = Mock(Client)
        VoltDbClient.voltClient = client

        expect: ts.createReservationApi() != null
    }
    def "createInventoryTrackingApi"() {
        def logger = Mock(Logger)
        def ts = new DataStoreFactoryImpl("sServers", logger)

        def client = Mock(Client)
        VoltDbClient.voltClient = client

        expect: ts.createInventoryTrackingApi() != null
    }
//    def "createTier2Connection"() { is difficult
//    def "createWorkQueue"() { is difficult
}
