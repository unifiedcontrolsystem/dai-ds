// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;

import java.net.URI;

/**
 * Description of interface ResponseTranslator.
 */
public abstract class ResponseTranslator {
    /**
     * Creates a translator from a derived class. Creates a JSON parser for general use.
     */
    protected ResponseTranslator() {
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null) throw new RuntimeException("Failed to create a 'json' parser");
    }

    /**
     * Required to override, creates a PropertyMap out of error information.
     *
     * @param code The HTTP error code.
     * @param message The text error message.
     * @param uri The URI the error is from on the server.
     * @param method The invoked HTTP method that the error occurred on the server.
     * @param cause Either null or the exception that occurred causing this error.
     * @return A map that can be converted to JSON using the toString method below.
     */
    abstract public PropertyMap makeError(int code, String message, URI uri, String method, Throwable cause);

    /**
     * Required to override, creates a PropertyMap out of a PropertyDocument (Map or Array) user payload.
     *
     * @param payload The user payload of the response.
     * @return A map that can be converted to JSON using the toString method below.
     */
    abstract public PropertyMap makeResponse(PropertyDocument payload);

    /**
     * Convert a PropertyMap to a compact string (no extra whitespace).
     * @param map The map to convert to a String for the server response.
     * @return The JSON string from the PropertyMap.
     */
    public final String toString(PropertyMap map) {
        return parser_.toString(map);
    }

    /**
     * Builds a "printable" String of the exception stack(s) for an exception.
     * @param cause The exception that needs to be converted to a string for the response from the server.
     * @return The resulting "printable" String from the exception.
     */
    public final String buildExceptionTrace(Throwable cause) {
        return buildTraceString(cause, true);
    }

    private String buildTraceString(Throwable cause, boolean firstTime) {
        if(cause == null) return "";
        StringBuilder builder = new StringBuilder();
        if(firstTime)
            builder.append(String.format("Exception: %s: ", cause.getClass().getSimpleName())).
                    append(cause.getMessage()).append("\n");
        else
            builder.append(String.format("Cause by: %s: ", cause.getClass().getSimpleName())).
                    append(cause.getMessage()).append("\n");
        for(StackTraceElement element: cause.getStackTrace()) {
            builder.append("   ").append(element.toString()).append("\n");
        }
        return builder.toString() + buildTraceString(cause.getCause(), false);
    }

    private ConfigIO parser_;
}
