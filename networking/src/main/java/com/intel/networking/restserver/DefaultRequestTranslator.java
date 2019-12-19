// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;

import java.util.HashSet;
import java.util.Set;

/**
 * Description of class DefaultRequestTranslator.
 */
public class DefaultRequestTranslator extends RequestTranslator {
    /**
     * Create a default RequestTranslator with SSE support as key "subjects" specifying a array or strings.
     */
    public DefaultRequestTranslator() {
        super();
    }

    /**
     * For SSE Requests, gets the set of subjects requested from the parsed HTTP request body.
     *
     * @param bodyMap the parsed body PropertyMap returned from getBodyMap.
     * @return The set of subject strings. Never null but may be empty signifying send all subjects.
     */
    @Override
    public Set<String> getSSESubjects(PropertyMap bodyMap) {
        Set<String> result = new HashSet<>();
        for(Object obj: bodyMap.getArrayOrDefault("subjects", new PropertyArray()))
            result.add(obj.toString());
        return result;
    }
}
