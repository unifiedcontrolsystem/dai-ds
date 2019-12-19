// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.authentication;

import com.intel.logging.Logger;

import java.util.Map;

/**
 * Interface for all token based authentication methods (targeted at REST server authentication).
 */
public interface TokenAuthentication {
    /**
     * Initialize the OAuth Bearer Token retriever class.
     *
     * @param logger The logger to use for the implementation.
     * @param arguments The arguments passed to the implementation (implementation specific)
     * @throws TokenAuthenticationException when the instance is already initialized or if the initialization fails.
     */
    void initialize(Logger logger, Map<String,String> arguments) throws TokenAuthenticationException;

    /**
     * Retrieve a token to use in the communication with an OAuth protected REST interface.
     *
     * @return The token as a string to use in the HTTP header "Authorization: Bearer <token>" line for the
     * REST requests.
     * @throws TokenAuthenticationException when the instance has not been initialized.
     */
    String getToken() throws TokenAuthenticationException;
}
