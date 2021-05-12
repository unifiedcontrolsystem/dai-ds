// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.Location;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.intel.runtime_utils.TimeUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

class HPCMEnvelopeProcessing {
    HPCMEnvelopeProcessing(Logger logger, DataStoreFactory factory) {
        log_ = logger;
        locations_ = factory.createLocation();
    }

    EnvelopeData getLocationAndTimestamp(String topic, PropertyMap item)
            throws PropertyNotExpectedType, ParseException, ConversionException {
        long ts = -1L;
        for(String name: possibleTimestampNames_) {
            try {
                ts = TimeUtils.millisecondsToNanoseconds(item.getLong(name));
            } catch (PropertyNotExpectedType e) {
                String tsString = item.getString(name);
                if(tsString != null)
                    ts = TimeUtils.nSFromIso8601(tsString);
            }
        }
        if(ts == -1L)
            ts = TimeUtils.getNsTimestamp();

        String location = "";
        for(String name: possibleLocationNames_) {
            if (item.containsKey(name))
                location = CommonFunctions.convertForeignToLocation(item.getString("location"));
            if(!location.isEmpty())
                break;
        }
        if(location.isEmpty()) {
            for(String name: possibleHostNames_) {
                if (item.containsKey(name))
                    location = hostnameToLocation(item.getString(name));
                if(!location.isEmpty())
                    break;
            }
        }
        if(location.isEmpty()) {
            for(String name: possibleIpNames_) {
                if (item.containsKey(name))
                    location = ipToLocation(item.getString(name));
                if(!location.isEmpty())
                    break;
            }
        }
        if(location.isEmpty()) {
            for(String name: possibleMacNames_) {
                if (item.containsKey(name))
                    location = macToLocation(item.getString(name));
                if(!location.isEmpty())
                    break;
            }
        }
        if(location.isEmpty())
            log_.warn("The location could not be resolved from the given JSON and so is an empty String.");
        return new EnvelopeData(topic, ts, location);
    }

    private String hostnameToLocation(String host) {
        return blankNullString(locations_.getLocationFromHostname(host));
    }

    private String ipToLocation(String ip) {
        return blankNullString(locations_.getLocationFromIP(ip));
    }

    private String macToLocation(String mac) {
        return blankNullString(locations_.getLocationFromMAC(mac));
    }

    private String blankNullString(String str) {
        if(str == null)
            return "";
        return str;
    }

    private final List<String> possibleLocationNames_ = new ArrayList<>() {{
        add("location");
    }};
    private final List<String> possibleTimestampNames_ = new ArrayList<>() {{
        add("timestamp");
        add("time");
        add("Timestamp");
    }};
    private final List<String> possibleHostNames_ = new ArrayList<>() {{
        add("host");
        add("sw_node_name");
    }};
    private final List<String> possibleIpNames_ = new ArrayList<>() {{
        add("IP");
    }};
    private final List<String> possibleMacNames_ = new ArrayList<>() {{
        add("MAC");
    }};
    private final Logger log_;
    private final Location locations_;
}
