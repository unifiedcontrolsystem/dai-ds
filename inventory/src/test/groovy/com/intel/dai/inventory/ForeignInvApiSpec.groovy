package com.intel.dai.inventory

import com.intel.logging.Logger
import com.intel.networking.restclient.RESTClientException
import spock.lang.Specification

class ForeignInvApiSpec extends Specification {
    ForeignInvApi ts;
    void setup() {
        ts = new ForeignInvApi(Mock(Logger))
    }

    def "toCanonicalHWInvJson"() {
        expect: ts.toCanonicalHWInvJson(null) == null
    }

    def "getForeignHWInvJson"() {
        expect: ts.getForeignHWInvJson(null) == null
    }

    def "getCanonicalHWInvJson"() {
        expect: ts.getCanonicalHWInvJson(null) == null
    }
}
