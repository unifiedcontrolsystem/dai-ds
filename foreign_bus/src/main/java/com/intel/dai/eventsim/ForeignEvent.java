// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.properties.PropertyMap;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;


public abstract class ForeignEvent {

    PropertyMap props_;
    protected ConfigIO parser_;
    private DateFormat format;

    ForeignEvent() {
        props_ = new PropertyMap();
        parser_ = ConfigIOFactory.getInstance("json");
        setDateFormat();
    }
    public void setTimestamp(long timeValueMicro) {
        Date date = new Date(TimeUnit.MICROSECONDS.toMillis(timeValueMicro));
        props_.put("timestamp", format.format(date));
    }

    public String getTimestampString() {
        return props_.getStringOrDefault("timestamp", "");
    }

    public abstract String getEventCategory();

    public void setLocation(String location) {
        props_.put("location", location);
        // Hardcode this for now, until we get more information on what src corresponds to
        props_.put("src", location);
    }

    public String getLocation() {
        return props_.getStringOrDefault("location", "");
    }

    public String getJSON() {
        return parser_.toString(props_);
    }

    private void setDateFormat() {
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSX");
    }
}
