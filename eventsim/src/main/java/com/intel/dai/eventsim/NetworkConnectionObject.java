package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restserver.RESTServerException;
import com.intel.networking.restserver.RESTServerHandler;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.sun.istack.NotNull;

import java.util.Map;

/**
 * Description of class NetworkConnectionObject.
 * acts as a abstraction for sse or callback classes.
 */
public abstract class NetworkConnectionObject {

    private static Logger log_;

    static NetworkConnectionObject createConnection(String network, Logger log, PropertyMap config) {
        log_ = log;
        if (NETWORK_TYPES.valueOf(network.toUpperCase()) == NETWORK_TYPES.SSE) {
            PropertyMap sseConfig = config.getMapOrDefault("sseConfig", null);
            sseServer = new SSENetwork(sseConfig, log_);
            return sseServer;
        }

        if (NETWORK_TYPES.valueOf(network.toUpperCase()) == NETWORK_TYPES.CALLBACK) {
            callBack = new CallBackNetwork(log_);
            return callBack;
        }

        if (NETWORK_TYPES.valueOf(network.toUpperCase()) == NETWORK_TYPES.RABBITMQ) {
            return null;
            //TO-DO Implementation of rabbitmq
        }
        return null;
    }

    /**
     * This method is used to configure callback network
     */
    void configureOtherNetworks() throws RESTClientException, RESTServerException {
        configureClientNetwork();
    }

    /**
     * This method to initialise callback instance.
     */
    private void configureClientNetwork() throws RESTClientException, RESTServerException {
        callBack = new CallBackNetwork(log_);
        callBack.initialize();
    }

    /**
     * This method is to subscribe url to callback network.
     *
     * @param url            subscription url.
     * @param subscriberName subscriber name.
     * @param parameters     callback method.
     * @throws RESTClientException when unable to register.
     *                             expects non null values.
     */
    public boolean register(@NotNull final String url, @NotNull final String subscriberName, @NotNull final Map<String, String> parameters) throws RESTClientException {
        return callBack.register(url, subscriberName, parameters);
    }

    /**
     * This method is to subscribe url to sse network.
     *
     * @param url        subscription url.
     * @param httpMethod GET/POST/PUT/DELETE.
     * @param callback   callback method.
     * @throws RESTServerException when unable to register.
     *                             expects non null values.
     */
    public void register(@NotNull final String url, @NotNull final String httpMethod, @NotNull final RESTServerHandler callback) throws RESTServerException {
        sseServer.register(url, httpMethod, callback);
    }

    /**
     * This method to fetch all available subscriptions.
     *
     * @return all subscription details.
     */
    public PropertyMap getSubscription(@NotNull final String subUrl, @NotNull final String subscriber) throws RESTClientException {
        return callBack.getSubscription(subUrl, subscriber);
    }


    /**
     * This method is used to un-subscribe all available subscriptions.
     *
     * @return true if all subscription is removed
     * false if all subscription is not removed or found.
     */
    public boolean unSubscribeAll() {
        return callBack.unSubscribeAll();
    }

    public abstract void publish(final String eventTYpe, final String message) throws RESTClientException;

    /**
     * This method is to publish data to network.
     *
     * @param subject       subscription subjects.
     * @param eventMessages data to publish.
     * @throws RESTClientException when null values passed to this method.
     */
    void send(final String subject, final String eventMessages) throws RESTClientException {
        if (subject.equals(ForeignEvent.EVENT_SUB_TYPE.other.toString()))
            callBack.publish(subject, eventMessages);
        if (subject.equals(ForeignEvent.EVENT_SUB_TYPE.events.toString()) || subject.equals(ForeignEvent.EVENT_SUB_TYPE.telemetry.toString()) || subject.equals(ForeignEvent.EVENT_SUB_TYPE.stateChanges.toString()))
            sseServer.publish(subject, eventMessages);
    }

    public abstract void initialize() throws RESTServerException, RESTClientException;

    /**
     * This method to start server.
     *
     * @throws RESTServerException when unable to start
     */
    public abstract void startServer() throws RESTServerException;

    /**
     * This method to stop server.
     *
     * @throws RESTServerException when unable to stop
     */
    public abstract void stopServer() throws RESTServerException;

    /**
     * This method to fetch server address.
     */
    public abstract String getAddress();

    /**
     * This method to fetch server port.
     */
    public abstract String getPort();

    /**
     * This method to fetch server status.
     *
     * @return true if running
     * false if not.
     */
    public abstract boolean serverStatus();

    /**
     * This method to fetch subscriptions for a given id.
     *
     * @return subscription details.
     */
    public PropertyMap getSubscriptionForId(long subId) {
        return callBack.getSubscriptionForId(subId);
    }


    /**
     * This method is used to un-subscribe for a given subscription-id.
     *
     * @param subId subscription id.
     * @return true if subscription is removed
     * false if subscription is not removed or found.
     */
    public boolean unSubscribeId(long subId) {
        return callBack.unSubscribeId(subId);
    }

    /**
     * This method to fetch all available subscriptions.
     *
     * @return all subscription details.
     */
    public PropertyDocument getAllSubscriptions() {
        return callBack.getAllSubscriptions();
    }

    enum NETWORK_TYPES {
        SSE,
        RABBITMQ,
        CALLBACK,
        OTHER
    }

    private static NetworkConnectionObject sseServer;
    private static NetworkConnectionObject callBack;
}