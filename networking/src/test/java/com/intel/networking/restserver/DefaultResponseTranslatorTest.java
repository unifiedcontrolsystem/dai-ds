package com.intel.networking.restserver;

import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

public class DefaultResponseTranslatorTest {
    @Test
    public void makeError() {
        PropertyMap map = translator_.makeError(404, "Message", URI.create("http://localhost:1024/path"), "post", null);
        assertEquals(404, map.getIntOrDefault("code", 0));
        assertEquals("error", map.getStringOrDefault("status", ""));
        assertEquals("Message", map.getStringOrDefault("error", ""));
        assertEquals("POST", map.getStringOrDefault("method", ""));
        assertEquals("/path", map.getStringOrDefault("uri", ""));
        assertFalse(map.containsKey("trace"));
    }
    @Test
    public void makeErrorWithCause() {
        PropertyMap map = translator_.makeError(404, "Message", URI.create("http://localhost:1024/path"), "post",
                new RuntimeException("TEST"));
        assertTrue(map.containsKey("trace"));
    }

    @Test
    public void makeResponse() {
        PropertyMap payload = new PropertyMap();
        PropertyMap map = translator_.makeResponse(payload);
        assertEquals("ok", map.getStringOrDefault("status", ""));
        assertEquals(payload, map.getMapOrDefault("payload", null));
    }

    private ResponseTranslator translator_ = new DefaultResponseTranslator();
}