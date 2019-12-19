// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.builder;

import com.intel.networking.restclient.SSERequestBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Description of class SSEStreamBuilder.
 */
public class SSEStreamRequestBuilder implements SSERequestBuilder {
    public SSEStreamRequestBuilder() { }

    @Override
    public String buildRequest(Collection<String> eventTypes, Map<String, String> builderSpecific) {
        StringBuilder result = new StringBuilder();
        for(String key: builderSpecific.keySet()) {
            if(allowedKeys_.contains(key)) {
                if(result.length() == 0)
                    result.append("?");
                else
                    result.append("&");
                String baseKey = key.split("\\.")[1];
                result.append(baseKey).append("=").append(builderSpecific.get(key));
            }
        }
        return result.toString();
    }

    private static final List<String> allowedKeys_ = new ArrayList<>() {{
        add("requestBuilderSelectors.stream_id");
        add("requestBuilderSelectors.count");
        add("requestBuilderSelectors.batchsize");
        add("requestBuilderSelectors.cname");
    }};
}
