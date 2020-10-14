// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring

import com.intel.dai.network_listener.CommonDataFormat
import com.intel.dai.network_listener.DataType
import com.intel.dai.network_listener.NetworkListenerConfig
import com.intel.dai.network_listener.NetworkListenerProviderException
import com.intel.dai.network_listener.SystemActions
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import spock.lang.Specification

import java.time.Instant

class EnvironmentalProviderForeignBusSpec extends Specification {
    def config_
    def actions_
    def provider_
    long timestamp_
    def underTest_
    void setup() {
        actions_ = Mock(SystemActions)
        config_ = Mock(NetworkListenerConfig)
        provider_ = new PropertyMap()
        provider_.put("windowSize", 2)
        provider_.put("publish", true)
        config_.getProviderConfigurationFromClassName(_ as String) >> provider_
        timestamp_ = Instant.now().toEpochMilli() * 1_000_000L
        underTest_ = new EnvironmentalProviderForeignBus(Mock(Logger))
        underTest_.initialize()
    }

    def "Test processRawStringData"() {
        underTest_.processRawStringData(payload_, config_)
        def results = underTest_.processRawStringData(payload_, config_)
        expect: results.size() == 2
    }

    def "Test processRawStringData Negative"() {
        when: underTest_.processRawStringData(badJson_, config_)
        then: thrown(NetworkListenerProviderException)
    }

    def "Test actOnData"() {
        underTest_.accumulators_ = new HashMap<>()
        underTest_.configDone_ = true
        underTest_.doAggregation_ = true
        underTest_.publish_ = true
        EnvironmentalProviderForeignBus.Accumulator.useTime_ = USETIME
        EnvironmentalProviderForeignBus.Accumulator.moving_ = MOVING
        EnvironmentalProviderForeignBus.Accumulator.count_ = 3
        EnvironmentalProviderForeignBus.Accumulator.ns_ = 10

        def data = new CommonDataFormat(timestamp_, "location", DataType.EnvironmentalData)
        data.setDescription("name")
        data.setValue(84.0)
        underTest_.aggregateData(data)
        underTest_.actOnData(data, config_, actions_)

        data = new CommonDataFormat(timestamp_ + 5, "location", DataType.EnvironmentalData)
        data.setDescription("name")
        data.setValue(92.0)
        underTest_.aggregateData(data)
        underTest_.actOnData(data, config_, actions_)

        data = new CommonDataFormat(timestamp_ + 11, "location", DataType.EnvironmentalData)
        data.setDescription("name")
        data.setValue(96.0)
        underTest_.aggregateData(data)
        underTest_.actOnData(data, config_, actions_)

        data = new CommonDataFormat(timestamp_, "location", DataType.EnvironmentalData)
        data.setDescription("name")
        data.setValue(84.0)
        underTest_.publish_ = false
        underTest_.aggregateData(data)
        underTest_.actOnData(data, config_, actions_)

        expect: RESULT

        where:
        USETIME | MOVING || RESULT
        false   | false  || true
        false   | true   || true
        true    | false  || true
        true    | true   || true
    }

    boolean doubleEquals(double value, double expected) {
        return (value >= (expected - 0.0001) && value <= (expected + 0.0001))
    }

    def "Test windows count accumulation"() {
        EnvironmentalProviderForeignBus.Accumulator accumulator =
                new EnvironmentalProviderForeignBus.Accumulator(Mock(Logger))
        EnvironmentalProviderForeignBus.Accumulator.count_ = 10
        EnvironmentalProviderForeignBus.Accumulator.useTime_ = false
        EnvironmentalProviderForeignBus.Accumulator.moving_ = false
        CommonDataFormat data
        for(int v = 1; v <= 10; v++) {
            data = new CommonDataFormat(1_000_000_000L * v, "loc", DataType.EnvironmentalData)
            data.setValue((double)v)
            accumulator.addValue(data)
        }
        expect: data != null
        and:    doubleEquals(data.average, 5.5)
        and:    doubleEquals(data.maximum, 10.0)
        and:    doubleEquals(data.minimum, 1.0)
        and:    accumulator.values_.size() == 0
    }

    def "Test time window accumulation"() {
        EnvironmentalProviderForeignBus.Accumulator accumulator =
                new EnvironmentalProviderForeignBus.Accumulator(Mock(Logger))
        EnvironmentalProviderForeignBus.Accumulator.ns_ = 10_000_000_000L
        EnvironmentalProviderForeignBus.Accumulator.useTime_ = true
        EnvironmentalProviderForeignBus.Accumulator.moving_ = false
        CommonDataFormat data
        for(int v = 1; v <= 11; v++) {
            data = new CommonDataFormat(1_000_000_000L * v, "loc", DataType.EnvironmentalData)
            data.setValue((double)v)
            accumulator.addValue(data)
        }
        expect: data != null
        and:    doubleEquals(data.average, 6)
        and:    doubleEquals(data.maximum, 11.0)
        and:    doubleEquals(data.minimum, 1.0)
        and:    accumulator.values_.size() == 0
    }

    def "Test count moving average accumulation"() {
        EnvironmentalProviderForeignBus.Accumulator accumulator =
                new EnvironmentalProviderForeignBus.Accumulator(Mock(Logger))
        EnvironmentalProviderForeignBus.Accumulator.count_ = 5
        EnvironmentalProviderForeignBus.Accumulator.useTime_ = false
        EnvironmentalProviderForeignBus.Accumulator.moving_ = true
        CommonDataFormat data
        for(int v = 1; v <= 10; v++) {
            data = new CommonDataFormat(1_000_000_000L * v, "loc", DataType.EnvironmentalData)
            data.setValue((double)v)
            accumulator.addValue(data)
        }
        expect: data != null
        and:    doubleEquals(data.average, 8)
        and:    doubleEquals(data.maximum, 10.0)
        and:    doubleEquals(data.minimum, 6.0)
        and:    accumulator.values_.size() == 4
    }

    def payload_ = """
{
  "metrics": {
    "messages": [
      {
        "Events": [
          {
            "EventTimestamp": "2020-04-21T01:37:48.219Z",
            "MessageId": "CrayFabricPerfTelemetry.RFC3635",
            "Oem": {
              "Sensors": [
                {
                  "DeviceSpecificContext": "edge",
                  "Index": 9,
                  "Location": "x3000c0s34b4n0",
                  "ParentalIndex": 1,
                  "PhysicalContext": "IfOutOctets",
                  "SubIndex": 0,
                  "Timestamp": "2020-04-21T01:37:48.219Z",
                  "Value": "84"
                }
              ]
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
            "EventTimestamp": "2020-04-21T01:37:48.219Z",
            "MessageId": "CrayFabricPerfTelemetry.RFC3635",
            "Oem": {
              "Sensors": [
                {
                  "DeviceSpecificContext": "edge",
                  "Index": 9,
                  "Location": "x3000c0s34b4n0",
                  "ParentalIndex": 1,
                  "PhysicalContext": "IfOutOctets",
                  "SubIndex": 0,
                  "Timestamp": "2020-04-21T01:37:48.219Z",
                  "Value": "92"
                }
              ]
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
            "EventTimestamp": "2020-04-21T01:37:48.219Z",
            "MessageId": "CrayFabricPerfTelemetry.RFC3635",
            "Oem": {
              "Sensors": [
                {
                  "DeviceSpecificContext": "edge",
                  "Index": 9,
                  "Location": "bad_location",
                  "ParentalIndex": 1,
                  "PhysicalContext": "IfOutOctets",
                  "SubIndex": 0,
                  "Timestamp": "2020-04-21T01:37:48.219Z",
                  "Value": "84"
                }
              ]
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
            "EventTimestamp": "2020-04-21T01:37:48.219Z",
            "MessageId": "CrayFabricPerfTelemetry.RFC3635",
            "Oem": {
              "Sensors": [
                {
                  "DeviceSpecificContext": "edge",
                  "Index": 9,
                  "Location": "x3000c0s34b4n0",
                  "ParentalIndex": 1,
                  "PhysicalContext": "IfOutOctets",
                  "SubIndex": 0,
                  "Timestamp": "bad_timestamp",
                  "Value": "84"
                }
              ]
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
            "EventTimestamp": "2020-04-21T01:37:48.219Z",
            "MessageId": "CrayFabricPerfTelemetry.RFC3635",
            "Oem": {
              "Sensors": [
                {
                  "DeviceSpecificContext": "edge",
                  "Index": 9,
                  "Location": "x3000c0s34b4n0",
                  "ParentalIndex": 1,
                  "PhysicalContext": "IfOutOctets",
                  "SubIndex": 0,
                  "Timestamp": "2020-04-21T01:37:48.219Z",
                }
              ]
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
            "EventTimestamp": "2020-04-21T01:37:48.219Z",
            "MessageId": "CrayFabricPerfTelemetry.RFC3635",
            "Oem": {
              "Sensors": [
                {
                  "DeviceSpecificContext": "edge",
                  "Index": 9,
                  "Location": "x3000c0s34b4n0",
                  "ParentalIndex": 1,
                  "PhysicalContext": "IfOutOctets",
                  "SubIndex": 0,
                  "Timestamp": false,
                  "Value": "84"
                }
              ]
            }
          }
        ]
      }
    ]
  }
}
"""
    def badJson_ = """
{
  "Events": [
    {
      "EventTimestamp": "2020-04-21T01:37:48.219Z",
      "MessageId": "CrayFabricPerfTelemetry.RFC3635",
      "Oem": {
        "Sensors": [ [
          {
            "DeviceSpecificContext": "edge",
            "Index": 9,
            "Location": "x3000c0s34b4n0",
            "ParentalIndex": 1,
            "PhysicalContext": "IfOutOctets",
            "SubIndex": 0,
            "Timestamp": "2020-04-21T01:37:48.219Z",
            "Value": "84"
          }
        ]
      }
    }
  ]
}
"""
}
