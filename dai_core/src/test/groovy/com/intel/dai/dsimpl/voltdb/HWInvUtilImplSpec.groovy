// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.dsimpl.voltdb

import com.intel.dai.dsapi.HWInvLoc
import com.intel.logging.Logger
import java.nio.file.Paths

class HWInvUtilImplSpec extends spock.lang.Specification {
    HWInvLoc loc
    HWInvUtilImpl util

    def setup() {
        loc = new HWInvLoc()
        util = new HWInvUtilImpl(Mock(Logger))
        "rm -f build/tmp/readOnly.json build/tmp/somefile.json".execute().text
    }

    def "toCanonicalJson"() {
        expect: util.toCanonicalJson(null) == "null"
    }
    def "toCanonicalHistoryJson"() {
        expect: util.toCanonicalHistoryJson(null) == "null"
    }
    def "toCanonicalPOJO from String"() {
        expect: util.toCanonicalPOJO(null as String) == null
    }
    def "toCanonicalHistoryPOJO from String"() {
        expect: util.toCanonicalHistoryPOJO(null as String) == null
    }
    def "fromStringToFile"() {
        when: util.toFile("Ming", "build/tmp/somefile.json")
        then: notThrown IOException
    }
    def "fromFile - negative"() {
        when: util.fromFile(Paths.get("noSuchFile"))
        then: thrown IOException
    }
    def "fromFile"() {
        setup: "touch build/tmp/empty.txt".execute().text
        when: util.fromFile(Paths.get("build/tmp/empty.txt"))
        then: notThrown IOException
    }
    def "subtract"() {
        def emptyList = new ArrayList<HWInvLoc>()
        expect: util.subtract(emptyList, emptyList) == emptyList

    }
}