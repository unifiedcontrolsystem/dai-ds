// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.config_io;

/**
 * Thrown by this package when the basic underlying JSON parser throws its error to this package. If developing a
 * JSON parser provider, you must catch all underlying parser exceptions and throw this exception instead.
 */
@SuppressWarnings("serial")
public class ConfigIOParseException extends Exception {
    /**
     * Create the exception using the character position of the failure in the JSON stream.
     *
     * @param streamPosition The character position in the JSON document.
     */
    ConfigIOParseException(Integer streamPosition) {
        super(String.format("While parsing JSON, failed parsing at character position %d!", streamPosition));
    }

    /**
     * Create the exception from an inner exception.
     *
     * @param inner The inner exception.
     */
    public ConfigIOParseException(Throwable inner) {
        super(String.format("Internal JSON parser error: %s!", inner.getMessage()));
    }

    /**
     * Create a exception from a String message.
     *
     * @param msg The message of the exception.
     */
    public ConfigIOParseException(String msg) { super(msg); }

    /**
     * Create a exception from a String message and a inner exception.
     *
     * @param msg The message of the exception.
     * @param inner The inner exception or cause of the exception.
     */
    public ConfigIOParseException(String msg, Throwable inner) { super(msg, inner); }
}
