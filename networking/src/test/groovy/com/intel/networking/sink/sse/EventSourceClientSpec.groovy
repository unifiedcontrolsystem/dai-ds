// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink.sse

import com.intel.authentication.TokenAuthentication
import org.apache.http.HttpResponse
import org.apache.http.ProtocolVersion
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.BasicHttpEntity
import org.apache.http.impl.client.CloseableHttpClient
import org.apache.http.impl.client.HttpClientBuilder
import org.apache.http.message.BasicHttpResponse
import org.apache.http.message.BasicStatusLine
import spock.lang.Specification

class EventSourceClientSpec extends Specification {
    static HttpClientBuilder clientBuilder_
    static CloseableHttpClient client_
    static EventSourceHandlers handlers_
    static TokenAuthentication auth_
    static int counter_ = 1
    static boolean throwExecuteException_ = false
    static boolean useRealState_ = false
    static File streamFile_
    static File streamFileNoId_

    def setupSpec() {
        streamFile_ = new File("./build/tmp/testStream")
        streamFileNoId_ = new File("./build/tmp/testStream2")
        handlers_ = Mock(EventSourceHandlers)
        client_ = Mock(CloseableHttpClient)
        clientBuilder_ = Mock(HttpClientBuilder)
        clientBuilder_.build() >> client_
        client_.execute(_ as HttpUriRequest, _ as ResponseHandler<?>) >> {
            HttpUriRequest request, ResponseHandler<?> handler ->
            if(throwExecuteException_)
                throw new IOException("TEST")
            else
                return null
        }
    }

    def cleanupSpec() {
        streamFile_.delete()
        streamFileNoId_.delete()
    }

    EventSourceClient underTest_
    def setup() {
        streamFile_.text = """:
event: name
id: 0
data: my data
data: more data

event: name
data:

"""
        streamFileNoId_.text = """:
event: name
data: my data
data: more data

event: name
idd2:
data:

"""
        throwExecuteException_ = false
        counter_ = 2
        useRealState_ = false
        auth_ = Mock(TokenAuthentication)
        auth_.getToken() >> "skhjdfgskdjhfgskdhfskhjdfgskdjhfgskdhfskhjdfgskdjhfgskdhfskhjdfgskdjhfgskdhf"
        underTest_ = new MockEventSourceClient("https://localhost/sse/stream1", handlers_, null, auth_)
    }

    def "Test ctor negative"() {
        when: new MockEventSourceClient(URIPATH, CALLBACK, null, null)
        then: thrown(IllegalArgumentException)
        where:
        URIPATH | CALLBACK
        null    | handlers_
        "/s1"   | null
    }

    def "Test run"() {
        underTest_ = new MockEventSourceClient(String.format("http%s://localhost/sse/stream1", SSL?"s":""),
                handlers_, ID, AUTH)
        throwExecuteException_ = false
        counter_ = 4
        if(EXP)
            throwExecuteException_ = true
        underTest_.run()
        expect: underTest_.getState() == EventSourceClient.State.CLOSED
        where:
        ID   | AUTH  | EXP   | SSL   || RESULT
        null | null  | false | true  || EventSourceClient.State.CLOSED
        "0"  | null  | false | true  || EventSourceClient.State.CLOSED
        null | auth_ | false | true  || EventSourceClient.State.CLOSED
        "0"  | auth_ | false | false || EventSourceClient.State.CLOSED
        "0"  | auth_ | true  | true  || EventSourceClient.State.CLOSED
    }

    def "Test stop"() {
        underTest_ = new EventSourceClient("https://localhost/sse/stream1", handlers_, null, null)
        underTest_.state_ = START
        underTest_.stop()
        expect: underTest_.state_ == RESULT
        where:
        START                     || RESULT
        EventSourceClient.State.OPEN    || EventSourceClient.State.CLOSED
        EventSourceClient.State.INITIAL || EventSourceClient.State.INITIAL
    }

    def "Test protected overrides"() {
        underTest_ = new EventSourceClient("/", handlers_, null, null)
        underTest_.safeSleep(1)
        expect: underTest_.state_ == EventSourceClient.State.INITIAL
        and:    underTest_.createClientBuilder() != null
    }

    def "Test handleResponse"() {
        useRealState_ = true
        HttpResponse response = new BasicHttpResponse(new BasicStatusLine(new ProtocolVersion("https", 2,
                0), CODE, "reason"))
        response.addHeader("Content-Type", TYPE)
        BasicHttpEntity entity = new BasicHttpEntity()
        if(FILE != null)
            entity.content = new FileInputStream(FILE)
        else {
            InputStream stream = Mock(InputStream)
            stream.read(_ as byte[]) >> { throw new IOException("TEST") }
            stream.read(_ as byte[],_ as int,_ as int) >> { throw new IOException("TEST") }
            entity.content = stream
        }
        response.setEntity(entity)
        underTest_.handleResponse(response)
        expect: underTest_.getState() == RESULT
        where:
        CODE | TYPE                | FILE            || RESULT
        501  | "text/event-stream" | streamFile_     || EventSourceClient.State.CLOSED
        503  | "text/event-stream" | streamFile_     || EventSourceClient.State.INITIAL
        103  | "text/event-stream" | streamFile_     || EventSourceClient.State.CLOSED
        503  | "unknown"           | streamFile_     || EventSourceClient.State.CLOSED
        200  | "unknown"           | streamFile_     || EventSourceClient.State.CLOSED
        200  | "text/event-stream" | streamFile_     || EventSourceClient.State.OPEN
        200  | "text/event-stream" | streamFileNoId_ || EventSourceClient.State.OPEN
        200  | "text/event-stream" | null            || EventSourceClient.State.OPEN
    }

    static class MockEventSourceClient extends EventSourceClient {
        MockEventSourceClient(String uri, EventSourceHandlers callback, String lastId, TokenAuthentication authentication) {
            super(uri, callback, lastId, authentication)
        }
        @Override protected HttpClientBuilder createClientBuilder() { return clientBuilder_ }
        @Override protected EventSourceClient.State getState() {
            if(useRealState_)
                return super.getState()
            else if(counter_-- <= 0)
                return EventSourceClient.State.CLOSED
            else
                return EventSourceClient.State.OPEN
        }
        @Override protected void safeSleep(long ms) { }
    }
}
