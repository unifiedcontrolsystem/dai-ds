package com.intel.networking.restserver;

import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;

import static org.junit.Assert.*;

public class ResponseTranslatorTest {
    static class MockTranslator extends ResponseTranslator {
        @Override
        public PropertyMap makeError(int code, String message, URI uri, String method, Throwable cause) {
            return new PropertyMap();
        }

        @Override
        public PropertyMap makeResponse(PropertyDocument payload) {
            return new PropertyMap();
        }
    }

    @Before
    public void setUp() throws Exception {
        translator_ = new MockTranslator();
    }

    @Test
    public void toString1() {
        assertEquals("{}", translator_.toString(new PropertyMap()));
    }

    @Test
    public void buildExceptionTrace() {
        Exception compound = new Exception("Outer", new RuntimeException("Inner"));
        String trace = translator_.buildExceptionTrace(compound);
        assertTrue(trace.startsWith("Exception: Exception: Outer\n"));
        assertTrue(trace.contains("\nCause by: RuntimeException: Inner\n"));
    }

    ResponseTranslator translator_;
}
