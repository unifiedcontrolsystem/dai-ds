// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.logging;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.appender.ConsoleAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.config.builder.api.*;
import org.apache.logging.log4j.core.config.builder.impl.BuiltConfiguration;
import org.apache.logging.log4j.spi.LoggerContext;

import java.util.Map;

final class LoggerLog4j2 implements Logger {
    LoggerLog4j2(String type, String name) {
        Level level = getLevel();
        setupLog4j2(level);
        name_ = name;
        type_ = type;
        log_ = loggerContext_.getLogger(String.format("%s.%s", type, name));
    }

    /**
     * Optionally Initialize the logger.
     */
    @Override
    public void initialize() {
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
        log_.fatal(LoggerUtils.formatMessage(String.format(fmt, args), info));
    }

    /**
     * Log an error.
     *
     * @param msg The string to log.
     */
    @Override
    public void error(String msg) {
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.ERROR, STACK_DEPTH, name_, type_);
        log_.error(LoggerUtils.formatMessage(msg, info));
    }

    /**
     * Log an error.
     *
     * @param fmt  Format for error message.
     * @param args Arguments for format string.
     */
    @Override
    public void error(String fmt, Object... args) {
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.ERROR, STACK_DEPTH, name_, type_);
        log_.error(LoggerUtils.formatMessage(String.format(fmt, args), info));
    }

    /**
     * Log a warning.
     *
     * @param msg The string to log.
     */
    @Override
    public void warn(String msg) {
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.WARN, STACK_DEPTH, name_, type_);
        log_.warn(LoggerUtils.formatMessage(msg, info));
    }

    /**
     * Log a warning.
     *
     * @param fmt  Format for warning message.
     * @param args Arguments for format string.
     */
    @Override
    public void warn(String fmt, Object... args) {
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.WARN, STACK_DEPTH, name_, type_);
        log_.warn(LoggerUtils.formatMessage(String.format(fmt, args), info));
    }

    /**
     * Log a information message.
     *
     * @param msg The string to log.
     */
    @Override
    public void info(String msg) {
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.INFO, STACK_DEPTH, name_, type_);
        log_.info(LoggerUtils.formatMessage(msg, info));
    }

    /**
     * Log a information message.
     *
     * @param fmt  Format for information message.
     * @param args Arguments for format string.
     */
    @Override
    public void info(String fmt, Object... args) {
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.INFO, STACK_DEPTH, name_, type_);
        log_.info(LoggerUtils.formatMessage(String.format(fmt, args), info));
    }

    /**
     * Log a debug message.
     *
     * @param msg The string to log.
     */
    @Override
    public void debug(String msg) {
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.DEBUG, STACK_DEPTH, name_, type_);
        log_.debug(LoggerUtils.formatMessage(msg, info));
    }

    /**
     * Log a debug message.
     *
     * @param fmt  Format for debug message.
     * @param args Arguments for format string.
     */
    @Override
    public void debug(String fmt, Object... args) {
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.DEBUG, STACK_DEPTH, name_, type_);
        log_.debug(LoggerUtils.formatMessage(String.format(fmt, args), info));
    }

    /**
     * Log an exception to the error level with the stack trace going to the debug level.
     *
     * @param e   Exception to log.
     * @param msg Prefix message for exception message.
     */
    @Override
    public void exception(Throwable e, String msg) {
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.ERROR, STACK_DEPTH, name_, type_);
        log_.error(LoggerUtils.formatMessage(msg, info));
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
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.ERROR, STACK_DEPTH, name_, type_);
        log_.error(LoggerUtils.formatMessage(String.format(fmt, args), info));
        printStackTrace(e, info);
    }

    /**
     * Log an exception to the error level with the stack trace going to the debug level.
     *
     * @param e Exception to log.
     */
    @Override
    public void exception(Throwable e) {
        Map<String, String> info = LoggerUtils.getLoggingInfo(LoggerUtils.ERROR, STACK_DEPTH, name_, type_);
        printStackTrace(e, info);
    }

    private void printStackTrace(Throwable e, Map<String, String> info) {
        log_.error(LoggerUtils.formatMessage("Exception: " + e.getMessage(), info));
        for(String line: LoggerUtils.getFormattedExceptionTrace(e, info))
            log_.info(line);
        Throwable cause = e.getCause();
        while (cause != null) {
            log_.error(LoggerUtils.formatMessage(String.format("Caused by: %s", cause.getMessage()), info));
            for(String line: LoggerUtils.getFormattedExceptionTrace(cause, info))
                log_.info(line);
            cause = cause.getCause();
        }
    }

    private void setupLog4j2(Level level) {
        ConfigurationBuilder<BuiltConfiguration> configBuilder = ConfigurationBuilderFactory.newConfigurationBuilder();
        configBuilder.setStatusLevel(Level.INFO);
        configBuilder.setConfigurationName("DaiLogger");
        setDaiLoggingLevel(level, configBuilder);
        AppenderComponentBuilder configAppender = setLoggerToStdoutAndUsePattern(configBuilder);
        setLocalDaiFilter(configBuilder, configAppender);
        buildLocalLoggerConfig(level, configBuilder, configAppender);
        buildRootLoggerConfig(configBuilder);
        loggerContext_ = Configurator.initialize(configBuilder.build());
    }

    private void buildRootLoggerConfig(ConfigurationBuilder<BuiltConfiguration> configBuilder) {
        // Almost there, build the root logger.
        RootLoggerComponentBuilder rootBuilder = configBuilder.newRootLogger(Level.INFO);
        AppenderRefComponentBuilder ref = configBuilder.newAppenderRef("Stdout");
        rootBuilder.add(ref);
        configBuilder.add(rootBuilder);
    }

    private void buildLocalLoggerConfig(Level level, ConfigurationBuilder<BuiltConfiguration> configBuilder, AppenderComponentBuilder configAppender) {
        configBuilder.add(configAppender);
        LoggerComponentBuilder loggerBuilder = configBuilder.newLogger("org.apache.logging.log4j", level);
        loggerBuilder.add(configBuilder.newAppenderRef("Stdout"));
        loggerBuilder.addAttribute("additivity", false);
        configBuilder.add(loggerBuilder);
    }

    private void setLocalDaiFilter(ConfigurationBuilder<BuiltConfiguration> configBuilder, AppenderComponentBuilder configAppender) {
        // Set the local filter configuration.
        FilterComponentBuilder filter = configBuilder.newFilter("MarkerFilter", Filter.Result.DENY, Filter.Result.NEUTRAL);
        filter.addAttribute("marker", "FLOW");
        configAppender.add(filter);
    }

    private AppenderComponentBuilder setLoggerToStdoutAndUsePattern(ConfigurationBuilder<BuiltConfiguration> configBuilder) {
        // Send output to stdout
        AppenderComponentBuilder configAppender = configBuilder.newAppender("Stdout", "CONSOLE");
        configAppender.addAttribute("target", ConsoleAppender.Target.SYSTEM_OUT);

        // Set the DAI layout format to a simple pattern.
        LayoutComponentBuilder layout = configBuilder.newLayout("PatternLayout");
        layout.addAttribute("pattern", "%m%n");
        configAppender.add(layout);
        return configAppender;
    }

    private void setDaiLoggingLevel(Level level, ConfigurationBuilder<BuiltConfiguration> configBuilder) {
        // Set DAI Logging filter configuration to passed value.
        FilterComponentBuilder filter = configBuilder.newFilter("ThresholdFilter", Filter.Result.ACCEPT,
                Filter.Result.NEUTRAL);
        filter.addAttribute("level", level);
        configBuilder.add(filter);
    }

    Level getLevel() {
        String sLevel = System.getProperty("daiLoggingLevel", "INFO");
        Level level;
        switch(sLevel) {
            case "ERROR":
                level = Level.ERROR;
                break;
            case "WARN":
                level = Level.WARN;
                break;
            case "DEBUG":
                level = Level.DEBUG;
                break;
            default:
                level = Level.INFO;
                break;
        }
        return level;
    }

    private String name_;
    private String type_;
    private org.apache.logging.log4j.Logger log_;
    LoggerContext loggerContext_;

    private static final int STACK_DEPTH = 3; // Where in the stack trace the callers info is located.
}
