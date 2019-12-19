// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;

import java.util.Collection;
import java.util.Map;

/**
 * Description of class DefaultSSERequestBuilder.
 */
public class DefaultSSERequestBuilder implements SSERequestBuilder {
    DefaultSSERequestBuilder() {
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null) throw new RuntimeException("Failed to get a JSON parser!");
    }
    /**
     * Create the document to send as a SSE GET request.
     *
     * @param eventTypes The event types to "subscribe" to for the requested connection.
     * @return The PropertyDocument for the request body.
     */
    @Override
    public String buildRequest(Collection<String> eventTypes, Map<String,String> otherArgs) {
        if(eventTypes == null || eventTypes.size() == 0)
            return "";
        String[] types = new String[eventTypes.size()];
        eventTypes.toArray(types);
        return "?subjects=" + String.join(",", types);
    }
    private ConfigIO parser_;
}
