// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.exceptions;

/**
 * Main exception class for exceptions that providers can throw, in other words, exceptions that can cross the boundary
 * between adapters and providers.  Any exceptions that a provider can throw to an adapter should either be a
 * ProviderException or derive from ProviderException.  This also means that any interfaces defined in the adapter
 * layer for providers to follow or implement must use ProviderException as part of the interface (if they throw
 * any exceptions), as opposed to specifying exceptions specific to a particular implementation.
 *
 * Note that based on the constructors provided, a provider must include a message in every exception thrown to an
 * adapter (to provide proper context about the error).
 */
@SuppressWarnings("serial")
public class ProviderException extends Exception {
    public ProviderException(String message) {
        super(message);
    }

    public ProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
