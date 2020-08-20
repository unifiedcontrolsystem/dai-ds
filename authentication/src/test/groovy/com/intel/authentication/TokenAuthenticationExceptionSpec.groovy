// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.authentication

import spock.lang.Specification

class TokenAuthenticationExceptionSpec extends Specification {
    def "Test exception construction"() {
        def exception = new TokenAuthenticationException("Test message")
        expect: exception.getMessage() == "Test message"
    }
}
