package com.intel.networking;

import org.junit.Test;

import static org.junit.Assert.*;

public class NetworkExceptionTest {
    @Test
    public void ctor() {
        assertNotNull(new NetworkException("test message"));
    }
}