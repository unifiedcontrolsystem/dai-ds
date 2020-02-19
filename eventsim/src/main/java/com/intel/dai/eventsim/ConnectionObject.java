package com.intel.dai.eventsim;

import com.intel.properties.PropertyMap;

import java.util.Map;

public class ConnectionObject {

    ConnectionObject(String url, String subscriber) {
        url_ = url;
        subscriber_ = subscriber;
    }

    protected void initialiseProperties(Map<String, String> properties) {
        for(Map.Entry<String, String> property : properties.entrySet())
            prop_.put(property.getKey(), property.getValue());
    }

    protected PropertyMap connProperties() {
        return prop_;
    }

    final String url_;
    final String subscriber_;
    private final PropertyMap prop_ = new PropertyMap();
}
