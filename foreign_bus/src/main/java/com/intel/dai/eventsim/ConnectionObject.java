package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restclient.RESTClientFactory;
import com.intel.properties.PropertyMap;

import java.net.URI;
import java.util.Map;

public class ConnectionObject {

    ConnectionObject(String url, String subscriber, Logger log) {
        url_ = url;
        subscriber_ = subscriber;
        log_ = log;
    }

    private void createClient() throws RESTClientException {
        restClient_ = RESTClientFactory.getInstance("jdk11", log_);
        if(restClient_ == null)
            throw new RESTClientException("Failed to get the REST client implementation");
    }

    protected void initialiseProperties(Map<String, String> properties) {
        for(String prop : properties.keySet())
            prop_.put(prop, properties.get(prop));
    }

    protected PropertyMap connProperties() {
        return prop_;
    }

    public boolean publish(String uri, String message) throws RESTClientException {
        if(restClient_ == null)
            createClient();
        try {
            restClient_.postRESTRequestBlocking(URI.create(uri), message);
            return true;
        } catch (RESTClientException e) {
           createClient();
           publish(uri, message);
        }
        return false;
    }

    String url_;
    String subscriber_;
    private PropertyMap prop_ = new PropertyMap();
    private final Logger log_;
    RESTClient restClient_ = null;
}
