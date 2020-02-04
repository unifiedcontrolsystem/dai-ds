package com.intel.networking.restclient

import spock.lang.Specification

class PermissiveX509TrustManagerSpec extends Specification {
    def underTest_
    void setup() {
        underTest_ = new PermissiveX509TrustManager()
    }

    def "Test CheckClientTrusted"() {
        underTest_.checkClientTrusted(null, null)
        expect: true
    }

    def "Test CheckServerTrusted"() {
        underTest_.checkServerTrusted(null, null)
        expect: true
    }

    def "Test GetAcceptedIssuers"() {
        expect: underTest_.getAcceptedIssuers().length == 0
    }
}
