// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

import com.intel.networking.HttpMethod;

import java.util.Map;

/**
 * Interface for HTTP network requests.
 */
public interface Request {
    /**
     * Return the HTTP method of the request.
     *
     * @return The correct HttpMethod enum value.
     * @throws RequestException Thrown if the request method was not recognized.
     */
    HttpMethod getMethod() throws RequestException;

    /**
     * Returns the URL path (excluding the query string).
     *
     * @return The URL path of the request.
     * @throws RequestException If the URL cannot be parsed.
     */
    String getPath() throws RequestException;

    /**
     * Returns the request's Map<String,String> for the request headers.
     *
     * @return The map of the header key and values.
     * @throws RequestException If the request cannot be parsed.
     */
    Map<String,String> getHeaders() throws RequestException;

    /**
     * Returns the request's body as a UTF-8 String, this is usually JSON.
     *
     * @return The body of the request.
     * @throws RequestException If the request cannot be read or parsed.
     */
    String getBody() throws RequestException;

    /**
     * Returns the query portion of the URL for the request.
     *
     * @return The full query string, no parsing is done.
     * @throws RequestException If the URL cannot be parsed.
     */
    String getQuery() throws RequestException;
}
