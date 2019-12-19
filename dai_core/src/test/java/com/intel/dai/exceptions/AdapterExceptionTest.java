// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.exceptions;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class AdapterExceptionTest {
    @Test
    public void canThrowWithMessage() {
        String message = ":o!";
        try {
            throw new AdapterException(message);
        } catch (AdapterException ex) {
            assertEquals(message, ex.getMessage());
        }
    }

    @Test
    public void canThrowWithMessageAndCause() {
        String message = ":o!";
        Exception cause = new Exception(":O!!");
        try {
            throw new AdapterException(message, cause);
        } catch (AdapterException ex) {
            assertEquals(message, ex.getMessage());
            assertEquals(cause, ex.getCause());
        }
    }
}