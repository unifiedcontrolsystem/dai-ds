package com.intel.dai.dsimpl.voltdb

import com.intel.dai.IAdapter
import com.intel.dai.exceptions.DataStoreException
import com.intel.logging.Logger
import spock.lang.Specification
import org.voltdb.*
import org.voltdb.client.ClientResponse
import org.voltdb.types.*
import org.voltdb.client.Client
import com.intel.config_io.ConfigIO
import com.intel.dai.AdapterInformation

class BasicNodeInfoSpec extends Specification {
    def "getHostname" () {
        def ts = new BasicNodeInfo("hostname", 40, false, "owner")
        expect: ts.getHostname() == "hostname"
    }
    def "getOwningSubSystem" () {
        def ts = new BasicNodeInfo("hostname", 40, false, "owner")
        expect: ts.getOwningSubSystem() == "owner"
    }
    def "getSequenceNumber" () {
        def ts = new BasicNodeInfo("hostname", 40, false, "owner")
        expect: ts.getSequenceNumber() == 40
    }
    def "isServiceNode" () {
        def ts = new BasicNodeInfo("hostname", 40, false, "owner")
        expect: ts.isServiceNode() == false
    }
}

class VoltDbBootImageSpec extends Specification {
    def "getClient"() {
        String[] servers = { "server0" }
        def ts = new VoltDbBootImage(servers, "type", Mock(Logger))
        def cl = Mock(Client)
        VoltDbClient.voltClient = cl

        expect: ts.getClient() == cl
    }

    def "updateComputeNodeBootImageId"() {
        String[] servers = { "server0" }
        def ts = new VoltDbBootImage(servers, "type", Mock(Logger))

        when:
        ts.updateComputeNodeBootImageId("lctn", "id", "adapterType")
        then:
        thrown DataStoreException
    }
}

class VoltDbJobsSpec extends Specification {
    def "getClient"() {
        String[] servers = {"localhost"}
        def logger = Mock(Logger)
        def ts = new VoltDbJobs(logger, servers)
        def cl = Mock(Client)
        VoltDbClient.voltClient = cl

        expect: ts.getClient() == cl
    }
}

class VoltDbLocationSpec extends Specification {
    def "getSystemLocations"() {
        String[] servers = {"localhost"};
        def logger = Mock(Logger)
        def ts = new VoltDbLocation(logger, servers)

        expect: ts.getSystemLocations().size() == 2
    }
    def "getSystemLabel"() {
        String[] servers = {"localhost"};
        def logger = Mock(Logger)
        def ts = new VoltDbLocation(logger, servers)
        ts.system_ = "system_"

        expect: ts.getSystemLabel() == "system_"
    }
    def "getClient"() {
        String[] servers = { "localhost" }
        def logger = Mock(Logger)
        def ts = new VoltDbLocation(logger, servers)
        def cl = Mock(Client)
        VoltDbClient.voltClient = cl

        expect: ts.getClient() == cl
    }
}

class VoltDbManagerSpec extends Specification {
    def "getClient"() {
        String[] servers = { "server0" }
        def ts = new VoltDbManager(servers, Mock(Logger))
        def cl = Mock(Client)
        VoltDbClient.voltClient = cl

        expect: ts.getClient() == cl
    }
}

class VoltDbLegacyDirectAccessSpec extends Specification {
    def "getVoltDbClient"() {
        String[] servers = { "server0" }
        def ts = new VoltDbLegacyDirectAccess(servers)
        def cl = Mock(Client)
        VoltDbClient.voltClient = cl

        expect: ts.getVoltDbClient() == cl
    }
}

class VoltDbWLMInformationSpec extends Specification {
    def "getVoltDbClient"() {
        String[] servers = { "server0" }
        def logger = Mock(Logger)
        def ts = new VoltDbWLMInformation(logger, servers)
        def cl = Mock(Client)
        VoltDbClient.voltClient = cl

        expect: ts.getClient() == cl
    }
}

class VoltDbReservationsSpec extends Specification {
    def "getClient"() {
        String[] servers = { "server0" }
        def logger = Mock(Logger)
        def ts = new VoltDbReservations(logger, servers)
        def cl = Mock(Client)
        VoltDbClient.voltClient = cl

        expect: ts.getClient() == cl
    }
}

class VoltDbServiceInformationSpec extends Specification {
    def "getClient"() {
        String[] servers = { "server0" }
        def logger = Mock(Logger)
        def ts = new VoltDbServiceInformation(logger, servers)
        def cl = Mock(Client)
        VoltDbClient.voltClient = cl

        expect: ts.getClient() == cl
    }
}

class VoltDbNodeInformationSpec extends Specification {
    def "getClient"() {
        String[] servers = { "server0" }
        def logger = Mock(Logger)
        def ts = new VoltDbNodeInformation(logger, servers)
        def cl = Mock(Client)
        VoltDbClient.voltClient = cl

        expect: ts.getClient() == cl
    }
}

class VoltInventoryApiSpec extends Specification {
    def "getClient"() {
        String[] servers = { "server0" }
        def logger = Mock(Logger)
        def adapter = Mock(AdapterInformation)
        def ts = new VoltInventoryApi(logger, adapter, servers)
        def cl = Mock(Client)
        VoltDbClient.voltClient = cl

        expect: ts.getClient() == cl
    }
    def "getNodeInformation"() {
        String[] servers = {"localhost"};
        def logger = Mock(Logger)
        def adapter = Mock(AdapterInformation)
        def ts = new VoltInventoryApi(logger, adapter, servers)

        expect: ts.getNodeInformation() != null
    }
}

class VoltDbInventoryTrackingApiSpec extends Specification {
    def "VoltDbInventoryTrackingApi"() {
        def config = Mock(ConfigIO)
        def logger = Mock(Logger)
        String[] servers = new String[1]
        servers[0] = "127.0.0.1"
        VoltDbClient.initializeVoltDbClient(servers)
        def ts = new VoltDbInventoryTrackingApi(config, logger)
        expect: ts != null
    }
}

class VoltDbEventsLogSpec extends Specification {
    def "initializeVoltClient"() {
        String[] servers = {"localhost"};
        def ts = new VoltDbEventsLog(servers, "adapterName", "adapterType", Mock(Logger))
        def cl = Mock(Client)
        VoltDbClient.voltClient = cl
        when: ts.initializeVoltClient(servers)
        then: notThrown Exception
    }
    def "listAllRasEventTypes"() {
        String[] servers = {"localhost"};
        VoltTable[] voltdbRes = new VoltTable[1]
        voltdbRes[0] = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING))

        def ts = new VoltDbEventsLog(servers, "adapterName", "adapterType", Mock(Logger))

        def cr = Mock(ClientResponse)
        cr.getResults() >> voltdbRes

        def cl = Mock(Client)
        cl.callProcedure(_) >> cr
        ts.voltClient = cl

        expect: ts.listAllRasEventTypes() != null
    }
    def "loadRasEventLog"() {
        String[] servers = {"localhost"};
        def ts = new VoltDbEventsLog(servers, "adapterName", "adapterType", Mock(Logger))

        when: ts.loadRasEventLog(servers, "adapterName", "adapterType")
        then: ts._raseventlog != null
    }
}
// VoltDbWorkQueue is difficult
