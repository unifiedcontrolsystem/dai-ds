package com.intel.dai.dsimpl.voltdb
import org.voltdb.client.Client

class VoltDbClientSpec extends spock.lang.Specification {
    def "Test failedConnection"() {
        def dummyVoltDbClient = Mock(Client)
        VoltDbClient.voltClient = dummyVoltDbClient

        when: VoltDbClient.failedConnection()
        then: notThrown Exception
    }
}
