package com.intel.networking.restserver;

import org.junit.Test;

import static org.junit.Assert.*;

public class RequestExceptionTest {
    @Test
    public void serialID() {
        assertEquals(16385L, RequestException.serialVersionUID);
    }

    @Test
    public void ctors() {
        new RequestException();
        new RequestException(new Exception());
        new RequestException("Message");
        new RequestException("Message", new Exception());
    }
}
