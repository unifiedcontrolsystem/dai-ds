// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink.http_callback;

import java.net.URI;

/**
 * Description of interface SubscriptionResponseParser.
 */
@FunctionalInterface
public interface SubscriptionResponseParser {
    /**
     * Once the subscription request by the client, the response must be parsed by the client's server to get the URI
     * for callbacks. This defines the functional interface to do this work.
     *
     * @param message The raw subscription request (usually JSON).
     * @param subscriptionUri The subscription URL.
     * @return THe callback URI.
     */
    URI parseResponse(String message, String subscriptionUri);
}
