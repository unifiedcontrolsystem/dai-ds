// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient.java11;

import com.intel.logging.Logger;
import com.intel.networking.HttpMethod;
import com.intel.networking.restclient.RequestInfo;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;

import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class Java11RESTClientTest {
    static class MockResponse implements HttpResponse<String> {
        MockResponse(HttpRequest request, URI uri, String body, HttpHeaders headers) {
            request_ = request;
            uri_ = uri;
            body_ = body;
            headers_ = headers;
        }

        @Override
        public int statusCode() {
            return 200;
        }

        @Override
        public HttpRequest request() {
            return request_;
        }

        @Override
        public Optional<HttpResponse<String>> previousResponse() {
            return Optional.empty();
        }

        @Override
        public HttpHeaders headers() {
            return headers_;
        }

        @Override
        public String body() {
            return body_;
        }

        @Override
        public Optional<SSLSession> sslSession() {
            return Optional.empty();
        }

        @Override
        public URI uri() {
            return uri_;
        }

        @Override
        public HttpClient.Version version() {
            return HttpClient.Version.HTTP_1_1;
        }
        private HttpRequest request_;
        private URI uri_;
        private String body_;
        private HttpHeaders headers_;
    }
    static class MockJava11RESTClient extends Java11RESTClient {
        MockJava11RESTClient(Logger log) {
            super(log);
            try {
                client_ = mock(HttpClient.class);
                request_ = mock(HttpRequest.class);
                builder_ = mock(HttpRequest.Builder.class);
                response_ = new MockResponse(request_, URI.create("https://localhost:8443/api/test"), "{}", null);
                when(builder_.build()).thenReturn(request_);
                when(client_.send(any(HttpRequest.class),
                        ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenReturn(response_);
            } catch(Exception e) {
                throw new RuntimeException(e);
            }
        }

        @Override protected HttpRequest.Builder createBuilder() { return builder_; }

        HttpRequest request_;
        HttpRequest.Builder builder_;
        HttpResponse<String> response_;
    }

    @Before public void setUp() throws Exception {
        info_ = new RequestInfo(HttpMethod.POST, URI.create("https://localhost:8443/api/test"), "{}");
        client_ = new MockJava11RESTClient(mock(Logger.class));
    }

    private void callback(int code, String responseBody, RequestInfo originalInfo) {
    }

    private void events(String eventType, String event, String id) {
    }

    @Test
    public void doSSERequest() {
        client_.doSSERequest(info_, this::callback, this::events);
    }

    @Test
    public void doSSERequestNegative() throws Exception {
        when(client_.client_.send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(IOException.class);
        client_.doSSERequest(info_, this::callback, this::events);
    }

    @Test
    public void doRESTRequest() {
        client_.doRESTRequest(info_);
        info_ = new RequestInfo(HttpMethod.POST, URI.create("https://localhost:8443/api/test"), null);
        client_.doRESTRequest(info_);
    }

    @Test
    public void doRESTRequestNegative() throws Exception {
        when(client_.client_.send(any(HttpRequest.class),
                ArgumentMatchers.<HttpResponse.BodyHandler<String>>any())).thenThrow(IOException.class);
        client_.doRESTRequest(info_);
    }

    @Test
    public void makeException() {
        StringBuilder builder = new StringBuilder();
        client_.makeException(new RuntimeException(new IOException()), builder);
    }

    private MockJava11RESTClient client_;
    private RequestInfo info_;
}
