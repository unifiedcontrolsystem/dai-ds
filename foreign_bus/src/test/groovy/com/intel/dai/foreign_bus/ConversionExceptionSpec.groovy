// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.foreign_bus

import com.intel.dai.foreign_bus.ConversionException
import spock.lang.Specification

class ConversionExceptionSpec extends Specification {
    def "Test ctor1"() {
        new ConversionException("message only")
        expect: true
    }
    def "Test ctor2"() {
        new ConversionException("message", new RuntimeException("Cause"))
        expect: true
    }
}
