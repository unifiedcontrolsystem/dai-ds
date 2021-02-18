package com.intel.networking.source.kafka

import com.intel.logging.Logger
import com.intel.networking.NetworkException
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import spock.lang.Specification

class NetworkDataSourceKafkaSpec extends Specification {

    void setup() {
        source_ = Mock(KafkaProducer)
        log_ = Mock(Logger)
        underTest_ = new NetworkDataSourceKafka(log_, args_)
    }

    def "Test ctor Negative 1"() {
        when: new NetworkDataSourceKafka(null, args_)
        then: thrown(AssertionError)
        when: new NetworkDataSourceKafka(log_, null)
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

    def "Test initialize Negative 3"() {
        args_.put("bootstrap_servers", "")
        when: underTest_.initialize()
        then: thrown(NetworkException)
    }

    def "Test setLogger"() {
        Logger newLog = Mock(Logger)

        underTest_ = new NetworkDataSourceKafka(log_, args_)
        when: underTest_.setLogger(null)
        then: underTest_.logger_ == log_
        when: underTest_.setLogger(newLog)
        then: underTest_.logger_ != log_
    }

    def "Test getProviderName"() {
        expect: underTest_.getProviderName() == "kafka"
    }

    def "Test start and stop Kafka Client"() {
        underTest_ = new NetworkDataSourceKafka(log_, args_)
        underTest_.initialize()
        underTest_.connect() >> {}
        underTest_.kafkaProducer_ = source_
        source_.send(any() as ProducerRecord) >> {}
        when: underTest_.sendMessage("subject", "message")
        then: underTest_.kafkaProducer_ != null
        when: underTest_.close()
        then: underTest_.kafkaProducer_ == null
    }

    def "Test subject NOP Methods"() {
        underTest_.connect("")
        underTest_.close()
        expect: true
    }

    private static KafkaProducer source_
    private NetworkDataSourceKafka underTest_
    private Logger log_
    def args_ = [
            "bootstrap.servers": "localhost:9092",
            "schema.registry.url": "http://localhost:8081"
    ]
}
