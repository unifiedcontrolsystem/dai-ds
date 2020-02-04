package com.intel.networking.restclient;

import com.intel.networking.HttpMethod;
import com.intel.properties.PropertyMap;
import org.junit.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class RequestInfoTest {
    @Test
    public void ctor() {
        Map<String,String> headers = new HashMap<>() {{
           put("X-Unknown", "value");
        }};
        RequestInfo info = new RequestInfo(HttpMethod.GET, URI.create("http://localhost"), null, headers);
        assertEquals(HttpMethod.GET, info.method());
        assertEquals("http://localhost", info.uri().toString());
        assertEquals(1, info.headers().size());
        assertNull(info.body());
    }

    @Test
    public void toStringTest() {
        Map<String,String> headers = new HashMap<>() {{
            put("X-Unknown", "value");
        }};
        RequestInfo info = new RequestInfo(HttpMethod.GET, URI.create("http://localhost"), "Body Text.", headers);
        assertEquals("com.intel.networking.restclient.RequestInfo instance:\n" +
                "  method=GET\n" +
                "  uri=http://localhost\n" +
                "  body=Body Text.\n" +
                "  headers:\n" +
                "    X-Unknown: value\n", info.toString());
    }
}
