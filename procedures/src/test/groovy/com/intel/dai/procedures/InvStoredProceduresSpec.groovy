// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.procedures

import org.voltdb.VoltTable

class InvStoredProceduresSpec extends spock.lang.Specification {
    def "UpsertLocationIntoHWInv"() {
        given:
        def upserter = Spy(RawInventoryInsert)
        upserter.voltQueueSQL(*_) >> {}
        upserter.voltExecuteSQL() >> []

        when:
        def res = upserter.run("id", "type", 42, null,
                fruId, fruType, fruSubType, null)

        then:
        res == expectedResult

        where:
        fruId      | fruType    | fruSubType   || expectedResult
        'fruId'    | 'fruType'  | 'fruSubType' || RawInventoryInsert.SUCCESSFUL
        'fruId'    | null       | null         || RawInventoryInsert.SUCCESSFUL
        'fruId'    | null       | "not-null"   || RawInventoryInsert.FAILED
        'empty-ID' | null       | null         || RawInventoryInsert.SUCCESSFUL
        'empty-ID' | "not-null" | null         || RawInventoryInsert.FAILED
        null       | null       | null         || RawInventoryInsert.FAILED
    }

    def "RawInventoryDump"() {
        given:
        def testSubject = Spy(RawInventoryDump)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> new VoltTable[1];

        when:
        def res = testSubject.run("x0c0s21b0n0")

        then:
        res == null
    }

    def "FwInventoryUpsert"() {
        given:
        def ts = Spy(FwVersionUpsert)
        ts.voltQueueSQL(*_) >> {}
        ts.voltExecuteSQL() >> []

        expect: ts.run("id", "targetId", "version") == FwVersionUpsert.SUCCESSFUL
    }

    def "FwVersionDump"() {
        given:
        def ts = Spy(FwVersionDump)
        ts.voltQueueSQL(*_) >> {}
        ts.voltExecuteSQL(*_) >> new VoltTable[1]

        when: ts.run('x0')
        then: notThrown Exception
    }

    def "FwVersionHistoryInsert"() {
        given:
        def ts = Spy(FwVersionHistoryInsert)
        ts.voltQueueSQL(*_) >> {}
        ts.voltExecuteSQL() >> []

        expect: ts.run("id", "targetId", "version") == FwVersionUpsert.SUCCESSFUL
    }

    def "FwVersionHistoryDump"() {
        given:
        def ts = Spy(FwVersionHistoryDump)
        ts.voltQueueSQL(*_) >> {}
        ts.voltExecuteSQL(*_) >> new VoltTable[1]

        when: ts.run('x0')
        then: notThrown Exception
    }
}
