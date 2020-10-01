// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.networking.sink.sse;

import com.intel.authentication.TokenAuthentication;
import com.intel.authentication.TokenAuthenticationException;
import org.apache.http.HeaderIterator;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

/**
 * Implements the EventSource Object using Apache HTTP client as the communication library. Please read the
 * SSE Specification for details of this algorithm:
 *
 *  https://html.spec.whatwg.org/multipage/server-sent-events.html
 */
class EventSourceClient implements Runnable {
    /**
     * @param uri Full url to resource including any '?' parameters needed.
     * @param callback The listener for SSE events.
     * @param lastId The last ID used (or null if not known) for SSE communication resiliency.
     * @param authentication Object that gets an OUth2 token or null isf not required.
     * @param bufferSize Size of the buffer to use when reading the stream. A value <1 will use the default buffer size.
     */
    EventSourceClient(String uri, EventSourceHandlers callback, String lastId, TokenAuthentication authentication,
                      int bufferSize) {
        if(uri == null) throw new IllegalArgumentException("'uri' in EventListener cannot be null");
        if(callback == null) throw new IllegalArgumentException("'callback' in initialize cannot be null");
        uri_ = uri;
        callback_ = callback;
        id_ = lastId;
        auth_ = authentication;
        bufferSize_ = (bufferSize < 1)?DEFAULT_BUFFER_SIZE:bufferSize;
    }

    /**
     * Implementation based on Runnable so it can be easily Threaded.
     */
    @Override
    public void run() {
        try {
            HttpClientBuilder builder = createClientBuilder().disableAutomaticRetries();
            if(uri_.startsWith("https:")) {
                debug("Creating permissive SSL context...");
                SSLContext context = SSLContext.getInstance("TLS");
                TrustManager[] tm = new TrustManager[]{new PermissiveX509TrustManager()};
                context.init(null, tm, new SecureRandom());
                builder.setSSLContext(context).setSSLHostnameVerifier(NoopHostnameVerifier.INSTANCE);
            }
            client_ = builder.build();
            debug("Beginning reconnect/retry loop...");
            while (getState() != State.CLOSED) {
                connect();
                if (getState() != State.CLOSED)
                    safeSleep(RETRY_DELAY_MS);
            }
        } catch(NoSuchAlgorithmException | KeyManagementException e) {
            signalError(e);
            signalClosed();
        } finally {
            try {
                client_.close();
            } catch(IOException e) {
                signalError(e);
            }
        }
    }

    /**
     * Stop the run loop when on a different thread.
     */
    public void stop() {
        if(getState() == State.OPEN)
            signalClosed();
    }

    protected State getState() {
        return state_;
    }

    protected HttpClientBuilder createClientBuilder() {
        return HttpClientBuilder.create();
    }

    private void connect() {
        HttpUriRequest request = new HttpGet(uri_);
        for(String header: headers_.keySet())
            request.removeHeaders(header);
        for(Map.Entry<String,String> entry: headers_.entrySet()) {
            if(entry.getKey().equals("Authorization")) {
                if(auth_ != null) {
                    try {
                        request.addHeader(entry.getKey(), "Bearer " + auth_.getToken());
                    } catch (TokenAuthenticationException e) {
                        signalError(e);
                        return;
                    }
                }
            } else if(entry.getKey().equals("Last-Event-ID")) {
                if(id_ != null)
                    request.addHeader(entry.getKey(), id_);
            } else
                request.addHeader(entry.getKey(), entry.getValue());
        }
        signalConnecting();
        HeaderIterator it = request.headerIterator();
        while(it.hasNext()) {
            String header = it.nextHeader().toString();
            if(header.length() > 50)
                header = header.substring(0, 50) + "...";
            debug("REQUEST HEADER: %s", header);
        }
        try {
            debug("Making SSE request...");
            client_.execute(request, (response) -> {
                handleResponse(response);
                return null;
            });
        } catch(IOException e) {
            signalError(e);
        }
    }

    protected void handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
        int code = response.getStatusLine().getStatusCode();
        String type = response.getLastHeader("Content-Type").getValue();
        if(code >= 200 && code < 300 && type.equals("text/event-stream")) {
            signalOpen();
            try (Reader baseReader = new InputStreamReader(response.getEntity().getContent(),
                    StandardCharsets.UTF_8)) {
                try (LineNumberReader reader = new LineNumberReader(baseReader, bufferSize_)) {
                    String line = reader.readLine();
                    while (line != null) {
                        processLine(line);
                        line = reader.readLine();
                        if(state_ == State.CLOSED)
                            break;
                    }
                }
            } catch (IOException e) {
                signalError(e);
            }
        } else {
            signalError(new ClientProtocolException(String.format("StatusCode=%d; ContentType=%s", code, type)));
            if(code != 503 || !type.equals("text/event-stream")) // If not "Service Unavailable" then treat as fatal.
                signalClosed();
        }
    }

    private void signalClosed() {
        state_ = State.CLOSED;
        callback_.onClosed();
    }

    private void signalOpen() {
        state_ = State.OPEN;
        callback_.onOpen();
    }

    private void signalConnecting() {
        state_ = State.CONNECTING;
        callback_.onConnecting();
    }

    private void signalError(Exception cause) {
        callback_.onError(cause);
    }

    private void debug(String fmt, Object... args) {
        callback_.onDebug(fmt, args);
    }

    private void dispatchMessage() {
        callback_.onMessage(event_, data_, id_);
        reset();
    }

    private void reset() {
        event_ = "";
        data_ = "";
    }

    protected void safeSleep(long ms) {
        try { Thread.sleep(ms); } catch(InterruptedException e) { /* Ignore Interruption Error*/ }
    }

    private void processLine(String line) {
        if (line.startsWith(":"))
            return; // Comment line
        if(line.trim().isEmpty()) { // do dispatch...
            if(data_.trim().isEmpty()) // empty event; reset and abort
                reset();
            else
                dispatchMessage();
        }
        else if (line.startsWith("event:"))
            event_ = line.substring(6).trim();
        else if(line.startsWith("data:")) {
            if(data_.length() > 0)
                data_ += "\n";
            data_ += line.substring(5).trim();
        } else if(line.startsWith("id:"))
            id_ = line.substring(3).trim();
    }

    private final String uri_;
    private String id_;
            State state_ = State.INITIAL;
    private final TokenAuthentication auth_;
    private CloseableHttpClient client_;
    private final EventSourceHandlers callback_;
    private String event_ = "";
    private String data_ = "";
    private final int bufferSize_;

    @SuppressWarnings("serial")
    private static final Map<String,String> headers_ = new HashMap<String,String>() {{
        put("Cache-Control", "no-cache");
        put("Accept","text/event-stream");
        put("Accept-Encoding", "gzip, deflate");
        put("Authorization", "Bearer ");
        put("Last-Event-ID", "");
    }};
    protected enum State {
        INITIAL,
        CONNECTING,
        OPEN,
        CLOSED
    }

    protected static class PermissiveX509TrustManager implements X509TrustManager {
        @Override public void checkClientTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException { }
        @Override public void checkServerTrusted(X509Certificate[] x509Certificates, String s)
                throws CertificateException { }
        @Override public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
    }

    private static final long RETRY_DELAY_MS = 10_000L; // ten second delay between connection attempts.
    private static final int DEFAULT_BUFFER_SIZE = 1024; // Default input buffer size.
}
