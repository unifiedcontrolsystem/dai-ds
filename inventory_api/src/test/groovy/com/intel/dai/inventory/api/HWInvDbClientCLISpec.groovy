// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api

import com.intel.dai.dsimpl.voltdb.VoltDbClient
import org.voltdb.client.Client
import spock.lang.Specification

class HWInvDbClientCLISpec extends Specification {
    HWInvDbClientCLI cli

    def setupSpec() {
        VoltDbClient.voltClient = Mock(Client)
    }

    def setup() {
        cli = new HWInvDbClientCLI()
    }

    def "Test run args" () {
        String[] myArgs = args
        expect: cli.run(myArgs) == expectedValue
        where:
        args                                        || expectedValue
        []                                          || 1
        ["-v", "nonexistentVendorFile.json"]        || 1
        ["-c", "nonexistentCanonicalFile.json"]     || 1
    }
}
