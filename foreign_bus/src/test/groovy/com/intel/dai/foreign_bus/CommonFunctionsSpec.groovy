package com.intel.dai.foreign_bus

import spock.lang.Specification

import java.text.ParseException

class CommonFunctionsSpec extends Specification {
    def "Test ConvertISOToLongTimestamp"() {
        long result = CommonFunctions.convertISOToLongTimestamp(VALUE);
        expect: result == RESULT;
        where: // Check all allowed formats...
        VALUE                             || RESULT
        "2020-03-27 12:00:00.001Z"        || 1585310400001000000L
        "2020-03-27 12:00:00.001GMT"      || 1585310400001000000L
        "2020-03-27T05:00:00.001PDT"      || 1585310400001000000L
        "2020-03-27 08:00:00.001EDT"      || 1585310400001000000L
        "2020-03-27T12:00:00.000001Z"     || 1585310400000001000L
        "2020-03-27 12:00:00.000000001Z"  || 1585310400000000001L
        "2020-03-27T05:00:00.00000001-07" || 1585310400000000010L
        "2020-03-27 12:00:00.0001Z"       || 1585310400000100000L
        "2020-03-27T04:00:00-08:00"       || 1585310400000000000L
        "2020-03-27T07:00:00-0500"        || 1585310400000000000L
    }

    def "Test ConvertISOToLongTimestamp Negative"() {
        when: CommonFunctions.convertISOToLongTimestamp(VALUE)
        then: thrown(ParseException)
        where: // Check for unacceptable syntax...
        VALUE                             || DUMMY
        "2020-03-27 12:00:00"             || false // No TZ info
        "2020-03-27 12:00:00."            || false // missing fraction but included decimal and no TZ
        "2020-03-27 12:00:00.Z"           || false // missing fraction but included decimal with TZ
        "2020-03-27 12:00:00.0000000000Z" || false // too many digits for a nano second fraction
    }

    def "Test parseForeignTelemetry"() {
        def sample = """{
  "metrics": {
    "messages": [
      {
        "Events": [
          {
            "EventTimestamp": "2020-06-03T22:35:45Z",
            "MessageId": "CrayTelemetry.Power",
            "Oem": {
              "Sensors": [
                {
                  "Timestamp": "2020-06-03T22:35:45Z",
                  "Location": "x3000c0s17b0",
                  "PhysicalContext": "Chassis",
                  "DeviceSpecificContext": "sensor",
                  "Index": 0,
                  "Value": "258"
                }
              ],
              "TelemetrySource": "River"
            }
          }
        ]
      }
    ]
  }
}
{
  "metrics": {
    "messages": [
      {
        "Events": [
          {
            "EventTimestamp": "2020-06-03T22:35:45Z",
            "MessageId": "CrayTelemetry.Power",
            "Oem": {
              "Sensors": [
                {
                  "Timestamp": "2020-06-03T22:35:45Z",
                  "Location": "x3000c0s17b0",
                  "PhysicalContext": "Chassis",
                  "Index": 0,
                  "Value": "258"
                }
              ],
              "TelemetrySource": "River"
            }
          }
        ]
      }
    ]
  }
}"""
        def results = CommonFunctions.parseForeignTelemetry(sample)
        expect: results.size() == 2
    }
}
