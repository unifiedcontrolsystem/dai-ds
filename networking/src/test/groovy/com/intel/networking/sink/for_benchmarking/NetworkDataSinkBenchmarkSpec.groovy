package com.intel.networking.sink.for_benchmarking

import com.intel.logging.Logger
import com.intel.networking.sink.NetworkDataSinkDelegate
import com.intel.networking.sink.NetworkDataSinkFactory
import spock.lang.Specification

import java.time.Instant

class NetworkDataSinkBenchmarkSpec extends Specification implements NetworkDataSinkDelegate {
    def log_ = Mock(Logger)
    def filename_ = "/tmp/TestData.txt"
    def file_ = new File(filename_)
    def text_ = """{"event-type":"ec_marker","location":"x0c0s26b0n0","timestamp":"2019-10-31 18:05:51.143198Z","message":"Test RAS Event"}
{"event-type":"ec_marker","location":"x0c0s26b0n0","timestamp":"2019-10-31 18:05:51.143198Z","message":"Test RAS Event"}
{"event-type":"ec_marker","location":"x0c0s26b0n0","timestamp":"2019-10-31 18:05:51.143198Z","message":"Test RAS Event"}
"""

    def underTest_
    void setup() {
        file_.setText(text_)
        file_.deleteOnExit()
        def args = new HashMap<String,String>();
        args.put("initialDelaySeconds", "0")
        args.put("rawDataFileName", filename_)
        args.put("publishedSubject", "test")
        underTest_ = NetworkDataSinkFactory.createInstance(log_, "benchmark", args)
        underTest_.setCallbackDelegate(this)
    }

    @Override
    void processIncomingData(String subject, String payload) {}

    def "Test ClearSubjects"() {
        underTest_.clearSubjects()
        expect: true
    }

    def "Test SetMonitoringSubject"() {
        underTest_.setMonitoringSubject(null)
        expect: true
    }

    def "Test SetMonitoringSubjects"() {
        underTest_.setMonitoringSubjects(null)
        expect: true
    }

    def "Test SetConnectionInfo"() {
        underTest_.setConnectionInfo(null)
        expect: true
    }

    def "Test Start And Stop Listening"() {
        underTest_.stopListening()
        def off = underTest_.isListening()
        underTest_.startListening()
        Thread.sleep(1100)
        underTest_.startListening()
        file_.setLastModified(Instant.now().toEpochMilli())
        def on = underTest_.isListening()
        Thread.sleep(50)
        underTest_.stopListening()
        expect: !off
        and: on
        and: !underTest_.isListening()
    }

    def "Test SetLogger"() {
        underTest_.setLogger(Mock(Logger))
        expect: true
    }

    def "Test GetProviderName"() {
        expect: underTest_.getProviderName() == "benchmark"
    }
}
