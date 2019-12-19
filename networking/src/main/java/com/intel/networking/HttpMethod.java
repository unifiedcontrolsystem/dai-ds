// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking;

/**
 * Enum representing all HTTP methods
 */
public enum HttpMethod {
    /**
     * HTTP GET method (see HTTP 2.0 or 1.1 specification)
     */
    GET,
    /**
     * HTTP HEAD method (see HTTP 2.0 or 1.1 specification)
     */
    HEAD,
    /**
     * HTTP POST method (see HTTP 2.0 or 1.1 specification)
     */
    POST,
    /**
     * HTTP PUT method (see HTTP 2.0 or 1.1 specification)
     */
    PUT,
    /**
     * HTTP DELETE method (see HTTP 2.0 or 1.1 specification)
     */
    DELETE,
    /**
     * HTTP OPTIONS method (see HTTP 2.0 or 1.1 specification)
     */
    OPTIONS,
    /**
     * HTTP PATCH method (see HTTP 2.0 or 1.1 specification)
     */
    PATCH
}
