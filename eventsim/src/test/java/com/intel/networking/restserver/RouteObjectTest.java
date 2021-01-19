package com.intel.networking.restserver;

import com.intel.networking.HttpMethod;
import org.junit.Test;

import static org.junit.Assert.*;

public class RouteObjectTest {
    void callback(Request in, Response out) {}

    @Test
    public void allTests() {
        RouteObject route = new RouteObject("/", HttpMethod.DELETE, this::callback, true, null);
        assertEquals("/", route.url);
        assertEquals(HttpMethod.DELETE, route.method);
        assertNotNull(route.handler);
        assertTrue(route.sseSupport);
        assertNull(route.eventTypes);
    }
}
