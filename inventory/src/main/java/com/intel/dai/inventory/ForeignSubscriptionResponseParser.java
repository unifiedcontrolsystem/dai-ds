// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.networking.sink.http_callback.SubscriptionResponseParser;
import com.intel.properties.PropertyMap;

import java.net.URI;

/**
 * Description of class ForeignSubscriptionResponseParser.
 */
public class ForeignSubscriptionResponseParser implements SubscriptionResponseParser {
    public ForeignSubscriptionResponseParser() {}

    /**
     * This method is used to parser response receiver.
     */
    @Override
    public URI parseResponse(String message, String subscriptionUri) {
        ConfigIO parser = ConfigIOFactory.getInstance("json");
        if(parser == null) return null;
        try {
            PropertyMap map = parser.fromString(message).getAsMap();
            String id = map.getStringOrDefault("ID", null);
            if(id == null || id.isBlank()) return null;
            StringBuilder builder = new StringBuilder().append(subscriptionUri);
            if(!subscriptionUri.endsWith("/"))
                builder.append("/");
            builder.append(id);
            return URI.create(builder.toString());
        } catch(ConfigIOParseException e) {
            return null;
        }
    }
}
