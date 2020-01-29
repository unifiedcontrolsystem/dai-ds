// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink.restsse;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.networking.NetworkException;
import com.intel.networking.restclient.*;
import com.intel.networking.sink.NetworkDataSink;
import com.intel.networking.sink.NetworkDataSinkDelegate;
import com.intel.properties.PropertyMap;
import com.intel.authentication.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.time.Instant;
import java.util.*;

/**
 * Description of class NetworkDataSinkSSE.
 */
public class NetworkDataSinkSSE implements NetworkDataSink {
    /**
     * Create a network sink object for SSE events.
     * @param args The arguments to the publisher. Supported argument keys are:
     *
     *   parser            - (def: "json")      The payload parser to use, mainly for SSE's "id:" tag.
     *   implementation    - (def: "jdk11")     The implementation for the REST client.
     *   connectAddress    - (def: "127.0.0.1") The address to connect to.
     *   connectPort       - (def: 19216)       The port to connect to.
     *   urlPath           - (def: "/restsse/") The url path to send subscription request to.
     *   subjects          - (def: null)        The comma separated list of subjects to listen for (no spaces).
     *   requestBuilder    - (def: null)        The SSERequestBuilder implementation class name to create.
     *   connectTimeout    - (def: 300)         The connection timeout for the SSE implementation in seconds.
     *   use-ssl           - (def: "false")     The flag for SSL. Set to "true" to use SSL.
     *   tokenAuthProvider - (def: null)        The name of the provider for OAuth 2.0 token authentication.
     *   tokenServer       - (def: null)        The OAuth 2.0 token server URL.
     *   realm             - (def: null)        The realm for the OAuth token retrieval.
     *   clientId          - (def: null)        The Client ID for the OAuth token retrieval.
     *   clientSecret      - (def: null)        The Client Secret for the OAuth token retrieval.
     *   username          - (def: null)        The Optional username for the OAuth token retrieval.
     *   password          - (def: null)        The Optional password for the OAuth token retrieval.

     NOTE: "requestType" was removed as only GET is supported in the SSE specification, POST is not allowed!
     */
    public NetworkDataSinkSSE(Logger logger, Map<String,String> args) {
        assert logger != null;
        log_ = logger;
        args_ = args;
        parser_ = ConfigIOFactory.getInstance(args.getOrDefault("parser", "json"));
        if(parser_ == null) throw new RuntimeException();
    }

    @Override
    public void initialize() {
        parseSubjects(args_.getOrDefault("subjects", null));
        String ssl = (args_.getOrDefault("use-ssl", "false").toLowerCase().equals("true"))?"s":"";
        uri_ = URI.create(String.format("http%s://%s:%d%s", ssl, args_.getOrDefault("connectAddress", "127.0.0.1"),
                Integer.parseInt(args_.getOrDefault("connectPort", "19216")),
                args_.getOrDefault("urlPath", "/restsse/")));
        implementation_ = args_.getOrDefault("implementation", "jdk11");
        if(args_.getOrDefault("requestBuilder", null) != null) {
            createBuilder(args_.getOrDefault("requestBuilder", null));
            if(builder_ == null) throw new NetworkException("Failed to create a network REST request builder");
        }
        requestType_ = "GET";
        for(Map.Entry<String,String> entry: args_.entrySet())
            extraArgs_.put(entry.getKey(), entry.getValue());
        if(args_.containsKey("connectTimeout"))
            connectionTimeoutSeconds_ = Integer.parseInt(args_.get("connectTimeout"));
        String tokenProviderName = args_.getOrDefault("tokenAuthProvider", null);
        if(tokenProviderName != null) {
            createTokenProvider(tokenProviderName, args_);
            if(tokenProvider_ == null) throw new NetworkException("Failed to create the OAuth token provider class");
        }
    }

    /**
     * Clear all previously set subjects. Has no effect if startListening was already called.
     */
    @Override
    public void clearSubjects() {
        subjects_.clear();
    }

    /**
     * Add a subject to this object. Must be called prior to startListening.
     *
     * @param subject The string that is the subject to add for listening on the network bus.
     */
    @Override
    public void setMonitoringSubject(String subject) {
        if(subject != null && !subject.isBlank())
            subjects_.add(subject.trim());
    }

    /**
     * A collection of subjects to this object. Must be called prior to startListening.
     *
     * @param subjects The collection of subjects to add for listening on the network bus.
     */
    @Override
    public void setMonitoringSubjects(Collection<String> subjects) {
        if(subjects != null)
            for(String subject: subjects)
                setMonitoringSubject(subject);
    }

    /**
     * This sets the connection info for the provider in the provider specific format.
     *
     * @param info The connection string for the provider.
     */
    @Override
    public void setConnectionInfo(String info) {
        if(info != null)
            uri_ = URI.create(info);
    }

    /**
     * Sets the callback delegate {@link NetworkDataSinkDelegate} to call back with the data received and translated by
     * the provider.
     *
     * @param delegate The object implementing the {@link NetworkDataSinkDelegate} interface.
     */
    @Override
    public void setCallbackDelegate(NetworkDataSinkDelegate delegate) {
        if(delegate != null)
            callback_ = delegate;
    }

    /**
     * Called to start listening for incoming messages filtered by subject. This can only be called once per provider
     * instance.
     */
    @Override
    public void startListening() {
        if(!isListening()) {
            long timeoutTargetSeconds = Instant.now().getEpochSecond() + connectionTimeoutSeconds_;
            while(Instant.now().getEpochSecond() <= timeoutTargetSeconds && !isListening()) {
                try {
                    client_ = RESTClientFactory.getInstance(implementation_, log_);
                    if (client_ != null) {
                        client_.setSSERequestBuilder(builder_);
                        client_.setTokenOAuthRetriever(tokenProvider_);
                        client_.subscribeToSSEGET(uri_, subjects_, this::responseCallback, this::eventCallback,
                                extraArgs_);
                    } else {
                        log_.error("The RESTClientFactory returned null for the implementation: %s", implementation_);
                        break;
                    }
                    // Wait to see if the connection worked asynchronously...
                    try { Thread.sleep(2500); } catch(InterruptedException intr) { /* Ignore this exception. */ }
                    if(client_ == null)
                        log_.warn("SSE Connection failed, retrying the connection...");
                } catch (RESTClientException e) {
                    client_ = null;
                }
            }
        }
    }

    /**
     * Called to stop listening for incoming messages. This should only be called once and only after startListening
     * was called.
     */
    @Override
    public void stopListening() {
        if(isListening()) {
            client_ = null;
        }
    }

    /**
     * @return A flag to determine if the provider implemented is currently listening.
     */
    @Override
    public boolean isListening() {
        return client_ != null;
    }

    /**
     * @param logger Sets the {@link Logger} API instance into the provider so that it can also log errors/info to the
     *               owning process.
     */
    @Override
    public void setLogger(Logger logger) {
        if(logger != null)
            log_ = logger;
    }

    /**
     * Get the factory name of the implemented provider.
     *
     * @return The name of the provider to be created by the {@link ../NetworkDataSinkFactory}.
     */
    @Override
    public String getProviderName() {
        return "sse";
    }

    private void parseSubjects(String subjects) {
        if(subjects != null) {
            String[] list = subjects.split(",");
            for(String subject: list)
                setMonitoringSubject(subject);
        }
    }

    private void responseCallback(int code, String responseBody, RequestInfo originalInfo) {
        if(code != 200) {
            log_.warn("Subscription failed to '%s'", originalInfo.uri().toString());
            client_ = null;
            startListening();
        }
    }

    private void eventCallback(String eventType, String event, String id) {
        if(isListening() && callback_ != null)
            callback_.processIncomingData(eventType, processRawEvent(event, id));
    }

    private String processRawEvent(String event, String id) {
        if(id == null) return event;
        try {
            PropertyMap map = parser_.fromString(event).getAsMap();
            if (map != null) {
                map.put("sse_id", id);
                event = parser_.toString(map);
            }
        } catch(ConfigIOParseException e) { /* Ignore, we don't recognize the payload format. */ }
        return event;
    }

    private void createTokenProvider(String className, Map<String,String> config) {
        try {
            Class<?> classObj = Class.forName(className);
            Constructor<?> ctor = classObj.getDeclaredConstructor();
            tokenProvider_ = (TokenAuthentication) ctor.newInstance();
            tokenProvider_.initialize(log_, config);
        } catch(ClassNotFoundException e) {
            log_.exception(e, String.format("Missing TokenAuthentication implementation '%s'", className));
        } catch (NoSuchMethodException e) {
            log_.exception(e, String.format("Missing public constructor for TokenAuthentication implementation '%s'",
                    className));
        } catch (IllegalAccessException e) {
            log_.exception(e, String.format("Default constructor for TokenAuthentication implementation " +
                    "'%s' must be public", className));
        } catch (InstantiationException | InvocationTargetException | TokenAuthenticationException e) {
            log_.exception(e, String.format("Cannot construct TokenAuthentication implementation '%s'", className));
        }
    }

    private void createBuilder(String requestBuilder) {
        try {
            Class<?> classObj = Class.forName(requestBuilder);
            Constructor<?> ctor = classObj.getDeclaredConstructor();
            builder_ = (SSERequestBuilder) ctor.newInstance();
        } catch(ClassNotFoundException e) {
            log_.exception(e, String.format("Missing SSERequestBuilder implementation '%s'", requestBuilder));
        } catch (NoSuchMethodException e) {
            log_.exception(e, String.format("Missing public constructor for SSERequestBuilder implementation '%s'",
                    requestBuilder));
        } catch (IllegalAccessException e) {
            log_.exception(e, String.format("Default constructor for SSERequestBuilder implementation " +
                    "'%s' must be public", requestBuilder));
        } catch (InstantiationException | InvocationTargetException e) {
            log_.exception(e, String.format("Cannot construct SSERequestBuilder implementation '%s'", requestBuilder));
        }
    }

    private Map<String,String> args_;
    private Set<String> subjects_ = new HashSet<>();
    private ConfigIO parser_;
    private Logger log_;
    private URI uri_;
    private int connectionTimeoutSeconds_ = 300;
    private NetworkDataSinkDelegate callback_;
    private RESTClient client_ = null;
    private String implementation_;
    private SSERequestBuilder builder_ = null;
    private TokenAuthentication tokenProvider_ = null;
    private String requestType_;
    private Map<String,String> extraArgs_ = new HashMap<>();
}
