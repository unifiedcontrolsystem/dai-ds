package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restserver.RESTServerException;
import com.intel.networking.restserver.RESTServerHandler;
import com.intel.networking.source.NetworkDataSource;
import com.intel.networking.source.NetworkDataSourceEx;
import com.intel.networking.source.NetworkDataSourceFactory;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;

import java.util.HashMap;
import java.util.Map;

/**
 * Description of class NetworkConnectionObject.
 * acts as a abstraction for sse or callback classes.
 */
public abstract class NetworkConnectionObject {

    private static Logger log_;

    static NetworkConnectionObject createConnection(String network, Logger log, PropertyMap config) throws SimulatorException {
        log_ = log;

        NETWORK_TYPES serverNetwork = NETWORK_TYPES.valueOf(network);
        if (serverNetwork.equals(NETWORK_TYPES.sse))
            return createSSENetwork(config);

        if (serverNetwork.equals(NETWORK_TYPES.callback))
            return createCallBackNetwork(config);

        return null;
    }

    /**
     * This method is used to create publisher instance to send data.
     * @param publisherType type of network to publish data
     * @param publisherConfig network config details
     */
    public void createNetworkPublisher(String publisherType, PropertyMap publisherConfig) {
        NETWORK_TYPES publisher = NETWORK_TYPES.valueOf(publisherType);

        if (publisher.equals(NETWORK_TYPES.sse)) {
            publisher_ = sseServer;
            return;
        }

        if (publisher.equals(NETWORK_TYPES.callback)) {
            publisher_ = callBack;
            return;
        }

        Map<String, String> convertedConfig = new HashMap<>();
        for(String key : publisherConfig.keySet()) {
            if(!key.equals(ADDITIONAL_PUBLISH_PROPERTY)) {
                String value = publisherConfig.getStringOrDefault(key, null);
                convertedConfig.put(key, value);
            }
        }
        try {
            pubSource_ = NetworkDataSourceFactory.createInstance(log_, publisher.toString(), convertedConfig);
            if(publisherConfig.containsKey(ADDITIONAL_PUBLISH_PROPERTY) && pubSource_ instanceof NetworkDataSourceEx) {
                NetworkDataSourceEx ex = (NetworkDataSourceEx)pubSource_;
                for(Map.Entry<String, Object> property :
                        publisherConfig.getMapOrDefault(ADDITIONAL_PUBLISH_PROPERTY, new PropertyMap()).entrySet())
                    ex.setPublisherProperty(property.getKey(), property.getValue());
            }
            publisher_ = pubSource_;
        } catch (Exception e) {
            log_.error("Error while creating publisher to send data");
        }
    }

    /**
     * This method is used to configure callback network
     */
    void configureOtherNetworks() throws RESTClientException, RESTServerException {
        configureClientNetwork();
    }

    public boolean isStreamIDValid(String streamID) {
        return sseServer.isStreamIDValid(streamID);
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
    public boolean register(final String url, final String subscriberName, final Map<String, String> parameters) throws RESTClientException {
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
    public void register(final String url, final String httpMethod, final RESTServerHandler callback) throws RESTServerException {
        sseServer.register(url, httpMethod, callback);
    }

    /**
     * This method to fetch all available subscriptions.
     *
     * @return all subscription details.
     */
    public PropertyMap getSubscription(final String subUrl, final String subscriber) throws RESTClientException {
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
        if(publisher_ instanceof SSENetwork) {
            sseServer.publish(subject, eventMessages);
            return;
        }
        else if(publisher_ instanceof CallBackNetwork) {
            callBack.publish(subject, eventMessages);
            return;
        }
        else if(publisher_ instanceof NetworkDataSource) {
            pubSource_.sendMessage(subject, eventMessages);
            return;
        }
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

    /**
     * This method is used to create a SSE network instance
     * @param sseConfig SSE connection config details
     * @return sse network instance
     */
    private static NetworkConnectionObject createSSENetwork(PropertyMap sseConfig) throws SimulatorException {
        sseServer = new SSENetwork(sseConfig, log_);
        return sseServer;
    }

    /**
     * This method is used to create a callback network instance
     * @param callBackConfig callback connection config details
     * @return callback network instance
     */
    private static NetworkConnectionObject createCallBackNetwork(PropertyMap callBackConfig) {
        callBack = new CallBackNetwork(log_);
        return callBack;
    }

    enum NETWORK_TYPES {
        sse,
        rabbitmq,
        callback,
        kafka,
        other
    }

    private Object publisher_;
    private NetworkDataSource pubSource_;

    private static NetworkConnectionObject sseServer;
    private static NetworkConnectionObject callBack;

    private final static String ADDITIONAL_PUBLISH_PROPERTY = "additional-publish-property";
}