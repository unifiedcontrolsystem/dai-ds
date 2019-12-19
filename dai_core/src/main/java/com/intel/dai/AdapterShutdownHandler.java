// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.exceptions.AdapterException;

public interface AdapterShutdownHandler {
    void handleShutdown() throws AdapterException;
}