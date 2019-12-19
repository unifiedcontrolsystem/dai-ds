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
    String buildRequest(Collection<String> subjects, String subscriberID, String callbackUrl);
}
