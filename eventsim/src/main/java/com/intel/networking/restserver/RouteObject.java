// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

import com.intel.networking.HttpMethod;

import java.util.Collection;

/**
 * Description of class RouteObject.
 */
public class RouteObject {
    /**
     * Construct the RouterObject storage.
     *
     * @param url The URL path to store as a map routing.
     * @param method The HTTP Method to store as a map routing along with the "url'. For SSE routes this is always
     *              HttpMethod.POST.
     * @param handler The user callback handler iff the sseSupport field is false.
     * @param sseSupport The SSE support flag. If true then this route is a Server Side Eventing URL.
     * @param eventTypes The list of event types supplied by the SSE path. Can be null.
     */
    public RouteObject(String url, HttpMethod method, RESTServerHandler handler, boolean sseSupport,
                       Collection<String> eventTypes) {
        if(url.endsWith("/*")) {
            wildcard = true;
            this.url = url.substring(0, url.length() - 1);
        } else {
            wildcard = false;
            this.url = url;
        }
        this.method = method;
        this.handler = handler;
        this.sseSupport = sseSupport;
        this.eventTypes = eventTypes;
    }

    public final String url;
    public final HttpMethod method;
    public final RESTServerHandler handler;
    public final boolean sseSupport;
    public Collection<String> eventTypes;
    final boolean wildcard;
}
