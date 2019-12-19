// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.logging;

final public class LoggerFactory {
    private LoggerFactory() {}

    @Deprecated
    public static Logger getInstance() {
        if(instance_ == null) throw new RuntimeException("Cannot call the factory with no parameters the first " +
                "time, use the factory with 3 parameters!");
        return instance_;
    }

    public static Logger getInstance(String adapterType, String adapterName, String implementation) {
        if(instance_ != null) return instance_;
        if(implementation == null || implementation.trim().equals("") || implementation.trim().equals("console")) {
            instance_ = new LoggerConsole(adapterType, adapterName);
            instance_.initialize();
            return instance_;
        }
        if(implementation.equals("log4j2")) {
            instance_ = new LoggerLog4j2(adapterType, adapterName);
            instance_.initialize();
            return instance_;
        }
        throw new RuntimeException("Unknown Logger implementation specified!");
    }

    static Logger instance_ = null;
}
