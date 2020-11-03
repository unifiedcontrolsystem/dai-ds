// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

/**
 * Enum DataType used by the CommonDataFormat class.
 */
public enum DataType {
    Unknown,
    EnvironmentalData,
    RasEvent,
    StateChangeEvent,
    InventoryChangeEvent,
    InitialNodeStateData,
    LogData
}
