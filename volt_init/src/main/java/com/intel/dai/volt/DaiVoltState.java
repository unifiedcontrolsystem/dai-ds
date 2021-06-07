// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.volt;

/**
 * Enum for possible VoltDB states.
 */
enum DaiVoltState {
    /**
     * Cannot connect to DB, Down or using wrong addresses.
     */
    CANNOT_CONNECT,
    /**
     * DB up but completely empty
     */
    EMPTY,
    /**
     * DB up and schema is loading
     */
    SCHEMA_LOADING,
    /**
     * DB up and schema is loaded
     */
    SCHEMA_LOADED,
    /**
     * DB up, schema loading was attempted but failed, requires DB restart
     */
    SCHEMA_ERROR,
    /**
     * DB up, schema loaded, and data population in progress
     */
    POPULATE_STARTED,
    /**
     * DB up, schema loaded, and data population attempted but failed, requires DB restart
     */
    POPULATED_ERROR,
    /**
     * DB up, schema loaded, data populated, ready for use
     */
    READY
}
