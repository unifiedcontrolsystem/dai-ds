// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api

import com.intel.dai.dsimpl.voltdb.HWInvUtilImpl
import java.nio.file.*
import spock.lang.*

class HWInvTranslatorSpec extends Specification {
    HWInvTranslator ts

    static def dataDir = "src/test/resources/data/"
    static def tmpDir = "build/tmp/"

    def setupSpec() {
        "rm noSuchFile".execute().text
    }
    def setup() {
        ts = new HWInvTranslator()
    }

    def "Test extractParentId"() {
        expect: ts.extractParentId(id) == parentId

        where:
        id     || parentId
        "x0n0" || "x0"
        "x0"   || ""
        ""     || ""
        "123"  || ""
    }
    def "toCanonical from ForeignHWInvByLoc - negative" () {
        def arg = new ForeignHWInvByLoc()
        arg.ID = ID
        arg.Type = Type
        arg.Ordinal = Ordinal
        arg.Status = Status
        arg.PopulatedFRU = PopulatedFRU

        expect: ts.toCanonical(arg) == null

        where:
        ID      | Type      | Ordinal   | Status            | PopulatedFRU
        null    | "Type"    | 0         | "Empty"           | null
        "ID"    | null      | 0         | "Empty"           | null
        "ID"    | "Type"    | -1        | "Empty"           | null
        "ID"    | "Type"    | 0         | null              | null
        "ID"    | "Type"    | 0         | "Populated"       | null
        "ID"    | "Type"    | 0         | "noSuchStatus"    | null
        "ID"    | "Type"    | 0         | "Empty"           | new ForeignFRU()
    }
    def "extractParentId"() {
        expect: ts.extractParentId(candidate) == result

        where:
        candidate       | result
        "x0"            | ""
        "x0c0s0b0n0"    | "x0c0s0b0"
        "x0c0s0b0n"     | ""
        "^#^@!"         | ""
        null            | null
    }
    def "foreignToCanonical - Path"() {
        def ts = new HWInvTranslator(new HWInvUtilImpl())
        expect:
        ts.foreignToCanonical(Paths.get(inputFileName)).getKey() == location

        where:
        inputFileName                                           | outputFileName                        | location
        dataDir+"foreignHwByLoc/flatNode.json"                  | tmpDir+"flatNode.json.tr"             | "x0c0s26b0n0"
        dataDir+"foreignHwByLocList/preview4HWInventory.json"   | tmpDir+"preview4HWInventory.json.tr"  | ""
        dataDir+"foreignHwInventory/nodeNoMemoryNoCpu.json"     | tmpDir+"nodeNoMemoryNoCpu.json.tr"    | "x0c0s21b0n0"
        dataDir+"foreignHwInventory/hsm-inv-hw-query-s0.json"   | tmpDir+"hsm-inv-hw-query-s0.json.tr"  | null
    }
    def "isValidLocationName"() {
        expect: ts.isValidLocationName(candidate) == result

        where:
        candidate       | result
        "x0"            | true
        "x0c0s0b0n0"    | true
        "x0c0s21b0n0"   | true
        "x0c0s0b0n"     | false
        "^#^@!"         | false
        null            | false
        "X0"            | false
        "x0*"           | false
        "+"             | false
    }
}
