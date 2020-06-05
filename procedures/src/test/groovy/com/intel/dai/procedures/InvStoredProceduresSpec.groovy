// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.procedures

import org.voltdb.VoltTable

class InvStoredProceduresSpec extends spock.lang.Specification {
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
        res.m_rowCount = 0
        def vt = new VoltTable[1]
        vt[0] = res

        given:
        def testSubject = Spy(NumberOfLocationsInHWInv)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> vt;

        expect: testSubject.run() == -1
    }

    def "HwInventoryHistoryInsert"() {
        given:
        def testSubject = Spy(HwInventoryHistoryInsert)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> {}

        expect: testSubject.run(Action, ID, FRUID) == Res

        where:
        Action      | ID    | FRUID     | TimeStamp || Res
        'INSERTED'  | 'x0'  | 'model-T' | "ts"      || HwInventoryHistoryInsert.SUCCESSFUL
        'DELETED'   | 'x0'  | 'model-T' | "ts"      || HwInventoryHistoryInsert.SUCCESSFUL
        'INSERTED'  | 'x0'  | 'model-T' | "ts"      || HwInventoryHistoryInsert.SUCCESSFUL
        'INVALIDED' | 'x0'  | 'model-T' | "ts"      || HwInventoryHistoryInsert.FAILED
        null        | 'x0'  | 'model-T' | "ts"      || HwInventoryHistoryInsert.FAILED
        'INSERTED'  | null  | 'model-T' | "ts"      || HwInventoryHistoryInsert.FAILED
        'INSERTED'  | 'x0'  | null      | "ts"      || HwInventoryHistoryInsert.FAILED
        // 'INSERTED'  | 'x0'  | 'model-T' | null      || HwInventoryHistoryInsert.FAILED
    }

    def "HwInventoryHistoryDump"() {
        given:
        def testSubject = Spy(HwInventoryHistoryDump)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> new VoltTable[1]

        when: testSubject.run('x0')
        then: notThrown Exception
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

    def "HwInventoryHistoryLastUpdateTimestamp"() {
        def res = new VoltTable()
        res.m_rowCount = 0
        def vt = new VoltTable[1]
        vt[0] = res

        given:
        def testSubject = Spy(HwInventoryHistoryLastUpdateTimestamp)
        testSubject.voltQueueSQL(*_) >> {}
        testSubject.voltExecuteSQL(*_) >> vt;

        expect: testSubject.run() == -1
    }
}
