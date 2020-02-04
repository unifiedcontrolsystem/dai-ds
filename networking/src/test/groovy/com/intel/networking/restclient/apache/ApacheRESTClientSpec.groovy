package com.intel.networking.restclient.apache

import com.intel.logging.Logger
import com.intel.networking.HttpMethod
import com.intel.networking.restclient.RequestInfo
import com.intel.networking.restclient.ResponseCallback
import com.intel.networking.restclient.SSEEvent
import org.apache.http.HttpEntity
import org.apache.http.StatusLine
import org.apache.http.client.methods.CloseableHttpResponse
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.impl.client.CloseableHttpClient
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class ApacheRESTClientSpec extends Specification implements ResponseCallback, SSEEvent {
    CloseableHttpClient client_
    CloseableHttpResponse response_
    HttpEntity entity_
    InputStream content_
    StatusLine status_
    static URI uri_ = URI.create("http://127.0.0.1:8080/api")
    static Map<String,String> headers_ = new HashMap() {{
        put("X-Unknown", "Testing")
    }}

    def underTest_
    void setup() {
        underTest_ = new ApacheRESTClient(Mock(Logger))
        status_ = GroovyMock(StatusLine)
        status_.getStatusCode() >> { 200 }
        content_ = Mock(InputStream)
        entity_ = GroovyMock(HttpEntity)
        entity_.getContent() >> { content_ }
        response_ = GroovyMock(CloseableHttpResponse)
        response_.getEntity() >> { entity_ }
        response_.getStatusLine() >> { status_ }
        client_ = GroovyMock(CloseableHttpClient)
        client_.execute(_ as HttpUriRequest) >> { response_ }
        underTest_.client_ = client_
    }

    @Override
    void responseCallback(int code, String responseBody, RequestInfo originalInfo) {
    }

    @Override
    void event(String eventType, String event, String id) {
    }

    def "Test DoSSERequest"() {
        underTest_.doSSERequest(new RequestInfo(HttpMethod.GET,
                URI.create("http://127.0.0.1:8080/api"), null), this, this)
        expect: true
    }

    def "Test DoRESTRequest"() {
        underTest_.doRESTRequest(new RequestInfo(HttpMethod.GET,
                URI.create("http://127.0.0.1:8080/api"), null));
        expect: true
    }

    def "Test MakeException"() {
        StringBuilder builder = new StringBuilder()
        underTest_.makeException(new Exception("testing...", new Exception("cause")), builder)
        expect: true
    }

    def "Test StreamToBody"() {
        ByteArrayInputStream stream = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8))
        expect: underTest_.streamToBody(stream) == "{}"
    }

    def "Test makeRequest"() {
        given:
        underTest_.makeRequest(INFO)

        expect: EXPECTED

        where:
        INFO                                                      | EXPECTED
        new RequestInfo(HttpMethod.HEAD, uri_, "")                | true
        new RequestInfo(HttpMethod.POST, uri_, "{}")              | true
        new RequestInfo(HttpMethod.DELETE, uri_, "", headers_)    | true
        new RequestInfo(HttpMethod.PUT, uri_, "{}")               | true
        new RequestInfo(HttpMethod.OPTIONS, uri_, null, headers_) | true
        new RequestInfo(HttpMethod.PATCH, uri_, "{}")             | true
    }
}
