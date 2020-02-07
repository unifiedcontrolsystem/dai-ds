// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.procedures

import org.voltdb.VoltTable

class HWInvStoredProceduresSpec extends spock.lang.Specification {
    def "UpsertLocationIntoHWInv"() {
        given:
        def upserter = Spy(UpsertLocationIntoHWInv)
        upserter.voltQueueSQL(*_) >> {}
        upserter.voltExecuteSQL() >> []

        when:
        def res = upserter.run("id", "type", 42,
                fruId, fruType, fruSubType)

        then:
        res == expectedResult

        where:
        fruId      | fruType    | fruSubType   || expectedResult
        'fruId'    | 'fruType'  | 'fruSubType' || UpsertLocationIntoHWInv.SUCCESSFUL
        'fruId'    | null       | null         || UpsertLocationIntoHWInv.SUCCESSFUL
        'fruId'    | null       | "not-null"   || UpsertLocationIntoHWInv.FAILED
        'empty-ID' | null       | null         || UpsertLocationIntoHWInv.SUCCESSFUL
        'empty-ID' | "not-null" | null         || UpsertLocationIntoHWInv.FAILED
        null       | null       | null         || UpsertLocationIntoHWInv.FAILED
    }

    def "DeleteAllLocationsAtIdFromHWInv"() {
        given:
        def testSubject = Spy(DeleteAllLocationsAtIdFromHWInv)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> {}

        when:
        def res = testSubject.run("x0c0s21b0n0")

        then:
        res == 0
    }

    def "AllLocationsAtIdFromHWInv"() {
        given:
        def testSubject = Spy(AllLocationsAtIdFromHWInv)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> new VoltTable[1];

        when:
        def res = testSubject.run("x0c0s21b0n0")

        then:
        res == null
    }

    def "NumberOfLocationsInHWInv"() {
        def res = new VoltTable()
        res.m_rowCount = 42
        def vt = new VoltTable[1]
        vt[0] = res

        given:
        def testSubject = Spy(NumberOfLocationsInHWInv)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> vt;

        expect: testSubject.run() == 42
    }
}
