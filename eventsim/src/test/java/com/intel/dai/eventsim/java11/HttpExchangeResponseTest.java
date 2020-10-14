package com.intel.dai.eventsim.java11;

import com.sun.net.httpserver.HttpExchange;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.Mockito.mock;

public class HttpExchangeResponseTest {
    @Before
    public void setUp() throws Exception {
        response_ = new HttpExchangeResponse(mock(HttpExchange.class));
    }

    @Test
    public void setCode() {
        response_.setCode(200);
    }

    @Test
    public void setBody() {
        response_.setBody("{}");
    }

    @Test
    public void addHeader() {
        response_.addHeader("Content-Type", "application/json");
    }

    @Test
    public void applyChanges() throws Exception {
        response_.applyChanges();
    }

    HttpExchangeResponse response_;
}
