package com.intel.networking.sink.http_callback

import spock.lang.Specification

class SubjectMessagePairSpec extends Specification {
    def "Test SubjectMessagePair"() {
        SubjectMessagePair pair = new SubjectMessagePair("subject", "message")
        expect: pair.subject == "subject"
        and:    pair.message == "message"
    }
}
