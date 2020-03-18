// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.networking.restclient.SSERequestBuilder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * SSEStreamBuilder implementation for requests for foreign bus systems.
 */
public class SSEStreamRequestBuilder implements SSERequestBuilder {
    public SSEStreamRequestBuilder() { }

    @Override
    public String buildRequest(Collection<String> eventTypes, Map<String, String> builderSpecific) {
        StringBuilder result = new StringBuilder();
        for(Map.Entry<String,String> entry : builderSpecific.entrySet()) {
            if(allowedKeys_.contains(entry.getKey())) {
                if(result.length() == 0)
                    result.append("?");
                else
                    result.append("&");
                String baseKey = entry.getKey().split("\\.")[1];
                result.append(baseKey).append("=").append(entry.getValue());
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
