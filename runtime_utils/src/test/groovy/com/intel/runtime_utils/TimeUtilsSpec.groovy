// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.runtime_utils

import spock.lang.Specification

import java.text.ParseException
import java.time.Instant

class TimeUtilsSpec extends Specification {
    def "Test getNsTimestamp"() {
        Instant now = Instant.now()
        long golden = now.getEpochSecond() * 1_000_000_000L + now.getNano()
        long testValue = TimeUtils.getNsTimestamp()
        expect: testValue >= golden
    }

    def "Test Simple Conversions"() {
        expect: TimeUtils.nanosecondsToMicroseconds(INPUT) == RESULT1
        and:    TimeUtils.nanosecondsToMilliseconds(INPUT) == RESULT2
        and:    TimeUtils.nanosecondsToSeconds(INPUT)      == RESULT3
        where:
        INPUT                || RESULT1         || RESULT2     || RESULT3
        10_000_000_000_000L  || 10_000_000_000L || 10_000_000L || 10_000L
        10_000_000_000L      || 10_000_000L     || 10_000L     || 10L
        10_000_000L          || 10_000L         || 10L         || 0L
    }

    def "Test Reverse Simple Conversions"() {
        expect: TimeUtils.microsecondsToNanoseconds(INPUT) == RESULT1
        and:    TimeUtils.millisecondsToNanoseconds(INPUT) == RESULT2
        and:    TimeUtils.secondsToNanoseconds(INPUT)      == RESULT3
        where:
        INPUT    || RESULT1     || RESULT2         || RESULT3
        10_000L  || 10_000_000L || 10_000_000_000L || 10_000_000_000_000L
        10L      || 10_000L     || 10_000_000L     || 10_000_000_000L
        1L       || 1_000L      || 1_000_000L      || 1_000_000_000L
    }

    def "Test NSFromIso8601 and Back"() {
        expect: TimeUtils.nSFromIso8601(TS) == RESULT
        and:    TimeUtils.nsToIso8601(RESULT) == RESULT2
        where: // Check all allowed formats...
        TS                                || RESULT               || RESULT2
        "2020-03-27 12:00:00.001Z"        || 1585310400001000000L || "2020-03-27T12:00:00.001Z"
        "2020-03-27 12:00:00.001GMT"      || 1585310400001000000L || "2020-03-27T12:00:00.001Z"
        "2020-03-27T05:00:00.001PDT"      || 1585310400001000000L || "2020-03-27T12:00:00.001Z"
        "2020-03-27 08:00:00.001EDT"      || 1585310400001000000L || "2020-03-27T12:00:00.001Z"
        "2020-03-27T12:00:00.000001Z"     || 1585310400000001000L || "2020-03-27T12:00:00.000001Z"
        "2020-03-27 12:00:00.000000001Z"  || 1585310400000000001L || "2020-03-27T12:00:00.000000001Z"
        "2020-03-27T05:00:00.00000001-07" || 1585310400000000010L || "2020-03-27T12:00:00.000000010Z"
        "2020-03-27 12:00:00.0001Z"       || 1585310400000100000L || "2020-03-27T12:00:00.000100Z"
        "2020-03-27T04:00:00-08:00"       || 1585310400000000000L || "2020-03-27T12:00:00Z"
        "2020-03-27T07:00:00-0500"        || 1585310400000000000L || "2020-03-27T12:00:00Z"
    }

    def "Test NSFromIso8601 Negative"() {
        when: TimeUtils.nSFromIso8601(VALUE)
        then: thrown(ParseException)
        where: // Check for unacceptable syntax...
        VALUE                             || DUMMY
        "2020-03-27 12:00:00"             || false // No TZ info
        "2020-03-27 12:00:00."            || false // missing fraction but included decimal and no TZ
        "2020-03-27 12:00:00.Z"           || false // missing fraction but included decimal with TZ
        "2020-03-27 12:00:00.0000000000Z" || false // too many digits for a nano second fraction
    }
}
