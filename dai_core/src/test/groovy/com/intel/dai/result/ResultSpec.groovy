package com.intel.dai.result

import spock.lang.Specification

class ResultSpec extends Specification {
    def "Result"() {
        expect: new Result() != null
    }
}
