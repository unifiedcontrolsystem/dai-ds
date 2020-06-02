package com.intel.dai.dsimpl.voltdb

import com.intel.dai.exceptions.DataStoreException
import com.intel.logging.Logger
import com.intel.logging.LoggerFactory
import org.voltdb.client.Client
import spock.lang.Specification
import java.nio.file.Paths

class VoltHWInvApiSpec extends Specification {
    VoltHWInvApi api
    Logger logger = LoggerFactory.getInstance("Test", "VoltHWInvApiSpec", "console");

    def setup() {
        VoltDbClient.voltClient = Mock(Client)
        String[] servers = ["localhost"]
        api = new VoltHWInvApi(logger, new HWInvUtilImpl(), servers)
    }

    def "initialize"() {
        when: api.initialize()
        then: notThrown Exception
    }
    def "ingest from String failed"() {
        util.toCanonicalPOJO(_) >> null
        expect: api.ingest(null as String) == 1
    }
    def "ingest from empty HWInvHistory"() {
        HWInvHistory hist = new HWInvHistory();
        expect: api.ingest(hist) == 1
    }
    def "ingestHistory from String failed"() {
        util.toCanonicalHistoryPOJO(_) >> null
        expect: api.ingestHistory(null as String) == 1
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
    def "delete - null client"() {
        when: api.delete "x0"
        then: thrown DataStoreException
    }
    def "numberOfLocationsInHWInv"() {
        when: api.numberOfLocationsInHWInv()
        then: thrown DataStoreException
    }
    def "insertHistoricalRecord"() {
        api.client = Mock(Client)
        when: api.insertHistoricalRecord(null, null, null)
        then: notThrown DataStoreException
    }
}
