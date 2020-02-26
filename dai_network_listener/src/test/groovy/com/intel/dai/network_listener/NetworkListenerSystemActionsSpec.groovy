package com.intel.dai.network_listener

import com.intel.dai.AdapterInformation
import com.intel.dai.dsapi.BootState
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.HWInvApi
import com.intel.dai.dsapi.HWInvLoc
import com.intel.dai.dsapi.NodeInformation
import com.intel.dai.dsapi.RasEventLog
import com.intel.dai.exceptions.DataStoreException
import com.intel.logging.Logger
import com.intel.networking.source.NetworkDataSource
import com.intel.networking.source.NetworkDataSourceFactory
import com.intel.properties.PropertyMap
import org.apache.commons.io.IOExceptionWithCause
import spock.lang.Specification

class NetworkListenerSystemActionsSpec extends Specification {
    static class TestSource implements NetworkDataSource {
        @Override void initialize() {}
        @Override void connect(String info) {}
        @Override void setLogger(Logger logger) {}
        @Override String getProviderName() { return "test" }
        @Override boolean sendMessage(String subject, String message) { return true }
        @Override void close() throws IOException {}
    }

    def config_
    def listenerConfig_
    def factory_
    def invApi_
    def underTest_
    void setup() {
        NetworkDataSourceFactory.registerNewImplementation("test", TestSource.class)
        listenerConfig_ = new PropertyMap()
        listenerConfig_.put("sourceType", "test")
        config_ = Mock(NetworkListenerConfig)
        config_.getProviderConfigurationFromClassName(_ as String) >> listenerConfig_
        factory_ = Mock(DataStoreFactory)
        factory_.createRasEventLog(_ as AdapterInformation) >> Mock(RasEventLog)
        invApi_ = Mock(HWInvApi)
        factory_.createHWInvApi() >> invApi_
        NodeInformation info = Mock(NodeInformation)
        info.isServiceNodeLocation() >> true
        factory_.createNodeInformation() >> info
        underTest_ = new NetworkListenerSystemActions(Mock(Logger), factory_, Mock(AdapterInformation), config_)
    }

    void cleanup() {
        NetworkDataSourceFactory.unregisterImplementation("test")
    }

//    def "upsertHWInventory"() {
//        when: underTest_.upsertHWInventory(null)
//        then: notThrown Exception
//    }
//
//    def "ingestCanonicalHWInvJson"() {
//        expect: underTest_.ingestCanonicalHWInvJson(null) == null
//    }
//
//    def "toCanonicalHWInvJson"() {
//        expect: underTest_.toCanonicalHWInvJson(null) == null
//    }
//
//    def "getForeignHWInvJson"() {
//        expect: underTest_.getForeignHWInvJson(null) == null
//    }

    def "formatRawMessage"() {
        def json = """{"location":"location","type":"type","value":0.0,"timestamp":"1970-01-01 00:00:00.000000099Z"}"""
        expect: underTest_.formatRawMessage("type", "location", 99L, 0.0) == json
    }

    def "formatAggregateMessage"() {
        def json = """{"average":0.3,"maximum":0.2,"location":"location","type":"type","""
        json += """"minimum":0.1,"timestamp":"1970-01-01 00:00:00.000000099Z"}"""
        expect: underTest_.formatAggregateMessage("type", "location", 99L, 0.1, 0.2, 0.3) == json
    }

    def "formatEventMessage"() {
        def json = """{"location":"location","event":"type","instanceData":"data","""
        json += """"timestamp":"1970-01-01 00:00:00.000000099Z"}"""
        expect: underTest_.formatEventMessage("type", "location", 99L, "data") == json
    }

    def "formatBootMessage"() {
        def json = """{"location":"location","event":"NODE_ONLINE","timestamp":"1970-01-01 00:00:00.000000099Z"}"""
        expect: underTest_.formatBootMessage(BootState.NODE_ONLINE, "location", 99L) == json
    }

    def "initialize"() {
        underTest_.initialize()
        expect: true
    }

    def "logFailedToUpdateNodeBootImageId"() {
        underTest_.logFailedToUpdateNodeBootImageId("location","instanceData")
        expect: true
    }

    def "logFailedToUpdateBootImageInfo"() {
        underTest_.logFailedToUpdateBootImageInfo("instanceData")
        expect: true
    }

//    def "isHWInventoryEmpty"() {
//        expect: underTest_.isHWInventoryEmpty()
//    }
//
//    def "insertHistoricalRecord"() {
//        given: underTest_.insertHistoricalRecord("action", new HWInvLoc())
//        expect: true
//    }
}
