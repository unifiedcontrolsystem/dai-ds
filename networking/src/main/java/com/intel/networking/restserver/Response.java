// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

/**
 * The object containing the HTTP response elements for a request.
 */
public interface Response {
    /**
     * Set the HTTP response code for the response (default is 200).
     *
     * @param code Must be a recognized HTTP response code.
     */
    void setCode(int code);

    /**
     * Set the returned body for a request as a UTF-8 string. This defaults to empty string but is usually JSON. Can
     * be a valid response body or error response body.
     *
     * @param body The body as a UTF-8 string.
     */
    void setBody(String body);

    /**
     * Sets a key/value pair as a head in the response to a HTTP request.
     *
     * @param key The HTTP HEADER Key (see the HTTP 2.0 or 1.1 specification).
     * @param value The value of the key (must be valid for the key type).
     */
    void addHeader(String key, String value);

    /**
     * Called when all elements of the HTTP response are in the object. This method may or may not initiates the
     * sending of the response based on the implementation.
     *
     * @throws ResponseException If the response could not be sent or converted to the internal response format.
     */
    void applyChanges() throws ResponseException;
}
