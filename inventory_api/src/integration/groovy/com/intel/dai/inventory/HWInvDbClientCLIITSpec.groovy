// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory

import com.intel.dai.inventory.api.HWInvDbClientCLI
import org.apache.commons.io.FileUtils
import spock.lang.Specification

class HWInvDbClientCLIITSpec extends Specification {
    HWInvDbClientCLI cli

    static def dataDir = "src/test/resources/data/"
    def removeTranslatedFiles() { "rm `find . -name '*.tr'`".execute().text }

    def setupSpec() {
         removeTranslatedFiles()
    }
    def cleanupSpec() {
        removeTranslatedFiles()
    }

    def setup() {
        "src/integration/resources/scripts/del-hw-inv.sh 21212".execute().text
        cli = new HWInvDbClientCLI()
    }

    def "Test run args - c2d" () {
        String[] myArgs = ["-c", fileName]
        expect: cli.run(myArgs) == 0
        where:
        fileName                                                          | dummy
        dataDir+"foreignHwByLocList/translated/preview4HWInventory.json"  | null
    }
    def "Test run args - v2d -- mapping error" () {
        String[] myArgs = ["-v", fileName]

        expect:
        cli.run(myArgs) == res

        where:
        fileName                                                    | res
        dataDir+"foreignHwByLoc/nestedNode.json"                    | 1
        dataDir+"foreignHwInventory/nestedNodeOnlyHWInventory.json" | 1
        dataDir+"foreignHwByLocList/preview4HWInventory.json"       | 0
    }
}
