// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink.sse;

import com.intel.authentication.TokenAuthentication;
import com.intel.authentication.TokenAuthenticationException;
import com.intel.logging.Logger;
import com.intel.networking.NetworkException;
import com.intel.networking.sink.NetworkDataSinkEx;
import com.intel.networking.sink.NetworkDataSinkDelegate;
import com.intel.networking.sink.StreamLocationHandler;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Description of class NetworkDataSinkEventSource.
 */
public class NetworkDataSinkEventSource implements NetworkDataSinkEx, EventSourceHandlers {
    /**
     * Create a network sink object for EventSource events.
     * @param args The arguments to the publisher. Supported argument keys are:
     *
     *   fullUrl           - (req)              The full URL for the SSE stream.
     *   lastId            - (def: null)        The last ID processed from the SSE stream.
     *
     *   tokenAuthProvider - (def: null)        The name of the provider for OAuth 2.0 token authentication.
     *   tokenServer       - (def: null)        The OAuth 2.0 token server URL.
     *   realm             - (def: null)        The realm for the OAuth token retrieval.
     *   clientId          - (def: null)        The Client ID for the OAuth token retrieval.
     *   clientSecret      - (def: null)        The Client Secret for the OAuth token retrieval.
     *   username          - (def: null)        The Optional username for the OAuth token retrieval.
     *   password          - (def: null)        The Optional password for the OAuth token retrieval.
     */
    public NetworkDataSinkEventSource(Logger logger, Map<String,String> args) {
        assert logger != null;
        assert args != null;
        log_ = logger;
        args_ = args;
    }

    /**
     * Optionally used to initialize the implementation.
     */
    @Override
    public void initialize() {
        parseSubjects(args_.getOrDefault("subjects", "*"));
        if(!args_.containsKey("fullUrl"))
            throw new NetworkException("Missing required argument in eventSource stream configuration: 'fullUrl'");
        try {
            uri_ = URI.create(args_.get("fullUrl"));
        } catch(IllegalArgumentException e) {
           log_.exception(e, "Argument in eventSource stream configuration could not be parsed: 'fullUrl'");
           throw new NetworkException("Argument in eventSource stream configuration could not be parsed: 'fullUrl'");
        } catch(NullPointerException e) {
            throw new NetworkException("Argument in eventSource stream configuration was null: 'fullUrl'");
        }
        makeQueryMap(uri_.getQuery());
        String tokenProviderName = args_.getOrDefault("tokenAuthProvider", null);
        if(tokenProviderName != null && args_.getOrDefault("tokenServer", null) != null) {
            createTokenProvider(tokenProviderName, args_);
            if(tokenProvider_ == null)
                throw new NetworkException("Failed to create the OAuth token provider class");
        }
        lastId_ = args_.getOrDefault("lastId", null);
    }

    /**
     * Clear all previously set subjects. Has no effect if startListening was already called.
     */
    @Override
    public void clearSubjects() {
    }

    /**
     * Add a subject to this object. Must be called prior to startListening.
     *
     * @param subject The string that is the subject to add for listening on the network bus.
     */
    @Override
    public void setMonitoringSubject(String subject) {
    }

    /**
     * A collection of subjects to this object. Must be called prior to startListening.
     *
     * @param subjects The collection of subjects to add for listening on the network bus.
     */
    @Override
    public void setMonitoringSubjects(Collection<String> subjects) {
    }

    /**
     * This sets the connection info for the provider in the provider specific format.
     *
     * @param info The connection string for the provider.
     */
    @Override
    public void setConnectionInfo(String info) {
        uri_ = URI.create(info);
        query_.clear();
        makeQueryMap(uri_.getQuery());
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
        if(thread_ == null) {
            implementation_ = createEventSource();
            thread_ = new Thread(implementation_);
            thread_.start();
        }
    }

    /**
     * Called to stop listening for incoming messages. This should only be called once and only after startListening
     * was called.
     */
    @Override
    public void stopListening() {
        if(running_.get()) {
            implementation_.stop();
            try {
                thread_.join(15_000); // 15 seconds for now, may make it configurable if needed.
            } catch(InterruptedException e) { /* Ignore interruption here */ }
        }
    }

    /**
     * @return A flag to determine if the provider implemented is currently listening.
     */
    @Override
    public boolean isListening() {
        return running_.get();
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
     * @return The name of the provider to be created by the {@link com.intel.networking.sink.NetworkDataSinkFactory}.
     */
    @Override
    public String getProviderName() {
        return "eventSource";
    }

    /**
     * Add the SSE ID callback handler (StreamLocationHandler) for this NetworkDataSinkEx implementation.
     *
     * @param handler The callback where the stream location can be persisted so on exit the stream can be
     *                continued where you left off.
     */
    @Override
    public void setStreamLocationCallback(StreamLocationHandler handler) {
        locationIdHandler_ = handler;
    }

    /**
     * Sets the SSE ID to be in the initial request on connection.
     *
     * @param id The initial ID to use in the SSE request.
     */
    @Override
    public void setLocationId(String id) {
        lastId_ = id;
    }

    /**
     * Called when the event source is closed due to error.
     */
    @Override
    public void onClosed() {
        running_.set(false);
        log_.info("EventSourceClient closed.");
    }

    /**
     * Called when the event source has an error. The error is only fatal if onClosed is called after the error.
     *
     * @param cause The Exception that caused the error.
     */
    @Override
    public void onError(Exception cause) {
        log_.exception(cause);
    }

    /**
     * Called when the event source starts listening to the SSE stream.
     */
    @Override
    public void onOpen() {
        running_.set(true);
        log_.info("EventSourceClient connected.");
    }

    /**
     * Called when the event source is attempting to connect.
     */
    @Override
    public void onConnecting() {
        log_.info("Connecting to '%s'...", uri_.toString());
    }

    /**
     * Called to report debug information.
     *
     * @param fmt  The string format for the following args or if no args then the string to report.
     * @param args The arguments for the format string.
     */
    @Override
    public void onDebug(String fmt, Object... args) {
        log_.debug(fmt, args);
    }

    /**
     * Called when a completed message is received.
     *
     * @param event   The "event:" tag for the message.
     * @param message The combined "data:" of the message.
     * @param id      The "id:" tag for the message. May be null or empty.
     */
    @Override
    public void onMessage(String event, String message, String id) {
        log_.debug("\n\n*** EVENT='%s';ID='%s';MESSAGE='%s'\n", event, id, message);
        if(callback_ != null) {
            callback_.processIncomingData(event, message);
            lastId_ = id;
            if(locationIdHandler_ != null)
                locationIdHandler_.newStreamLocation(id, uri_.getPath().split("\\?")[0],
                        query_.getOrDefault("stream_id", null));
        }
    }

    protected EventSourceClient createEventSource() { // Overridden for testing only.
        int inputBufferSize = Integer.parseInt(args_.getOrDefault("inputBufferSize", "1024"));
        return new EventSourceClient(uri_.toString(), this, lastId_, tokenProvider_, inputBufferSize);
    }

    private void makeQueryMap(String query) {
        String[] keyValues = query.split("&");
        for(String keyValue: keyValues) {
            String[] pair = keyValue.split("=");
            query_.put(pair[0], pair[1]);
        }
    }

    private void parseSubjects(String subjects) {
        if(subjects != null) {
            String[] list = subjects.split(",");
            for(String subject: list)
                setMonitoringSubject(subject);
        }
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

    private final Map<String,String> args_;
    private Logger log_;
    private NetworkDataSinkDelegate callback_;
    private EventSourceClient implementation_;
    private URI uri_;
    private TokenAuthentication tokenProvider_ = null;
    private String lastId_;
    private StreamLocationHandler locationIdHandler_ = null;
    private final AtomicBoolean running_ = new AtomicBoolean(false);
    private Thread thread_;
    private final Map<String,String> query_ = new HashMap<>();
}
