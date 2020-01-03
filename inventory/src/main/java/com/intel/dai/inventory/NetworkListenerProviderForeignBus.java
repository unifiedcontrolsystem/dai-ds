// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of class BootEventTransformer.
 */
public class NetworkListenerProviderForeignBus implements NetworkListenerProvider, Initializer {
    public NetworkListenerProviderForeignBus(Logger logger) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null)
            throw new RuntimeException("Failed to create a JSON parser for class" + getClass().getCanonicalName());
    }

    @Override
    public void initialize() { /* Not used but is required */ }

    @Override
    public List<CommonDataFormat> processRawStringData(String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        // TODO: translate boot state event or inventory change event to common data format here!
        return new ArrayList<>();
    }

    @Override
    public void actOnData(CommonDataFormat data, NetworkListenerConfig config, SystemActions systemActions) {
        // TODO: act on boot or inventory event here using systemActions (Add to the API if needed).
    }
    private String makeInstanceDataForFailedNodeUpdate(CommonDataFormat data) {
        return String.format("ForeignLocation='%s'; UcsLocation='%s'; BootMessage='%s'",
                data.retrieveExtraData("xnameLocation"), data.getLocation(), data.getStateEvent().toString());
    }

    private Logger log_;
    private ConfigIO parser_;
}
