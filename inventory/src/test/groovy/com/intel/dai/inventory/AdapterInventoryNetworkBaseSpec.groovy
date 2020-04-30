package com.intel.dai.inventory

import com.intel.dai.AdapterInformation
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.HWInvApi
import com.intel.dai.inventory.api.HWInvDiscovery
import com.intel.dai.network_listener.NetworkListenerCore
import com.intel.logging.Logger

import org.apache.commons.lang3.tuple.ImmutablePair
import spock.lang.Specification

class AdapterInventoryNetworkBaseSpec extends Specification {
    static class TestProvider extends AdapterInventoryNetworkBase {
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
    TestProvider underTest_;
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
        expect: AdapterInventoryNetworkBase.getConfigStream(configName_) != null
    }

    def "Test GetConfigStream negative"() {
        when: AdapterInventoryNetworkBase.getConfigStream("bogusFile.json")
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

    def "Test preInitialise for location"() {
        underTest_.preInitialise()
        HWInvApi hWInvApiMock = Mock(HWInvApi)
        underTest_.hwInvApi_ = hWInvApiMock
        underTest_.hwInvApi_.ingest(any()) >> 0

        def value = new ImmutablePair<>(0, hwInvForLocation)
        HWInvDiscovery invDiscoveryMock = Mock(HWInvDiscovery)
        underTest_.hwInvDiscovery_ = invDiscoveryMock
        underTest_.hwInvDiscovery_.queryHWInvTree() >> value
        underTest_.postInitialize()
        expect: true
    }

    String hwInvForLocation = "{\n" +
            "  \"HWInventoryByLocationType\": \"HWInvByLocNode\",\n" +
            "  \"ID\": \"x0c0s24b0n0\",\n" +
            "  \"NodeLocationInfo\": {\n" +
            "    \"Description\": \"Computer system providing compute resources\",\n" +
            "    \"HostName\": \"\",\n" +
            "    \"Id\": \"fru id\"\n" +
            "  },\n" +
            "  \"Ordinal\": 0,\n" +
            "  \"PopulatedFRU\": {\n" +
            "    \"FRUID\": \"fru id\",\n" +
            "    \"HWInventoryByFRUType\": \"HWInvByFRUNode\",\n" +
            "    \"NodeFRUInfo\": {\n" +
            "      \"AssetTag\": \"....................\",\n" +
            "      \"BiosVersion\": \"bios version\",\n" +
            "      \"Manufacturer\": \"manufacture data\",\n" +
            "      \"Model\": \"model data\",\n" +
            "      \"PartNumber\": \"part number data\",\n" +
            "      \"SKU\": \"sku data\",\n" +
            "      \"SerialNumber\": \"ser num\",\n" +
            "      \"SystemType\": \"Physical\",\n" +
            "      \"UUID\": \"id\"\n" +
            "    },\n" +
            "    \"Subtype\": \"\",\n" +
            "    \"Type\": \"Node\"\n" +
            "  },\n" +
            "  \"Status\": \"Populated\",\n" +
            "  \"Type\": \"Node\"\n" +
            "}"
}
