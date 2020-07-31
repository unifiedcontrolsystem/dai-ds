// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink.sse

import com.intel.logging.Logger
import com.intel.networking.NetworkException
import com.intel.networking.sink.NetworkDataSinkDelegate
import com.intel.networking.sink.StreamLocationHandler
import spock.lang.Specification

class NetworkDataSinkEventSourceSpec extends Specification {
    static class MockNetworkDataSinkEventSource extends NetworkDataSinkEventSource {
        MockNetworkDataSinkEventSource(Logger logger, Map<String, String> args) { super(logger, args) }
        @Override EventSourceClient createEventSource() { return source_ }
    }

    static class Callback implements NetworkDataSinkDelegate {
        @Override void processIncomingData(String subject, String payload) {}
    }
    static class Callback2 implements StreamLocationHandler {
        @Override void newStreamLocation(String streamLocation, String urlPath, String streamId) {}
    }

    static def source_
    def args_ = [
            "fullUrl": "http://127.0.0.1:12345/api/stream?stream_id=testing"
    ]
    def callback_ = new Callback()
    def callback2_ = new Callback2()

    NetworkDataSinkEventSource underTest_
    void setup() {
        source_ = Mock(EventSourceClient)
        underTest_ = new MockNetworkDataSinkEventSource(Mock(Logger), args_)
    }

    def "Test ctor Negative 1"() {
        when: new NetworkDataSinkEventSource(null, args_)
        then: thrown(AssertionError)
    }

    def "Test ctor Negative 2"() {
        when: new NetworkDataSinkEventSource(Mock(Logger), null)
        then: thrown(AssertionError)
    }

    def "Test initialize"() {
        underTest_.initialize()
        expect: true
    }

    def "Test initialize Negative 1"() {
        args_.clear()
        when: underTest_.initialize()
        then: thrown(NetworkException)
    }

    def "Test initialize Negative 2"() {
        args_.put("fullUrl", null)
        when: underTest_.initialize()
        then: thrown(NetworkException)
    }

    def "Test initialize Negative 3"() {
        args_.put("fullUrl", "://")
        when: underTest_.initialize()
        then: thrown(NetworkException)
    }

    def "Test createTokenProvider"() {
        args_.put("tokenAuthProvider", "com.intel.authentication.KeycloakTokenAuthentication")
        args_.put("tokenServer", "https://127.0.0.1:12346/getToken")
        underTest_.initialize()
        expect: true
    }

    def "Test createTokenProvider Negative"() {
        args_.put("tokenAuthProvider", "com.intel.authentication.NoTokenAuthentication")
        args_.put("tokenServer", "https://127.0.0.1:12346/getToken")
        when: underTest_.initialize()
        then: thrown(NetworkException)
    }

    def "Test subject NOP Methods"() {
        underTest_.clearSubjects()
        underTest_.setMonitoringSubject("subject")
        underTest_.setMonitoringSubjects(["subject2", "subject3"])
        expect: true
    }

    def "Test setConnectionInfo"() {
        underTest_.setConnectionInfo("http://127.0.0.1:12345/api/stream?stream_id=testing")
        expect: true
    }

    def "Test setConnectionInfo Negative"() {
        when: underTest_.setConnectionInfo("://")
        then: thrown(IllegalArgumentException)
    }

    def "Test setCallbackDelegate"() {
        underTest_.setCallbackDelegate(null)
        underTest_.setCallbackDelegate(callback_)
        expect: true
    }

    def "Test isListening"() {
        expect: !underTest_.isListening()
    }

    def "Test setLogger"() {
        underTest_.setLogger(null)
        underTest_.setLogger(Mock(Logger))
        expect: true
    }

    def "Test getProviderName"() {
        expect: underTest_.getProviderName() == "eventSource"
    }

    def "Test setStreamLocationCallback"() {
        underTest_.setStreamLocationCallback(null)
        expect: true
    }

    def "Test setLocationId"() {
        underTest_.setLocationId(null)
        expect: true
    }

    def "Test EventSource Callbacks"() {
        underTest_.setCallbackDelegate(callback_)
        underTest_.setStreamLocationCallback(callback2_)
        underTest_.initialize()
        underTest_.onConnecting()
        underTest_.onOpen()
        underTest_.onDebug("Testing")
        underTest_.onMessage("event", "{}", "id")
        def pt1 = underTest_.isListening()
        underTest_.onError(new Exception("Testing"))
        underTest_.onClosed()
        expect: !underTest_.isListening()
        and:    pt1
    }

    def "Test start and stop eventsource"() {
        underTest_.initialize()
        underTest_.startListening()
        underTest_.onOpen()
        underTest_.stopListening()
        expect: true
    }
}
