package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.networking.HttpMethod;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restserver.RESTServerException;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;

import java.util.Map;

/**
 * Description of class NetworkObject.
 * exposed class for others helpful to make actions on any type of network like sse/callback.
 */
public class NetworkObject {

    NetworkObject(PropertyMap config, Logger log, ApiReqData apiReqData) throws SimulatorException {
        config_ = config;
        log_ = log;
        apiReqData_ = apiReqData;
        validateNetworkConfigurations();
    }

    /**
     * This method is used to initialse network based on network name.
     */
    public void initialise() throws SimulatorException {
        try {
            networkConnectionObject = NetworkConnectionObject.createConnection(getNetworkName(), log_, getNetworkConfiguration(getNetworkName()));
            DataValidation.isNull(networkConnectionObject, String.format("Cannot initialise the given network : %s", getNetworkName()));
            networkConnectionObject.initialize();
            networkConnectionObject.configureOtherNetworks();
            networkConnectionObject.createNetworkPublisher(getPublisherName(), getNetworkConfiguration(getPublisherName()));
        } catch (RESTServerException | RESTClientException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method to fetch server address.
     */
    public String getAddress() {
        return networkConnectionObject.getAddress();
    }

    /**
     * This method to fetch all available subscriptions.
     *
     * @return all subscription details.
     */
    public PropertyDocument getAllSubscriptions() {
        return networkConnectionObject.getAllSubscriptions();
    }

    /**
     * This method to fetch all available subscriptions.
     *
     * @return all subscription details.
     */
    public PropertyMap getSubscription(final String subUrl, final String subscriber) throws SimulatorException {
        if (subUrl == null || subscriber == null)
            throw new SimulatorException("Could not find details with url or subscriber 'NULL' value(s)");
        try {
            return networkConnectionObject.getSubscription(subUrl, subscriber);
        } catch (final RESTClientException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method to fetch subscriptions for a given id.
     *
     * @return subscription details.
     */
    public PropertyDocument getSubscriptionForId(long subId) {
        return networkConnectionObject.getSubscriptionForId(subId);
    }

    /**
     * This method to fetch server port.
     */
    public String getPort() {
        return networkConnectionObject.getPort();
    }

    /**
     * This method is to subscribe url to callback network.
     *
     * @param url            subscription url.
     * @param subscriberName subscriber name.
     * @param parameters     callback method.
     * @throws SimulatorException when unable to register.
     *                            expects non null values.
     */
    public void register(final String url, final String subscriberName, final Map<String, String> parameters) throws SimulatorException {
        if (url == null || subscriberName == null || parameters == null)
            throw new SimulatorException("Could not register URL or HttpMethod or input params : NULL value(s)");
        try {
            networkConnectionObject.register(url, subscriberName, parameters);
        } catch (final RESTClientException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is to subscribe url to sse network.
     *
     * @param url        subscription url.
     * @param httpMethod GET/POST/PUT/DELETE.
     * @param callback   callback method.
     * @throws SimulatorException when unable to register.
     *                            expects non null values.
     */
    public void register(final String url, final String httpMethod, final NetworkSimulator callback) throws SimulatorException {
        if (url == null || httpMethod == null || callback == null)
            throw new SimulatorException("Could not register URL or HttpMethod or call back method : NULL value(s)");
        try {
            networkConnectionObject.register(url, httpMethod, apiReqData_::apiCallBack);
            PropertyMap urlMethodObj = new PropertyMap();
            urlMethodObj.put(url, HttpMethod.valueOf(httpMethod));
            apiReqData_.registerPathCallBack(urlMethodObj, callback);
        } catch (final RESTServerException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is to publish data to network.
     *
     * @param subject      subscription subjects.
     * @param events       data to publish.
     * @throws RESTClientException when null values passed to this method.
     */
    void send(final String subject, final String events) throws RESTClientException {
        log_.debug("Sending to "+ subject);
        networkConnectionObject.send(subject, events);
    }

    /**
     * This method to fetch server status.
     *
     * @return true if running
     * false if not.
     */
    public boolean serverStatus() {
        return networkConnectionObject.serverStatus();
    }

    /**
     * This method to start server.
     *
     * @throws SimulatorException when unable to start
     */
    public void startServer() throws SimulatorException {
        try {
            networkConnectionObject.startServer();
        } catch (final RESTServerException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method to stop server.
     *
     * @throws SimulatorException when unable to stop
     */
    public void stopServer() throws SimulatorException {
        try {
            networkConnectionObject.stopServer();
        } catch (final RESTServerException e) {
            throw new SimulatorException(e.getMessage());
        }
    }

    /**
     * This method is used to un-subscribe all available subscriptions.
     *
     * @return true if all subscription is removed
     * false if all subscription is not removed or found.
     */
    public boolean unRegisterAll() {
        return networkConnectionObject.unSubscribeAll();
    }

    /**
     * This method is used to un-subscribe for a given subscription-id.
     *
     * @param subId subscription id.
     * @return true if subscription is removed
     * false if subscription is not removed or found.
     */
    public boolean unRegisterId(long subId) {
        return networkConnectionObject.unSubscribeId(subId);
    }

    public boolean isStreamIDValid( String streamID ) {
        return networkConnectionObject.isStreamIDValid(streamID);
    }

    /**
     * This method is used to fetch type of network to be connected.
     *
     * @return network name
     */
    String getNetworkName() {
        return config_.getStringOrDefault(NETWORK_TYPE, null);
    }

    /**
     * This method is used to network name to which server publish data.
     *
     * @return network name
     */
    private String getPublisherName() {
        return config_.getStringOrDefault(PUBLISHER_NETWORK_TYPE, null);
    }

    /**
     * This method is used to get configuration details for a given network name
     *
     * @return network config details
     */
    private PropertyMap getNetworkConfiguration(String networkName) {
        return config_.getMapOrDefault(networkName, null);
    }

    /**
     * This method is used to validate server network configuration details.
     *
     * @throws SimulatorException when required parameters are missing.
     */
    private void validateNetworkConfigurations() throws SimulatorException {
        DataValidation.validateKeysAndNullValues(config_, NETWORK_CONFIG_KEYS, MISSING_SERVER_CONFIG);
    }

    public void setProperty(String schema, String streamId) {
        networkConnectionObject.setProperty(schema, streamId);
    }

    private final PropertyMap config_;
    private final Logger log_;
    private final ApiReqData apiReqData_;
    private NetworkConnectionObject networkConnectionObject;

    private final String[] NETWORK_CONFIG_KEYS = new String[]{NETWORK_TYPE, PUBLISHER_NETWORK_TYPE};

    private final static String NETWORK_TYPE = "server-network";
    private final static String PUBLISHER_NETWORK_TYPE = "publisher-network";
    private final static String MISSING_SERVER_CONFIG = "Eventsim config file is missing required entry, entry = ";
}