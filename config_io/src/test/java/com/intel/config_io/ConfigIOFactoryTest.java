// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.config_io;

import org.junit.Test;

import static org.junit.Assert.*;

public class ConfigIOFactoryTest {

    @Test
    public void getInstance() {
        ConfigIO parser = ConfigIOFactory.getInstance("unknown");
        assertNull(parser);
        parser = ConfigIOFactory.getInstance(null);
        assertNotNull(parser);
        parser = ConfigIOFactory.getInstance("json");
        assertNotNull(parser);
    }
}