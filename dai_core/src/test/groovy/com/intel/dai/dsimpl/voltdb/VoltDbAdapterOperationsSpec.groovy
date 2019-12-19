package com.intel.dai.dsimpl.voltdb

import com.intel.dai.AdapterInformation
import com.intel.dai.dsapi.RasEventLog
import com.intel.dai.dsapi.WorkQueue
import com.intel.logging.Logger
import org.voltdb.client.Client
import org.voltdb.client.ClientResponse
import spock.lang.Specification

class VoltDbAdapterOperationsSpec extends Specification {
    ClientResponse response_
    def underTest_
    void setup() {
        def adapter = new AdapterInformation("TYPE", "NAME", "location","hostname")
        def servers = new String[1]
        response_ = Mock(ClientResponse)
        servers[0] = "127.0.0.1"
        underTest_ = new VoltDbAdapterOperations(Mock(Logger), servers, adapter)
        underTest_.client_ = Mock(Client)
        underTest_.createRas()
        underTest_.ras_ = Mock(RasEventLog)
        underTest_.workQueue_ = Mock(WorkQueue)
        underTest_.nodes_ = new HashMap()
        underTest_.nodes_.put("location", new BasicNodeInfo("hostname", 0L, false, "manual"))
    }

    def "Test MarkNodeInErrorState"() {
        underTest_.nodes_.put("location", new BasicNodeInfo("hostname", 0L, COMPUTE, "manual"))
        underTest_.markNodeInErrorState("location", INFORM)
        expect: RESULT

        where:
        INFORM | COMPUTE || RESULT
        true   | true    || true
        false  | false   || true
    }

    def "Test FailedToLogNodeStateChange"() {
        def response = Mock(ClientResponse)
        response.getStatusString() >> "status"
        underTest_.failedToLogNodeStateChange("location", "procedureName", null)
        underTest_.failedToLogNodeStateChange("location", "procedureName", response)
        expect: true
    }

    def "Test sub-class CallbackForErrorNodeStateChange"() {
        def ops = Mock(VoltDbAdapterOperations)
        def logger = Mock(Logger)
        def ut = new VoltDbAdapterOperations.CallbackForErrorNodeStateChange(ops, "location", "name", logger)
        def response = Mock(ClientResponse)
        response.getStatus() >> STATUS
        ut.clientCallback(response)
        expect: RESULT

        where:
        STATUS                             || RESULT
        ClientResponse.SUCCESS             || true
        ClientResponse.OPERATIONAL_FAILURE || true
    }
}
