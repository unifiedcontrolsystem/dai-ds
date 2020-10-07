// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

import com.intel.logging.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Description of class TestTransformer.
 */
class TestTransformer implements NetworkListenerProvider, Initializer {
    public TestTransformer(Logger logger) {}

    @Override
    public List<CommonDataFormat> processRawStringData(String data, NetworkListenerConfig config) throws NetworkListenerProviderException {
        return new ArrayList<CommonDataFormat>() {{ add(new CommonDataFormat(99999L, "TestLocation", DataType.RasEvent)); }};
    }

    @Override
    public void actOnData(CommonDataFormat data, NetworkListenerConfig config, SystemActions systemActions) {}

    @Override public void initialize() { }
}
