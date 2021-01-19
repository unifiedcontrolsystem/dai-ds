package com.intel.networking.restserver;

import org.junit.Test;

import static org.junit.Assert.*;

public class ResponseExceptionTest {
    @Test
    public void serialID() {
        assertEquals(16386L, ResponseException.serialVersionUID);
    }

    @Test
    public void ctors() {
        new ResponseException();
        new ResponseException(new Exception());
        new ResponseException("Message");
        new ResponseException("Message", new Exception());
    }
}
