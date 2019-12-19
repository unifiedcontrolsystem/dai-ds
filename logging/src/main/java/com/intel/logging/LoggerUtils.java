// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.logging;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

final class LoggerUtils {
    static final int DEBUG = 0;
    static final int INFO = 1;
    static final int WARN = 2;
    static final int ERROR = 3;
    static final int FATAL = 4;
    private static final String[] levelLookup_ = new String[] {
            "DEBUG",
            "INFO",
            "WARN",
            "ERROR",
            "FATAL",
    };

    static String formatMessage(String rawMessage, Map<String, String> info) {
        return String.format("%s %-5s [%s] %s.%s (%s:%s) - %s (%s) %s", info.get("timestamp"), info.get("level"),
                info.get("thread"), info.get("class"), info.get("method"), info.get("file"),
                info.get("line"), info.get("name"), info.get("type"), rawMessage);
    }

    static String[] getFormattedExceptionTrace(Throwable exception, Map<String, String> info) {
        String[] result = new String[exception.getStackTrace().length];
        for(int i = 0; i < exception.getStackTrace().length; i++)
            result[i] = String.format("%s DEBUG [%s] :  %s", info.get("timestamp"), info.get("thread"),
                    exception.getStackTrace()[i].toString());
        return result;
    }

    static Map<String, String> getLoggingInfo(int level, int depth, String name, String type) {
        if(level < 0 || level > 4) throw new IndexOutOfBoundsException("level must be 0-4 inclusive!");
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd' 'HH:mm:ss.SSS'Z'", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("GMT"));
        String ts = format.format(Date.from(Instant.now()));
        StackTraceElement ste = Thread.currentThread().getStackTrace()[depth];
        HashMap<String, String> result = new HashMap<>();
        result.put("timestamp", ts);
        result.put("class", ste.getClassName());
        result.put("method", ste.getMethodName());
        result.put("file", ste.getFileName());
        result.put("line", String.format("%d", ste.getLineNumber()));
        result.put("thread", Thread.currentThread().getName());
        result.put("level", levelLookup_[level]);
        result.put("name", name);
        result.put("type", type);
        return result;
    }

    private LoggerUtils() {}
}
