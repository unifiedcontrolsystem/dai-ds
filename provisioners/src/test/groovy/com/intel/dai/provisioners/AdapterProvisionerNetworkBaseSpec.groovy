package com.intel.dai.provisioners

import com.intel.dai.AdapterInformation
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.network_listener.NetworkListenerCore
import com.intel.logging.Logger
import spock.lang.Specification

class AdapterProvisionerNetworkBaseSpec extends Specification {
    static class TestProvider extends AdapterProvisionerNetworkBase {
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
    "bootEventData": "com.intel.dai.provisioners.TestTransformer",
  },
  "networkStreams": {
    "stateChangeSource": {
      "arguments": {
        "connectAddress": "localhost",
        "connectPort": 5678,
        "bindAddress": "sms01-nmn",
        "bindPort": 54321,
        "urlPath": "/apis/smd/hsm/v1/Subscriptions/SCN",
        "subjects": "stateChange",
        "requestBuilder": "com.intel.dai.provisioners.TestSubscriptionRequest",
        "responseParser": "com.intel.dai.provisioners.TestSubscriptionResponseParser",
        "subscriberName": "daiSubscriptionID",
        "use-ssl": false
      },
      "name": "http_callback"
    }
  },
  "adapterProfiles": {
    "default": {
      "networkStreamsRef": [
        "stateChangeSource"
      ],
      "subjects": [
        "stateChanges"
      ],
      "adapterProvider": "bootEventData"
    }
  },
  "providerConfigurations": {
    "com.intel.dai.provisioners.TestTransformer": {}
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
        expect: AdapterProvisionerNetworkBase.getConfigStream(configName_) != null
    }

    def "Test GetConfigStream negative"() {
        when: AdapterProvisionerNetworkBase.getConfigStream("bogusFile.json")
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
