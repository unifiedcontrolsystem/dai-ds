// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink;

/**
 * Adds new functionality without modifying the parent interface.
 */
public interface NetworkDataSinkEx extends NetworkDataSink {
    /**
     * Add the SSE ID callback handler (StreamLocationHandler) for this NetworkDataSinkEx implementation.
     *
     * @param handler The callback where the stream location can be persisted so on exit the stream can be
     *                continued where you left off.
     */
    void setStreamLocationCallback(StreamLocationHandler handler);

    /**
     * Sets the SSE ID to be in the initial request on connection.
     *
     * @param id The initial ID to use in the SSE request.
     */
    void setLocationId(String id);
}
