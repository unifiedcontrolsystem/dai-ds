// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restserver;

import com.intel.logging.Logger;
import com.intel.networking.HttpMethod;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Description of class RESTServer.
 */
public abstract class RESTServer implements AutoCloseable, Closeable {
    /**
     * Default port used for the RESTServer to bind to on the specified or default address(es).
     */
    private final static int DEFAULT_PORT = 5678; // Randomly chosen default port.
    /**
     * Default addresses used for the RESTServer to bind to on the specified or default port. A String of "*"
     * indicates all interfaces will be listening.
     */
    private final static String DEFAULT_ADDRESS = "*"; // bind all interfaces by default...

    /**
     * Construct a RESTServer object with a address and port. All implementations must declare a ctor with this
     * signature.
     *
     * @throws RESTServerException Will not throw unless the DEFAULT_ADDRESS or DEFAULT_PORT is changed to
     * illegal values.
     */
    public RESTServer(Logger log) throws RESTServerException {
        setLogger(log);
        setAddress(DEFAULT_ADDRESS);
        setPort(DEFAULT_PORT);
    }

    /**
     * Set the logger into the REST server.
     *
     * @param log The logger to use for logging, cannot be null.
     */
    public final void setLogger(Logger log) throws RESTServerException {
        if(log == null) throw new RESTServerException("The logger used cannot be null");
        log_ = log;
    }

    /**
     * Get accessor for the port value for the server.
     *
     * @return The int value of the currently set port.
     */
    public final int getPort() { return port_; }

    /**
     * Set accessor for the port value for the server.
     *
     * @param port An integer between 1024 and 65535 inclusive.
     * @throws RESTServerException If the port is out of range or the server is already running.
     */
    public final void setPort(int port) throws RESTServerException {
        if(isRunning()) throw new RESTServerException("Server is already running, cannot set port");
        if(port < 1024 || port >65535) throw new RESTServerException("Bad port (outside allowed range)");
        port_ = port;
    }

    /**
     * Get accessor for the address value for the server.
     *
     * @return The String value of the currently set address.
     */
    public final String getAddress() { return address_; }

    /**
     * Set accessor for the port value for the server.
     *
     * @param address An non-null, non-blank string with the bind address for the server.
     * @throws RESTServerException If the address null or blank or the server is already running.
     */
    public final void setAddress(String address) throws RESTServerException {
        if(isRunning()) throw new RESTServerException("Server is already running, cannot set port");
        if(address == null || address.isBlank()) throw new RESTServerException("Bad address (null or blank)");
        address_ = address.trim();
    }

    /**
     * Set the translator for error and normal response messages. Based on PropertyMap and can be converted to JSON.
     *
     * @param translator The implementation to use. Cannot be null.
     * @throws IllegalArgumentException if null is passed as the argument.
     */
    public final void setResponseTranslator(ResponseTranslator translator) {
        if(translator == null) throw new IllegalArgumentException("argument translator must not be null");
        responseTranslator_ = translator;
    }

    /**
     * Set the RequestTranslator for the RESTServer including SSE subject request translation.
     *
     * @param translator The request translator to interpret incoming HTTP requests. Cannot be null.
     */
    public final void setRequestTranslator(RequestTranslator translator) {
        if(translator == null) throw new IllegalArgumentException("argument translator must not be null");
        requestTranslator_ = translator;
    }

    /**
     * Starts the server. Will succeed silently if the server is already running.
     *
     * @throws RESTServerException If the server fails to start.
     */
    public final void start() throws RESTServerException {
        if(!isRunning())
            startServer();
    }

    /**
     * Stops the server. Will succeed silently if the server is not running.
     *
     * @throws RESTServerException If the server fails to stop.
     */
    public final void stop() throws RESTServerException {
        if(isRunning())
            stopServer();
    }

    /**
     * Checks to see if the server is running.
     *
     * @return true if the server is running, false otherwise.
     */
    public final boolean isRunning() {
        return running_.get();
    }

    /**
     * Adds a route handler into the REST server.
     *
     * @param url The path of the url to map in the REST server (must not contain query, scheme or authority).
     * @param method The HTTP method to map in conjunction with the url above.
     * @param handler Tha callback that will get called when the REST server received a request on a matching url
     *               and method.
     * @throws RESTServerException If the underlying implementation fails to set the route.
     */
    public final void addHandler(String url, HttpMethod method, RESTServerHandler handler)
            throws RESTServerException {
        synchronized (this) {
            if (routes_.containsKey(url) && routes_.get(url).containsKey(method)) {
                log_.warn("Route '%s' using method '%s' is already mapped; will not override existing handler", url,
                        method);
                return;
            }
            RouteObject route = new RouteObject(url, method, handler, false, null);
            if (routes_.containsKey(route.url)) {
                routes_.get(route.url).put(method, route);
                addInternalRouteMethod(route);
                log_.debug("*** Added route method %s to the existing URL %s", route.method, route.url);
            } else {
                routes_.put(route.url, new HashMap<>() {{
                    put(method, route);
                }});
                addInternalRouteUrl(route);
                log_.debug("*** Added route method %s to the new URL %s", route.method, route.url);
            }
        }
    }

    /**
     * Add a URL to be used for SSE publishing.
     *
     * @param url The URL that responds to SSE eventing requests.
     * @throws RESTServerException  If the underlying implementation fails to set the route.
     */
    public final void addSSEHandler(String url, Collection<String> possibleEventTypes) throws RESTServerException {
        synchronized (this) {
            HttpMethod method = HttpMethod.GET;
            if (routes_.containsKey(url) && routes_.get(url).containsKey(method))
                throw new RESTServerException(String.format("Route '%s' using method '%s' is already mapped", url,
                        method));
            if(url.endsWith("/*"))
                throw new RESTServerException("SSE Routes cannot be wildcard-ed with a '*' at the end");
            RouteObject route = new RouteObject(url, method, null, true, possibleEventTypes);
            if (routes_.containsKey(url)) {
                routes_.get(url).put(method, route);
                addInternalRouteMethod(route);
            } else {
                routes_.put(url, new HashMap<>() {{
                    put(method, route);
                }});
                addInternalRouteUrl(route);
            }
        }
    }

    /**
     * Removes a route handler from the REST server.
     *
     * @param url The path of the url to map in the REST server (must not contain query, scheme or authority).
     * @param method The HTTP method to map in conjunction with the url above.
     * @throws RESTServerException If the underlying implementation fails to remove the route.
     */
    public final void removeHandler(String url, HttpMethod method) throws RESTServerException {
        synchronized (this) {
            if (routes_.containsKey(url) && routes_.get(url).containsKey(method)) {
                RouteObject removing = routes_.get(url).get(method);
                routes_.get(url).remove(method);
                removeInternalRouteMethod(removing);
                if (routes_.get(url).size() == 0) {
                    routes_.remove(url);
                    removeInternalRouteUrl(removing);
                }
            }
            else
                throw new RESTServerException(String.format("Route '%s' and method '%s' is not mapped", url, method));
        }
    }

    /**
     * Gets the callback handler matching the url and method.
     *
     * @param url The url path to match against routes.
     * @param method The HTTP method to match against the routes.
     * @return Either the RESTServerHandler object or null if not found.
     */
    public final RESTServerHandler getHandler(String url, HttpMethod method) {
        RESTServerHandler result = null;
        synchronized (this) {
            if (routes_.containsKey(url) && routes_.get(url).containsKey(method))
                result = routes_.get(url).get(method).handler;
        }
        return result;
    }

    /**
     * Gets the SSE events type collection or null.
     *
     * @param url The url path to match (method POST is assumed for SSE).
     * @return The collection of supported event types for this url path or null for "any" event type.
     */
    public Collection<String> getEventTypesFromPath(String url) {
        Collection<String> result = null;
        synchronized (this) {
            if (routes_.containsKey(url) && routes_.get(url).containsKey(HttpMethod.GET))
                result = routes_.get(url).get(HttpMethod.GET).eventTypes;
        }
        return result;
    }

    /**
     * Publish via SSE to all listening and subject subscribed clients.
     *
     * @param subject The subject the data is published against.
     * @param data The data to send.
     * @param id SSE "id' field.
     * @throws RESTServerException When the implementation cannot send the event.
     */
    abstract public void ssePublish(String subject, String data, String id) throws RESTServerException;

    /**
     * Implementation MUST implement this to start the implementation server.
     *
     * @throws RESTServerException If the implementation cannot start the server.
     */
    protected abstract void startServer() throws RESTServerException;

    /**
     * Implementation MUST implement this to stop the implementation server.
     *
     * @throws RESTServerException  If the implementation cannot stop the server.
     */
    protected abstract void stopServer() throws RESTServerException;

    /**
     * Implementation must implement this method to add a new URL and a method to the routes for the internal
     * implementation.
     *
     * @param route The route object to use to configure the underlying server implementation.
     * @throws RESTServerException If the route cannot be added to the underlying implementation. NOTE: cleanup of the
     * routes_ map is left to the caller of the API's addHandler method.
     */
    protected abstract void addInternalRouteUrl(RouteObject route) throws RESTServerException;

    /**
     * Implementation must implement this method to add a existing URL and a new method to the routes for the internal
     * implementation.
     *
     * @param route The route object to use to configure the underlying server implementation.
     * @throws RESTServerException If the route cannot be added to the underlying implementation. NOTE: cleanup of the
     * routes_ map is left to the caller of the API's addHandler method.
     */
    protected abstract void addInternalRouteMethod(RouteObject route) throws RESTServerException;

    /**
     * Allow the caller to remove a route URL from the routes in the underlying implementation.
     *
     * @param route The route (url and method) to remove from the underlying implementation.
     * @throws RESTServerException When the underlying route URL cannot be removed.
     */
    protected abstract void removeInternalRouteUrl(RouteObject route) throws RESTServerException;

    /**
     * Allow the caller to remove a route method from the routes in the underlying implementation.
     *
     * @param route The route (url and method) to remove from the underlying implementation.
     * @throws RESTServerException When the underlying route URL cannot be removed.
     */
    protected abstract void removeInternalRouteMethod(RouteObject route) throws RESTServerException;

    @Override
    public void close() throws IOException {
        try {
            stop();
        } catch(RESTServerException e) {
            log_.exception(e, "Failed to close RESTServer implementation");
        }
    }

    /**
     * Match the maximum path in the routes to the passed path and method.
     *
     * @param path The path received via HTTP.
     * @param method The method received via HTTP.
     * @return Either null on no match or the RouteObject for a match.
     */
    protected RouteObject matchUrlPathAndMethod(String path, HttpMethod method) {
        Map<HttpMethod, RouteObject> routeMap;
        RouteObject route;
        for(String possiblePath: routes_.descendingKeySet()) {
            routeMap = routes_.get(possiblePath);
            if(routeMap.containsKey(method)) {
                route = routeMap.get(method);
                if(route.wildcard && path.startsWith(possiblePath))
                    return route;
                else if(path.equals(possiblePath))
                    return route;
            }
        }
        return null;
    }

    private int port_;
    private String address_;
    protected Logger log_;
    protected AtomicBoolean running_ = new AtomicBoolean(false);
    protected TreeMap<String, Map<HttpMethod,RouteObject>> routes_ = new TreeMap<>();
    protected ResponseTranslator responseTranslator_ = new DefaultResponseTranslator();
    protected RequestTranslator requestTranslator_ = new DefaultRequestTranslator();
}
