// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring

import com.intel.dai.network_listener.CommonDataFormat
import com.intel.dai.network_listener.NetworkListenerConfig
import com.intel.dai.network_listener.SystemActions
import com.intel.logging.Logger
import com.intel.properties.PropertyMap
import spock.lang.Specification

class EnvironmentalProviderHPCMSpec extends Specification {
    def networkConfig_ = Mock(NetworkListenerConfig)
    def actions_ = Mock(SystemActions)
    def logger_ = Mock(Logger)
    int publishRawCounter_
    int publishAggCounter_
    PropertyMap configMap_

    def underTest_
    void setup() {
        underTest_ = new EnvironmentalProviderHPCM(logger_)
        underTest_.initialize()
        configMap_ = new PropertyMap()
        configMap_.put("publish", true)
        configMap_.put("publishAggregatedTopic", "aggData")
        configMap_.put("publishRawTopic", "rawData")
        configMap_.put("useTimeWindow", false)
        configMap_.put("windowSize", 25)
        configMap_.put("useMovingAverage", false)
        configMap_.put("useAggregation", true)
        networkConfig_.getProviderConfigurationFromClassName(_ as String) >> configMap_
        publishRawCounter_ = 0
        publishAggCounter_ = 0
        actions_.publishNormalizedData(_ as String, _ as String, _ as String, _ as Long, _ as Double) >> {
            String topic, String name, String location, Long ns, Double value ->
                publishRawCounter_++
        }
        actions_.publishAggregatedData(_ as String, _ as String, _ as String, _ as Long, _ as Double, _ as Double, _ as Double) >> {
            String topic, String name, String location, Long ns, Double min, Double max, Double avg ->
                publishAggCounter_++
        }
    }

    def "Test Initialize"() {
        expect: underTest_.dispatchMap_.size() == 2
    }

    def "Test ProcessRawStringData Thermal"() {
        def list = underTest_.processRawStringData(rawThermal, networkConfig_)
        expect: list.size() == 6
    }

    def "Test ProcessRawStringData Bad Thermal 1"() {
        PropertyMap map = underTest_.parser_.fromString(rawThermal).getAsMap()
        map.remove("@odata.context")
        String modified = underTest_.parser_.toString(map)
        def list = underTest_.processRawStringData(modified, networkConfig_)
        expect: list.size() == 0
    }

    def "Test ProcessRawStringData Bad Thermal 3"() {
        PropertyMap map = underTest_.parser_.fromString(rawThermal).getAsMap()
        map.getArray("Fans").getMap(0).remove("Name")
        map.getArray("Fans").getMap(1).remove("Reading")
        map.getArray("Fans").set(2, "string")
        map.getArray("Temperatures").getMap(0).remove("Name")
        map.getArray("Temperatures").getMap(1).remove("ReadingCelsius")
        map.getArray("Temperatures").set(2, "string")
        String modified = underTest_.parser_.toString(map)
        def list = underTest_.processRawStringData(modified, networkConfig_)
        expect: list.size() == 2
    }

    def "Test ProcessRawStringData ProcessorMetrics"() {
        def list = underTest_.processRawStringData(rawProcessorMetrics, networkConfig_)
        expect: list.size() == 1
    }

    def "Test ProcessRawStringData ProcessorMetrics Bad ProcessorMetrics 1"() {
        PropertyMap map = underTest_.parser_.fromString(rawProcessorMetrics).getAsMap()
        map.remove("Name")
        String modified = underTest_.parser_.toString(map)
        def list = underTest_.processRawStringData(modified, networkConfig_)
        expect: list.size() == 1
    }

    def "Test ProcessRawStringData ProcessorMetrics Bad ProcessorMetrics 2"() {
        PropertyMap map = underTest_.parser_.fromString(rawProcessorMetrics).getAsMap()
        map.remove("TemperatureCelsius")
        String modified = underTest_.parser_.toString(map)
        def list = underTest_.processRawStringData(modified, networkConfig_)
        expect: list.size() == 0
    }

    def "Test ActOnData"() {
        for(int i = 0; i < 25; i++) {
            def list = underTest_.processRawStringData(rawThermal, networkConfig_)
            for (CommonDataFormat data : list)
                underTest_.actOnData(data, networkConfig_, actions_)
        }
        expect: publishRawCounter_ == 150 // 24 loops for 6 data
        and:    publishAggCounter_ == 6   // 6 data aggregated
    }

    def "Test ActOnData No Publish"() {
        configMap_.put("publish", false)
        configMap_.put("useTimeWindow", true)
        configMap_.put("useMovingAverage", true)
        underTest_.publish_ = false
        for(int i = 0; i < 25; i++) {
            def list = underTest_.processRawStringData(rawThermal, networkConfig_)
            for(CommonDataFormat data: list)
                data.nsTimestamp_ = data.getNanoSecondTimestamp() + (i * 600_000_001_000)
            for (CommonDataFormat data : list)
                underTest_.actOnData(data, networkConfig_, actions_)
        }
        expect: underTest_.publish_ == false
    }

    String rawThermal = """{
  "@odata.context": "/redfish/v1/\$metadata#Thermal.Thermal",
  "@odata.id": "/redfish/v1/Chassis/RackMount/Baseboard/Thermal",
  "@odata.type": "#Thermal.v1_5_2.Thermal",
  "timestamp": "2021-03-04 16:45:00.000000000Z",
  "location": "x0c0s36b0n0",
  "Fans": [
    {
      "@odata.id": "/redfish/v1/Chassis/RackMount/Baseboard/Thermal#/Fans/0",
      "LowerThresholdCritical": 1710,
      "LowerThresholdNonCritical": 1980,
      "MaxReadingRange": 22950,
      "MemberId": "0",
      "MinReadingRange": 0,
      "Name": "System Fan 1",
      "Reading": 7740,
      "ReadingUnits": "RPM",
      "Redundancy": [
        {
          "@odata.id": "/redfish/v1/Chassis/RackMount/Baseboard/Thermal#/Redundancy/0"
        }
      ],
      "Status": {
        "Health": "OK",
        "HealthRollup": "OK",
        "State": "Enabled"
      }
    },
    {
      "@odata.id": "/redfish/v1/Chassis/RackMount/Baseboard/Thermal#/Fans/1",
      "LowerThresholdCritical": 1710,
      "LowerThresholdNonCritical": 1980,
      "MaxReadingRange": 22950,
      "MemberId": "1",
      "MinReadingRange": 0,
      "Name": "System Fan 2",
      "Reading": 7830,
      "ReadingUnits": "RPM",
      "Redundancy": [
        {
          "@odata.id": "/redfish/v1/Chassis/RackMount/Baseboard/Thermal#/Redundancy/0"
        }
      ],
      "Status": {
        "Health": "OK",
        "HealthRollup": "OK",
        "State": "Enabled"
      }
    },
    {
      "@odata.id": "/redfish/v1/Chassis/RackMount/Baseboard/Thermal#/Fans/2",
      "LowerThresholdCritical": 1710,
      "LowerThresholdNonCritical": 1980,
      "MaxReadingRange": 22950,
      "MemberId": "2",
      "MinReadingRange": 0,
      "Name": "System Fan 3",
      "Reading": 7740,
      "ReadingUnits": "RPM",
      "Redundancy": [
        {
          "@odata.id": "/redfish/v1/Chassis/RackMount/Baseboard/Thermal#/Redundancy/0"
        }
      ],
      "Status": {
        "Health": "OK",
        "HealthRollup": "OK",
        "State": "Enabled"
      }
    }
  ],
  "Temperatures": [
    {
      "@odata.id": "/redfish/v1/Chassis/RackMount/Baseboard/Thermal#/Temperatures/0",
      "LowerThresholdCritical": 0,
      "LowerThresholdNonCritical": 5,
      "MemberId": "0",
      "Name": "BB P0 VR Temp",
      "ReadingCelsius": 23,
      "SensorNumber": 32,
      "Status": {
        "Health": "OK",
        "HealthRollup": "OK",
        "State": "Enabled"
      },
      "UpperThresholdCritical": 115,
      "UpperThresholdNonCritical": 110
    },
    {
      "@odata.id": "/redfish/v1/Chassis/RackMount/Baseboard/Thermal#/Temperatures/1",
      "LowerThresholdCritical": 0,
      "LowerThresholdNonCritical": 5,
      "MemberId": "1",
      "Name": "Front Panel Temp",
      "ReadingCelsius": 23,
      "SensorNumber": 33,
      "Status": {
        "Health": "OK",
        "HealthRollup": "OK",
        "State": "Enabled"
      },
      "UpperThresholdCritical": 55,
      "UpperThresholdNonCritical": 50
    },
    {
      "@odata.id": "/redfish/v1/Chassis/RackMount/Baseboard/Thermal#/Temperatures/2",
      "LowerThresholdCritical": 0,
      "LowerThresholdNonCritical": 5,
      "MemberId": "2",
      "Name": "PCH Temp",
      "ReadingCelsius": 28,
      "SensorNumber": 34,
      "Status": {
        "Health": "OK",
        "HealthRollup": "OK",
        "State": "Enabled"
      },
      "UpperThresholdCritical": 103,
      "UpperThresholdNonCritical": 98
    }
  ]
}
"""
    String rawProcessorMetrics = """{
  "@odata.context": "/redfish/v1/\$metadata#ProcessorMetrics.ProcessorMetrics",
  "@odata.id": "/redfish/v1/Systems/LUC301700005/Processors/CPU1/ProcessorMetrics",
  "@odata.type": "#ProcessorMetrics.v1_0_0.ProcessorMetrics",
  "timestamp": "2021-03-04 16:45:00.000000005Z",
  "location": "x0c0s36b0n0",
  "AverageFrequencyMHz": null,
  "BandwidthPercent": null,
  "ConsumedPowerWatt": null,
  "Description": "Processor Metrics",
  "Health": [
    "Processor Present"
  ],
  "Id": "CPU1 ProcessorMetrics",
  "Name": "Metrics for CPU1",
  "TemperatureCelsius": 28,
  "ThrottlingCelsius": 70
}
"""
}
