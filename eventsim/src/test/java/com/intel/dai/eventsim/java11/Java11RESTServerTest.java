package com.intel.dai.eventsim.java11;

import com.intel.logging.Logger;
import com.intel.networking.HttpMethod;
import com.intel.networking.restserver.*;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;

import static org.mockito.Mockito.*;

public class Java11RESTServerTest {
    static class MockJava11RESTServer extends Java11RESTServer {
        public MockJava11RESTServer(Logger log) throws RESTServerException { super(log); }

        @Override protected HttpServer serverCreate() throws IOException {
            if(startException_) throw new IOException("Testing...");
            return httpServer_;
        }

        @Override protected SSEConnectionManager managerCreate() {
            return manager_;
        }

        HttpServer httpServer_ = mock(HttpServer.class);
        SSEConnectionManager manager_ = mock(SSEConnectionManager.class);
        boolean startException_ = false;
    }

    private void callback(Request request, Response response) {
        response.addHeader("Header", "Value");
        if(callbackException)
            throw new RuntimeException("User Exception test...");
    }

    private void simulateIncomingMessage(HttpExchange exchange, String path, HttpMethod method, String json)
            throws Exception {
        when(exchange.getResponseHeaders()).thenReturn(mock(Headers.class));
        when(exchange.getRequestHeaders()).thenReturn(mock(Headers.class));
        when(exchange.getRequestMethod()).thenReturn(method.toString());
        when(exchange.getRequestURI()).thenReturn(URI.create("http://localhost:1024" + path));
        OutputStream outStream = mock(OutputStream.class);
        if(throwIOException_)
            doThrow(IOException.class).when(outStream).write(any(byte[].class));
        when(exchange.getResponseBody()).thenReturn(outStream);
        if(json != null) {
            if(throwHttpRequestException_) {
                InputStream inStream = mock(InputStream.class);
                when(exchange.getRequestBody()).thenReturn(inStream);
                doThrow(IOException.class).when(inStream).readAllBytes();
            } else {
                ByteArrayInputStream inStream = new ByteArrayInputStream(json.getBytes());
                when(exchange.getRequestBody()).thenReturn(inStream);
            }
        }
        server_.urlHandler(exchange);
    }

    @Before public void setUp() throws Exception {
        throwIOException_ = false;
        throwHttpRequestException_ = false;
        callbackException = false;
        server_ = new MockJava11RESTServer(mock(Logger.class));
    }

    @Test public void ssePublish() throws Exception {
        server_.start();
        server_.ssePublish("type", "data", "1");
    }

    @Test(expected = RESTServerException.class)
    public void ssePublishNegative() throws Exception {
        server_.ssePublish("type", "data", null);
    }

    @Test public void startServer() throws Exception {
        server_.start();
    }

    @Test(expected = RESTServerException.class)
    public void startServerNegative() throws Exception {
        server_.startException_ = true;
        server_.start();
    }

    @Test(expected = RESTServerException.class)
    public void startServerNegativeHttpServerFail() throws Exception {
        doThrow(IOException.class).when(server_.httpServer_).bind(any(InetSocketAddress.class), anyInt());
        server_.start();
    }

    @Test(expected = RESTServerException.class)
    public void startServerNegativeFailed() throws Exception {
        server_.startException_ = true;
        try {
            server_.start();
        } catch(RESTServerException e) { /* Ignore this exception, but not the next. */ }
        server_.startException_ = false;
        server_.start();
    }

    @Test public void stopServer() throws Exception {
        server_.start();
        server_.stop();
        server_.stop();
    }

    @Test public void addInternalRoute() throws Exception {
        server_.addHandler("/path", HttpMethod.GET, this::callback);
        server_.addSSEHandler("/events", new ArrayList<String>() {{ add("type2"); }});
    }

    @Test public void removeInternalRoute() throws Exception {
        server_.addHandler("/path", HttpMethod.GET, this::callback);
        server_.removeHandler("/path", HttpMethod.GET);
    }

    @Test public void incomingMessage1() throws Exception {
        server_.addHandler("/path", HttpMethod.GET, this::callback);
        HttpExchange exchange = mock(HttpExchange.class);
        simulateIncomingMessage(exchange, "/path", HttpMethod.GET, null);
        simulateIncomingMessage(exchange, "/path2", HttpMethod.GET, null);
        simulateIncomingMessage(exchange, "/path", HttpMethod.PUT, null);
    }

    @Test public void incomingMessage2() throws Exception {
        server_.addSSEHandler("/events", new ArrayList<String>() {{ add("type2"); }});
        HttpExchange exchange = mock(HttpExchange.class);
        simulateIncomingMessage(exchange, "/events", HttpMethod.POST, "{\"subjects\":[\"type2\"]}");
    }

    @Test public void incomingMessage3() throws Exception {
        server_.addSSEHandler("/events", new ArrayList<String>() {{ add("type2"); }});
        HttpExchange exchange = mock(HttpExchange.class);
        throwIOException_ = true;
        simulateIncomingMessage(exchange, "/events", HttpMethod.POST, "{\"subjects\":[\"type2\"]}");
    }

    @Test public void incomingMessage4() throws Exception {
        server_.addSSEHandler("/events", new ArrayList<String>() {{ add("type2"); }});
        HttpExchange exchange = mock(HttpExchange.class);
        throwHttpRequestException_ = true;
        simulateIncomingMessage(exchange, "/events", HttpMethod.POST, "{\"subjects\":[\"type2\"]}");
    }

    @Test public void incomingMessage5() throws Exception {
        server_.addHandler("/api", HttpMethod.GET, this::callback);
        HttpExchange exchange = mock(HttpExchange.class);
        throwIOException_ = true;
        simulateIncomingMessage(exchange, "/api", HttpMethod.GET, null);
    }

    @Test public void incomingMessage6() throws Exception {
        server_.addHandler("/api", HttpMethod.GET, this::callback);
        HttpExchange exchange = mock(HttpExchange.class);
        callbackException = true;
        simulateIncomingMessage(exchange, "/api", HttpMethod.GET, null);
    }

    private MockJava11RESTServer server_;
    private boolean throwIOException_ = false;
    private boolean throwHttpRequestException_ = false;
    private boolean callbackException = false;
}
