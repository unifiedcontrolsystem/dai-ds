// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

/**
 * Description of interface RESTServerHandler.
 */
@FunctionalInterface
public interface RESTServerHandler {
    /**
     * Callback for RESTServer on receipt of a non-SSE URL request. The callback must parse the request and build
     * the appropriate response object than as a last step call Response.applyChanges() before returning.
     *
     * @param request The Request object of the HTTP request.
     * @param response The Response object of the HTTP of the request.
     */
    void handle(Request request, Response response) throws Exception;
}
