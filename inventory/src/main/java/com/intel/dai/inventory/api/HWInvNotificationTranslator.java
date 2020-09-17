// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intel.dai.inventory.api.pojo.scn.ForeignHWInvChangeNotification;
import com.intel.logging.Logger;

/**
 * The class parses a HW inventory state change notification (SCN) and store the extracted information into a POJO.
 */
public class HWInvNotificationTranslator {

    /**
     * Constructs the HWInvNotificationTranslator object by initializing the GSON object.
     */
    public HWInvNotificationTranslator(Logger log) {
        logger = log;

        GsonBuilder builder = new GsonBuilder();
        builder.setPrettyPrinting();
        gson = builder.create();
    }

    /**
     * Converts the given SCN json into a POJO.
     * @param json contains a HW inventory SCN
     * @return a POJO containing the converted HW inventory SCN
     */
    public ForeignHWInvChangeNotification toPOJO(String json) {
        try {
            return gson.fromJson(json, ForeignHWInvChangeNotification.class);
        } catch (Exception e) {
            // EOFException can occur if the json is incomplete
            logger.fatal("GSON parsing error: %s", e.getMessage());
            return null;
        }
    }

    private final Gson gson;
    private final Logger logger;
}
