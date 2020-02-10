package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.sun.istack.NotNull;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class ConnectionManager {

    ConnectionManager(RESTClient restClient, Logger logger) {
        restClient_ = restClient;
        log_ = logger;
    }

    public boolean addSubscription(@NotNull final String url, @NotNull final String subscriber, Map<String, String> parameters) throws RESTClientException {
        if (subscriber == null || url == null)
            throw new RESTClientException("Could not add subscription: url or subscriber null value(s)");
        if (!checkSubscription(url, subscriber)) {
            ConnectionObject connection = new ConnectionObject(url, subscriber);
            connection.initialiseProperties(parameters);
            connectionsToId.put(connection, connId);
            idToConnections.put(connId++, connection);
            log_.info(String.format("*** Added subscripion for given URL = %s, subscriber = %s ***", url, subscriber));
            return true;
        }
        return false;
    }

    public PropertyMap getAllSubscriptions() {
        if (connectionsToId.size() == 0) {
            return null;
        }
        PropertyArray result = new PropertyArray();
        for (ConnectionObject connectionObject : connectionsToId.keySet()) {
            PropertyMap data = new PropertyMap();
            data.put("ID", connectionsToId.get(connectionObject));
            data.putAll(connectionObject.connProperties());
            result.add(data);
        }
        PropertyMap output = new PropertyMap();
        output.put("SubscriptionList", result);
        return output;
    }

    public PropertyMap getSubscription(@NotNull final String url, @NotNull final String subscriber) throws RESTClientException {
        if (url == null || subscriber == null)
            throw new RESTClientException("Insufficient details to get subscription: url or subscriber null value(s)");
        for (ConnectionObject connection : connectionsToId.keySet()) {
            if (connection.url_.equals(url) && connection.subscriber_.equals(subscriber)) {
                PropertyMap result = new PropertyMap();
                result.put("ID", connectionsToId.get(connection));
                result.putAll(connection.connProperties());
                return result;
            }
        }
        return null;
    }

    public PropertyMap getSubscriptionForId(long id) {
        ConnectionObject conn = idToConnections.get(id);
        if (conn != null) {
            return conn.connProperties();
        }
        return null;
    }

    public boolean removeSubscriptionId(final long removeId) {
        if (idToConnections.containsKey(removeId)) {
            ConnectionObject conn = idToConnections.get(removeId);
            idToConnections.remove(removeId);
            connectionsToId.remove(conn);
            log_.info(String.format("*** Removed subscripion for given id = %s***", removeId));
            return true;
        }
        return false;
    }

    public boolean removeAllSubscriptions() {
        idToConnections.clear();
        connectionsToId.clear();
        connId = 1;
        log_.info("*** Removed all existing subscriptions ***");
        return true;
    }

    private boolean checkSubscription(final String url, final String subscriber) {
        if (connectionsToId.size() == 0)
            return false;
        for (ConnectionObject connectionObject : connectionsToId.keySet())
            if (connectionObject.url_.equals(url) && connectionObject.subscriber_.equals(subscriber))
                return true;
        return false;
    }

    private Set<ConnectionObject> getConnections() {
        return connectionsToId.keySet();
    }

    public void publish(final List<String> events, final boolean constantMode, final long timeDelayMus) throws RESTClientException {
        long publishedEvents = 0;
        long droppedEvents = 0;
        if(getConnections().size() == 0) {
            log_.info("No callback subscriptions to publish events");
            droppedEvents = events.size();
        }
        for (ConnectionObject connection : getConnections()) {
            for(String event : events) {
                restClient_.postRESTRequestBlocking(URI.create(connection.url_), event);
                publishedEvents++;
                if(constantMode)
                    delayMicroSecond(timeDelayMus);
            }
        }
        log_.info(String.format("***Successfully published events = %s***", publishedEvents));
        log_.info(String.format("***Dropped events = %s***", droppedEvents));
    }

    private void delayMicroSecond(long delayTimeMus) {
        long waitUntil = System.nanoTime() + TimeUnit.MICROSECONDS.toNanos(delayTimeMus);
        while( waitUntil > System.nanoTime());
    }

    private final RESTClient restClient_;
    private final Logger log_;
    private HashMap<ConnectionObject, Long> connectionsToId = new HashMap<>();
    private HashMap<Long, ConnectionObject> idToConnections = new HashMap<>();
    private static long connId = 1;
}