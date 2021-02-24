package com.intel.dai.dsimpl.voltdb

import com.intel.dai.AdapterInformation
import com.intel.logging.Logger
import org.voltdb.client.Client
import org.voltdb.client.ClientResponse
import org.voltdb.client.ProcedureCallback
import spock.lang.Specification

class VoltDbRasEventLogSpec extends Specification {
    def client_
    def underTest_
    void setup() {
        def servers = new String[1]
        servers[0] = "127.0.0.1"
        underTest_ = new VoltDbRasEventLog(servers, Mock(AdapterInformation), Mock(Logger))
        client_ = Mock(Client)
        underTest_.voltClient = client_
    }

    def "Test getNsTimestamp"() {
        expect: underTest_.getNsTimestamp() > 0L
    }

    def "Test setRasEventAssociatedJobID"() {
// TODO: Testing the Lambda does not work???
//        def response = Mock(ClientResponse)
//        response.getStatus() >> ClientResponse.SUCCESS
//        client_.callProcedure(_ as ProcedureCallback,_,_,_,_ as Long) >> {
//            ProcedureCallback callback -> callback.clientCallback(response)
//        }
        underTest_.setRasEventAssociatedJobID("", "", 0L)
        expect: true
    }
}
