package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restclient.RESTClientFactory;
import com.intel.properties.PropertyMap;

import java.util.Map;

/**
 * Description of class CallBackNetwork.
 * creates connection manager.
 * publish  data to callback type network.
 */
public class CallBackNetwork extends NetworkConnectionObject {

    CallBackNetwork(final Logger log) {
        log_ = log;
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
        return connectionManager_.getSubscription(url, subscriber);
    }

    /**
     * This method is to fetch subscription details for a given subscription id.
     * @param subId subscription id.
     * @return subscription details for a given id.
     */
    public PropertyMap getSubscriptionForId(long subId) {
        return connectionManager_.getSubscriptionForId(subId);
    }

    /**
     * This method is to fetch all available subscriptions.
     * @return available subscription details.
     */
    public PropertyMap getAllSubscriptions() {
        return connectionManager_.getAllSubscriptions();
    }

    /**
     * This method is to publish data to network.
     * @param url subscription url.
     * @param events data to publish.
     * @throws RESTClientException when null values passed to this method.
     * expects non null values.
     */
    public void publish(final String url, final String events) throws RESTClientException {
        if (events == null)
            throw new RESTClientException("No events to publish to network");
        connectionManager_.publish(events);
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
    public boolean register(final String url, final String subscriber, final Map<String, String> parameters) throws RESTClientException {
        if (url == null || subscriber == null)
            throw new RESTClientException("Could not register url or httpmethod : NULL value(s)");
        return connectionManager_.addSubscription(url, subscriber, parameters);
    }

    /**
     * This method is to remove all subscriptions.
     * @return true if all subscriptions are removed.
     *         false if all subscription are not removed.
     */
    public boolean unSubscribeAll() {
        return connectionManager_.removeAllSubscriptions();
    }

    /**
     * This method is to remove subscription for a given id.
     * @param subId subscription id.
     * @return true if subscription is removed.
     *         false if subscription is not removed or not found.
     */
    public boolean unSubscribeId(final long subId) {
        return connectionManager_.removeSubscriptionId(subId);
    }

    /**
     * This class is to imitate like a server.
     * This method to create rest client and connection manager.
     * @throws RESTClientException unable to create rest client or connection manager.
     */
    @Override
    public void initialize() throws RESTClientException {
        createClient();
        createManager();
    }

    /**
     * This class is to imitate like a server.
     * This method to fetch server address.
     */
    @Override
    public String getAddress() {
        return null;
    }

    /**
     * This class is to imitate like a server.
     * This method to fetch server port.
     */
    @Override
    public String getPort() {
        return null;
    }

    /**
     * This class is to imitate like a server.
     * This method to fetch server status.
     */
    @Override
    public boolean serverStatus() {
        return true;
    }

    /**
     * This class is to imitate like a server.
     * This method to start server.
     */
    @Override
    public void startServer() { /*Do Nothing */ }

    /**
     * This class is to imitate like a server.
     * This method to stop server.
     */
    @Override
    public void stopServer() { /*Do Nothing */ }

    /**
     * This method creates rest client.
     * @throws RESTClientException when unable to fetch created rest client instance.
     */
    private void createClient() throws RESTClientException {
        if (restClient_ == null)
            restClient_ = RESTClientFactory.getInstance("apache", log_);
    }

    /**
     * This method creates connection manager using rest client and logger.
     */
    private void createManager() {
        connectionManager_ = new ConnectionManager(restClient_, log_);
    }

    private final Logger log_;
    private ConnectionManager connectionManager_;
    private RESTClient restClient_ = null;
}