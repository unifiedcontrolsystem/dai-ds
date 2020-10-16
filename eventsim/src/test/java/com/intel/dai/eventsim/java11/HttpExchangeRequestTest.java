package com.intel.dai.eventsim.java11;

import com.intel.networking.HttpMethod;
import com.intel.networking.restserver.RequestException;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class HttpExchangeRequestTest {
    @Before
    public void setUp() throws Exception {
        headers_ = new Headers();
        headers_.put("Content-type", new ArrayList<String>() {{ add("application/json"); }});
        exchange_ = mock(HttpExchange.class);
        request_ = new HttpExchangeRequest(exchange_);
        when(exchange_.getRequestBody()).thenReturn(new ByteArrayInputStream("{}\n  \n".getBytes()));
        when(exchange_.getRequestMethod()).thenReturn("POST");
        when(exchange_.getRequestURI()).thenReturn(URI.create("http://localhost:1024/path?query=true"));
        when(exchange_.getRequestHeaders()).thenReturn(headers_);
    }

    @Test
    public void getMethod() throws Exception {
        assertEquals(HttpMethod.POST, request_.getMethod());
    }

    @Test
    public void getPath() throws Exception {
        assertEquals("/path", request_.getPath());
    }

    @Test
    public void getHeaders() throws Exception {
        assertEquals(1, request_.getHeaders().size());
        assertTrue(request_.getHeaders().keySet().contains("Content-type"));
        assertEquals("application/json", request_.getHeaders().get("Content-type"));
    }

    @Test
    public void getBody() throws Exception {
        assertEquals("{}", request_.getBody());
    }

    @Test(expected = RequestException.class)
    public void getBodyNegative() throws Exception {
        InputStream stream = mock(InputStream.class);
        when(stream.readAllBytes()).thenThrow(IOException.class);
        when(exchange_.getRequestBody()).thenReturn(stream);
        request_.getBody();
    }

    @Test
    public void getQuery() throws Exception {
        assertEquals("query=true", request_.getQuery());
    }

    private HttpExchangeRequest request_;
    private HttpExchange exchange_;
    Headers headers_;
}
