package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.networking.HttpMethod;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restserver.RESTServerException;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import com.sun.istack.NotNull;

import java.util.List;
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
     *
     * @throws RESTClientException when unable to create client.
     * @throws RESTServerException when unable to create server
     */
    public void initialise() throws RESTClientException, RESTServerException, SimulatorException {
        networkConnectionObject = NetworkConnectionObject.createConnection(getNetworkName(), log_, config_);
        if (networkConnectionObject == null)
            throw new SimulatorException(String.format("Cannot initialise the given network : %s", getNetworkName()));
        networkConnectionObject.initialize();
        networkConnectionObject.configureOtherNetworks();
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
    public PropertyMap getSubscription(@NotNull final String subUrl, @NotNull final String subscriber) throws SimulatorException {
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
    public void register(@NotNull final String url, @NotNull final String subscriberName, @NotNull final Map<String, String> parameters) throws SimulatorException {
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
    public void register(@NotNull final String url, @NotNull final String httpMethod, @NotNull final NetworkSimulator callback) throws SimulatorException {
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
     * @param constantMode send data with or without delay.
     * @param timeDelayMus delay time.
     * @throws RESTClientException when null values passed to this method.
     */
    void send(final String subject, final List<String> events, final boolean constantMode, final long timeDelayMus) throws RESTClientException {
        networkConnectionObject.send(subject, events, constantMode, timeDelayMus);
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

    /**
     * This method is used to fetch type of network to be connected.
     *
     * @return network name
     */
    private String getNetworkName() {
        return config_.getStringOrDefault("network", null);
    }

    /**
     * This method is used to fetch rabbitmq cofiguration details
     *
     * @return rabbitmq details
     */
    private PropertyMap getRabbitMQConfiguration() {
        return config_.getMapOrDefault("rabbitmq", null);
    }

    /**
     * This method is used to fetch sse cofiguration details
     *
     * @return sse details
     */
    private PropertyMap getSSEConfiguration() {
        return config_.getMapOrDefault("sseConfig", null);
    }

    /**
     * This method is used to validate sse and rabbitmq configuration details.
     *
     * @throws SimulatorException when required parameters are missing.
     */
    private void validateNetworkConfigurations() throws SimulatorException {
        validateSSEConfig();
        validateRabbitMqConfig();
    }

    /**
     * This method is used to validate rabbitmq configuration details.
     *
     * @throws SimulatorException when required parameters are missing.
     */
    private void validateRabbitMqConfig() throws SimulatorException {
        PropertyMap config = getRabbitMQConfiguration();
        if (!config.containsKey("exchangeName"))
            throw new SimulatorException("EventSim Configuration file doesn't contain 'exchangeName' entry");

        if (!config.containsKey("uri"))
            throw new SimulatorException("EventSim Configuration file doesn't contain 'uri' entry");
    }

    /**
     * This method is used to validate sse configuration details.
     *
     * @throws SimulatorException when required parameters are missing.
     */
    private void validateSSEConfig() throws SimulatorException {
        PropertyMap config = getSSEConfiguration();
        if (!config.containsKey("serverAddress"))
            throw new SimulatorException("EventSim Configuration file doesn't contain serverAddress entry");

        if (!config.containsKey("serverPort"))
            throw new SimulatorException("EventSim Configuration file doesn't contain serverPort entry");

        if (!config.containsKey("urls"))
            throw new SimulatorException("EventSim Configuration file doesn't contain urls entry");
    }

    private final PropertyMap config_;
    private final Logger log_;
    private final ApiReqData apiReqData_;
    private NetworkConnectionObject networkConnectionObject;
}