// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.partitioned_monitor;

import org.junit.Test;

import static org.junit.Assert.*;

public class DataTransformerExceptionTest {
    @Test
    public void allConstructors() {
        new DataTransformerException();
        new DataTransformerException("Message");
        new DataTransformerException(new Exception("CAUSE"));
        new DataTransformerException("Message", new Exception("CAUSE"));
    }
}
