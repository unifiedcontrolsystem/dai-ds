// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.exceptions;

/**
 * Main exception class for exceptions that adapters can throw, in other words, exceptions that can cross the boundary
 * between the main application and adapters.  Any exceptions that an adapter can throw to main should either be an
 * AdapterException or derive from AdapterException.
 *
 * Note that based on the constructors provided, an adapter must include a message in every exception thrown to main
 * (to provide proper context about the error).
 */
@SuppressWarnings("serial")
public class AdapterException extends Exception {
    public AdapterException(String message) {
        super(message);
    }

    public AdapterException(String message, Throwable cause) {
        super(message, cause);
    }
}
