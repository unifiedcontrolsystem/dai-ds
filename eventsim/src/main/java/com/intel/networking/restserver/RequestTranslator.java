// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyMap;

import java.util.Set;

/**
 * Description of class RequestTranslator.
 */
public abstract class RequestTranslator {
    /**
     * Creates the base class from derived classes.
     *
     * @throws RuntimeException When the parser factory fails to create a JSON parser object.
     */
    protected RequestTranslator() {
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null) throw new RuntimeException("Failed to create a 'json' parser");
    }

    /**
     * Get the request body as a PropertyMap from the HTTP request.
     *
     * @return the parsed body PropertyMap returned from getBodyMap.
     * @throws RuntimeException When the body cannot be parsed as JSON.
     */
    public PropertyMap getBodyMap(String body) {
        PropertyMap result;
        try {
            result = parser_.fromString(body).getAsMap();
        } catch(ConfigIOParseException e) {
            throw new RuntimeException("Failed to parse the HTTP request body as JSON", e);
        }
        return result;
    }

    /**
     * For SSE Requests, gets the set of subjects requested from the parsed HTTP request body.
     *
     * @return The set of subject strings. Never null but may be empty signifying send all subjects.
     */
    public abstract Set<String> getSSESubjects(PropertyMap bodyMap);

    protected ConfigIO parser_;
}
