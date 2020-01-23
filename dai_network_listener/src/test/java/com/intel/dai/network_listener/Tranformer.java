// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

import com.intel.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of class Transformer.
 */
class Tranformer implements NetworkListenerProvider, Initializer {
    public Tranformer(Logger logger) {}

    @Override
    public List<CommonDataFormat> processRawStringData(String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        return new ArrayList<>() {{
            add(new CommonDataFormat(100000L, "location", DataType.EnvironmentalData));
        }};
    }

    @Override
    public void actOnData(CommonDataFormat data, NetworkListenerConfig config, SystemActions system) {
    }

    @Override public void initialize() { }
}
