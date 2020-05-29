package com.intel.dai.monitoring

import com.intel.dai.AdapterInformation
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.network_listener.NetworkListenerCore
import com.intel.logging.Logger
import spock.lang.Specification

class AdapterMonitoringNetworkBaseSpec extends Specification {
    static class TestProvider extends AdapterMonitoringNetworkBase {
        TestProvider(Logger logger, DataStoreFactory factory, AdapterInformation info) {
            super(logger, factory, info, "./build/tmp/benchmarking.json", 1)
        }

        @Override
        protected boolean execute(NetworkListenerCore core) {
            if(callSuper)
                return super.execute(core)
            else
                return true
        }

        boolean callSuper = false;
    }

    def configName_ = this.getClass().getSimpleName() + ".json"
    def configFile_ = new File("/tmp/" + configName_)
    def underTest_;
    void setup() {
        configFile_.delete()
        configFile_.write(""" 
{
  "providerClassMap": {
    "environmentalData": "com.intel.dai.monitoring.TestProvider",
  },
  "networkStreams": {
    "allTelemetry": {
      "arguments": {
        "connectPort": 5678,
        "connectAddress": "10.30.126.57",
        "urlPath": "/v1/stream/all-telemetry",
        "connectTimeout": "30",
        "requestBuilder": "com.intel.dai.monitoring.SSEStreamRequestBuilder",
        "requestType": "GET",
        "requestBuilderSelectors": {
          "stream_id": "fanMetrics"
        }
      },
      "name": "sse"
    }
  },
  "adapterProfiles": {
    "default": {
      "networkStreamsRef": [
        "allTelemetry"
      ],
      "subjects": [
        "telemetry"
      ],
      "adapterProvider": "environmentalData"
    }
  },
  "providerConfigurations": {
    "com.intel.dai.monitoring.TestProvider": {},
  },
  "subjectMap": {
    "telemetry": "EnvironmentalData",
    "inventoryChanges": "InventoryChangeEvent",
    "logs": "LogData",
    "events": "RasEvent",
    "stateChanges": "StateChangeEvent"
  }
}
""")
        underTest_ = new TestProvider(Mock(Logger),Mock(DataStoreFactory),Mock(AdapterInformation))
    }

    void cleanup() {
        configFile_.delete()
    }

    def "Test GetConfigStream positive"() {
        expect: AdapterMonitoringNetworkBase.getConfigStream(configName_) != null
    }

    def "Test GetConfigStream negative"() {
        when: AdapterMonitoringNetworkBase.getConfigStream("bogusFile.json")
        then: thrown(IOException)
    }

    def "Test Execute"() {
        underTest_.callSuper = true
        expect: underTest_.execute(Mock(NetworkListenerCore))
    }

    def "Test EntryPoint"() {
        def stream = new FileInputStream(configFile_)
        expect: underTest_.entryPoint(stream)
    }
}
