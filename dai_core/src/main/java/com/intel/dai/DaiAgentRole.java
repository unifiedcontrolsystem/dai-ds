// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.exceptions.AdapterException;

import java.util.Map;

public interface DaiAgentRole {
    void performWork(String workToBeDone, long workItemId, Map<String, String> params) throws AdapterException;
    void shutDown();
}
