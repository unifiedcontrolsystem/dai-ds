// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient;

import com.intel.networking.HttpMethod;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Description of class RequestInfo class representing a HTTP request.
 */
public class RequestInfo {
    public RequestInfo(HttpMethod method, URI uri, String body) {
        this(method, uri, body, new HashMap<>());
    }

    public RequestInfo(HttpMethod method, URI uri, String body, Map<String,String> headers) {
        method_ = method;
        uri_ = uri;
        body_ = body;
        headers_ = headers;
    }

    /**
     * Gets the HTTP method of this object.
     *
     * @return The store value.
     */
    public HttpMethod method() { return method_; }

    /**
     * Gets the URI of this object.
     *
     * @return The store value.
     */
    public URI uri() { return uri_; }

    /**
     * Gets the body of this object as a String.
     *
     * @return The store value.
     */
    public String body() { return body_; }

    /**
     * Gets the extra HTTP headers to add to the request.
     *
     * @return The map of headers (may be empty but not null).
     */
    public Map<String,String> headers() { return headers_; }

    /**
     * Make a PropertyMap out of this object.
     *
     * @return THe PropertyMap representation of this object.
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(this.getClass().getCanonicalName()).append(" instance:\n");
        builder.append("  method=").append(method_.toString()).append("\n");
        builder.append("  uri=").append(uri_.toString()).append("\n");
        builder.append("  body=").append(body_).append("\n");
        builder.append("  headers:\n");
        for(String header: headers_.keySet())
            builder.append("    ").append(header).append(": ").append(headers_.getOrDefault(header, "")).append("\n");
        return builder.toString();
    }

    private final HttpMethod method_;
    private final URI uri_;
    private final String body_;
    private final Map<String,String> headers_;
}
