package com.intel.dai.dsimpl.voltdb

import com.intel.dai.exceptions.DataStoreException
import com.intel.logging.Logger
import com.intel.logging.LoggerFactory
import org.voltdb.client.Client

class VoltHWInvApiSpec extends spock.lang.Specification {
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
    // Ingesting nonexistent file now results in a no-op
    def "ingest -- nonexistent file"() {
        when: api.ingest "noSuchFile"
        then: notThrown IOException
    }
    def "ingest -- null client"() {
        when: api.ingest "src/test/resources/data/empty.json"
        then: thrown DataStoreException
    }
    def "delete - null client"() {
        when: api.delete "x0"
        then: thrown DataStoreException
    }
}
