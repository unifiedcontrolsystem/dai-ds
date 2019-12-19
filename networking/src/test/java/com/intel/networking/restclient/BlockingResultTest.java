// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient;

import org.junit.Test;

import static org.junit.Assert.*;

public class BlockingResultTest {
    @Test
    public void ctor() {
        BlockingResult result = new BlockingResult(200, null, null);
        assertEquals(200, result.code);
        assertNull(result.responseDocument);
        assertNull(result.requestInfo);
    }
}
