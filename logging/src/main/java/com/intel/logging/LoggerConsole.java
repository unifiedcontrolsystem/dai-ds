// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.logging;

import java.util.Map;

class LoggerConsole implements Logger {
    LoggerConsole(String type, String name) {
        name_ = name;
        type_ = type;
    }

    /**
     * Optionally Initialize the logger.
     */
    @Override
    public void initialize() {
        filterLevel_ = getLevel();
    }

    /**
     * Log a fatal error.
     *
     * @param fmt  Format for error message.
     * @param args Arguments for format string.
     */
    @Override
    public void fatal(String fmt, Object... args) {
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.FATAL, STACK_DEPTH, name_, type_);
        output(LoggerUtils.formatMessage(String.format(fmt, args), info));
    }

    /**
     * Log an error.
     *
     * @param msg The string to log.
     */
    @Override
    public void error(String msg) {
        if(LoggerUtils.ERROR < filterLevel_) return;
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.ERROR, STACK_DEPTH, name_, type_);
        output(LoggerUtils.formatMessage(msg, info));
    }

    /**
     * Log an error.
     *
     * @param fmt  Format for error message.
     * @param args Arguments for format string.
     */
    @Override
    public void error(String fmt, Object... args) {
        if(LoggerUtils.ERROR < filterLevel_) return;
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.ERROR, STACK_DEPTH, name_, type_);
        output(LoggerUtils.formatMessage(String.format(fmt, args), info));
    }

    /**
     * Log a warning.
     *
     * @param msg The string to log.
     */
    @Override
    public void warn(String msg) {
        if(LoggerUtils.WARN < filterLevel_) return;
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.WARN, STACK_DEPTH, name_, type_);
        output(LoggerUtils.formatMessage(msg, info));
    }

    /**
     * Log a warning.
     *
     * @param fmt  Format for warning message.
     * @param args Arguments for format string.
     */
    @Override
    public void warn(String fmt, Object... args) {
        if(LoggerUtils.WARN < filterLevel_) return;
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.WARN, STACK_DEPTH, name_, type_);
        output(LoggerUtils.formatMessage(String.format(fmt, args), info));
    }

    /**
     * Log a information message.
     *
     * @param msg The string to log.
     */
    @Override
    public void info(String msg) {
        if(LoggerUtils.INFO < filterLevel_) return;
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.INFO, STACK_DEPTH, name_, type_);
        output(LoggerUtils.formatMessage(msg, info));
    }

    /**
     * Log a information message.
     *
     * @param fmt  Format for information message.
     * @param args Arguments for format string.
     */
    @Override
    public void info(String fmt, Object... args) {
        if(LoggerUtils.INFO < filterLevel_) return;
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.INFO, STACK_DEPTH, name_, type_);
        output(LoggerUtils.formatMessage(String.format(fmt, args), info));
    }

    /**
     * Log a debug message.
     *
     * @param msg The string to log.
     */
    @Override
    public void debug(String msg) {
        if(LoggerUtils.DEBUG < filterLevel_) return;
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.DEBUG, STACK_DEPTH, name_, type_);
        output(LoggerUtils.formatMessage(msg, info));
    }

    /**
     * Log a debug message.
     *
     * @param fmt  Format for debug message.
     * @param args Arguments for format string.
     */
    @Override
    public void debug(String fmt, Object... args) {
        if(LoggerUtils.DEBUG < filterLevel_) return;
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.DEBUG, STACK_DEPTH, name_, type_);
        output(LoggerUtils.formatMessage(String.format(fmt, args), info));
    }

    /**
     * Log an exception to the error level with the stack trace going to the debug level.
     *
     * @param e   Exception to log.
     * @param msg Prefix message for exception message.
     */
    @Override
    public void exception(Throwable e, String msg) {
        if(LoggerUtils.ERROR < filterLevel_) return;
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.ERROR, STACK_DEPTH, name_, type_);
        output(LoggerUtils.formatMessage(msg, info));
        printStackTrace(e, info);
    }

    /**
     * Log an exception to the error level with the stack trace going to the debug level.
     *
     * @param e    Exception to log.
     * @param fmt  Format for debug message.
     * @param args Arguments for format string.
     */
    @Override
    public void exception(Throwable e, String fmt, Object... args) {
        if(LoggerUtils.ERROR < filterLevel_) return;
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.ERROR, STACK_DEPTH, name_, type_);
        output(LoggerUtils.formatMessage(String.format(fmt, args), info));
        printStackTrace(e, info);
    }

    /**
     * Log an exception to the error level with the stack trace going to the debug level.
     *
     * @param e Exception to log.
     */
    @Override
    public void exception(Throwable e) {
        if(LoggerUtils.ERROR < filterLevel_) return;
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.ERROR, STACK_DEPTH, name_, type_);
        printStackTrace(e, info);
    }

    private void output(String fullMsg) {
        System.out.println(fullMsg); // All "logging" goes to stdout, this is not CLI output.
    }

    private void printStackTrace(Throwable e, Map<String, String> info) {
        output(LoggerUtils.formatMessage("Exception: " + e.getMessage(), info));
        if(LoggerUtils.DEBUG >= filterLevel_) {
            for (String line : LoggerUtils.getFormattedExceptionTrace(e, info))
                output(line);
        }
        Throwable cause = e.getCause();
        while (cause != null) {
            output(LoggerUtils.formatMessage(String.format("Caused by: %s", cause.getMessage()), info));
            if(LoggerUtils.DEBUG >= filterLevel_) {
                for (String line : LoggerUtils.getFormattedExceptionTrace(cause, info))
                    output(line);
            }
            cause = cause.getCause();
        }
    }

    int getLevel() {
        String sLevel = System.getProperty("daiLoggingLevel", "WARN"); // Default changed to WARN for production.
        int level;
        switch(sLevel) {
            case "ERROR":
                level = LoggerUtils.ERROR;
                break;
            case "WARN":
                level = LoggerUtils.WARN;
                break;
            case "DEBUG":
                level = LoggerUtils.DEBUG;
                break;
            default:
                level = LoggerUtils.INFO;
                break;
        }
        return level;
    }

    private String name_;
    private String type_;
    private int filterLevel_;

    private static final int STACK_DEPTH = 3; // Where in the stack trace the callers info is located.
}
