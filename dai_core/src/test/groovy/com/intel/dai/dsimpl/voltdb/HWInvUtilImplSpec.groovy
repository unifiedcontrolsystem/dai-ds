package com.intel.dai.dsimpl.voltdb

import com.intel.dai.dsapi.*
import java.nio.file.*

class HWInvUtilImplSpec extends spock.lang.Specification {
    HWInvSlot slot
    HWInvUtilImpl util

    def setup() {
        slot = new HWInvSlot()
        util = new HWInvUtilImpl()
        "rm -f build/tmp/readOnly.json build/tmp/somefile.json".execute().text
    }

    def "Test toCanonicalJson" () {
        expect: util.toCanonicalJson(null) == "null"
    }
    def "Test fromStringToFile" () {
        when: util.fromStringToFile("Ming", "build/tmp/somefile.json")
        then: notThrown IOException
    }
    def "Test fromStringToFile - cannot write to read only file" () {
        "touch build/tmp/readOnly.json".execute().text
        "chmod -w build/tmp/readOnly.json".execute().text
        when: util.fromStringToFile("Merciless", "build/tmp/readOnly.json")
        then: thrown IOException
    }
    def "Test fromFile - negative"() {
        when: util.fromFile(Paths.get("noSuchFile"))
        then: thrown IOException
    }
    def "Test fromFile"() {
        setup: "touch build/tmp/empty.txt".execute().text
        when: util.fromFile(Paths.get("build/tmp/empty.txt"))
        then: notThrown IOException
    }
}