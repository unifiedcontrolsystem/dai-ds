// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.AdapterOperations;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.exceptions.AdapterException;
import com.intel.dai.exceptions.ProviderException;
import com.intel.logging.Logger;
import com.intel.networking.sink.NetworkDataSink;
import com.intel.networking.sink.NetworkDataSinkEx;
import com.intel.networking.sink.NetworkDataSinkFactory;
import com.intel.perflogging.BenchmarkHelper;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * This is the core code for this component. The adapter functionality and business logic for ALL providers using
 * this component.
 */
public class NetworkListenerCore {
    private static final int THREAD_COUNT = Runtime.getRuntime().availableProcessors(); // Logical hw threads.
    private static final int MAX_THREADS = 3; // Maximum processing threads.

    @SuppressWarnings("serial")
    static class Exception extends java.lang.Exception {
        Exception(String message) { super(message); }
    }

    /**
     * Constructor for this class accepting all objects from the application needed to create all interfaces used.
     *
     * @param logger The application created logger.
     * @param configuration The application created and loaded configuration object.
     * @param factory The Data Store (DS) factory where all online and near-line tier API interfaces are created.
     * @param benchmarking Object to do benchmarking inside all providers using this component.
     */
    public NetworkListenerCore(Logger logger, NetworkListenerConfig configuration, DataStoreFactory factory,
                               BenchmarkHelper benchmarking) {
        assert logger != null:"Passed a null Logger to NetworkListenerCore.ctor()!";
        assert configuration != null:"Passed a null NetworkListenerConfig to NetworkListenerCore.ctor()!";
        assert factory != null:"Passed a null DataStoreFactory to NetworkListenerCore.ctor()!";
        assert parser_ != null:"JSON parser was null";
        log_ = logger;
        config_ = configuration;
        adapter_ = config_.getAdapterInformation();
        factory_ = factory;
        benchmarking_ = benchmarking;
    }

    /**
     * Entry point for application run.
     *
     * @return a Linux shell compatible integer for the exit code if used.
     */
    public int run() {
        int result;
        log_.info("Starting the adapter");
        if (setUpAdapter()) return 1;
        log_.info("Registering the adapter");
        if (registerAdapter()) return 1;
        log_.info("Starting capture from monitoring provider(s)...");
        try {
            result = runMainLoop();
        } catch(RuntimeException e) {
            log_.exception(e);
            result = 2;
        }

        log_.info("Stopping capture from monitoring provider(s).");
        stopAllConnections();
        shutdownAdapter();
        return result;
    }

    // Normal application shutdown method call by run().
    private void shutdownAdapter() {
        try {
            adapterOperations_.shutdownAdapter();
        } catch(AdapterException e) {
            log_.exception(e, "Problem occurred while attempting to shutdown the adapter");
        }
        try { actions_.close(); } catch(IOException e) { log_.exception(e); }
    }

    // Based on the network configuration in the configuration object, attempt connection to all data sources.
    private boolean connectToAllDataSources() {
        try {
            return startAllConnections();
        } catch(NetworkDataSinkFactory.FactoryException | NullPointerException e) {
            log_.exception(e, "Failed to create one of the network connections");
            try {
                adapterOperations_.shutdownAdapter(e);
            } catch(AdapterException e2) {
                log_.exception(e2, "Problem occurred while attempting to shutdown the adapter after another failure");
            }
            return false;
        }
    }

    // Sets up a single profile from the configuration object.
    private boolean setUpProfile(String profile) {
        if(config_.getCurrentProfile() == null) {
            adapter_.setUniqueNameExtension(profile);
            log_.debug("*** Setting profile: %s", profile);
            config_.setCurrentProfile(profile);
            subjects_ = config_.getProfileSubjects();
            log_.debug("Allowed subjects in this adapter instance: '%s'", String.join(",", subjects_));
            try {
                log_.debug("*** Creating providers...");
                createTransformAndActionProviders();
            } catch(ProviderException e) {
                log_.exception(e, "Failed to create the providers");
                return false;
            }
            log_.debug("*** Connecting to all data sources...");
            return connectToAllDataSources();
        }
        return true;
    }

    // Register the application in online tier.
    private boolean registerAdapter() {
        adapterOperations_ = factory_.createAdapterOperations(adapter_);
        if (!workQueue_.isThisNewWorkItem())
            log_.info("In a re-queued HandleInputFromExternalComponent flow");
        try {
            adapterOperations_.registerAdapter();
        } catch(ProcCallException | IOException e) {
            log_.exception(e, "Failed to register the adapter");
            return true;
        }
        return false;
    }

    // Setup the application's system actions object
    private boolean setUpAdapter() {
        // If the configuration object or the environment variable below indicate benchmarking then create an alternate
        // SystemActions object.
        boolean useBenchmarking = config_.useBenchmarking();
        try {
            if (useBenchmarking) {
                actions_ = new BenchmarkingSystemActions(log_, factory_, adapter_, config_);
                System.out.println("*** USING BENCHMARKING SYSTEM ACTIONS"); // Intentionally not logged!
            } else
                actions_ = new NetworkListenerSystemActions(log_, factory_, adapter_, config_);
        } catch(Exception e) {
            log_.exception(e, "Failed to register the adapter");
            return true;
        }
        workQueue_ = factory_.createWorkQueue(adapter_);
        return false;
    }

    // Create all dynamically create the provider class.
    private void createTransformAndActionProviders() throws ProviderException {
        provider_ = createNetworkListenerProvider(config_.getProviderName());
    }

    // Start all connections; SSE, HTTP Callback, RabbitMQ, etc...
    private boolean startAllConnections() throws NetworkDataSinkFactory.FactoryException {
        ArrayList<NetworkDataSink> unconnectedList = new ArrayList<>();
        streamLocations = new PropertyMap();
        for(String networkStreamName: config_.getProfileStreams()) {
            PropertyMap arguments = config_.getNetworkArguments(networkStreamName);
            if (arguments == null) throw new NullPointerException("A null arguments object");
            String name = config_.getNetworkName(networkStreamName);
            if (name == null) throw new NullPointerException("A null network name is not allowed!");
            log_.debug("*** Creating a network sink of type '%s'...", name);
            arguments.put("subjects", subjects_);
            Map<String,String> args = buildArgumentsForNetwork(arguments);
            NetworkDataSink sink = NetworkDataSinkFactory.createInstance(log_, name, args);
            if(sink == null) {
                log_.warn("The NetworkDataSinkFactory returned 'null' for implementation '%s'", name);
                continue;
            }
            if(sink instanceof NetworkDataSinkEx) {
                NetworkDataSinkEx ex = (NetworkDataSinkEx)sink;
                ex.setStreamLocationCallback(this::streamLocationCallback);
                String json = workQueue_.workingResults();
                String path = null;
                if(json != null && !json.trim().isEmpty()) {
                    try {
                        path = args.get("urlPath");
                        PropertyMap map = parser_.fromString(json).getAsMap();
                        if(path != null) {
                            String id = map.getString(path);
                            ex.setLocationId(id);
                            streamLocations.put(path, id);
                        }
                    } catch(ConfigIOParseException | NullPointerException | PropertyNotExpectedType e) {
                        log_.exception(e, "Failed to parse Working Results for stream '%s', reading from tip of the stream", path);
                    }
                }
            }
            sinks_.add(sink);
            sink.setLogger(log_);
            sink.setCallbackDelegate(this::processSinkMessage);
            sink.startListening();
        }
        log_.debug("*** Done Creating all network connections.");
        safeSleep(1500); // stabilize for 1.5 seconds...
        for(NetworkDataSink sink: sinks_) {
            if(!sink.isListening())
                unconnectedList.add(sink);
        }
        if(!unconnectedList.isEmpty()) {
            log_.warn("One or more of the connections failed for this monitoring adapter, they will continue to " +
                    "attempt to connection in the background");
            // Continue to try failures in the background.
            new Thread(() -> backgroundContinueConnections(unconnectedList)).start();
        }
        return true;
    }

    private void streamLocationCallback(String streamLocation, String urlPath, String streamId) {
        log_.debug("*** Updating work item with latest ID from stream: urlPath=%s; newStreamLocation=%s",
                urlPath, streamLocation);
        streamLocations.put(urlPath, streamLocation);
        String json = parser_.toString(streamLocations);
        try {
            workQueue_.saveWorkItemsRestartData(workQueue_.workItemId(), json, false);
        } catch(IOException e) {
            log_.exception(e);
            actions_.logFailedToUpdateWorkItemResults(json);
        }
    }

    // Used if some connection failed, this will continue trying until list is empty (all connection made).
    private void backgroundContinueConnections(List<NetworkDataSink> unconnectedList) {
        List<NetworkDataSink> succeededList = new ArrayList<>();
        while (!unconnectedList.isEmpty()) {
            for (NetworkDataSink sink : unconnectedList) {
                sink.startListening();
                safeSleep(1500); // stabilize for 1.5 seconds...
                if (sink.isListening()) {
                    succeededList.add(sink);
                    log_.info("A lazy connection was established in the background.");
                }
            }
            while (!succeededList.isEmpty()) {
                unconnectedList.remove(succeededList.get(0));
                succeededList.remove(0);
            }
        }
        log_.info("All remaining connections were completed.");
    }

    // Stop all listening connections.
    private void stopAllConnections() {
        for(NetworkDataSink sink: sinks_)
            sink.stopListening();
    }

    // Receive raw message and queue it up for processing.
    private void processSinkMessage(String subject, String message) {
        log_.debug("Received message for subject: %s", subject);
        queue_.add(new FullMessage(subject, message));
    }

    // On a thread, process the incoming queued messages.
    private void processDataQueueThreaded() {
        int count = Math.min(THREAD_COUNT / 2, MAX_THREADS); // 1-MAX_THREADS threads for processing.
        int extraThreads = count - 1;
        Thread[] threads = new Thread[extraThreads];
        log_.info("*** Using %d threads for monitoring...", count);
        for(int i = 0; i < extraThreads; i++) {
            threads[i] = new Thread(this::processDataQueue);
            threads[i].start();
        }
        baseThreadId_ = Thread.currentThread().getId();
        processDataQueue(); // Use this thread (current thread) for one of the processing threads...
        for(int i = 0; i < extraThreads; i++) { // wait for extraThreads...
            try {
                threads[i].join();
            } catch(InterruptedException e) { /* Interrupt is ignored and treated and joined */ }
        }
    }

    // Called from threaded method above to process messages.
    private void processDataQueue() {
        long backOffSleep = 1;
        while(!adapter_.isShuttingDown()) {
            FullMessage full = queue_.poll();
            if(full != null) {
                processMessage(full.subject, full.message);
                backOffSleep = 1;
            } else {
                safeSleep(backOffSleep);
                if(backOffSleep < 25L) backOffSleep += 2;
                if(Thread.currentThread().getId() == baseThreadId_) benchmarking_.tick();
            }
        }
        log_.debug("*** Ending processing loop...");
    }

    // process a single message.
    private void processMessage(String subject, String message) {
        if(subjects_.contains(subject) || subjects_.contains("*")) {
            try {
                benchmarking_.addNamedValue(subject + "_messages", 1);
                log_.debug("Transforming data for subject '%s'...", subject);
                List<CommonDataFormat> dataList = provider_.processRawStringData(message, config_);
                if(dataList != null) {
                    log_.debug("Performing actions...");
                    for (CommonDataFormat data : dataList)
                        provider_.actOnData(data, config_, actions_);
                    benchmarking_.addNamedValue(subject + "_" + config_.getCurrentProfile(), dataList.size());
                }
            } catch(NetworkListenerProviderException e) {
                log_.exception(e, "Dropping a message on the floor due to transformation error");
                log_.debug("%s==>>%s", subject, message);
            }
        } else {
            log_.debug("Dropping a message on the floor due to the subject filter.");
            log_.debug("%s==>>%s", subject, message);
        }
    }

    // Shutting down the application.
    public void shutDown() {
        log_.info("Shutting down the adapter gracefully");
        adapter_.signalToShutdown();
    }

    // Online tier application logic.
    private int runMainLoop() {
        while(!adapter_.isShuttingDown()) { // while application is running....
            try {
                if (workQueue_.grabNextAvailWorkItem()) { // Get the online tier work item from online tier.
                    if (workQueue_.workToBeDone().equals("HandleInputFromExternalComponent")) {
                        adapter_.setBaseWorkItemId(workQueue_.baseWorkItemId());
                        Map<String, String> parameters = workQueue_.getClientParameters();
                        String profile = parameters.getOrDefault("Profile", "default");
                        log_.info("*** Got valid work item and using monitoring profile '%s'...", profile);
                        benchmarking_.replaceFilenameVariable("PROFILE", profile);
                        try {
                            if(!setUpProfile(profile)) {
                                log_.error("Failed to connect to network data sources");
                                adapter_.signalToShutdown();
                                continue;
                            }
                        } catch(IllegalArgumentException e) {
                            log_.exception(e, "The profile named '%s' was not part of the configuration!",
                                    parameters.get("Profile"));
                            adapter_.signalToShutdown();
                            continue;
                        } catch(AssertionError e) {
                            log_.exception(e, "The profile specified was blank or null!");
                            adapter_.signalToShutdown();
                            continue;
                        }
                        log_.debug("*** Starting processing loop...");
                        processDataQueueThreaded();
                    } else // Not a valid work item.
                        workQueue_.handleProcessingWhenUnexpectedWorkItem();
                    adapter_.setId(-1L);
                }
                if (workQueue_.amtTimeToWait() > 0)
                    safeSleep(Math.min(workQueue_.amtTimeToWait(), 5) * 100);
            } catch(IOException e) {
                safeSleep(2000); // VoltDB connection failure
            }
        }
        return 0;
    }

    // Sleep without fear of exception...
    private void safeSleep(long msDelay) {
        try { Thread.sleep(msDelay); } catch(InterruptedException e) { /* Ignore this exception */ }
    }

    // Part of processing of the configuration object with a focus on networking.
    private Map<String,String> buildArgumentsForNetwork(PropertyMap args) {
        Map<String,String> result = new HashMap<>();
        for(Map.Entry<String,Object> entry: args.entrySet()) {
            switch(entry.getKey()) {
                case "subjects":
                    int size = 0;
                    List<?> subjects = new ArrayList<>();
                    if(entry.getValue() instanceof List) {
                        subjects = (List<?>)entry.getValue();
                        size = subjects.size();
                    }
                    String[] array = new String[size];
                    for(int i = 0; i < array.length; i++)
                        array[i] = subjects.get(i).toString();
                    result.put(entry.getKey(), String.join(",", array));
                    break;
                case "requestBuilderSelectors":
                    parseSelector(args.getMapOrDefault(entry.getKey(), new PropertyMap()), result);
                    break;
                case "tokenAuthProvider":
                    result.put(entry.getKey(), entry.getValue().toString());
                    Object value = entry.getValue();
                    if(value != null)
                        parseTokenConfig(config_.getProviderConfigurationFromClassName(value.toString()), result);
                    break;
                default: // Drop any unexpected keys...
                    result.put(entry.getKey(), entry.getValue().toString());
                    break;
            }
        }
        return result;
    }

    void parseTokenConfig(PropertyMap config, Map<String,String> result) {
        if(config != null)
            for(Map.Entry<String,Object> subKey: config.entrySet())
                if(subKey.getValue() != null)
                    result.put(subKey.getKey(), subKey.getValue().toString());
                else
                    result.put(subKey.getKey(), null);
    }

    void parseSelector(PropertyMap map, Map<String,String> result) {
        for(Map.Entry<String,Object> entry: map.entrySet()) {
            if(entry.getValue() instanceof PropertyMap) continue; // Map is unsupported!!!
            if(entry.getValue() instanceof PropertyArray) {
                String[] array = new String[map.getArrayOrDefault(entry.getKey(), null).size()];
                for(int i = 0; i < array.length; i++)
                    array[i] = map.getArrayOrDefault(entry.getKey(), null).get(i).toString();
                result.put(entry.getKey(), String.join(",", array));
            }
            result.put("requestBuilderSelectors." + entry.getKey(), entry.getValue().toString());
        }
    }

    private NetworkListenerProvider createNetworkListenerProvider(String canonicalName) throws ProviderException {
        log_.info("Attempting to create provider: %s", canonicalName);
        Object object = createProvider(canonicalName);
        if(!(object instanceof NetworkListenerProvider))
            throw new ProviderException(String.format("The name '%s' was created but was not the " +
                    "expected NetworkListenerProvider type", canonicalName));
        return (NetworkListenerProvider)object;
    }

    private Object createProvider(String canonicalName) throws ProviderException {
        try {
            Class<?> classType = Class.forName(canonicalName);
            Constructor<?> ctor = classType.getDeclaredConstructor(Logger.class);
            Object instance = ctor.newInstance(log_);
            if(!(instance instanceof Initializer))
                throw new ProviderException("All providers MUST implement 'Initializer' interface");
            else
                ((Initializer)instance).initialize();
            return instance;
        } catch(ClassNotFoundException | NoSuchMethodException | InstantiationException |
                IllegalAccessException | InvocationTargetException e) {
            throw new ProviderException(String.format("Failed to create '%s'", canonicalName), e);
        }
    }

    private NetworkListenerConfig config_;
    private BenchmarkHelper benchmarking_;
    private Logger log_;
    private AdapterInformation adapter_;
    private WorkQueue workQueue_;
    private AdapterOperations adapterOperations_ = null;
            DataStoreFactory factory_;
    private List<NetworkDataSink> sinks_ = new ArrayList<>();
    private NetworkListenerProvider provider_ = null;
    private SystemActions actions_;
    private List<String> subjects_;
            ConcurrentLinkedQueue<FullMessage> queue_ = new ConcurrentLinkedQueue<>();
    private long baseThreadId_;
    private static long STABILIZATION_VALUE = 1500L;
    private final ConfigIO parser_ = ConfigIOFactory.getInstance("json");
    private PropertyMap streamLocations = null;

    private static final class FullMessage {
        FullMessage(String subject, String message) {
            this.subject = subject;
            this.message = message;
        }
        final String subject;
        final String message;
    }
}
