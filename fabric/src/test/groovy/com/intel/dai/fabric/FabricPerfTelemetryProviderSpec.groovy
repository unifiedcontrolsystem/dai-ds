// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.fabric

import com.intel.dai.IAdapter
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.RasEventLog
import com.intel.dai.dsapi.StoreTelemetry
import com.intel.dai.dsapi.WorkQueue
import com.intel.logging.Logger
import spock.lang.Specification

class FabricPerfTelemetryProviderSpec extends Specification {
    def configFile_ = new File("./build/tmp/FabricPerfTelemetryProviderSpec.json")
    def factory_
    def adapter_
    def underTest_
    def workQueue_
    def example_
    void setup() {
        adapter_ = Mock(IAdapter)

        adapter_.adapterShuttingDown() >>> [false, false, false, true]
        factory_ = Mock(DataStoreFactory)
        workQueue_ = Mock(WorkQueue)
        workQueue_.baseWorkItemId() >> 99L
        underTest_ = new FabricPerfTelemetryProvider("127.0.0.1", Mock(Logger), factory_, adapter_)
        underTest_.subjects_ = ["subject"]
        underTest_.workQueue_ = workQueue_
        underTest_.rasEventLogging_ = Mock(RasEventLog)
        configFile_.createNewFile()
        configFile_.text = """{"jobid":"3453425","storeBlacklist":"A,B.*"}"""
        underTest_.config_ = underTest_.getConfigMapDefaults()
        underTest_.processConfigItems(underTest_.config_)
        example_ = """{
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
    }

    void cleanup() {
        configFile_.delete()
    }

    def "Test ProcessRawMessage"() {
        underTest_.subjects_ = SUBJECTS
        underTest_.processRawMessage("subject", example_)
        expect: RESULT
        where:
        SUBJECTS      || RESULT
        ["*"]         || true
        ["subject"]   || true
        ["*", "test"] || true
        ["test"]      || true
    }

    def "Test ProcessItem"() {
        def item = new FabricTelemetryItem(0L, "name", "location", 3.0)
        underTest_.storeTelemetry_ = Mock(StoreTelemetry)
        underTest_.config_ = underTest_.buildConfig(configFile_)
        underTest_.processItem(item)
        expect: true
    }

    def "Test ProcessItem With Aggregated Data"() {
        def item = new FabricTelemetryItem(0L, "name", "location", 3.0)
        item.setStatistics(3.0, 3.0, 3.0)
        underTest_.storeTelemetry_ = Mock(StoreTelemetry)
        underTest_.config_ = underTest_.buildConfig(configFile_)
        underTest_.workQueue_ = Mock(WorkQueue)
        underTest_.processItem(item)
        expect: true
    }

    def "Test processConfigItems"() {
        underTest_.processConfigItems(DATA)
        expect: RESULT
        where:
        DATA                     || RESULT
        [:]                      || true
        ["storeBlacklist":null]  || true
        ["storeBlacklist":"  "]  || true
        ["storeBlacklist":"A,B"] || true
    }
}
