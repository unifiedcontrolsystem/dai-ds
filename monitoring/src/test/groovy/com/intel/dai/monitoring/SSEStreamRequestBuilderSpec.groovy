package com.intel.dai.monitoring

import spock.lang.Specification

class SSEStreamRequestBuilderSpec extends Specification {
    def underTest_
    void setup() {
        underTest_ = new SSEStreamRequestBuilder()
    }

    def "BuildRequest"() {
        def values = new HashMap<String,String>()
        values.put("requestBuilderSelectors.stream_id", "1")
        values.put("requestBuilderSelectors.count", "2")
        values.put("requestBuilderSelectors.batchsize", "3")
        values.put("requestBuilderSelectors.cname", "4")
        values.put("thrownOut", "5")
        def golden = "?stream_id=1&cname=4&count=2&batchsize=3"
        def result = underTest_.buildRequest(null, values)
        expect: result == golden
    }
}
