// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient;

import com.intel.authentication.TokenAuthentication;
import com.intel.authentication.TokenAuthenticationException;
import com.intel.logging.Logger;
import com.intel.networking.HttpMethod;

import java.net.URI;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Description of class RESTClient.
 */
public abstract class RESTClient {
    /**
     * Set a new SSERequestBuilderObject into the RESTClient. NOTE: The server must understand the format generated
     * by the builder.
     *
     * @param builder The SSERequestBuilder implementation.
     */
    public void setSSERequestBuilder(SSERequestBuilder builder) {
        if(builder == null)
            builder = new DefaultSSERequestBuilder();
        builder_ = builder;
    }

    /**
     * Set in an optional OAuth 2.0 token retriever implementation or null.
     *
     * @param oauth The instance of a TokenAuthentication implm,entation or null.
     */
    public void setTokenOAuthRetriever(TokenAuthentication oauth) {
        tokenRetriever_ = oauth;
    }

    /**
     * Make a generic REST GET request to the URI passed.
     *
     * @param uri The URI to the RESTServer running in another process.
     * @return The response as a BlockingResult and a valid payload in the responseDocument property.
     * @throws RESTClientException when OAuth token retrieval fails.
     */
    public final BlockingResult getRESTRequestBlocking(URI uri) throws RESTClientException {
        return methodRESTRequestBlocking(HttpMethod.GET, uri, null);
    }

    /**
     * Make a generic REST HEAD request on a GET resource to the URI passed.
     *
     * @param uri The URI to the RESTServer running in another process.
     * @return The response as a BlockingResult with the responseDocument set to null.
     * @throws RESTClientException when OAuth token retrieval fails.
     */
    public final BlockingResult headRESTRequestBlocking(URI uri) throws RESTClientException {
        return methodRESTRequestBlocking(HttpMethod.HEAD, uri, null);
    }

    /**
     * Make a generic REST POST request on a resource to the URI passed.
     *
     * @param uri The URI to the RESTServer running in another process.
     * @param body The body String to POST.
     * @return The response as a BlockingResult with the responseDocument set to null.
     * @throws RESTClientException when OAuth token retrieval fails.
     */
    public final BlockingResult postRESTRequestBlocking(URI uri, String body) throws RESTClientException {
        return methodRESTRequestBlocking(HttpMethod.POST, uri, body);
    }

    /**
     * Make a generic REST PUT request on a resource to the URI passed.
     *
     * @param uri The URI to the RESTServer running in another process.
     * @param body The body String to PUT.
     * @return The response as a BlockingResult with the responseDocument set to null.
     * @throws RESTClientException when OAuth token retrieval fails.
     */
    public final BlockingResult putRESTRequestBlocking(URI uri, String body) throws RESTClientException {
        return methodRESTRequestBlocking(HttpMethod.PUT, uri, body);
    }

    /**
     * Make a generic REST PATCH request on a resource to the URI passed.
     *
     * @param uri The URI to the RESTServer running in another process.
     * @param body The body String to PATCH.
     * @return The response as a BlockingResult with the responseDocument set to null.
     * @throws RESTClientException when OAuth token retrieval fails.
     */
    public final BlockingResult patchRESTRequestBlocking(URI uri, String body) throws RESTClientException {
        return methodRESTRequestBlocking(HttpMethod.PATCH, uri, body);
    }

    /**
     * Make a generic REST DELETE request on a resource to the URI passed.
     *
     * @param uri The URI to the RESTServer running in another process.
     * @return The response as a BlockingResult with the responseDocument set to null.
     * @throws RESTClientException when OAuth token retrieval fails.
     */
    public final BlockingResult deleteRESTRequestBlocking(URI uri) throws RESTClientException {
        return methodRESTRequestBlocking(HttpMethod.DELETE, uri, null);
    }

    /**
     * Make a generic asynchronous REST GET request to the URI passed. The callback will be called upon completion.
     *
     * @param uri The URI to the RESTServer running in another process.
     * @param callback The response callback with response info passed.
     * @return The response as a BlockingResult and a valid payload in the responseDocument property.
     */
    public final void getRESTRequestAsync(URI uri, ResponseCallback callback) {
        methodRESTRequestAsync(HttpMethod.GET, uri, null, callback);
    }

    /**
     * Make a generic asynchronous REST POST request to the URI passed. The callback will be called upon completion.
     *
     * @param uri The URI to the RESTServer running in another process.
     * @param body The body String to POST.
     * @param callback The response callback with response info passed.
     * @return The response as a BlockingResult and a valid payload in the responseDocument property.
     */
    public final void postRESTRequestAsync(URI uri, String body, ResponseCallback callback) {
        methodRESTRequestAsync(HttpMethod.POST, uri, body, callback);
    }

    /**
     * Make a generic asynchronous REST PUT request to the URI passed. The callback will be called upon completion.
     *
     * @param uri The URI to the RESTServer running in another process.
     * @param body The body String to POST.
     * @param callback The response callback with response info passed.
     * @return The response as a BlockingResult and a valid payload in the responseDocument property.
     */
    public final void putRESTRequestAsync(URI uri, String body, ResponseCallback callback) {
        methodRESTRequestAsync(HttpMethod.PUT, uri, body, callback);
    }

    /**
     * Make a generic asynchronous REST PATCH request to the URI passed. The callback will be called upon completion.
     *
     * @param uri The URI to the RESTServer running in another process.
     * @param body The body String to POST.
     * @param callback The response callback with response info passed.
     * @return The response as a BlockingResult and a valid payload in the responseDocument property.
     */
    public final void patchRESTRequestAsync(URI uri, String body, ResponseCallback callback) {
        methodRESTRequestAsync(HttpMethod.PATCH, uri, body, callback);
    }

    /**
     * Make a generic asynchronous REST DELETE request to the URI passed. The callback will be called upon completion.
     *
     * @param uri The URI to the RESTServer running in another process.
     * @param callback The response callback with response info passed.
     * @return The response as a BlockingResult and with the responseDocument set to null.
     */
    public final void deleteRESTRequestAsync(URI uri, ResponseCallback callback) {
        methodRESTRequestAsync(HttpMethod.DELETE, uri, null, callback);
    }

    /**
     * Generic HTTP method request (blocking).
     *
     * @param method The HttpMethod to use in the request.
     * @param uri The URI of the request.
     * @param body If method is POST, PUT, PATCH then this is the String to use as the request body.
     * @return The Blocking result containing the HTTP response code integer (-1 on failure to connect) and the response
     * body as a PropertyDocument as well as the original request info.
     * @throws RESTClientException when OAuth token retrieval fails.
     */
    public final BlockingResult methodRESTRequestBlocking(HttpMethod method, URI uri, String body) throws RESTClientException {
        Map<String,String> headers = new HashMap<>();
        getHeaders(headers);
        return doRESTRequest(new RequestInfo(method, uri, body, headers));
    }

    /**
     * General Asynchronous HTTP Method request the used the general blocking method.
     *
     * @param method  The HttpMethod to use in the request.
     * @param uri The URI of the request.
     * @param body If method is POST, PUT, PATCH then this is the String to use as the request body.
     * @param callback This ResponseCallback instance will be called on failure or completion.
     */
    public final void methodRESTRequestAsync(HttpMethod method, URI uri, String body,
                                             ResponseCallback callback) {
        new Thread(() -> {
            try {
                BlockingResult result = methodRESTRequestBlocking(method, uri, body);
                if (callback != null) {
                    try {
                        callback.responseCallback(result.code, result.responseDocument, result.requestInfo);
                    } catch (Exception e) { // Anything can happen in user code!
                        log_.exception(e, "User callback from getRESTRequestAsync threw an unexpected exception");
                    }
                }
            } catch(RESTClientException e) {
                callback.responseCallback(-1, null, new RequestInfo(method, uri, body));
            }
        }).start();
    }

    /**
     * Subscribes to server side events at the URI specified. The request is a POST with the subjects in the body
     * formatted by the SSERequestBuilder. This request is always asynchronous due the nature of the SSE protocol.
     *
     * @param uri The URI of the SSE subscription request (server must support this on the URI).
     * @param eventTypes The list of event types to be formatted by the current SSERequestBuilder instance.
     * @param callback The callback for events
     * @param otherArgs Extra information for the request builder object.
     * @throws RESTClientException when OAuth token retrieval fails.
     */
    public final void subscribeToSSEGET(URI uri, Collection<String> eventTypes, ResponseCallback callback,
                                         SSEEvent eventsCallback, Map<String,String> otherArgs)
            throws RESTClientException {
        String query = builder_.buildRequest(eventTypes, otherArgs);
        URI newUri = URI.create(uri.toString() + query);
        Map<String,String> headers = new HashMap<>();
        getHeaders(headers);
        doSSERequest(new RequestInfo(HttpMethod.GET, newUri, null, headers), callback, eventsCallback);
    }

    /**
     * Called by the subscribeToSSEPOST method after the SSERequestBuilder has converted the event types list to a
     * PropertyDocument body. This MUST be implemented by the RESTClient implementation.
     *
     * @param request The RequestInfo to make the SSE subscription request.
     * @param callback Called when the initial HTTP response is sent from the server.
     * @param eventsCallback Called for every non-comment event that is sent.
     */
    protected abstract void doSSERequest(RequestInfo request, ResponseCallback callback, SSEEvent eventsCallback);

    /**
     * Called by ALL REST methods (not SSE) to do the actual HTTP request.
     *
     * @param request The RequestInfo for the request.
     * @return The blocking result.
     */
    protected abstract BlockingResult doRESTRequest(RequestInfo request);

    /**
     * Called by derived classes only to construct this parent implementation.
     *
     * @param log The logger to use.
     */
    protected RESTClient(Logger log) {
        if(log == null)
            throw new AssertionError("The Logger cannot be null!");
        log_ = log;
        setSSERequestBuilder(null);
    }

    private void getHeaders(Map<String,String> headers) throws RESTClientException {
        if(tokenRetriever_ != null) {
            try {
                headers.put("Authorization", "Bearer " + tokenRetriever_.getToken());
            } catch(TokenAuthenticationException e) {
                log_.exception(e, "Failed to retrieve OAuth 2.0 token from the specified token server.");
                throw new RESTClientException(e);
            }
        }
    }

    /**
     * Used in derived implementations as the logger.
     */
    protected Logger log_;
    private SSERequestBuilder builder_;
    private TokenAuthentication tokenRetriever_;
}
