package com.intel.dai.eventsim;

import com.intel.properties.PropertyMap;

import java.util.Map;

/**
 * Description of class ConnectionObject.
 * creates a unique object for each url and subscriber details
 * store/fetch other property details came along with subscription request
 */
public class ConnectionObject {

    ConnectionObject(String url, String subscriber) {
        url_ = url;
        subscriber_ = subscriber;
    }

    /**
     * This method is to used to store other property details came with subscription request
     */
    protected void initialiseProperties(Map<String, String> properties) {
        for(Map.Entry<String, String> property : properties.entrySet())
            prop_.put(property.getKey(), property.getValue());
    }

    /**
     * This method is to used to fetch other property details came with subscription request
     */
    protected PropertyMap connProperties() {
        return prop_;
    }

    final String url_;
    final String subscriber_;
    private final PropertyMap prop_ = new PropertyMap();
}
