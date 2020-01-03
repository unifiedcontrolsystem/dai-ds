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
        String[] myArgs = ["-m", "c2d", "-i", fileName]
        expect: cli.run(myArgs) == 0
        where:
        fileName                                                          | dummy
        dataDir+"foreignHwByLocList/translated/preview4HWInventory.json"  | null
    }
    def "Test run args - v2d" () {
        String[] myArgs = ["-m", "v2d", "-i", fileName]

        expect:
        cli.run(myArgs) == 0
        FileUtils.contentEquals(new File(fileName+".tr"), new File(expectedTranslatedFileName))

        where:
        fileName                                                    | expectedTranslatedFileName
        dataDir+"foreignHwByLoc/nestedNode.json"                    | dataDir+"foreignHwByLoc/translated/nestedNode.json"
        dataDir+"foreignHwInventory/nestedNodeOnlyHWInventory.json" | dataDir+"foreignHwInventory/translated/nestedNodeOnlyHWInventory.json"
        dataDir+"foreignHwByLocList/preview4HWInventory.json"       | dataDir+"foreignHwByLocList/translated/preview4HWInventory.json"
    }
}
