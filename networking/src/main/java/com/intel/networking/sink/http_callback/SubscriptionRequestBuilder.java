// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink.http_callback;

import java.util.Collection;

/**
 * Description of interface SubscriptionRequestBuilder.
 */
@FunctionalInterface
public interface SubscriptionRequestBuilder {
    /**
     * The subscription request for a new HTTP callback based eventing needs to be built (usually a POST)
     * needs to be build dynamically. This defines the functional interface to do that.
     *
     * @param subjects Subjects for the subscription request.
     * @param subscriberID Subscriber ID that identified this subscription.
     * @param callbackUrl The HTTP server location for all callback events.
     * @return The formatted (usually JSON) subscription request body.
     */
    String buildRequest(Collection<String> subjects, String subscriberID, String callbackUrl);
}
