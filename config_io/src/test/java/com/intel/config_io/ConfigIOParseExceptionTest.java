// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.config_io;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigIOParseExceptionTest {
    @Test
    public void ctor() {
        ConfigIOParseException e = new ConfigIOParseException(10);
        assertEquals("While parsing JSON, failed parsing at character position 10!", e.getMessage());
        e = new ConfigIOParseException(new IndexOutOfBoundsException("Oops"));
        assertEquals("Internal JSON parser error: Oops!", e.getMessage());
        e = new ConfigIOParseException("Test Message");
        assertEquals("Test Message", e.getMessage());
        e = new ConfigIOParseException("Test Message", new Exception("Inner Test Message"));
        assertEquals("Test Message", e.getMessage());
        assertEquals("Inner Test Message", e.getCause().getMessage());
    }
}
