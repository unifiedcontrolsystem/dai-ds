package com.intel.dai.builder

import spock.lang.Specification

class ForeignSubscriptionResponseParserSpec extends Specification {
    def "Test ParseResponse positive"() {
        ForeignSubscriptionResponseParser parser = new ForeignSubscriptionResponseParser()
        URI result = parser.parseResponse(JSON, SUB_URL)
        expect: result == RESULT

        where:
        JSON              | SUB_URL                                || RESULT
        "{\"ID\":42}"     | "http://127.0.0.1:12345/subscription"  || URI.create("http://127.0.0.1:12345/subscription/42")
        "{\"ID\":42}"     | "http://127.0.0.1:12345/subscription/" || URI.create("http://127.0.0.1:12345/subscription/42")
        "{\"ID\":\"42\"}" | "http://127.0.0.1:12345/subscription"  || URI.create("http://127.0.0.1:12345/subscription/42")
        "{\"ID\":\"\"}"   | "http://127.0.0.1:12345/subscription"  || null
        "{}"              | "http://127.0.0.1:12345/subscription"  || null
        "{]"              | "http://127.0.0.1:12345/subscription"  || null
    }
}
