// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api

import org.apache.commons.cli.CommandLine
import java.nio.file.Paths

class CommonCliUtilSpec extends spock.lang.Specification {
    CommandLine cmd = Mock()
    def cliUtil = new CommonCliUtil()

    def "Test getOptionValue Integer"() {
        cmd.getOptionValue(_) >> 52
        expect: cliUtil.getOptionValue(cmd,"c", 42) == 52
    }
    def "Test getOptionValue Integer - null value"() {
        cmd.getOptionValue(_) >> null
        expect: cliUtil.getOptionValue(cmd,"c", 42) == 42
    }
    def "Test getOptionValue Path"() {
        cmd.getOptionValue(_) >> "a.txt"
        expect: cliUtil.getOptionValue(cmd,"i", null) == Paths.get("a.txt")
    }
    def "Test getOptionValue Path - null value"() {
        cmd.getOptionValue(_) >> null
        expect: cliUtil.getOptionValue(cmd,"i", Paths.get("cow.txt")) == Paths.get("cow.txt")
    }
}