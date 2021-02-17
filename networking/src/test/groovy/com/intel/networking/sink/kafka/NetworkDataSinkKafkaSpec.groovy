package com.intel.networking.sink.kafka

import com.intel.logging.Logger
import com.intel.networking.NetworkException
import com.intel.networking.sink.NetworkDataSinkDelegate
import org.apache.kafka.clients.consumer.KafkaConsumer
import spock.lang.Specification

class NetworkDataSinkKafkaSpec extends Specification {
    static class MockNetworkDataSinkKafka extends NetworkDataSinkKafka {
        MockNetworkDataSinkKafka(Logger logger, Map<String, String> args) { super(logger, args) }
        @Override KafkaConsumer<String, String> createKafkaClient() { return source_ }
    }

    static class Callback implements NetworkDataSinkDelegate {
        @Override void processIncomingData(String subject, String payload) {}
    }

    void setup() {
        source_ = Mock(KafkaConsumer)
        underTest_ = new MockNetworkDataSinkKafka(Mock(Logger), args_)
    }

    def "Test ctor Negative 1"() {
        when: new NetworkDataSinkKafka(null, args_)
        then: thrown(AssertionError)
    }

    def "Test ctor Negative 2"() {
        when: new NetworkDataSinkKafka(Mock(Logger), null)
        then: thrown(AssertionError)
    }

    def "Test initialize Negative 1"() {
        args_.clear()
        when: underTest_.initialize()
        then: thrown(NetworkException)
    }

    def "Test initialize Negative 2"() {
        args_.put("bootstrap_servers", null)
        when: underTest_.initialize()
        then: thrown(NetworkException)
    }

    def "Test subject NOP Methods"() {
        underTest_.clearSubjects()
        underTest_.setMonitoringSubject("subject")
        underTest_.setMonitoringSubjects(["subject2", "subject3"])
        underTest_.setConnectionInfo()
        underTest_.setStreamLocationCallback()
        underTest_.setLocationId()
        expect: true
    }

    def "Test setCallbackDelegate"() {
        NetworkDataSinkDelegate callback = Mock(NetworkDataSinkDelegate)
        NetworkDataSinkDelegate newCallback = Mock(NetworkDataSinkDelegate)

        underTest_ = new NetworkDataSinkKafka(Mock(Logger), args_)
        underTest_.callback_ = callback
        when: underTest_.setCallbackDelegate(null)
        then: underTest_.callback_ == callback
        when: underTest_.setCallbackDelegate(newCallback)
        then: underTest_.logger_ != callback
    }

    def "Test isListening"() {
        expect: !underTest_.isListening()
    }

    def "Test setLogger"() {
        Logger log = Mock(Logger)
        Logger newLog = Mock(Logger)

        underTest_ = new NetworkDataSinkKafka(log, args_)
        underTest_.logger_ = log
        when: underTest_.setLogger(null)
        then: underTest_.logger_ == log
        when: underTest_.setLogger(newLog)
        then: underTest_.logger_ != log
    }

    def "Test getProviderName"() {
        expect: underTest_.getProviderName() == "kafka"
    }

    def "Test Kafka Client Callbacks"() {
        underTest_.setCallbackDelegate(callback_)
        underTest_.initialize()
        def isListening = underTest_.isListening()
        expect: isListening == false
    }

    def "Test start and stop Kafka Client"() {
        underTest_.initialize()
        underTest_.setMonitoringSubject("topic")
        underTest_.startListening()
        underTest_.running_.set(true)
        underTest_.stopListening()
        expect: true
    }

    def "Test empty kafka topics to start client"() {
        underTest_.initialize()
        underTest_.clearSubjects()
        when: underTest_.startListening()
        then: thrown(NetworkException)
    }

    static def source_
    def callback_ = new Callback()
    NetworkDataSinkKafka underTest_
    def args_ = [
            "bootstrap.servers": "localhost:9092",
            "group.id": "group_id",
            "schema.registry.url": "http://localhost:8081",
            "topics": "test"
    ]
}
