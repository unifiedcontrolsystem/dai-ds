// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.source.restsse;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.networking.restserver.RESTServer;
import com.intel.networking.restserver.RESTServerException;
import com.intel.networking.restserver.RESTServerFactory;
import com.intel.networking.source.NetworkDataSource;
import com.intel.properties.PropertyMap;

import java.io.IOException;
import java.util.*;

/**
 * Description of class NetworkDataSourceSSE.
 */
public class NetworkDataSourceSSE implements NetworkDataSource {
    /**
     * Create and start the SSE based publisher.
     *
     * @param logger The logger to use in this implementation class.
     * @param args The arguments to the publisher. Supported argument keys are:
     *
     *   parser         - (def: "json")      The payload parser to use, mainly for SSE's "id:" tag.
     *   implementation - (def: "jdk11")     The implementation for the RESTServer.
     *   bindAddress    - (def: "127.0.0.1") The address to bind the server on.
     *   bindPort       - (def: 19216)       The port to bind the server on.
     *   urlPath        - (def: "/restsse/") The url to send subscription requests to.
     */
    public NetworkDataSourceSSE(Logger logger, Map<String,String> args) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance(args.getOrDefault("parser", "json"));
        if(parser_ == null) throw new RuntimeException("Failed to create a parser");
        startSSEServer(args);
    }

    /**
     * Optionally initialize the implementation.
     */
    @Override
    public void initialize() {
    }

    /**
     * This sets up the connection for the provider in the provider specific format.
     *
     * @param info The network connection string for the provider.
     */
    @Override
    public void connect(String info) {
        // This is a server not a client to a broker, this is not required.
    }

    /**
     * Sets the logger for the network source provider.
     *
     * @param logger Sets the {@link Logger} API instance into the provider so that it can also log errors/info to the
     *               owning process.
     */
    @Override
    public void setLogger(Logger logger) {
        if(logger != null)
            log_ = logger;
    }

    /**
     * Get the factory name of the implemented provider.
     *
     * @return The name of the provider to be created by the {@link ../NetworkDataSourceFactory}.
     */
    @Override
    public String getProviderName() {
        return "sse";
    }

    /**
     * Sends a message on a particular subject to the network.
     *
     * @param subject The subject to send the message for.
     * @param message The actual message to send.
     * @return True if the message was queued for delivery, false otherwise.
     */
    @Override
    public boolean sendMessage(String subject, String message) {
        String id = null;
        try {
            PropertyMap mapMessage = parser_.fromString(message).getAsMap();
            if(mapMessage != null) id = mapMessage.getStringOrDefault("sse_id", null);
        } catch(ConfigIOParseException e) { /* Ignore, we don't understand the format. */ }
        try {
            server_.ssePublish(subject, message, id);
            return true;
        } catch(RESTServerException e) {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        try {
            server_.stop();
            server_.close();
        } catch(RESTServerException e) {
            log_.exception(e, "Failed to stop the SSE REST server");
            throw new IOException("Failed to close the SSE REST server", e);
        }
    }

    private void startSSEServer(Map<String, String> args) {
        String implName = args.getOrDefault("implementation", "jdk11");
        try {
            server_ = RESTServerFactory.getSingletonInstance(implName, log_);
            assert server_ != null: "Failed to get a RESTServer implementation called '" + implName + "'";
            server_.setAddress(args.getOrDefault("bindAddress", "127.0.0.1"));
            server_.setPort(Integer.parseInt(args.getOrDefault("bindPort", "19216")));
            server_.addSSEHandler(args.getOrDefault("urlPath", "/restsse/"), null); // null means support all subjects.
            server_.start();
        } catch(RESTServerException e) {
            log_.exception(e, "Failed to create a REST server implementation called '%s'", implName);
            throw new RuntimeException("Failed to create the SSE REST server", e);
        }
    }

    private Logger log_;
    private RESTServer server_;
    private ConfigIO parser_;
}
