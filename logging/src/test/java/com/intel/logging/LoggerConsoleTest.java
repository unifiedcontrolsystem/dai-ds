// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.logging;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.junit.Assert.*;

public class LoggerConsoleTest {
    @Before
    public void setUp() throws Exception {
        stderr_ = System.err;
        stdout_ = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        System.setErr(new PrintStream(new ByteArrayOutputStream()));
        System.setProperty("daiLoggingLevel", "DEBUG");
        log_ = new LoggerConsole("TestType", "TestName");
    }

    @After
    public void tearDown() {
        System.setOut(stdout_);
        System.setErr(stderr_);
    }

    @Test
    public void error() {
        log_.error("test error: %s", "test");
        log_.error("test error: test");
    }

    @Test
    public void warn() {
        log_.warn("test warn: %s", "test");
        log_.warn("test warn: test");
    }

    @Test
    public void info() {
        log_.info("test info: %s", "test");
        log_.info("test info: test");
    }

    @Test
    public void debug() {
        log_.debug("test debug: %s", "test");
        log_.debug("test debug: test");
    }

    @Test
    public void exception1() {
        log_.exception(new RuntimeException("exception"), "test exception: %s", "test");
        log_.exception(new RuntimeException("exception"), "test exception: test");
    }

    @Test
    public void exception2() {
        log_.exception(new RuntimeException("exception"));
    }

    @Test
    public void nestedExceptions() {
        Throwable nested = new RuntimeException("nested exception");
        log_.exception(new RuntimeException("exception", nested));
    }

    @Test
    public void getLevel() {
        LoggerConsole logger = new LoggerConsole("testType", "testName");
        System.setProperty("daiLoggingLevel", "ERROR");
        assertEquals(LoggerUtils.ERROR, logger.getLevel());

        logger = new LoggerConsole("testType", "testName");
        System.setProperty("daiLoggingLevel", "WARN");
        assertEquals(LoggerUtils.WARN, logger.getLevel());

        logger = new LoggerConsole("testType", "testName");
        System.setProperty("daiLoggingLevel", "INFO");
        assertEquals(LoggerUtils.INFO, logger.getLevel());

        logger = new LoggerConsole("testType", "testName");
        System.setProperty("daiLoggingLevel", "DEBUG");
        assertEquals(LoggerUtils.DEBUG, logger.getLevel());

        logger = new LoggerConsole("testType", "testName");
        System.setProperty("daiLoggingLevel", "");
        assertEquals(LoggerUtils.INFO, logger.getLevel());
    }

    private Logger log_;
    private PrintStream stdout_;
    private PrintStream stderr_;
}
