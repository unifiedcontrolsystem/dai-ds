// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.exceptions;

/**
 * Main exception class for exceptions that classes in the DAI layer can throw, in other words, exceptions that can
 * cross the boundary between adapters and DAI.  Any exceptions that a class in the DAI layer can throw to an adapter
 * should either be a DaiException or derive from DaiException.  This also means that any interfaces defined in the
 * adapter layer for classes in the DAI layer to follow or implement must use DaiException as part of the interface (if
 * they throw any exceptions), as opposed to specifying exceptions specific to a particular implementation.
 *
 * Note that based on the constructors provided, a class in the DAI layer must include a message in every exception
 * thrown to an adapter (to provide proper context about the error).
 */
@SuppressWarnings("serial")
public class DaiException extends Exception {
    public DaiException(String message) {
        super(message);
    }

    public DaiException(String message, Throwable cause) {
        super(message, cause);
    }
}
