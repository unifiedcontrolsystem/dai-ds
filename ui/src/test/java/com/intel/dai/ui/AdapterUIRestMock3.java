// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.ui;

import com.intel.dai.IAdapter;

import java.io.IOException;

import static org.mockito.Mockito.mock;

/**
 * Description of class AdapterUIRestMock3.
 */
class AdapterUIRestMock3 extends AdapterUIRestMock2 {
    AdapterUIRestMock3(String[] args) throws IOException {
        super(args);
        setupFactoryObjects(args, mock(IAdapter.class));
    }
}
