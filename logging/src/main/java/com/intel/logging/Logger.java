// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.logging;

/**
 * Interface for all to use to log to the logger.
 */
public interface Logger {
    /**
     * Optionally Initialize the logger.
     */
    void initialize();

    /**
     * Log a fatal error.
     *
     * @param fmt Format for error message.
     * @param args Arguments for format string.
     */
    void fatal(String fmt, Object ...args);

    /**
     * Log an error.
     *
     * @param msg The string to log.
     */
    void error(String msg);

    /**
     * Log an error.
     *
     * @param fmt Format for error message.
     * @param args Arguments for format string.
     */
    void error(String fmt, Object ...args);

    /**
     * Log a warning.
     *
     * @param msg The string to log.
     */
    void warn(String msg);

    /**
     * Log a warning.
     *
     * @param fmt Format for warning message.
     * @param args Arguments for format string.
     */
    void warn(String fmt, Object ...args);

    /**
     * Log a information message.
     *
     * @param msg The string to log.
     */
    void info(String msg);

    /**
     * Log a information message.
     *
     * @param fmt Format for information message.
     * @param args Arguments for format string.
     */
    void info(String fmt, Object ...args);

    /**
     * Log a debug message.
     *
     * @param msg The string to log.
     */
    void debug(String msg);

    /**
     * Log a debug message.
     *
     * @param fmt Format for debug message.
     * @param args Arguments for format string.
     */
    void debug(String fmt, Object ...args);

    /**
     * Log an exception to the error level with the stack trace going to the debug level.
     *
     * @param e Exception to log.
     * @param msg Prefix message for exception message.
     */
    void exception(Throwable e, String msg);

    /**
     * Log an exception to the error level with the stack trace going to the debug level.
     *
     * @param e Exception to log.
     * @param fmt Format for debug message.
     * @param args Arguments for format string.
     */
    void exception(Throwable e, String fmt, Object ...args);

    /**
     * Log an exception to the error level with the stack trace going to the debug level.
     *
     * @param e Exception to log.
     */
    void exception(Throwable e);
}
