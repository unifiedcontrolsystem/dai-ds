package com.intel.networking.restclient;

import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.mockito.Mockito.mock;

public class RESTClientTest {
    static class MockRESTClient extends RESTClient {
        MockRESTClient(Logger log) {
            super(log);
        }

        @Override
        protected void doSSERequest(RequestInfo request, ResponseCallback callback, SSEEvent eventsCallback) {
        }

        @Override
        protected BlockingResult doRESTRequest(RequestInfo request) {
            return new BlockingResult(500, null, request);
        }
    }

    private void responseCallback(int code, String responseBody, RequestInfo originalInfo) {
    }

    private void responseCallbackException(int code, String responseBody, RequestInfo originalInfo) {
        throw new RuntimeException("TEST");
    }

    private void sseEvent(String eventType, String event, String id) {
    }

    @Before
    public void setUp() throws Exception {
        client_ = new MockRESTClient(mock(Logger.class));
    }

    @Test(expected = AssertionError.class)
    public void ctorNegative() throws Exception {
        new MockRESTClient(null);
    }

    @Test public void setSSERequestBuilder() {
        client_.setSSERequestBuilder(new DefaultSSERequestBuilder());
    }

    @Test
    public void getRESTRequestBlocking() throws RESTClientException {
        client_.getRESTRequestBlocking(URI.create("https://localhost"));
    }

    @Test
    public void headRESTRequestBlocking() throws RESTClientException {
        client_.headRESTRequestBlocking(URI.create("http://localhost"));
    }

    @Test
    public void postRESTRequestBlocking() throws RESTClientException {
        client_.postRESTRequestBlocking(URI.create("https://localhost"), null);
    }

    @Test
    public void putRESTRequestBlocking() throws RESTClientException {
        client_.putRESTRequestBlocking(URI.create("http://localhost"), null);
    }

    @Test
    public void patchRESTRequestBlocking() throws RESTClientException {
        client_.patchRESTRequestBlocking(URI.create("https://localhost"), null);
    }

    @Test
    public void deleteRESTRequestBlocking() throws RESTClientException {
        client_.deleteRESTRequestBlocking(URI.create("http://localhost"));
    }

    @Test
    public void getRESTRequestAsync() {
        client_.getRESTRequestAsync(URI.create("https://localhost"), null);
        client_.getRESTRequestAsync(URI.create("https://localhost"), this::responseCallback);
    }

    @Test
    public void postRESTRequestAsync() {
        client_.postRESTRequestAsync(URI.create("http://localhost"), null, null);
        client_.postRESTRequestAsync(URI.create("http://localhost"), null, this::responseCallback);
    }

    @Test
    public void putRESTRequestAsync() {
        client_.putRESTRequestAsync(URI.create("http://localhost"), null, null);
        client_.putRESTRequestAsync(URI.create("http://localhost"), null, this::responseCallback);
    }

    @Test
    public void patchRESTRequestAsync() {
        client_.patchRESTRequestAsync(URI.create("https://localhost"), null, null);
        client_.patchRESTRequestAsync(URI.create("https://localhost"), null, this::responseCallback);
    }

    @Test
    public void deleteRESTRequestAsync() {
        client_.deleteRESTRequestAsync(URI.create("http://localhost"), null);
        client_.deleteRESTRequestAsync(URI.create("http://localhost"), this::responseCallbackException);
    }

    private MockRESTClient client_;
}
