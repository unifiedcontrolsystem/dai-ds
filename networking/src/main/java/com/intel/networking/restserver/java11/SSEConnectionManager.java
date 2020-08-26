// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver.java11;

import com.intel.logging.Logger;
import com.intel.networking.restserver.RESTServer;
import com.sun.net.httpserver.HttpExchange;

import java.io.Closeable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Description of class SSEConnectionManager. This is an internal class for HTTP open connection for SSE streams.
 */
class SSEConnectionManager implements AutoCloseable, Closeable {
    private static final long PING_INTERVAL_SECONDS =
            Integer.parseInt(System.getProperty("SSEConnectionManager.pingInterval", "90"));

    SSEConnectionManager(RESTServer server, Logger log) {
        server_ = server;
        log_ = log;
    }

    @Override
    public void close() throws IOException {
        synchronized (this) {
            while(connections_.size() > 0)
                removeConnection(connections_.get(0));
        }
        if(keepAliveThread_ != null) {
            stopKeepAliveThread_.set(true);
            try {
                keepAliveThread_.join();
            } catch (InterruptedException e) { /* Ignore */ }
        }
    }

    void addConnection(HttpExchange exchange, Collection<String> eventTypes) {
        log_.debug("*** Adding connection from %s...", exchange.getRemoteAddress().toString());
        SSEConnection connection = new SSEConnection(exchange, eventTypes);
        synchronized (this) {
            connections_.add(connection);
        }
        if(keepAliveThread_ == null) {
            keepAliveThread_ = new Thread(this::keepAlive);
            keepAliveThread_.start();
        }
    }

    void publishToConnections(String eventType, String data, String id) {
        List<SSEConnection> brokenConnections = new ArrayList<>();
        synchronized (this) {
            for(SSEConnection connection: connections_) {
                if(shouldEventBeFired(eventType, connection.exchange.getRequestURI().getPath(),
                        connection.eventTypes)) {
                    if(publishSSE(connection, eventType, data, id))
                        brokenConnections.add(connection);
                    else
                        connection.lastPublished = Instant.now().getEpochSecond();
                } else { // Need Ping message for connection keep alive?
                    if (expired(connection.lastPublished))
                        if (publishSSEPing(connection))
                            brokenConnections.add(connection);
                }
            }
            // Drop broken connections...
            for(SSEConnection connection: brokenConnections)
                removeConnection(connection);
        }
        globalLastPublish_.set(Instant.now().getEpochSecond());
    }

    private boolean shouldEventBeFired(String eventType, String path, Collection<String> wantedEventTypes) {
        return checkForPossibleEventTypes(eventType, path) && checkForWantedEventTypes(eventType, wantedEventTypes);
    }

    boolean checkForWantedEventTypes(String eventType, Collection<String> wantedEventTypes) {
        return wantedEventTypes == null || wantedEventTypes.isEmpty() || wantedEventTypes.contains(eventType);
    }

    boolean checkForPossibleEventTypes(String eventType, String path) {
        Collection<String> pathEventTypes = server_.getEventTypesFromPath(path);
        return pathEventTypes == null || pathEventTypes.isEmpty() || pathEventTypes.contains(eventType);
    }

    private boolean expired(long lastPublishTime) {
        return (lastPublishTime + PING_INTERVAL_SECONDS) < Instant.now().getEpochSecond();
    }

    private void publishToConnectionsPing() {
        List<SSEConnection> brokenConnections = new ArrayList<>();
        synchronized (this) {
            for(SSEConnection connection: connections_) {
                    if (expired(connection.lastPublished))
                        if (publishSSEPing(connection))
                            brokenConnections.add(connection);
            }
            // Drop broken connections...
            for(SSEConnection connection: brokenConnections)
                removeConnection(connection);
        }
        globalLastPublish_.set(Instant.now().getEpochSecond());
    }

    private boolean publishSSEPing(SSEConnection connection) {
        try {
            connection.exchange.getResponseBody().write(":Ping\n".getBytes(StandardCharsets.UTF_8));
            connection.exchange.getResponseBody().flush();
        } catch(IOException e) {
            String host = connection.exchange.getRemoteAddress().getHostName();
            int port = connection.exchange.getRemoteAddress().getPort();
            log_.exception(e, String.format("Failed to send Ping message to connection at '%s:%d', removing connection",
                    host, port));
            return true;
        }
        return false;
    }

    private boolean publishSSE(SSEConnection connection, String eventType, String data, String id) {
        String[] parts = data.split("\n");
        try {
            connection.exchange.getResponseBody().write(String.format("event:%s%n",
                    eventType.trim()).getBytes(StandardCharsets.UTF_8));
            if(id != null && !id.isBlank())
                connection.exchange.getResponseBody().write(String.format("id:%s%n", id).
                            getBytes(StandardCharsets.UTF_8));
            else if(FORCE_EVENT_ID)
                connection.exchange.getResponseBody().write(String.format("id:%d%n", idCounter_.getAndIncrement()).
                        getBytes(StandardCharsets.UTF_8));
            for (String part : parts)
                connection.exchange.getResponseBody().write(String.format("data:%s%n", part).
                        getBytes(StandardCharsets.UTF_8));
            connection.exchange.getResponseBody().write("\n".getBytes(StandardCharsets.UTF_8));
            connection.exchange.getResponseBody().flush();
        } catch(IOException e) {
            String host = connection.exchange.getRemoteAddress().getHostName();
            int port = connection.exchange.getRemoteAddress().getPort();
            log_.exception(e, String.format("Failed to send message to connection at '%s:%d' using event type '%s', removing connection",
                    host, port, eventType));
            return true;
        }
        return false;
    }

    private void removeConnection(SSEConnection connection) {
        log_.debug("*** Removing connection from %s...", connection.exchange.getRemoteAddress().toString());
        connections_.remove(connection);
        connection.exchange.close();
    }

    private void keepAlive() {
        while(!stopKeepAliveThread_.get()) {
            try { Thread.sleep(1000); }
            catch(InterruptedException e) { /* Ignore */ }// 1 second sleep interval...
            if(expired(globalLastPublish_.get()))
                publishToConnectionsPing();
        }
    }

    private List<SSEConnection> connections_ = new ArrayList<>();
    private RESTServer server_;
    private Logger log_;
    private AtomicLong globalLastPublish_ = new AtomicLong(Instant.now().getEpochSecond());
    private AtomicBoolean stopKeepAliveThread_ = new AtomicBoolean(false);
    private Thread keepAliveThread_ = null;
    private AtomicLong idCounter_ = new AtomicLong(0L);

    // Set to "true" to enable automatic event ID simulation, "false" to disable automatic event ID simulation.
    private static final boolean FORCE_EVENT_ID = true;

    static class SSEConnection {
        SSEConnection(HttpExchange exchange, Collection<String> eventTypes) {
            this.exchange = exchange;
            this.eventTypes = eventTypes;
            this.lastPublished = Instant.now().getEpochSecond();
        }

        final HttpExchange exchange;
        final Collection<String> eventTypes;
        long lastPublished;
    }
}
