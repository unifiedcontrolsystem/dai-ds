package com.intel.perflogging

import org.voltdb.VoltTable
import org.voltdb.VoltType
import org.voltdb.client.Client
import org.voltdb.client.ClientResponse
import org.voltdb.client.ProcedureCallback
import spock.lang.Specification

class GenerateRasEventsSpec extends Specification {
    def args_
    Client client_;
    def underTest_

    ClientResponse buildNodesResponse() {
        ClientResponse response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.SUCCESS
        VoltTable table = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING)
        )
        table.addRow("X0-CH0-CN0")
        table.addRow("X0-CH0-CN1")
        table.addRow("X0-CH0-CN2")
        table.addRow("X0-CH0-CN3")
        table.addRow("X0-CH1-CN0")
        table.addRow("X0-CH1-CN1")
        table.addRow("X0-CH1-CN2")
        table.addRow("X0-CH1-CN3")
        table.addRow("X0-CH2-CN0")
        table.addRow("X0-CH2-CN1")
        table.addRow("X0-CH2-CN2")
        table.addRow("X0-CH2-CN3")
        table.addRow("X0-CH3-CN0")
        table.addRow("X0-CH3-CN1")
        table.addRow("X0-CH3-CN2")
        table.addRow("X0-CH3-CN3")
        response.getResults() >> [ table ]
        return response
    }

    ClientResponse buildWriteResponse() {
        ClientResponse response = Mock(ClientResponse)
        response.getStatus() >> ClientResponse.SUCCESS
        return response
    }

    void setup() {
        client_ = Mock(Client)
        GenerateRasEvents.client_ = client_
        args_ = new String[5]
        args_[0] = "localhost"
        args_[1] = "--count='1001'"
        args_[2] = "--seed=\"1001\""
        args_[3] = "--percent-jobs=50"
        args_[4] = "--percent-ctrl=50"
        underTest_ = new GenerateRasEvents(args_)

        client_.callProcedure("@AdHoc", "SELECT Lctn FROM ComputeNode ORDER BY Lctn;") >>
                buildNodesResponse()
        client_.callProcedure(_ as ProcedureCallback, "RASEVENT.Insert", _ as Long, "0000000000",
                _ as String, _, _ as String, 0, _ as String, "N", "InstanceData", _ as Long,
                _ as Long, "INITIALIZATION", -1) >> { underTest_.writeCallback(buildWriteResponse()) }
    }

    def "Test basic run path"() {
        underTest_.options_.put("percent-jobs", PERCENT)
        underTest_.options_.put("percent-ctrl", PERCENT)
        underTest_.run()
        expect: underTest_.options_.get("seed") == RESULT
        where:
        PERCENT | RESULT
        0L      | 1001L
        50L     | 1001L
        100L    | 1001L
    }

    def "Test writeCallback"() {
        int errors = underTest_.errors_
        ClientResponse response = Mock(ClientResponse)
        response.getStatus() >> RESULT
        underTest_.writeCallback(response)
        expect: underTest_.errors_ == (errors + DELTA)
        where:
        RESULT                             | DELTA
        ClientResponse.SUCCESS             | 0
        ClientResponse.OPERATIONAL_FAILURE | 1
    }

    def "Test bad option value"() {
        when: underTest_.setOptionValue("unknown", "0")
        then: thrown(RuntimeException)
    }
}
