// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink.http_callback;

import com.intel.authentication.TokenAuthentication;
import com.intel.authentication.TokenAuthenticationException;
import com.intel.logging.Logger;
import com.intel.networking.HttpMethod;
import com.intel.networking.NetworkException;
import com.intel.networking.restclient.BlockingResult;
import com.intel.networking.restclient.RESTClient;
import com.intel.networking.restclient.RESTClientException;
import com.intel.networking.restclient.RESTClientFactory;
import com.intel.networking.restserver.*;
import com.intel.networking.sink.NetworkDataSink;
import com.intel.networking.sink.NetworkDataSinkDelegate;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.util.*;

/**
 * Description of class NetworkDataSinkHttpCallback. See this
 */
public class NetworkDataSinkHttpCallback implements NetworkDataSink {
    /**
     * Create a network sink object for HTTP Callback based events.
     * @param args The arguments to the publisher. Supported argument keys are:
     *
     *   connectAddress    - (Required)         The address to connect to.
     *   connectPort       - (Required)         The port to connect to.
     *   bindAddress       - (Required)         The address to bind the callback server to it may not be "*".
     *   bindPort          - (Required)         The port to bind the callback server to.
     *   urlPath           - (Required)         The url path to send subscription request to.
     *   subjects          - (Required)         The comma separated list of subjects to listen for (no spaces).
     *   requestBuilder    - (Required)         The RequestBuilder class name to create subscription requests.
     *   responseParser    - (Required)         The ResponseParser class name to create the DELETE URL.
     *   subscriberName    - (Required)         Used to identify the network connection represented here.
     *   requestType       - (def: "POST")      The HTTP request method type.
     *   deleteType        - (def "DELETE")     The HTTP delete method type.
     *   connectTimeout    - (def: 300)         The connection timeout for the SSE implementation in seconds.
     *   use-ssl           - (def: "false")     The flag for SSL. Set to "true" to use SSL.
     *   tokenAuthProvider - (def: null)        The name of the provider for OAuth 2.0 token authentication.
     *   tokenServer       - (def: null)        The OAuth 2.0 token server URL.
     *   realm             - (def: null)        The realm for the OAuth token retrieval.
     *   clientId          - (def: null)        The Client ID for the OAuth token retrieval.
     *   clientSecret      - (def: null)        The Client Secret for the OAuth token retrieval.
     *   username          - (def: null)        The Optional username for the OAuth token retrieval.
     *   password          - (def: null)        The Optional password for the OAuth token retrieval.
     */
    public NetworkDataSinkHttpCallback(Logger logger, Map<String,String> args) {
        assert logger != null;
        log_ = logger;
        args_ = args;
        for(String required: requiredArgs_)
            if(!args.containsKey(required))
                throw new IllegalArgumentException(String.format("Missing required argument for HttpCallback " +
                        "data sink: %s", required));
        subscribeMethod_ = HttpMethod.valueOf(args_.getOrDefault("requestType", "POST"));
        unsubscribeMethod_ = HttpMethod.valueOf(args_.getOrDefault("deleteType", "DELETE"));
        subscriberName_ = args_.get("subscriberName");
        callbackUri_ = URI.create(String.format("http://%s:%s/networksink/%s", args_.get("bindAddress"),
                args_.get("bindPort"), subscriberName_));
    }

    @Override
    public void initialize() {
        parseSubjects(args_.get("subjects"));
        buildSubscriptonUri();
        buildProviders();
        if(args_.containsKey("connectTimeout"))
            connectionTimeoutSeconds_ = Integer.parseInt(args_.get("connectTimeout"));
    }

    @Override
    public void clearSubjects() {
        subject_ = null;
    }

    @Override
    public void setMonitoringSubject(String subject) {
        subject_ = subject;
    }

    @Override
    public void setMonitoringSubjects(Collection<String> subjects) {
        log_.error("An attempt was made to call 'setMonitoringSubjects' which is unsupported in this implementation");
        throw new NetworkException("Setting a collection of subjects is not supported by this implementation");
    }

    @Override
    public void setConnectionInfo(String info) {
        log_.error("An attempt was made to call 'setConnectionInfo' which is unsupported in this implementation");
        throw new UnsupportedOperationException("You must pass in this information via the ctor");
    }

    @Override
    public void setCallbackDelegate(NetworkDataSinkDelegate delegate) {
        if(delegate != null)
            incomingMessageCallback_ = delegate;
    }

    @Override
    public void startListening() {
        if(!isListening()) {
            createServer();
            createClient();
            subscribe();
        }
    }

    @Override
    public void stopListening() {
        if(isListening()) {
            unsubscribe();
            client_ = null;
            server_ = null;
        }
    }

    @Override
    public boolean isListening() {
        return subscriptionRemoveUrl_ != null;
    }

    @Override
    public void setLogger(Logger logger) {
        if(logger != null)
            log_ = logger;
    }

    @Override
    public String getProviderName() {
        return "http_callback";
    }

    private void createServer() {
        try {
            server_ = RESTServerFactory.getSingletonInstance(HTTP_IMPLEMENTATION, log_);
            if(server_ == null)
                throw new NetworkException("Failed to create an instance of the HTTP server implementation");
            server_.setAddress(args_.get("bindAddress"));
            server_.setPort(Integer.parseInt(args_.get("bindPort")));
            server_.start();
        } catch(RESTServerException e) {
            log_.exception(e);
            throw new NetworkException("Failed to create an instance of the HTTP server");
        }
    }

    private void processHttpMessage(Request request, Response response) {
        response.setCode(200);
        try {
            incomingMessageCallback_.processIncomingData(subject_, request.getBody());
        } catch(RequestException e) {
            log_.exception(e);
        }
    }

    private void createClient() {
        try {
            client_ = RESTClientFactory.getInstance(HTTP_IMPLEMENTATION, log_);
            if(client_ == null)
                throw new NetworkException("Failed to create and instance of the HTTP client implementation");
        } catch(RESTClientException e) {
            log_.exception(e);
            throw new NetworkException("Failed to create and instance of the HTTP client");
        }
    }

    private void subscribe() {
        int TRIES = 3;
        Collection<String> subjects = new ArrayList<>() {{ add(subject_); }};
        String body = requestBuilder_.buildRequest(subjects, subscriberName_, callbackUri_.toString());
        BlockingResult result = null;
        for(int tries = 0; tries < TRIES; tries++) {
            try {
                result = client_.methodRESTRequestBlocking(subscribeMethod_, subscriptionUri_, body);
                if(result.code != 200) {
                    if (tries < (TRIES - 1))
                        log_.warn("The subscription server returned HTTP code %d", result.code);
                    else {
                        log_.error("The subscription server returned HTTP code %d", result.code);
                        log_.debug("    BODY: %s", result.responseDocument);
                        result = null;
                    }
                } else
                    break;
            } catch(RESTClientException e) {
                if(tries < (TRIES - 1))
                    log_.exception(e, "Failed to connect to the subscriber server: %s", subscriptionUri_.toString());
                else
                    log_.warn("Failed to connect to the subscriber server: %s", subscriptionUri_.toString());
            }
        }
        if(result == null)
            throw new NetworkException(String.format("Subscription to '%s' failed", subscriptionUri_.toString()));
        subscriptionRemoveUrl_ = responseParser_.parseResponse(result.responseDocument,subscriptionUri_.toString());
        try {
            server_.addHandler(callbackUri_.getPath(), subscribeMethod_, this::processHttpMessage);
        } catch(RESTServerException e) {
            throw new NetworkException(e.getMessage());
        }
    }

    private void unsubscribe() {
        try {
            client_.methodRESTRequestBlocking(unsubscribeMethod_, subscriptionRemoveUrl_, null);
            try {
                server_.removeHandler(subscriptionUri_.toString(), subscribeMethod_);
            } catch(RESTServerException e) {
                log_.exception(e);
            }

        } catch(RESTClientException e) {
            log_.exception(e);
        }
        subscriptionRemoveUrl_ = null;
    }

    private void buildSubscriptonUri() {
        StringBuilder builder = new StringBuilder();
        builder.append(Boolean.parseBoolean(args_.getOrDefault("use-ssl", "false").toLowerCase())?"https":"http");
        builder.append("://").append(args_.getOrDefault("connectAddress", "127.0.0.1"));
        builder.append(":").append(args_.getOrDefault("connectPort", "8080"));
        if(!args_.get("urlPath").startsWith("/"))
            builder.append("/");
        builder.append(args_.get("urlPath"));
        subscriptionUri_ = URI.create(builder.toString());
    }

    private void buildProviders() {
        requestBuilder_ = createSubscriptionRequestBuilder(args_.get("requestBuilder"));
        if(requestBuilder_ == null)
            throw new NetworkException(String.format("Failed to create an implementation of " +
                            "'%s' from '%s'", SubscriptionRequestBuilder.class.getCanonicalName(),
                    args_.get("requestBuilder")));
        responseParser_ = createSubscriptionResponseParser(args_.get("responseParser"));
        if(responseParser_ == null)
            throw new NetworkException(String.format("Failed to create an implementation of " +
                            "'%s' from '%s'", SubscriptionResponseParser.class.getCanonicalName(),
                    args_.get("responseParser")));
        String tokenProviderName = args_.getOrDefault("tokenAuthProvider", null);
        if(tokenProviderName != null) {
            tokenProvider_ = createTokenProvider(tokenProviderName, args_);
            if(tokenProvider_ == null)
                throw new NetworkException("Failed to create the OAuth token provider class");
        }
    }

    private void parseSubjects(String subjects) {
        if(subjects != null) {
            clearSubjects();
            String[] list = subjects.split(",");
            setMonitoringSubject(list[0]);
        }
    }

    private SubscriptionRequestBuilder createSubscriptionRequestBuilder(String className) {
        return (SubscriptionRequestBuilder)commonDynamicBuilder(className);
    }

    private SubscriptionResponseParser createSubscriptionResponseParser(String className) {
        return (SubscriptionResponseParser)commonDynamicBuilder(className);
    }

    private Object commonDynamicBuilder(String className) {
        Object result = null;
        try {
            Class<?> classObj = Class.forName(className);
            Constructor<?> ctor = classObj.getDeclaredConstructor();
            result = ctor.newInstance();
        } catch(ClassNotFoundException e) {
            log_.exception(e, String.format("Missing implementation class '%s'", className));
        } catch (NoSuchMethodException e) {
            log_.exception(e, String.format("Missing public constructor for implementation class '%s'", className));
        } catch (IllegalAccessException e) {
            log_.exception(e, String.format("Default constructor for implementation class '%s' must be public",
                    className));
        } catch (InstantiationException | InvocationTargetException e) {
            log_.exception(e, String.format("Cannot construct implementation class '%s'", className));
        }
        return result;
    }

    private void logOnly(String subject, String message) {
        log_.debug("*** DROPPED MESSAGE (%s): %s", subject, message);
    }

    private TokenAuthentication createTokenProvider(String className, Map<String,String> config) {
        TokenAuthentication result = (TokenAuthentication)commonDynamicBuilder(className);
        if(result != null) {
            try {
                result.initialize(log_, config);
            } catch(TokenAuthenticationException e) {
                log_.exception(e);
                result = null;
            }
        }
        return result;
    }

    private final Map<String,String> args_;
    private String subject_;
    private Logger log_;
    private URI subscriptionUri_;
    private HttpMethod subscribeMethod_;
    private final HttpMethod unsubscribeMethod_;
    private SubscriptionRequestBuilder requestBuilder_;
    private SubscriptionResponseParser responseParser_;
    private RESTClient client_;
    private RESTServer server_;
    private NetworkDataSinkDelegate incomingMessageCallback_ = this::logOnly;
    private int connectionTimeoutSeconds_ = 300;
    private TokenAuthentication tokenProvider_ = null;
    private URI subscriptionRemoveUrl_ = null;
    private final String subscriberName_;
    private final URI callbackUri_;

    private static final List<String> requiredArgs_ = new ArrayList<>() {{
        add("bindAddress");
        add("bindPort");
        add("connectAddress");
        add("connectPort");
        add("urlPath");
        add("subjects");
        add("requestBuilder");
        add("responseParser");
        add("subscriberName");
    }};
    private static String HTTP_IMPLEMENTATION = "jdk11";
}
