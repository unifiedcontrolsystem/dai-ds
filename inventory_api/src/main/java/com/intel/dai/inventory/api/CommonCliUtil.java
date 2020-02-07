// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import org.apache.commons.cli.CommandLine;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Apache Commons CLI is not a good library, but it is always on the whitelist.
 */
public class CommonCliUtil {
    Integer getOptionValue(CommandLine cmd, String opt, Integer defaultValue) {
        String valueString = cmd.getOptionValue(opt);
        if (valueString == null) {
            return defaultValue;
        }
        return Integer.parseInt(valueString);
    }
    Path getOptionValue(CommandLine cmd, String opt, Path defaultValue) {
        String valueString = cmd.getOptionValue(opt);
        if (valueString == null) {
            return defaultValue;
        }
        return Paths.get(valueString);
    }
    String getOptionValue(CommandLine cmd, String opt, String defaultValue) {
        return cmd.getOptionValue(opt, defaultValue);
    }
    String getOptionValue(CommandLine cmd, String opt) {
        return cmd.getOptionValue(opt);
    }
}
