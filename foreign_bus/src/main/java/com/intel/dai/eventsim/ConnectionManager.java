package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.networking.NetworkException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class ConnectionManager {
    ConnectionManager(Logger logger) {
        log_ = logger;
    }

    void add(Map<String, String> parameters) {
        String subscriber = parameters.getOrDefault("Subscriber", null);
        String connUrl = parameters.getOrDefault("Url", null);
        if(subscriber == null || connUrl == null)
            throw new NetworkException("Insufficient data to subscribe a connection.");
        if(!check(connUrl, subscriber)) {
            ConnectionObject connection = new ConnectionObject(connUrl, subscriber, log_);
            connection.initialiseProperties(parameters);
            connectionsToId.put(connection, connId);
            idToConnections.put(connId++, connection);
            return;
        }
        throw new NetworkException("409::The subscription already exists for the specified subscriber and URL");
    }

    void remove(String connId) {
        long removeId = Long.parseLong(connId);
       if(idToConnections.containsKey(removeId)) {
           ConnectionObject conn = idToConnections.get(removeId);
           idToConnections.remove(removeId);
           connectionsToId.remove(conn);
           return;
       }
        throw new NetworkException("400::deletion of requested subscription doesn't exists.");
    }

    void removeAll() {
        idToConnections.clear();
        connectionsToId.clear();
        connId = 1;
        return;
    }

    boolean check(String url, String subscriber) {
        if(connectionsToId.size() == 0)
            return false;
        for(ConnectionObject connectionObject : connectionsToId.keySet())
            if(connectionObject.url_.equals(url) && connectionObject.subscriber_.equals(subscriber))
                return true;
        return false;
    }

    PropertyMap getConnection(String url, String subscriber) throws Exception {
        for(ConnectionObject connection : connectionsToId.keySet()) {
            if(connection.url_.equals(url) && connection.subscriber_.equals(subscriber)) {
                PropertyMap result = new PropertyMap();
                result.put("ID", connectionsToId.get(connection));
                result.putAll(connection.connProperties());
                return result;
            }
        }
        throw new Exception("Requested subscription do not exists.");
    }

    PropertyMap getAllConnections() throws Exception {
        if(connectionsToId.size() == 0) {
            throw new Exception("No subscriptions exists.");
        }
        PropertyArray result = new PropertyArray();
        for(ConnectionObject connectionObject : connectionsToId.keySet()) {
            PropertyMap data = new PropertyMap();
            data.put("ID", connectionsToId.get(connectionObject));
            data.putAll(connectionObject.connProperties());
            result.add(data);
        }
        PropertyMap output = new PropertyMap();
        output.put("SubscriptionList", result);
        return output;
    }

    PropertyMap getConnectionForId(long id) throws Exception {
        ConnectionObject conn = idToConnections.get(id);
        if(conn != null) {
            return conn.connProperties();
        }
        throw new Exception("Subscription details for requested id does not exists.");
    }

    Set<ConnectionObject> getConnections() {
        return connectionsToId.keySet();
    }


    private Logger log_;
    private HashMap<ConnectionObject, Long> connectionsToId = new HashMap<>();
    private HashMap<Long, ConnectionObject> idToConnections = new HashMap<>();
    private static long connId = 1;
}
