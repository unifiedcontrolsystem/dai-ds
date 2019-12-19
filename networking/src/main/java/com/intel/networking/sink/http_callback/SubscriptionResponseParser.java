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
    URI parseResponse(String message, String subscriptionUri);
}
