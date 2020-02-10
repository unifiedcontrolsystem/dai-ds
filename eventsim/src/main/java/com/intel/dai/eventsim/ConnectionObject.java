package com.intel.dai.eventsim;

import com.intel.properties.PropertyMap;

import java.util.Map;

public class ConnectionObject {

    ConnectionObject(String url, String subscriber) {
        url_ = url;
        subscriber_ = subscriber;
    }

    protected void initialiseProperties(Map<String, String> properties) {
        for(String prop : properties.keySet())
            prop_.put(prop, properties.get(prop));
    }

    protected PropertyMap connProperties() {
        return prop_;
    }

    final String url_;
    final String subscriber_;
    private final PropertyMap prop_ = new PropertyMap();
}
