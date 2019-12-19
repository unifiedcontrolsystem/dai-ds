// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.logging;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class LoggerFactoryTest {
    @Before
    public void setUp() throws Exception {
        LoggerFactory.instance_ = null;
    }

    @Test
    public void getInstance() {
        Logger log = LoggerFactory.getInstance("TestType", "TestName", null);
        assertNotNull(log);
    }

    @Test
    public void getInstanceConsole() {
        Logger log = LoggerFactory.getInstance("TestType", "TestName", "  ");
        assertEquals("LoggerConsole", log.getClass().getSimpleName());
        LoggerFactory.instance_ = null;
        log = LoggerFactory.getInstance("TestType", "TestName", "console");
        assertEquals("LoggerConsole", log.getClass().getSimpleName());

        log = LoggerFactory.getInstance("TestType", "TestName", "jog4j2");
        assertEquals("LoggerConsole", log.getClass().getSimpleName());
    }

    @Test
    public void getInstanceLog4j2() {
        Logger log = LoggerFactory.getInstance("TestType", "TestName", "log4j2");
        assertEquals("LoggerLog4j2", log.getClass().getSimpleName());
    }

    @Test
    public void getInstanceNegative() {
        try {
            LoggerFactory.getInstance("TestType", "TestName", "unknown");
            fail();
        } catch(RuntimeException e) {
            // Pass
        }
    }
}