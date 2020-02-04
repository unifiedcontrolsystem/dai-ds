// Copyright (C) 2020 Paul Amonson
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.restclient.apache;

import com.intel.logging.Logger;
import com.intel.networking.restclient.*;
import org.apache.http.*;
import org.apache.http.client.methods.*;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Map;

/**
 * Description of class ApacheRESTClient. This REST client uses permissive SSL (trusts ALL certificates and host names).
 * To change to exclusive certificate, use the default SSLContext object and create a keystore for Java with
 * the certificate in the store. Then point the SSLContext to the new keystore. This will limit the REST
 * client to communicate with ONLY the server using the certificate specified. All other SSL connections will
 * fail.
 *
 * This class is still backward compatible with regular non-SSL HTTP.
 */
public class ApacheRESTClient extends RESTClient {
    /**
     * Called by derived classes only to construct this parent implementation.
     *
     * @param log The logger to use.
     */
    public ApacheRESTClient(Logger log) {
        super(log);
        try { // This SSLContext accepts ALL certificates and should NEVER be used to talk to the internet!!!
            SSLContext context = SSLContext.getInstance("TLS");
            TrustManager[] tm = new TrustManager[] { new PermissiveX509TrustManager() };
            context.init(null, tm, new SecureRandom());
            client_ = HttpClientBuilder.create().
                    setSSLContext(context).
                    setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE).
                    build();
        } catch(NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to setup permissive SSL connection", e);
        }
    }

    /**
     * Called by the subscribeToSSE method after the SSERequestBuilder has converted the event types list to a
     * PropertyDocument body. This MUST be implemented by the RESTClient implementation.
     *
     * @param request        The RequestInfo to make the SSE subscription request.
     * @param callback       Called when the initial HTTP response is sent from the server.
     * @param eventsCallback Called for every non-comment event that is sent.
     */
    @Override
    protected void doSSERequest(RequestInfo request, ResponseCallback callback, SSEEvent eventsCallback) {
        HttpGet methodRequest = new HttpGet(request.uri());
        log_.debug("*** SSE Request: uri='%s'", request.uri());
        try {
            client_.execute(methodRequest, (response) -> {
                for(Header header: response.getAllHeaders())
                    log_.debug("HEADER: %s: %s", header.getName(), header.getValue());
                int code = response.getStatusLine().getStatusCode();
                if(code >= 200 && code < 300) {
                    callback.responseCallback(code, null, request);
                    try (Reader baseReader = new InputStreamReader(response.getEntity().getContent(),
                            StandardCharsets.UTF_8)) {
                        try (LineNumberReader reader = new LineNumberReader(baseReader)) {
                            String line = reader.readLine();
                            while(line != null) {
                                event_.processLine(line, eventsCallback);
                                line = reader.readLine();
                            }
                        }
                    } finally {
                        log_.debug("*** Closing the content stream #1...");
                        response.getEntity().getContent().close();
                    }
                } else {
                    callback.responseCallback(code, streamToBody(response.getEntity().getContent()), request);
                    log_.debug("*** Closing the content stream #2...");
                    response.getEntity().getContent().close();
                }
                return null;
            });
        } catch(IOException e) {
            log_.exception(e, "Failed to create SSE connection!");
            callback.responseCallback(-1, exceptionToString(e), request);
        }
    }

    /**
     * Called by ALL REST methods (not SSE) to do the actual HTTP request.
     *
     * @param request The RequestInfo for the request.
     * @return The blocking result.
     */
    @Override
    protected BlockingResult doRESTRequest(RequestInfo request) {
        HttpRequestBase methodRequest = makeRequest(request);
        log_.debug("*** REST Request: method='%s'; uri='%s'; body='%s'", request.method(), request.uri(),
                request.body());
        try (CloseableHttpResponse response = client_.execute(methodRequest)) {
            try (InputStreamReader reader = new InputStreamReader(response.getEntity().getContent(),
                    StandardCharsets.UTF_8)) {
                StringWriter writer = new StringWriter();
                reader.transferTo(writer);
                return new BlockingResult(response.getStatusLine().getStatusCode(), writer.toString(), request);
            }
        } catch (IOException e) {
            log_.exception(e);
            return new BlockingResult(-1, exceptionToString(e), request);
        }
    }

    private String streamToBody(InputStream stream) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            StringWriter writer = new StringWriter();
            reader.transferTo(writer);
            return writer.toString();
        }
    }

    private String exceptionToString(Exception e) {
        StringBuilder builder = new StringBuilder();
        makeException(e, builder);
        return builder.toString();
    }

    void makeException(Throwable e, StringBuilder builder) {
        builder.append("Exception: ").append(e.getClass().getCanonicalName()).append(": ").append(e.getMessage()).
                append("\n");
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

    private HttpRequestBase makeRequest(RequestInfo info) {
        HttpRequestBase result;
        switch(info.method()) {
            case GET:
            default:
                result = new HttpGet(info.uri());
                break;
            case HEAD:
                result = new HttpHead(info.uri());
                break;
            case POST:
                result = new HttpPost(info.uri());
                break;
            case PUT:
                result = new HttpPut(info.uri());
                break;
            case DELETE:
                result = new HttpDelete(info.uri());
                break;
            case OPTIONS:
                result = new HttpOptions(info.uri());
                break;
            case PATCH:
                result = new HttpPatch(info.uri());
                break;
        }
        for(Map.Entry<String,String> entry: info.headers().entrySet())
            result.addHeader(entry.getKey(), entry.getValue());
        if(info.body() != null && !info.body().isBlank() && result instanceof HttpEntityEnclosingRequestBase) {
            if(!info.headers().containsKey("Content-Type"))
                result.addHeader("Content-Type", "application/json");
            try {
                ((HttpEntityEnclosingRequestBase) result).setEntity(new StringEntity(info.body()));
            } catch (UnsupportedEncodingException e) {
                log_.exception(e);
            }
        }
        return result;
    }

    private EventSource event_ = new EventSource();
    private CloseableHttpClient client_;
}
