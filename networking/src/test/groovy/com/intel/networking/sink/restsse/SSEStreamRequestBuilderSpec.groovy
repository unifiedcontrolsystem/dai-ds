// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink.restsse

import spock.lang.Specification

class SSEStreamRequestBuilderSpec extends Specification {
    def map_
    def underTest_
    def setup() {
        underTest_ = new SSEStreamRequestBuilder()
        map_ = new HashMap<String,String>()
    }

    def "Test buildRequest 1"() {
        expect: underTest_.buildRequest(null, map_) == ""
    }

    def "Test buildRequest 2"() {
        map_.put("requestBuilderSelectors.stream_id", "testid")
        expect: underTest_.buildRequest(null, map_) == "?stream_id=testid"
    }

    def "Test buildRequest 3"() {
        map_.put("requestBuilderSelectors.stream_id", "testid")
        map_.put("requestBuilderSelectors.count", "5")
        map_.put("unknown", "blue")
        expect: underTest_.buildRequest(null, map_) == "?stream_id=testid&count=5"
    }
}
