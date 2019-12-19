// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient;

/**
 * Description of interface ResponseCallback.
 */
@FunctionalInterface
public interface ResponseCallback {
    /**
     * Called when a client response is received.
     *
     * @param code The HTTP response code or -2 if a SSE connection failed after first succeeding or -1 on a simple
     *            REST call that fails with an exception.
     * @param responseBody The body of the response as a String.
     * @param originalInfo The request information.
     */
    void responseCallback(int code, String responseBody, RequestInfo originalInfo);
}
