package com.intel.networking.restserver;

import org.junit.Test;

import static org.junit.Assert.*;

public class RESTServerExceptionTest {
    @Test
    public void serialID() {
        assertEquals(16384L, RESTServerException.serialVersionUID);
    }

    @Test
    public void ctors() {
        new RESTServerException();
        new RESTServerException(new Exception());
        new RESTServerException("Message");
        new RESTServerException("Message", new Exception());
    }
}
