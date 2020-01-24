// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver.java11;

import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.intel.logging.Logger;
import com.intel.networking.HttpMethod;
import com.intel.networking.restserver.*;
import com.intel.properties.PropertyMap;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;

/**
 * Description of class Java11RESTServer.
 */
public class Java11RESTServer extends RESTServer {
    private static final int DEFAULT_QUEUED_CONNECTIONS =
            Integer.parseInt(System.getProperty("Java11RESTServer.maxQueuedConnections", "100"));
    private static final int DEFAULT_SERVER_LINGER_SECONDS =
            Integer.parseInt(System.getProperty("Java11RESTServer.serverLingerSeconds", "5"));
    private static final long CONNECTION_LINGER =
            Long.parseLong(System.getProperty("Java11RESTServer.connectionLinger", "333"));

    public Java11RESTServer(Logger log) throws RESTServerException {
        super(log);
    }

    @Override
    public void ssePublish(String eventType, String data, String id) throws RESTServerException {
        if(running_.get())
            connectionManager_.publishToConnections(eventType, data, id);
        else
            throw new RESTServerException("Cannot publish an SSE packet until the server is running");
    }

    @Override
    protected void startServer() throws RESTServerException {
        lazyCreation();
        try {
            if(getAddress().equals("*"))
                server_.bind(new InetSocketAddress(getPort()), DEFAULT_QUEUED_CONNECTIONS);
            else
                server_.bind(new InetSocketAddress(getAddress(), getPort()), DEFAULT_QUEUED_CONNECTIONS);

            server_.start();
            running_.set(true);
        } catch (IOException cause) {
            server_ = null;
            try { connectionManager_.close(); } catch(IOException e) { /* Cannot happen! */ }
            connectionManager_ = null;
            failedServer_ = true;
            throw new RESTServerException("Failed to bind the Java11RESTServer", cause);
        }
    }

    @Override
    protected void stopServer() throws RESTServerException {
        checkFailedServer();
        running_.set(false);
        try { connectionManager_.close(); } catch (IOException e) { /* Cannot happen! */ }
        connectionManager_ = null;
        server_.stop(DEFAULT_SERVER_LINGER_SECONDS);
        server_ = null;
    }

    @Override
    protected void addInternalRouteUrl(RouteObject route) throws RESTServerException {
        lazyCreation();
    }

    @Override
    protected void addInternalRouteMethod(RouteObject route) throws RESTServerException {
        // Not needed for this implementation!!!
    }

    @Override
    protected void removeInternalRouteUrl(RouteObject route) throws RESTServerException {
        checkFailedServer();
        server_.removeContext(route.url);
    }

    @Override
    protected void removeInternalRouteMethod(RouteObject route) throws RESTServerException {
        // Not needed for this implementation!!!
    }

    protected SSEConnectionManager managerCreate() {
        return new SSEConnectionManager(this, log_);
    }

    protected HttpServer serverCreate() throws IOException {
        return HttpServer.create();
    }

    void urlHandler(HttpExchange exchange) {
        exchange.getResponseHeaders().put("Content-Type", new ArrayList<>() {{ add("application/json"); }});
        RouteObject route = getRouteFromExchange(exchange);
        if(route == null) {
            doError(exchange, 404, "Route not found on server", null);
            return;
        }
        if(route.sseSupport)
            sseHandler(route, exchange);
        else
            userHandler(route, exchange);
    }

    private void sseHandler(RouteObject route, HttpExchange exchange) {
        try {
            PropertyMap params = new PropertyMap();
            for(String pair: exchange.getRequestURI().getQuery().split("&")) {
                String[] parts = pair.split("=");
                params.put(parts[0], parts[1]);
            }
            route.eventTypes = requestTranslator_.getSSESubjects(params);
            exchange.getResponseHeaders().put("Connection", new ArrayList<>() {{ add("keep-alive"); }});
            exchange.getResponseHeaders().put("Content-Type", new ArrayList<>() {{ add("text/event-stream"); }});
            exchange.sendResponseHeaders(200, 0L);
            exchange.getResponseBody().write(":Accepted\n".getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().flush();
            connectionManager_.addConnection(exchange, route.eventTypes);
        } catch(IOException e) {
            doError(exchange, 500, "Failed to send SSE acknowledgement", e);
        }
    }

    private void userHandler(RouteObject route, HttpExchange exchange) {
        HttpExchangeRequest request = new HttpExchangeRequest(exchange);
        HttpExchangeResponse response = new HttpExchangeResponse(exchange);
        try {
            route.handler.handle(request, response);
            doReply(response);
        } catch(IOException e) {
            doError(exchange, 500, "Failed to send reply from server", e);
        } catch(Exception cause) {
            doError(exchange, 500, "User handler threw an exception on server", cause);
        }
    }

    private void doReply(HttpExchangeResponse response) throws IOException {
        HttpExchange exchange = response.exchange_;
        for(String key: response.headers_.keySet())
            exchange.getResponseHeaders().put(key, new ArrayList<>() {{ add(response.headers_.get(key)); }});
        exchange.sendResponseHeaders(response.code_, response.body_.length());
        exchange.getResponseBody().write(response.body_.getBytes(StandardCharsets.UTF_8));
        exchange.getResponseBody().flush();
        try { Thread.sleep(CONNECTION_LINGER); } catch(InterruptedException e) { /* Ignore interrupt */ }
        exchange.close();
    }

    private void doError(HttpExchange exchange, int code, String errorMessage, Throwable cause) {
        try {
            PropertyMap responseDocument = responseTranslator_.makeError(code, errorMessage, exchange.getRequestURI(),
                    exchange.getRequestMethod(), cause);
            String errorBody = responseTranslator_.toString(responseDocument);
            exchange.sendResponseHeaders(code, errorBody.length());
            OutputStream responseBody = exchange.getResponseBody();
            responseBody.write(errorBody.getBytes(StandardCharsets.UTF_8));
            exchange.getResponseBody().flush();
            try { Thread.sleep(CONNECTION_LINGER); } catch(InterruptedException e) { /* Ignore interrupt */ }
            responseBody.close();
        } catch(IOException e) {
            log_.exception(e, "Failed to send error reply to an exchange before closing the connection");
        }
        exchange.close();
    }

    private RouteObject getRouteFromExchange(HttpExchange exchange) {
        HttpMethod method = HttpMethod.valueOf(exchange.getRequestMethod().toUpperCase());
        String path = exchange.getRequestURI().getPath();
        return matchUrlPathAndMethod(path, method);
    }

    private void lazyCreation() throws RESTServerException {
        checkFailedServer();
        try {
            server_ = serverCreate();
            connectionManager_ = managerCreate();
            server_.createContext("/", this::urlHandler);
        } catch (IOException cause) {
            failedServer_ = true;
            throw new RESTServerException("Failed to create the Java11RESTServer", cause);
        }
    }

    private void checkFailedServer() throws RESTServerException {
        if(failedServer_)
            throw new RESTServerException("After failure of server to start, this object is not usable!");
    }

    private boolean failedServer_ = false;
    private HttpServer server_ = null;
    private SSEConnectionManager connectionManager_ = null;
}
