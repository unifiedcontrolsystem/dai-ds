// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.logging.Logger;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class AdapterSingletonFactoryTest {

    @Test
    public void initializeFactory() {
        AdapterSingletonFactory.initializeFactory("test_type", "test_name", mock(Logger.class));
        try {
            AdapterSingletonFactory.initializeFactory(null, "test_name", mock(Logger.class));
            fail();
        } catch(IllegalArgumentException e) {
            // Pass
        }
        try {
            AdapterSingletonFactory.initializeFactory("", "test_name", mock(Logger.class));
            fail();
        } catch(IllegalArgumentException e) {
            // Pass
        }
        try {
            AdapterSingletonFactory.initializeFactory("test_type", null, mock(Logger.class));
            fail();
        } catch(IllegalArgumentException e) {
            // Pass
        }
        try {
            AdapterSingletonFactory.initializeFactory("test_type", "", mock(Logger.class));
            fail();
        } catch(IllegalArgumentException e) {
            // Pass
        }
    }

    @Test
    public void getAdapter() throws Exception {
        AdapterSingletonFactory.name_ = null;
        AdapterSingletonFactory.type_ = null;
        AdapterSingletonFactory.adapter_ = null;
        try {
            AdapterSingletonFactory.getAdapter();
            fail();
        } catch(Exception e) {
            // Pass
        }
        IAdapter adapter = mock(IAdapter.class);
        AdapterSingletonFactory.adapter_ = adapter;
        assertEquals(adapter, AdapterSingletonFactory.getAdapter());
    }
}
