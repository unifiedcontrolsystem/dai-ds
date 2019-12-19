// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient;

/**
 * Description of class BlockingResult.
 */
public class BlockingResult {
    /**
     * Create a BlockingResult from a HTTP response. Used by RESTClient implementations.
     *
     * @param code The HTTP status code.
     * @param responseBody The response body parsed as JSON to a PropertyDocument.
     * @param info The original request info.
     */
    public BlockingResult(int code, String responseBody, RequestInfo info) {
        this.code = code;
        this.responseDocument = responseBody;
        this.requestInfo = info;
    }

    /**
     * HTTP return code for response.
     */
    public final int code;

    /**
     * HTTP body as a String.
     */
    public final String responseDocument;

    /**
     * Original request information for this response.
     */
    public final RequestInfo requestInfo;
}
