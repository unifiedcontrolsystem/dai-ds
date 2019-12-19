// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.builder;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.networking.sink.http_callback.SubscriptionRequestBuilder;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Description of class ForeignSubscriptionRequest.
 */
public class ForeignSubscriptionRequest implements SubscriptionRequestBuilder {
    public ForeignSubscriptionRequest() {
        roles_ = new PropertyArray(new ArrayList<String>(){{
            add("Compute");
            add("Service");
            add("System");
            add("Application");
            add("Storage");
        }});
        statuses_ = new PropertyArray(new ArrayList<String>() {{
            add("Unknown");
            add("AdminDown");
            add("Others");
        }});
        states_ = new PropertyArray(new ArrayList<String>(){{
            add("Unknown");
            add("Empty");
            add("Populated");
            add("Off");
            add("On");
            add("Active");
            add("Standby");
            add("Halt");
            add("Ready");
            add("Paused");
        }});
    }

    @Override
    public String buildRequest(Collection<String> subjects, String subscriberID, String callbackUrl) {
        ConfigIO parser = ConfigIOFactory.getInstance("json");
        if(parser == null) return null;
        PropertyMap document = new PropertyMap();
        document.put("Subscriber", subscriberID);
        document.put("Enabled", true);
        document.put("Url", callbackUrl);
        document.put("Roles", roles_);
        document.put("SoftwareStatus", statuses_);
        document.put("States", states_);
        return parser.toString(document);
    }

    private PropertyArray roles_;
    private PropertyArray statuses_;
    private PropertyArray states_;
}
