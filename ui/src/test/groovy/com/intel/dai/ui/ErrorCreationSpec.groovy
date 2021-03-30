package com.intel.dai.ui

import spock.lang.Specification


class ErrorCreationSpec extends Specification {

    def underTest_

    def setup() {

        underTest_ = new ErrorCreation("Error message")
    }

    def "Test constructSingleMessageSchema"() {

        expect:
        underTest_.constructSingleMessageSchema("Schema message")
    }

    def "Test constructSingleMessageData"() {

        expect:
        underTest_.constructSingleMessageData("Data message")
    }

    def "Test constructErrorResult"() {

        expect:
        underTest_.constructErrorResult()
    }
}
