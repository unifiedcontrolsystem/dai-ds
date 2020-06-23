// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.fabric;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.IAdapter;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.RasEventLog;
import com.intel.dai.dsapi.StoreTelemetry;
import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.exceptions.AdapterException;
import com.intel.logging.Logger;
import com.intel.networking.sink.NetworkDataSink;
import com.intel.networking.sink.NetworkDataSinkFactory;
import com.intel.networking.source.NetworkDataSource;
import com.intel.networking.source.NetworkDataSourceFactory;
import com.intel.properties.PropertyMap;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Base class for fabric telemetry processing. Configuration comes from the provider but defaults are provided in
 * the getConfigMapDefaults method.
 */
public abstract class FabricAdapter {
    /**
     * Constructor for fabric adapter base class. Called by derived classes only.
     *
     * @param servers The comma separated list of VoltDB servers from the command line arguments.
     * @param logger The logger to use in this class and derived classes.
     * @param factory The DataStoreFactory object to use in this class and derived classes.
     * @param adapter The Adapter based interface to be used in this class and derived classes.
     */
    protected FabricAdapter(String servers, Logger logger, DataStoreFactory factory, IAdapter adapter) {
        servers_ = servers;
        log_     = logger;
        factory_ = factory;
        adapter_ = adapter;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null)
            throw new NullPointerException("The JSON parser failed to be created!"); // This cannot happen...
    }

    /**
     * Required method for {@link com.intel.networking.sink.NetworkDataSinkDelegate} that must be implemented by the
     * provider.
     *
     * @param subject The message subject from {@link com.intel.networking.sink.NetworkDataSink} instance
     * @param message The actual raw JSON message from the {@link com.intel.networking.sink.NetworkDataSink}
     */
    abstract protected void processRawMessage(String subject, String message);

    /**
     * Get the adapter type from the implementation class.
     *
     * @return The adapter type name.
     */
    abstract protected String adapterType();

    /**
     * Overridable method to process work item parameters from the HandleInputFromExternalComponent work item.
     * This default method loads the following variables:
     *
     *  "connectAddress" - address to connect on the server
     *  "connectPort"    - port to connect on the server
     *  "urlPath"        - Optional; url of the connection to the server
     *
     * NOTE: These all override what is in the config file for the provider.
     *
     *  If you don't want this behavior override in the provider and don't call this parent method. Or if you want to
     *  add parameter parsing override but still call this parent method.
     *
     * @throws AdapterException If the parameters are illegal or a required parameter is missing.
     */
    protected void consumeWorkItemParameters() throws AdapterException {
        // Get work item parameters into a map.
        Map<String, String> workItemParameters = workQueue_.getClientParameters();

        // Get server stream information.
        config_.put(PARAM_CONNECT_ADDRESS, getConnectionAddress(workItemParameters));
        config_.put(PARAM_URL, getConnectionUrl(workItemParameters));
        config_.put(PARAM_CONNECT_PORT, getConnectionPort(workItemParameters));
    }

    /**
     * A method exposed to the provider to get a microsecond timestamp long.
     *
     * @return The timestamp.
     */
    final protected long usTimestamp() {
        Instant now = Instant.now();
        return (now.getEpochSecond() * 1_000_000L) + (now.getNano() / 1_000L);
    }

    /**
     * The mainProcessingFlow method is the main adapter loop for processing work items.
     *
     * @param config An arbitrary Map&lt;String, String&gt; representing configuration for the provider.
     */
    final protected void mainProcessingFlow(Map<String,String> config, String location) {
        if(config == null) throw new NullPointerException("The 'config' was null passed to mainProcessingFlow");
        try {
            config_ = config;
            workQueue_ = adapter_.setUpAdapter(servers_, location);
            try {
                rasEventLogging_ = factory_.createRasEventLog(adapter_);
            } catch(Exception e) {
                log_.exception(e, "Fatal error creating the RasEventLog interface implementation");
                return;
            }
            storeTelemetry_ = factory_.createStoreTelemetry();
            parseSubjects();
            setupPublisher();
            while (!adapter_.adapterShuttingDown()) {
                // Handle any work items that have been queued for this type of adapter.
                if(workQueue_.grabNextAvailWorkItem(adapter_.snLctn())) {
                    if(workQueue_.workToBeDone().equalsIgnoreCase("HandleInputFromExternalComponent")) {
                        consumeWorkItemParameters();
                        processWorkItemHandleInputFromExternalComponent();
                    } else
                        workQueue_.handleProcessingWhenUnexpectedWorkItem();
                }
                // Sleep for a little bit if no work items are queued for this adapter type.
                if (workQueue_.amtTimeToWait() > 0)
                    Thread.sleep( Math.min(workQueue_.amtTimeToWait(), 5) * 100);
            }
            storeTelemetry_.close();
            publisher_.close();
            adapter_.handleMainlineAdapterCleanup(adapter_.adapterAbnormalShutdown());
        } catch(InterruptedException | IOException | AdapterException e) {
            adapter_.handleMainlineAdapterException(e);
        }
    }

    /**
     * Method to create instance data for logged RAS events from the adapter or provider.
     *
     * @param error The message describing the error that occurred.
     * @param data Any supporting data making the error more useful.
     * @return The build instance data string.
     */
    final protected String makeInstanceData(String error, String data) {
        return "Name='" + adapter_.adapterName() +
                "';Type='" + adapterType() +
                "';Error='" + error +
                "';Data='" + data + "'";
    }

    /**
     * Method to log a log message, log a software RAS event, and throw a fatal AdapterException.
     *
     * @param msg The logged message string.
     * @param data Specific data to make the logged RAS event more useful.
     * @throws AdapterException after logging information, this will stop the provider instance.
     */
    final protected void logSoftwareRasEvent(String msg, String data) throws AdapterException {
        logBenignSoftwareRasEvent(msg, data);
        throw new AdapterException(msg);
    }

    /**
     * Method to log a log message, log a software RAS event.
     *
     * @param msg The logged message string.
     * @param data Specific data to make the logged RAS event more useful.
     */
    final protected void logBenignSoftwareRasEvent(String msg, String data) {
        log_.error(msg);
        rasEventLogging_.logRasEventSyncNoEffectedJob("RasGenAdapterException",
                makeInstanceData(msg, data), adapter_.snLctn(), usTimestamp(), adapterType(),
                workQueue_.baseWorkItemId());
    }

    /**
     * Convenience method to log to the log and to log a non-fatal event.
     *
     * @param exception The exception to log.
     * @param message The message or string format.
     * @param params THe parameters for the format string.
     */
    final protected void logException(Throwable exception, String message, Object... params) {
        String data = String.format(message, params);
        if(exception == null)
            log_.error(data);
        else
            log_.exception(exception, data);
        logBenignSoftwareRasEvent(data, "");
    }

    /**
     * Convenience method to log to the log and to log a non-fatal event.
     *
     * @param message The message or string format.
     * @param params THe parameters for the format string.
     */
    final protected void logError(String message, Object... params) {
        logException(null, message, params);
    }

    /**
     * Publish raw telemetry.
     *
     * @param message The JSON formatted message to publish.
     */
    final protected void publishRawData(String message) {
        String topic = config_.getOrDefault("rawTopic", "undefined_raw_topic");
        publishData(topic, message);
    }

    /**
     * Publish aggregated telemetry.
     *
     * @param message The JSON formatted message to publish.
     */
    final protected void publishAggregatedData(String message) {
        String topic = config_.getOrDefault("aggregatedTopic", "undefined_aggregated_topic");
        publishData(topic, message);
    }

    /**
     * Publish events.
     *
     * @param message The JSON formatted message to publish.
     */
    final protected void publishEventData(String message) {
        String topic = config_.getOrDefault("eventsTopic", "undefined_events_topic");
        publishData(topic, message);
    }

    /**
     * After loading the configuration, this function lets the provider save map values as class fields. The provider
     * must call this parent method.
     *
     * @param config The loaded configuration.
     */
    protected void processConfigItems(Map<String, String> config) {
        networkSinkType_ = config.getOrDefault("networkSinkType", "sse");
        String sList = config.getOrDefault("storeBlacklist", null);
        if(sList == null || sList.trim().isEmpty())
            blacklist_ = new ArrayList<>();
        else
            blacklist_ = Arrays.asList(sList.split(","));
    }

    /**
     * Populates the config with complete defaults for all providers, then load and overlay the config values from the
     * passed configuration file.
     *
     * @param filename The filename (with path) of the configuration file.
     * @return The configuration defaults with overlayed values from files.
     * @throws IOException The file was not found or could not be read.
     * @throws ConfigIOParseException The file contains bad or unexpected JSON.
     */
    protected Map<String,String> buildConfig(File filename) throws IOException, ConfigIOParseException {
        Map<String, String> config = getConfigMapDefaults();
        if(filename.exists()) {
            ConfigIO parser = ConfigIOFactory.getInstance("json");
            if(parser == null) // This cannot be null but must be checked.
                throw new NullPointerException("Failed to get a JSON parser for the config file");
            PropertyMap properties = parser.readConfig(filename).getAsMap();
            for(String key: properties.keySet())
                config.put(key, properties.getStringOrDefault(key, null));
            processConfigItems(config);
            return config;
        } else
            throw new FileNotFoundException("File '" + filename + "' was not found");
    }

    /**
     * Match an incoming subject or topic with the allowed topics for this provider. If the subjects size == 1 and
     * the string is "*" than all subjects are matched.
     *
     * @param subject The subject or topic to test.
     * @return true if the subject is allowed or false otherwise.
     */
    protected final boolean matchSubject(String subject) {
        return subjects_.contains(subject) || (subjects_.size() == 1 && subjects_.get(0).equals("*"));
    }

    /**
     * Called by providers to determine if there is a need to skip the given name when storing aggregate data.
     *
     * @param name The nameto check against the blacklist or regular expressions.
     * @return true if the name should not be stored, false if the name should be stored.
     */
    final protected boolean inBlacklist(String name) {
        boolean result = false;
        for(String regex: blacklist_) {
            result = name.matches(regex);
            if(result)
                break;
        }
        return result;
    }

    void badParameterValue(String parameter, String reason) throws AdapterException {
        String msg = String.format("The base work item's parameters contains a bad parameter '%s' because it is %s!",
                parameter, reason);
        logSoftwareRasEvent(msg, "none");
    }

    Map<String, String> getConfigMapDefaults() {
        Map<String,String> config = new HashMap<>();

        // Adapter/provider settings
        config.put("networkSinkType", "sse");
        config.put(PARAM_CONNECT_ADDRESS, "127.0.0.1");
        config.put(PARAM_CONNECT_PORT, "65535");
        config.put(PARAM_URL, "/");
        config.put("subjects", "*");
        config.put("requestBuilder", "com.intel.networking.sink.restsse.SSEStreamRequestBuilder");
        config.put("connectTimeout", "600");

        // Environmental data aggregation...
        config.put("aggregateEnabled", "true");
        config.put("aggregateUseTime", "false");
        config.put("aggregateCount", "25");
        config.put("aggregateTimeWindowSeconds", "600");
        config.put("aggregateUseMovingAverage", "false");

        // Re-publishing configuration
        config.put("publishServerUrl", "amqp://127.0.0.1"); // if null, no republish is done.
        config.put("exchangeName", "rePublishFabric"); // RabbitMQ's exchange name
        config.put("rawTopic", "ucs_fabric_raw_telemetry");
        config.put("aggregatedTopic", "ucs_fabric_aggregated_telemetry");
        config.put("eventsTopic", "ucs_fabric_events");

        // Connection information...
        config.put("use-ssl", "true");
        config.put("tokenAuthProvider", "com.intel.authentication.KeycloakTokenAuthentication");
        config.put("tokenServer", null); // Default is no authentication...

        // Store name blacklist: comma separated list (no spaces) of names that will not be stored in Tier 2.
        config.put("storeBlacklist", null);

        return config;
    }

    private void processWorkItemHandleInputFromExternalComponent() throws AdapterException {
        try {
            log_.info("Creating NetworkDataSink...");
            NetworkDataSink listener = NetworkDataSinkFactory.createInstanceWithLogger(log_, networkSinkType_, config_);
            if(listener == null)
                throw new NullPointerException("Failed to get a '" + networkSinkType_ +
                        "' based NetworkDataSink instance");
            listener.setCallbackDelegate(this::processRawMessage);
            listener.startListening();

            log_.info("Starting the listening loop...");
            while (!adapter_.adapterShuttingDown()) {
                try {
                    Thread.sleep(100L); // One tenth second pause.
                } catch (InterruptedException e) { /* Ignore interrupt and continue loop */ }
            }

            log_.info("Stopping the listening loop.");
            listener.stopListening();
        } catch(NetworkDataSinkFactory.FactoryException e) {
            throw new AdapterException("Failed to start listening for incoming messages", e);
        }
    }

    private void parseSubjects() {
        String subjects = config_.getOrDefault("subjects", null);
        if (subjects != null)
            subjects_ = Arrays.asList(subjects.split(","));
    }

    private void publishData(String topic, String message) {
        if(publisher_ != null)
            publisher_.sendMessage(topic, message);
    }

    private String getConnectionPort(Map<String, String> workItemParameters) throws AdapterException {
        String sPort = workItemParameters.getOrDefault(PARAM_CONNECT_PORT,
                config_.getOrDefault(PARAM_CONNECT_PORT, "65534"));
        if(sPort == null)
            badParameterValue(PARAM_CONNECT_PORT, "port value must not be null");
        if(!Pattern.matches("^\\d+$", sPort))
            badParameterValue(PARAM_CONNECT_PORT, "port value must be an integer, and match pattern '^\\d+$'");
        try {
            int port = Integer.parseInt(sPort);
            if(port < 1 || port > 65535)
                badParameterValue(PARAM_CONNECT_PORT, "port value must be in the range 1-65535 inclusive");
        } catch(NumberFormatException e) { /* Cannot get here do to pattern match check */ }
        return sPort;
    }

    private String getConnectionAddress(Map<String, String> workItemParameters) throws AdapterException {
        String address = workItemParameters.getOrDefault(PARAM_CONNECT_ADDRESS,
                config_.getOrDefault(PARAM_CONNECT_ADDRESS, "127.0.0.1"));
        if (address == null || address.trim().isEmpty())
            badParameterValue(PARAM_CONNECT_ADDRESS, "null or empty value");
        return address;
    }

    private String getConnectionUrl(Map<String, String> workItemParameters) throws AdapterException {
        String url = workItemParameters.getOrDefault(PARAM_URL, config_.getOrDefault(PARAM_URL, "/"));
        if (url == null || url.trim().isEmpty())
            badParameterValue(PARAM_URL, "null or empty value");
        return url;
    }

    private void setupPublisher() throws AdapterException {
        if(publisher_ == null) {
            try {
                publisher_ = NetworkDataSourceFactory.createInstanceWithLogger(log_, "rabbitmq", config_);
                if (publisher_ == null)
                    throw new AdapterException("Failed to create the data publisher from a factory");
                publisher_.setLogger(log_);
                publisher_.connect(config_.getOrDefault("publishServerUrl", "amqp://127.0.0.1"));
            } catch (NetworkDataSourceFactory.FactoryException e) {
                throw new AdapterException("Failed to create the data publisher", e);
            }
        }
    }

    protected final String servers_;
    protected final Logger log_;
    protected final DataStoreFactory factory_;
    protected       IAdapter adapter_;
    protected       WorkQueue workQueue_;
    protected       RasEventLog rasEventLogging_;
    protected       StoreTelemetry storeTelemetry_;
    protected       Map<String,String> config_;
    protected       NetworkDataSource publisher_ = null;
    protected       List<String> subjects_ = new ArrayList<>();
    protected       ConfigIO parser_;
    private         String networkSinkType_;
    private         List<String> blacklist_;

    private static final String PARAM_CONNECT_ADDRESS   = "connectAddress";
    private static final String PARAM_CONNECT_PORT      = "connectPort";
    private static final String PARAM_URL               = "urlPath";

    static final int INSTANCE_DATA_MAX = 9000;
}
