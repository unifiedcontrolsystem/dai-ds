package com.intel.dai.procedures

class UpsertFRUIntoHWInvSpec extends spock.lang.Specification {
    def "Test run"() {
        given:
        def upserter = Spy(UpsertFRUIntoHWInv)
        upserter.voltQueueSQL(_,_,_,  _,_,_,  _,_) >> {}
        upserter.voltExecuteSQL() >> []

        when:
        def res = upserter.run("id", "parentId", "type", 42,
                fruId, fruType, fruSubType)

        then:
        res == expectedResult

        where:
        fruId       | fruType       | fruSubType    || expectedResult
        'fruId'     | 'fruType'     | 'fruSubType'  || UpsertFRUIntoHWInv.UPSERT_SUCCESSFUL
        'fruId'     | null          | null          || UpsertFRUIntoHWInv.UPSERT_SUCCESSFUL
        'fruId'     | null          | "not-null"    || UpsertFRUIntoHWInv.UPSERT_FAILED
        'empty-ID'  | null          | null          || UpsertFRUIntoHWInv.UPSERT_SUCCESSFUL
        'empty-ID'  | "not-null"    | null          || UpsertFRUIntoHWInv.UPSERT_FAILED
        null        | null          | null          || UpsertFRUIntoHWInv.UPSERT_FAILED
    }
}
