// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink;

/**
 * Function interface to record a stream location for URL path and a stream ID.
 */
@FunctionalInterface
public interface StreamLocationHandler {
    /**
     * Called when a new SSE ID is encountered on a SSE stream (urlPath) for a stream ID (streamId).
     *
     * @param streamLocation The new ID value from the SSE stream.
     * @param urlPath The path portion of the URL that the SSE stream is connected to.
     * @param streamId The optional stream ID associated with the URL. May be null or empty.
     */
    void newStreamLocation(String streamLocation, String urlPath, String streamId);
}
