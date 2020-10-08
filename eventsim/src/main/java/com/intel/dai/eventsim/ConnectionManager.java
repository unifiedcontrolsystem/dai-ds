package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Description of class ConnectionManager.
 * creates/delete/modify/fetch callback subscriptions.
 * publish  data to all connections/subscriptions available
 */
public class ConnectionManager {

    ConnectionManager(RESTClient restClient, Logger logger) {
        restClient_ = restClient;
        log_ = logger;
    }

    /**
     * This method is to add subscriptions.
     * @param url subscription url.
     * @param subscriber name of the subscriber.
     * @param parameters data along with subscription details.
     * @return true if subscription is added.
     *         false if subscription is not added.
     * @throws RESTClientException when null values passed to this method.
     * expects non null values
     */
    public boolean addSubscription(final String url, final String subscriber, Map<String, String> parameters) throws RESTClientException {
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

    /**
     * This method is to fetch all available subscriptions.
     * @return all available subscription details.
     */
    public PropertyMap getAllSubscriptions() {
        if (connectionsToId.size() == 0) {
            return new PropertyMap();
        }
        PropertyArray result = new PropertyArray();
        for (Map.Entry<ConnectionObject, Long> connectionObject : connectionsToId.entrySet()) {
            PropertyMap data = new PropertyMap();
            data.put("ID", connectionObject.getValue());
            data.putAll(connectionObject.getKey().connProperties());
            result.add(data);
        }
        PropertyMap output = new PropertyMap();
        output.put("SubscriptionList", result);
        return output;
    }

    /**
     * This method is to fetch subscription for a given url and subscriber.
     * @param url subscription url.
     * @param subscriber name of the subscriber.
     * @throws RESTClientException when null values passed to this method.
     * expects non null values.
     */
    public PropertyMap getSubscription(final String url, final String subscriber) throws RESTClientException {
        if (url == null || subscriber == null)
            throw new RESTClientException("Insufficient details to get subscription: url or subscriber null value(s)");
        for (Map.Entry<ConnectionObject, Long> connection : connectionsToId.entrySet()) {
            ConnectionObject currentConnection = connection.getKey();
            if (currentConnection != null && currentConnection.url_.equals(url) && currentConnection.subscriber_.equals(subscriber)) {
                PropertyMap result = new PropertyMap();
                result.put("ID", connection.getValue());
                result.putAll(currentConnection.connProperties());
                return result;
            }
        }
        return new PropertyMap();
    }

    /**
     * This method is to fetch subscription details for a given subscription id.
     * @param id subscription id.
     * @return subscription details for a given id.
     */
    public PropertyMap getSubscriptionForId(long id) {
        ConnectionObject conn = idToConnections.get(id);
        if (conn != null) {
            return conn.connProperties();
        }
        return new PropertyMap();
    }

    /**
     * This method is to remove subscription for a given id.
     * @param removeId subscription id.
     * @return true if subscription is removed.
     *         false if subscription is not removed or not found.
     */
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

    /**
     * This method is to remove all subscriptions.
     * @return true if all subscriptions are removed.
     *         false if all subscription are not removed.
     */
    public boolean removeAllSubscriptions() {
        idToConnections.clear();
        connectionsToId.clear();
        connId = 1;
        log_.info("*** Removed all existing subscriptions ***");
        return true;
    }

    /**
     * This method is to check whether a subscription exits for a given url and subscriber details.
     */
    private boolean checkSubscription(final String url, final String subscriber) {
        if (connectionsToId.size() == 0)
            return false;
        for (ConnectionObject connectionObject : connectionsToId.keySet())
            if (connectionObject.url_.equals(url) && connectionObject.subscriber_.equals(subscriber))
                return true;
        return false;
    }

    /**
     * This method is to fetch all subscriptions/connection object with url and its respective subscriber details.
     */
    private Set<ConnectionObject> getConnections() {
        return connectionsToId.keySet();
    }

    /**
     * This method is to publish data to callback network.
     * @param message data to publish.
     * @throws RESTClientException when no available subscriptions.
     * expects non null values.
     */
    public void publish(final String message) throws RESTClientException {
        if(getConnections().size() == 0)
            log_.info("No callback subscriptions to publish events");
        for (ConnectionObject connection : getConnections()) {
            restClient_.postRESTRequestBlocking(URI.create(connection.url_), message);
        }
    }

    private final RESTClient restClient_;
    private final Logger log_;
    private HashMap<ConnectionObject, Long> connectionsToId = new HashMap<>();
    private HashMap<Long, ConnectionObject> idToConnections = new HashMap<>();
    private long connId = 1;
}