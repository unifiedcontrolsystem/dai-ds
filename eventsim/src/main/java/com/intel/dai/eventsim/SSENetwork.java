package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.networking.HttpMethod;
import com.intel.networking.restserver.RESTServer;
import com.intel.networking.restserver.RESTServerException;
import com.intel.networking.restserver.RESTServerFactory;
import com.intel.networking.restserver.RESTServerHandler;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.intel.runtime_utils.TimeUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description of class SSENetwork.
 * subscribe uri to sse network.
 * publish  data to sse type network.
 */
public class SSENetwork extends NetworkConnectionObject {

    public SSENetwork(final PropertyMap config, final Logger log) throws SimulatorException {
        DataValidation.validateKeysAndNullValues(config, SSE_CONFIG_KEYS, MISSING_SERVER_CONFIG);
        config_ = config;
        log_ = log;
    }

    /**
     * This method is to publish data to network.
     * @param eventType subscription subject.
     */
    public void publishImmediate(final String eventType, final String message) {
        try {
            String publishPath = subUrls_.getString(eventType);
            server_.ssePublish(publishPath, message, null);
        } catch (final RESTServerException | PropertyNotExpectedType e) {
            log_.warn("Error while publishing message to network. " + e.getMessage());
        }
    }

    /**
     * This method is to publish data to network in a more efficient way.
     * @param eventType subscription subject.
     */
    public void publish(final String eventType, final String message) {
        if(sendThread_ == null) {
            sendThread_ = new Thread(this::sendDelayedLoop);
            sendThread_.start();
        }
        Accumulator current = accumulator_.getOrDefault(eventType, new Accumulator(eventType));
        accumulator_.putIfAbsent(eventType, current);
        synchronized (current) {
            if(current.startTime == 0L)
                current.startTime = TimeUtils.getNsTimestamp();
            current.data += message;
            current.count++;
        }
    }
    private static final int DELAYED_COUNT = 20;       // fire delayed network message after 20 count or...
    private static final long DELAYED_NS = 1_000_000L; // ... 1ms elapsed time.
    private void sendDelayedLoop() {
        while(server_.isRunning()) {
            accumulator_.forEach((consumer, current) -> {
                synchronized (current) {
                    long diff = TimeUtils.getNsTimestamp() - current.startTime;
                    if (current.count >= DELAYED_COUNT || diff > DELAYED_NS) {
                        publishImmediate(current.eventType, current.data);
                        current.reset();
                    }
                }
            });
            try { Thread.sleep(0, 1_000); } catch(InterruptedException e) { /* No consequence */ }
        }
    }
    private static class Accumulator {
        Accumulator(String type) { eventType = type; reset(); }
        void reset() {startTime = 0L; data = ""; count = 0; }
        final String eventType;
        long startTime;
        String data;
        int count;
    }
    private Thread sendThread_ = null;
    private final Map<String, Accumulator> accumulator_ = new ConcurrentHashMap<>();

    /**
     * This method is used to subscribe to rest handler.
     * @param url subscription url.
     * @param httpMethod GET/POST/DELETE/PUT.
     * @param callback callback method.
     * @throws RESTServerException when unable to add handler.
     */
    public void register(final String url, final String httpMethod, final RESTServerHandler callback)
            throws RESTServerException {
        if (url == null || httpMethod == null)
            throw new RESTServerException("Could not register URL or HttpMethod or call back method : NULL value(s)");
        server_.addHandler(url, HttpMethod.valueOf(httpMethod), callback);
    }

    /**
     * This method to start server.
     */
    public void startServer() throws RESTServerException {
        server_.start();
    }

    /**
     * This method to stop server.
     */
    public void stopServer() throws RESTServerException {
        server_.stop();
    }

    /**
     * This method is used to fetch rest server instance using configuration details.
     * @throws RESTServerException when unable to create or fetch rest server instance.
     */
    @Override
    public void initialize() throws RESTServerException {
        initializeNetwork();
        configureNetwork();
        subscribeUrls();
    }

    /**
     * This method to fetch server address.
     */
    @Override
    public String getAddress() {
        return server_.getAddress();
    }

    /**
     * This method to fetch server port.
     */
    @Override
    public String getPort() {
        return String.valueOf(server_.getPort());
    }

    /**
     * This method to start server.
     */
    @Override
    public boolean serverStatus() {
        return server_.isRunning();
    }

    /**
     * This method checks if subjectID is associated with a URL
     * @param subjectID subject in question
     * @return true if subjectID is associated with a valid URL
     */
    @Override
    public boolean isStreamIDValid( String subjectID ) {
        return subUrls_.containsKey(subjectID);
    }

    /**
     * This method is used to fetch rest server configuration details.
     * @throws RESTServerException when unable to fetch rest server address or port data.
     */
    private void configureNetwork() throws RESTServerException {
        setAddress(config_.getStringOrDefault(SSE_SERVER_ADDR, null));
        setPort(config_.getStringOrDefault(SSE_SERVER_PORT, null));
    }

    /**
     * This method is used to fetch existing rest server instance.
     * @throws RESTServerException when unable to fetch rest server instance.
     */
    private void initializeNetwork() throws RESTServerException {
        if (server_ == null)
            server_ = RESTServerFactory.getInstance("jdk11", log_);
    }

    /**
     * This method to set server address.
     */
    private void setAddress(String address) throws RESTServerException {
        server_.setAddress(address);
    }

    /**
     * This method to set server port.
     */
    private void setPort(String port) throws RESTServerException {
        server_.setPort(Integer.parseInt(port));
    }

    /**
     * This method is used to subscribe urls to sse network handler.
     * @throws RESTServerException when unable to add url to sse handler.
     */
    private void subscribeUrls() throws RESTServerException {
        PropertyMap subscribeUrls = config_.getMapOrDefault(SSE_SERVER_REGISTER_URLS, null);
        log_.debug("*** Registering new SSE URLs...");
        for (Map.Entry<String, Object> subscribeUrl : subscribeUrls.entrySet()) {
            String url = subscribeUrl.getKey();
            String subject = subscribeUrl.getValue().toString();
            List<String> subjects = new ArrayList<>();
            subjects.add(subject);
            log_.debug("*** Added route method GET/SSE to new URL %s", url);
            server_.addSSEHandler(url, subjects);
            subUrls_.put(subject, url);
        }
    }

    private final PropertyMap config_;
    private final Logger log_;
    private RESTServer server_ = null;
    private PropertyMap subUrls_ = new PropertyMap();

    private final static String SSE_SERVER_ADDR = "server-address";
    private final static String SSE_SERVER_PORT = "server-port";
    private final static String SSE_SERVER_REGISTER_URLS = "urls";

    private final String[] SSE_CONFIG_KEYS = new String[]{SSE_SERVER_ADDR, SSE_SERVER_PORT, SSE_SERVER_REGISTER_URLS};

    private final static String MISSING_SERVER_CONFIG = "SSE server configuration is missing required entry, entry = ";
}