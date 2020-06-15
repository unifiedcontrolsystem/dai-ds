package com.intel.dai.foreign_bus

import spock.lang.Specification

import java.text.ParseException
import java.time.format.DateTimeParseException

class CommonFunctionsSpec extends Specification {
    def "Test ConvertISOToLongTimestamp"() {
        long result = CommonFunctions.convertISOToLongTimestamp(VALUE);
        expect: result == RESULT;
        where:
        VALUE                            || RESULT
        "2020-03-27 12:00:00.001Z"       || 1585310400001000000L
        "2020-03-27 12:00:00.000001Z"    || 1585310400000001000L
        "2020-03-27 12:00:00.000000001Z" || 1585310400000000001L
        "2020-03-27 12:00:00.00000001Z"  || 1585310400000000010L
        "2020-03-27 12:00:00.0001Z"      || 1585310400000100000L
        "2020-03-27 12:00:00Z"           || 1585310400000000000L
    }

    def "Test ConvertISOToLongTimestamp Negative"() {
        when: CommonFunctions.convertISOToLongTimestamp(VALUE)
        then: thrown(DateTimeParseException)
        where:
        VALUE || DUMMY
        "2020-03-27 12:00:00"             || false
        "2020-03-27 12:00:00."            || false
        "2020-03-27 12:00:00.Z"           || false
        "2020-03-27 12:00:00.0000000000Z" || false
        "2020-03-27 12:00:00.0000000000Z" || false
    }
}
