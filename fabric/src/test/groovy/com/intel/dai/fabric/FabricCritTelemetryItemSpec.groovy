// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.fabric

import spock.lang.Specification

class FabricCritTelemetryItemSpec extends Specification {
    def underTest_
    void setup() {
        underTest_ = new FabricCritTelemetryItem(99L, "name", "location", "serial", "jobid", "data")
    }

    def "Test Accessors"() {
        expect: underTest_.getSerialNumber() == "serial"
        and:    underTest_.getJobId() == "jobid"
        and:    underTest_.getInstanceData() == "data"
    }

    def "Test toString"() {
        def json = """{"jobId":"jobid","serialNumber":"serial","data":"data","name":"name","location":"location","timestamp":99}"""
        expect: underTest_.toString() == json
    }

    def "Test ctor for JSON"() {
        def inst = new FabricCritTelemetryItem(underTest_.toString())
        expect: underTest_.toString() == inst.toString()
    }
}
