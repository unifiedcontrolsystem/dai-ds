package com.intel.dai.eventsim

import com.intel.logging.Logger
import com.intel.networking.restserver.RESTServer
import com.intel.networking.restserver.Request
import com.intel.networking.restserver.Response
import spock.lang.Specification

class NetworkSourceSpec extends Specification {
    def server_ = Mock(RESTServer)

    def underTest_
    void setup() {
        NetworkSource.server_ = server_
        underTest_ = new NetworkSource("/tmp/file.json", Mock(Logger))
    }

    def "Test convertHttpRequestToMap"() {
        def request = Mock(Request)
        expect: underTest_.convertHttpRequestToMap(request) != null
    }

    def "Test getAddress"() {
        expect: underTest_.getAddress() == null
    }

    def "Test getPort"() {
        expect: underTest_.getPort() == 0
    }

    def "Test sendMessage"() {
        underTest_.sendMessage(null, null)
        expect: true
    }

    def "Test convertHttpBodytoMap"() {
        underTest_.convertHttpBodytoMap("")
        expect: true
    }
}