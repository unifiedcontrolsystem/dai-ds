package com.intel.networking.restclient;

import org.junit.Test;

import static org.junit.Assert.*;

public class RESTClientExceptionTest {
    @Test
    public void allTests() {
        assertEquals(16387, RESTClientException.serialVersionUID);
        assertNull(new RESTClientException().getMessage());
        assertEquals("", new RESTClientException("").getMessage());
        assertNotNull(new RESTClientException(new Exception()).getCause());
        Exception e = new RESTClientException("", new Exception());
        assertEquals("", e.getMessage());
        assertNotNull(e.getCause());
    }
}
