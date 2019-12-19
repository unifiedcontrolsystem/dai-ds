// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.config_io;

import java.util.HashMap;
import java.util.Map;

/**
 * Static factory class to create the ConfigIO parser instance.
 */
public final class ConfigIOFactory {
    private ConfigIOFactory() {}

    /**
     * Factory method for creating a ConfigIO parser.
     *
     * @param implementation Usually null but a future feature may be to support multiple configuration parsers.
     * @return The parser instance or null if an appropriate parser is not found.
     */
    public static ConfigIO getInstance(String implementation) {
        if(implementation == null || implementation.equals(JSON)) {
            if (!instances_.containsKey(JSON))
                instances_.put(JSON, new JsonCliftonLabsProvider());
            return instances_.get(JSON);
        }
        return null;
    }

    private static final Map<String,ConfigIO> instances_= new HashMap<>();
    private static final String JSON = "json";
}
