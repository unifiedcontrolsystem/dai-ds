// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient.java11;

import com.intel.logging.Logger;
import com.intel.networking.restclient.*;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

/**
 * Description of class Java11RESTClient. This REST client uses permissive SSL (trusts ALL certificates).
 * To change to exclusive certificate, use the default SSLContext object and create a keystore for Java with
 * the certificate in the store. Then point the SSLContext to the new keystore. This will limit the REST
 * client to communicate with ONLY the server using the certificate specified. All other SSL connections will
 * fail.
 *
 * This class is still backward compatible with regular non-SSL HTTP.
 */
public class Java11RESTClient extends RESTClient {
    /**
     * Called by RESTClientFactory. Should not be called directly to create instance!
     *
     * @param log The logger to use.
     */
    public Java11RESTClient(Logger log) {
        super(log);
        try { // This SSLContext accepts ALL certificates and should NEVER be used to talk to the internet!!!
            SSLContext context = SSLContext.getInstance("TLS");
            TrustManager[] tm = new TrustManager[] {
                    new X509TrustManager() {
                        @Override
                        public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                                throws CertificateException {
                        }

                        @Override
                        public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                                throws CertificateException {
                        }

                        @Override
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }
                    }
            };
            context.init(null, tm, new SecureRandom());
            client_ = HttpClient.newBuilder().sslContext(context).build();
        } catch(NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to setup permissive SSL connection", e);
        }
    }

    /**
     * Called by the subscribeToSSEPOST method after the SSERequestBuilder has converted the event types list to a
     * PropertyDocument body. This MUST be implemented by the RESTClient implementation.
     *
     * @param requestInfo    The RequestInfo to make the SSE subscription request.
     * @param callback       Called when the initial HTTP response is sent from the server.
     * @param eventsCallback Called for every non-comment event that is sent.
     */
    @Override
    protected void doSSERequest(RequestInfo requestInfo, ResponseCallback callback, SSEEvent eventsCallback) {
        new Thread(()-> {
            HttpRequest request = makeRESTRequest(requestInfo);
            log_.debug("*** SSE Request: method='%s'; uri='%s'; body='%s'", requestInfo.method(), requestInfo.uri(),
                    requestInfo.body());
            SSELineSubscriber lineSub = new SSELineSubscriber(requestInfo, eventsCallback, log_);
            HttpResponse.BodyHandler<Void> handler = HttpResponse.BodyHandlers.fromLineSubscriber(lineSub);
            try {
                client_.send(request, handler);
                log_.debug("*** Completed.");
                callback.responseCallback(-2, null, requestInfo);
            } catch(IOException | InterruptedException e) {
                log_.debug("*** SSE Connection Failed!");
                callback.responseCallback(-1, exceptionToString(e), requestInfo);
            }
        }).start();
    }

    /**
     * Called by ALL REST methods (not SSE) to do the actual HTTP request.
     *
     * @param requestInfo The RequestInfo for the request.
     * @return The blocking result.
     */
    @Override
    protected BlockingResult doRESTRequest(RequestInfo requestInfo) {
        HttpRequest request = makeRESTRequest(requestInfo);
        BlockingResult result;
        try {
            log_.debug("*** REST Request: method='%s'; uri='%s'; body='%s'", requestInfo.method(), requestInfo.uri(),
                    requestInfo.body());
            HttpResponse<String> response = client_.send(request, HttpResponse.BodyHandlers.ofString());
            result = new BlockingResult(response.statusCode(), response.body(), requestInfo);
        } catch(InterruptedException | IOException e) {
            result = new BlockingResult(-1, exceptionToString(e), requestInfo);
        }
        return result;
    }

    protected HttpRequest.Builder createBuilder() { // protected for testing...
        return HttpRequest.newBuilder();
    }

    private HttpRequest makeRESTRequest(RequestInfo requestInfo) {
        HttpRequest.Builder builder = createBuilder();
        builder.uri(requestInfo.uri());
        if(requestInfo.body() == null)
            builder.method(requestInfo.method().toString(), HttpRequest.BodyPublishers.noBody());
        else {
            builder.method(requestInfo.method().toString(),
                    HttpRequest.BodyPublishers.ofString(requestInfo.body()));
            builder.header("Content-Type", "application/json");
        }
        for(String header: requestInfo.headers().keySet())
            builder.header(header, requestInfo.headers().get(header));
        return builder.build();
    }

    private String exceptionToString(Exception e) {
        StringBuilder builder = new StringBuilder();
        makeException(e, builder);
        return builder.toString();
    }

    void makeException(Throwable e, StringBuilder builder) {
        builder.append("Exception: ").append(e.getClass().getCanonicalName()).append(": ").append(e.getMessage()).append("\n");
        makeTrace(e, builder);
        if(e.getCause() != null) {
            builder.append("Caused by ");
            makeException(e.getCause(), builder);
        }
    }

    private void makeTrace(Throwable exception, StringBuilder builder) {
        for(StackTraceElement element: exception.getStackTrace())
            builder.append("   ").append(element.toString()).append("\n");
    }

    protected HttpClient client_;
}
