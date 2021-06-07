// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.volt;

import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;

public class Tool {
    public static void main(String[] args) {
        Logger logger = LoggerFactory.getInstance("TOOL", "VoltInitializer", "console");
        if(args.length != 1) {
            logger.error("Must have only the comma separated list of VoltDB server as the 1 and only " +
                    "required argument!");
            logger.info("USAGE: <toolname> {voltdb_servers}");
            System.exit(1);
        }
        VoltDBSetup.setupVoltDBOrWait(args[0], 300L, logger);
        System.exit(0);
    }
}
