// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;

import java.net.URI;

/**
 * Description of class DefaultResponseTranslator.
 */
public class DefaultResponseTranslator extends ResponseTranslator {
    /**
     * Creates a PropertyMap from the argument input. The "status" field in the map is common with the makeResponse
     * generated map.
     *
     * @param code    The HTTP error code.
     * @param message The text error message.
     * @param uri     The URI the error is from on the server.
     * @param method  The invoked HTTP method that the error occurred on the server.
     * @param cause   Either null or the exception that occurred causing this error.
     * @return
     */
    @Override
    public PropertyMap makeError(int code, String message, URI uri, String method, Throwable cause) {
        PropertyMap results = new PropertyMap();
        results.put("status", "error");
        results.put("error", message);
        results.put("code", code);
        results.put("method", method.toUpperCase());
        String query = "";
        if(uri.getQuery() != null)
            query = "?" + uri.getQuery();
        results.put("uri", uri.getPath() + query);
        if(cause != null) results.put("trace", buildExceptionTrace(cause));
        return results;
    }

    /**
     * Creates a PropertyMap from the argument input where the "status" field is "ok" and the user payload is under
     * a key called "payload".
     *
     * @param payload The user payload of the response.
     * @return The standardized PropertyMap format.
     */
    @Override
    public PropertyMap makeResponse(PropertyDocument payload) {
        PropertyMap results = new PropertyMap();
        results.put("status", "ok");
        results.put("payload", payload);
        return results;
    }
}
