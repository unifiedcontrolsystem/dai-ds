// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.exceptions;

import org.junit.Test;
import org.junit.Assert;

public class DataStoreExceptionTest {
    @Test
    public void canThrowWithMessage() {
        String message = "Ooops!";
        try {
            throw new DataStoreException(message);
        } catch (DataStoreException ex) {
            Assert.assertEquals(message, ex.getMessage());
        }
    }

    @Test
    public void canThrowWithMessageAndCause() {
        String message = "Ooops!";
        Exception cause = new Exception("Oh, I see...");
        try {
            throw new DataStoreException(message, cause);
        } catch (DataStoreException ex) {
            Assert.assertEquals(message, ex.getMessage());
            Assert.assertEquals(cause, ex.getCause());
        }
    }
}