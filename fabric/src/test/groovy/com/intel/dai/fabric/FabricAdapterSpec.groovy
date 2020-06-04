// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.fabric

import com.intel.config_io.ConfigIOParseException
import com.intel.dai.IAdapter
import com.intel.dai.dsapi.DataStoreFactory
import com.intel.dai.dsapi.RasEventLog
import com.intel.dai.dsapi.StoreTelemetry
import com.intel.dai.dsapi.WorkQueue
import com.intel.dai.exceptions.AdapterException
import com.intel.logging.Logger
import com.intel.networking.sink.NetworkDataSink
import com.intel.networking.sink.NetworkDataSinkDelegate
import com.intel.networking.sink.NetworkDataSinkFactory
import com.intel.networking.sink.restsse.NetworkDataSinkSSE
import com.intel.networking.source.NetworkDataSource
import com.intel.networking.source.NetworkDataSourceFactory
import com.intel.networking.source.rabbitmq.NetworkDataSourceRabbitMQ
import com.sun.org.apache.xalan.internal.xsltc.compiler.util.ResultTreeType
import spock.lang.Specification

import java.time.Instant

class FabricAdapterSpec extends Specification {
    static class TestProvider extends FabricAdapter {
        TestProvider(String servers, Logger logger, DataStoreFactory factory, IAdapter adapter) {
            super(servers, logger, factory, adapter)
        }
        @Override protected void processRawMessage(String subject, String message) { }
        @Override protected void processConfigItems(Map<String, String> config) { super.processConfigItems(config) }
    }


    static class TestRabbitMQ implements NetworkDataSource {
        TestRabbitMQ(Logger logger, Map<String,String> config) {}
        @Override void initialize() throws Exception {}
        @Override void connect(String info) { }
        @Override void setLogger(Logger logger) { }
        @Override String getProviderName() { return "rabbitmq" }
        @Override boolean sendMessage(String subject, String message) { return true }
        @Override void close() throws IOException { }
    }

    static class TestSSE implements NetworkDataSink {
        TestSSE(Logger logger, Map<String,String> config) {}
        @Override void initialize() throws Exception { }
        @Override void clearSubjects() { }
        @Override void setMonitoringSubject(String subject) { }
        @Override void setMonitoringSubjects(Collection<String> subjects) { }
        @Override void setConnectionInfo(String info) { }
        @Override void setCallbackDelegate(NetworkDataSinkDelegate delegate) { }
        @Override void startListening() { }
        @Override void stopListening() { }
        @Override boolean isListening() { return true }
        @Override void setLogger(Logger logger) { }
        @Override String getProviderName() { return "sse" }
    }

    def configFile_ = new File("./build/tmp/FabricAdapterSpec.json")
    def config_
    def workItem_
    def workQueue_
    def adapter_
    def rasEvents_
    def params_
    FabricAdapter underTest_
    def setup() {
        def logger = Mock(Logger)
        def factory = Mock(DataStoreFactory)
        rasEvents_ = Mock(RasEventLog)
        adapter_ = Mock(IAdapter)

        NetworkDataSinkFactory.unregisterImplementation("sse")
        NetworkDataSinkFactory.registerNewImplementation("sse", TestSSE.class)
        NetworkDataSourceFactory.unregisterImplementation("rabbitmq")
        NetworkDataSourceFactory.registerNewImplementation("rabbitmq", TestRabbitMQ.class)

        params_ = ["connectAddress":"127.0.0.1",
                   "connectPort":"12345",
                   "urlPath":"/api/stream"]
        config_ = ["raw_topic":"ucs_fabric_raw_telemetry",
                   "aggregated_topic":"ucs_fabric_aggregated_telemetry",
                   "events_topic":"ucs_fabric_events"]

        workItem_ = "HandleInputFromExternalComponent"

        workQueue_ = Mock(WorkQueue)
        workQueue_.grabNextAvailWorkItem(_ as String) >>> [ true, false ]
        workQueue_.workToBeDone() >> { return workItem_ }
        workQueue_.getClientParameters() >> params_
        workQueue_.amtTimeToWait() >>> [0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1]

        adapter_.snLctn() >> "location"
        adapter_.adapterShuttingDown() >>> [ false, false, false, false, true, true ]
        adapter_.adapterName() >> "name"
        adapter_.setUpAdapter(_ as String, _ as String) >> workQueue_

        factory.createRasEventLog(_ as IAdapter) >> rasEvents_
        factory.createStoreTelemetry() >> Mock(StoreTelemetry)

        configFile_.createNewFile()
        configFile_.text = """{"jobid":"3453425","storeBlacklist":"somename"}"""

        underTest_ = new TestProvider("127.0.0.1", logger, factory, adapter_)
        underTest_.config_ = underTest_.buildConfig(configFile_)
    }

    def cleanup() {
        configFile_.delete()
        NetworkDataSinkFactory.unregisterImplementation("sse")
        NetworkDataSinkFactory.registerNewImplementation("sse", NetworkDataSinkSSE.class)
        NetworkDataSourceFactory.unregisterImplementation("rabbitmq")
        NetworkDataSourceFactory.registerNewImplementation("rabbitmq", NetworkDataSourceRabbitMQ.class)
    }

    def "Test MainProcessingFlow"() {
        given:
        workItem_ = ITEM
        underTest_.mainProcessingFlow(underTest_.config_)
        expect: RESULT
        where:
        ITEM                               || RESULT
        "Unknown"                          || true
        "HandleInputFromExternalComponent" || true
        "HandleInputFromExternalComponent" || true
    }

    def "Test usTimestamp"() {
        Instant inst = Instant.now()
        def now = (inst.getEpochSecond() * 1_000_000L) + (inst.getNano() / 1_000L)
        expect: underTest_.usTimestamp() >= now
    }

    def "Test makeInstanceData"() {
        def result = underTest_.makeInstanceData("error", "data")
        expect: result == "Name='name';Type='FABRIC';Error='error';Data='data'"
    }

    def "Test consumeWorkItemParameters"() {
        given:
        params_.clear()
        params_.put("connectAddress", ADDR)
        params_.put("connectPort", PORT)
        params_.put("urlPath", URLPATH)
        config_.put("urlPath", URLPATH)
        underTest_.mainProcessingFlow(config_)

        when:
        underTest_.consumeWorkItemParameters()

        then:
        thrown(AdapterException)

        where:
        ADDR        | PORT     | URLPATH
        null        | "1254"   | "/api"
        "127.0.0.1" | null     | "/api"
        "127.0.0.1" | "  "     | "/api"
        "127.0.0.1" | "12b54"  | "/api"
        "127.0.0.1" | "1254"   | null
        "127.0.0.1" | "1254"   | "   "
    }

    def "Test Publishing"() {
        underTest_.mainProcessingFlow(config_)
        underTest_.publishRawData("message")
        underTest_.publishAggregatedData("message")
        underTest_.publishEventData("message")
        expect: true
    }

    def "Test BuildConfig"() {
        def map = underTest_.buildConfig(configFile_)
        expect: map.containsKey("jobid")
        and:    map.containsKey("aggregateEnabled")
    }

    def "Test BuildConfig No File"() {
        given: configFile_ = new File(configFile_.toString() + ".nofile")
        when: underTest_.buildConfig(configFile_)
        then: thrown(IOException)
    }

    def "Test BuildConfig Bad JSON"() {
        given: configFile_.text = """[}"""
        when:  underTest_.buildConfig(configFile_)
        then:  thrown(ConfigIOParseException)
    }

    def "Test inBlacklist"() {
        expect: underTest_.inBlacklist(NAME) == RESULT
        where:
        NAME        || RESULT
        "somename"  || true
        "othername" || false
    }

    def "Test logError"() {
        underTest_.rasEventLogging_ = rasEvents_
        underTest_.adapter_ = adapter_
        underTest_.workQueue_ = workQueue_
        underTest_.logError("message")
        expect: true
    }
}
