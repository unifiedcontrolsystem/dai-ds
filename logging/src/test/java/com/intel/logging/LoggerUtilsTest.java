// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.logging;

import org.junit.Test;

import static org.junit.Assert.*;

public class LoggerUtilsTest {
    @Test
    public void getLoggingInfo() {
        LoggerUtils.getLoggingInfo(LoggerUtils.DEBUG, 0, "testName", "testType");
        try {
            LoggerUtils.getLoggingInfo(-1, 0, "testName", "testType");
            fail();
        } catch(IndexOutOfBoundsException e) { /* PASS */ }
        try {
            LoggerUtils.getLoggingInfo(10, 0, "testName", "testType");
            fail();
        } catch(IndexOutOfBoundsException e) { /* PASS */ }
    }
}
