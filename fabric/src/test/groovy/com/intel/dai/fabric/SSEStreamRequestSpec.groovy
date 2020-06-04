package com.intel.dai.fabric

import spock.lang.Specification

class SSEStreamRequestSpec extends Specification {
    def underTest_
    void setup() {
        underTest_ = new SSEStreamRequest();
    }

    def "BuildRequest"() {
        String golden = "?stream_id=dai_ds&batchsize=1024"
        expect: underTest_.buildRequest(null, null) == golden
    }
}
