package com.intel.dai.dsimpl.voltdb

import com.intel.dai.dsapi.HWInvHistory
import com.intel.dai.exceptions.DataStoreException
import com.intel.logging.Logger
import com.intel.logging.LoggerFactory
import org.voltdb.ClientResponseImpl
import org.voltdb.VoltTable
import org.voltdb.client.Client
import org.voltdb.client.ClientResponse
import spock.lang.Specification
import java.nio.file.Paths

class VoltHWInvDbApiSpec extends Specification {
    VoltHWInvDbApi api
    Logger logger = Mock(Logger)

    def setup() {
        VoltDbClient.voltClient = Mock(Client)
        String[] servers = ["localhost"]
        api = new VoltHWInvDbApi(logger, new HWInvUtilImpl(Mock(Logger)), servers)
    }

    def "initialize"() {
        when: api.initialize()
        then: notThrown Exception
    }
    def "ingest from String failed"() {
        String[] servers = ["localhost"]
        def util = Mock(HWInvUtilImpl)
        api = new VoltHWInvDbApi(logger, util, servers)
        util.toCanonicalPOJO(_) >> null
        expect: api.ingest(null as String) == 0
    }
    def "ingest from empty HWInvHistory"() {
        HWInvHistory hist = new HWInvHistory();
        expect: api.ingest(hist) == []
    }
    def "ingestHistory from String failed"() {
        String[] servers = ["localhost"]
        def util = Mock(HWInvUtilImpl)
        api = new VoltHWInvDbApi(logger, util, servers)
        util.toCanonicalHistoryPOJO(_) >> null
        expect: api.ingestHistory(null as String) == []
    }
    // Ingesting nonexistent file now results in a no-op
    def "ingest -- nonexistent file"() {
        when: api.ingest Paths.get("noSuchFile")
        then: notThrown IOException
    }
    def "ingest -- empty json"() {
        when: api.ingest Paths.get("src/test/resources/data/empty.json")
        then: thrown DataStoreException
    }
    def "ingest -- string"() {
        when: api.ingest ""
        then: notThrown DataStoreException
    }
    def "delete - null client"() {
        when: api.delete "x0"
        then: thrown DataStoreException
    }

    def "allLocationsAt"() {
        when: api.allLocationsAt(null, null)
        then: thrown DataStoreException
    }

    def "numberOfLocationsInHWInv"() {
        when: api.numberOfRawInventoryRows()
        then: thrown DataStoreException
    }

    def "insertHistoricalRecord"() {
        api.client = Mock(Client)
        when: api.insertRawHistoricalRecord null
        then: thrown DataStoreException
    }

    def "lastHwInvHistoryUpdate"() {
        api.client = Mock(Client)
        when: api.lastHwInvHistoryUpdate()
        then: thrown DataStoreException
    }

    def "deleteAllCookedNodes"() {
        api.client = Mock(Client)
        when: api.deleteAllCookedNodes()
        then: thrown DataStoreException
    }

    def "deleteAllRawHistoricalRecords"() {
        api.client = Mock(Client)
        when: api.deleteAllRawHistoricalRecords()
        then: thrown DataStoreException
    }

    def "dumpCookedNodes"() {
        api.client = Mock(Client)
        when: api.dumpCookedNodes()
        then: thrown DataStoreException
    }

    def "ingestCookedNode"() {
        setup:
        def cr = Mock(ClientResponseImpl)
        cr.getStatus() >> voltdbStatus
        cr.getResults() >> new VoltTable[1]
        cr.getResults()[0] = new VoltTable()
        api.client = Mock(Client)
        api.client.callProcedure(*_) >> cr

        expect: api.ingestCookedNode("nodeLocation", "2020-06-03T22:35:45Z") == res

        where:
        voltdbStatus                        || res
        ClientResponse.SUCCESS              || 1
        ClientResponse.UNEXPECTED_FAILURE   || 0
    }

    def "numberCookedNodeInventoryHistoryRows"() {
        api.client = Mock(Client)
        when: api.numberCookedNodeInventoryHistoryRows()
        then: thrown DataStoreException
    }

    def "numberOfCookedNodes"() {
        api.client = Mock(Client)
        when: api.numberOfCookedNodes()
        then: thrown DataStoreException
    }

    def "numberRawInventoryHistoryRows"() {
        api.client = Mock(Client)
        when: api.numberRawInventoryHistoryRows()
        then: thrown DataStoreException
    }

    def "ingestCookedNodesChanged"() {
        api.client = Mock(Client)
        when: api.ingestCookedNodesChanged([:])
        then: notThrown DataStoreException
    }

    def "insertNodeHistory"() {
        given:
        def cr = Mock(ClientResponseImpl)
        cr.getStatus() >> voltdbStatus
        api.client = Mock(Client)
        api.client.callProcedure(*_) >> cr

        when: api.insertNodeHistory(
                "nodeLocation", 42,
                "hwInfoJson", "nodeSerialNumber")
        then: notThrown DataStoreException

        where:
        voltdbStatus                        || res
        ClientResponse.SUCCESS              || true
        ClientResponse.UNEXPECTED_FAILURE   || true
    }
}
