// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient;

import java.util.Collection;
import java.util.Map;

/**
 * Description of interface SSERequestBuilder.
 */
public interface SSERequestBuilder {
    /**
     * Create the document to send as a SSE POST request or the query in a GET request (including the leading "?").
     *
     * @param eventTypes The event types to "subscribe" to for the requested connection.
     * @return The String for the request body or query line.
     */
    String buildRequest(Collection<String> eventTypes, Map<String, String> builderSpecific);
}
