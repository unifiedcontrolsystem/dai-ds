// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.logging.Logger;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Factory class for creating a process instance of IAdapter and returning the same instance to all callers.
 */
public final class AdapterSingletonFactory {
    private AdapterSingletonFactory() {} // Cannot construct!

    /**
     * Set the factory parameters, must be called before the first call to getAdapter().
     * @param type The DAI type of the adapter.
     * @param name The DAI name for this adapter.
     * @param logger The logger to use in the single class.
     */
    synchronized public static void initializeFactory(String type, String name, Logger logger) {
        if (type == null || type.trim().equals("")) throw new IllegalArgumentException("type");
        if (name == null || name.trim().equals("")) throw new IllegalArgumentException("name");
        logger_ = logger;
        type_ = type;
        name_ = name;
    }

    /**
     * Get the current or new instance of the IAdapter class. Must be called the first time after initializeFactory.
     * @return The IAdapter for this process.
     * @throws IOException Thrown if the IAdapter implementation fails to launch.
     */
    synchronized public static IAdapter getAdapter()
            throws IOException {
        if(adapter_ != null) return adapter_;
        if(type_ == null || name_ == null) {
            RuntimeException e = new RuntimeException("Must call AdapterSingletonFactory.initializeFactory before calling AdapterSingletonFactory.getAdapter!");
            e.printStackTrace();
            throw e;
        }
        adapter_ = new Adapter(type_, name_, logger_);
        adapter_.initialize();
        return adapter_;
    }

    /**
     * Intended for testing of this module and modules using this module
     * @param adapter - Usually a Mock adapter is passed
     */
    synchronized public static void setAdapter(IAdapter adapter) {
        adapter_ = adapter;
    }

    static IAdapter adapter_ = null;
    static String name_ = null;
    static String type_ = null;
    static Logger logger_ = null;
}
