package com.intel.dai.builder;

import com.intel.networking.restclient.SSERequestBuilder;

import java.util.Collection;
import java.util.Map;

public class CreateForeignStreamRequest implements SSERequestBuilder {
    @Override
    public String buildRequest(Collection<String> eventTypes, Map<String, String> builderSpecific) {
        String subUri = "subjects=" + eventTypes.toArray()[0];
        for(String key : builderSpecific.keySet()) {
            String value = builderSpecific.get(key);
            subUri += '&' + key + "=" + value;
        }
        return "?" + subUri;
    }
}
