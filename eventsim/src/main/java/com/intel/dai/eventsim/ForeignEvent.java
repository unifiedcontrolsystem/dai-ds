package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.properties.PropertyMap;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

/**
 * Description of class ForeignEvent.
 * creates events w.r.t event type
 */
public abstract class ForeignEvent {

    ForeignEvent() {
        props_ = new PropertyMap();
        parser_ = ConfigIOFactory.getInstance("json");
        setDateFormat();
    }

    /**
     * This method is used to set creation time of events
     */
    public void setTimestamp(long timeValueMicro) {
        Date date = new Date(TimeUnit.MICROSECONDS.toMillis(timeValueMicro));
        props_.put("timestamp", format.format(date));
    }

    /**
     * This method is used to fetch creation time of events
     */
    String getTimestampString() {
        return props_.getStringOrDefault("timestamp", "");
    }


    /**
     * This method is used to set location where event is created.
     */
    public void setLocation(String location) {
        props_.put("location", location);
        // Hardcode this for now, until we get more information on what src corresponds to
        props_.put("src", location);
    }

    /**
     * This method is used to fetch location where event is created.
     */
    public String getLocation() {
        return props_.getStringOrDefault("location", "");
    }

    /**
     * This method is used to fetch json in string format.
     */
    public String getJSON() {
        return parser_.toString(props_);
    }

    /**
     * This method is used to set time format.
     */
    private void setDateFormat() {
        format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSX");
    }

    enum EVENT_SUB_TYPE {
        events,
        stateChanges,
        telemetry
    }

    PropertyMap props_;
    private ConfigIO parser_;
    private DateFormat format;
}