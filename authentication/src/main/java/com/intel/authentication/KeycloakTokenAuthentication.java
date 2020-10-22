// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.authentication;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import org.apache.commons.io.IOUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

/**
 * Implementation of TokenAuthentication using REST HTTP POST. Supported arguments in the passes map are:
 *
 * tokenServer  - (Req.) The URL string to get the token from.  If the realm is part of the URL use {{REAL}} to
 *                       to substitute the realm in the URL.
 * clientId     - (Req.) The keycloak client Id to use for access.
 * clientSecret - (Opt.) The keycloak client secret to use for access.
 * realm        - (Req.) If required by the client Id this must be the realm of the client ID when it was created.
 * username     - (Opt.) If performing username/password authentication to the keycloak server this must be set.
 * password     - (Opt.) If performing username/password authentication to the keycloak server this must be set.
 * curl         - (Opt.) The path to curl on the system. Defaults to '/usr/bin/curl'.
 */
public class KeycloakTokenAuthentication implements TokenAuthentication {
    public KeycloakTokenAuthentication() {
        if (parser_ == null)
            throw new NullPointerException("Failed to instantiate JSON parser for this TokenAuthentication implementation.");
    }

    /**
     * Initialize the OAuth Bearer Token retriever class.
     *
     * @param logger The logger to use for the implementation.
     * @param arguments The arguments passed to the implementation (implementation specific)
     * @throws TokenAuthenticationException when the instance is already initialized or if the initialization fails.
     */
    @Override
    public void initialize(Logger logger, Map<String, String> arguments) throws TokenAuthenticationException {
        if(logger == null) throw new IllegalArgumentException("'logger' is null but not allowed to be null");
        if(arguments == null) throw new IllegalArgumentException("'arguments' is null but not allowed to be null");
        log_ = logger;
        log_.debug("OAuth Bearer Token configuration values:");
        for(Map.Entry<String,String> entry: arguments.entrySet())
            log_.debug("    ===>>> '%s' = '%s'", entry.getKey(), entry.getValue());
        log_.info(String.format("Creating an OAuth Token Bearer class for interface %s",
                TokenAuthentication.class.getCanonicalName()));
        serverUrl_ = getRequiredArgumentValue(arguments, ARG_SERVER_URL);
        realm_ = getRequiredArgumentValue(arguments, ARG_REALM);
        serverUrl_ = serverUrl_.replace(REALM_VAR, realm_);
        if(arguments.getOrDefault(ARG_USERNAME, null) != null) {
            username_ = getRequiredArgumentValue(arguments, ARG_USERNAME);
            password_ = getRequiredArgumentValue(arguments, ARG_PASSWORD);
            clientId_ = arguments.getOrDefault(ARG_CLIENT_ID, null);
            userPass_ = true;
        } else {
            clientId_ = getRequiredArgumentValue(arguments, ARG_CLIENT_ID);
            clientSecret_ = getRequiredArgumentValue(arguments, ARG_CLIENT_SECRET);
            userPass_ = false;
        }
        curl_ = arguments.getOrDefault("curl", "/usr/bin/curl");
        // Mitigate some security attacks...weak but better than nothing.
        checkCurl();
    }

    /**
     * Retrieve a token to use in the communication with an OAuth protected REST interface.
     *
     * @return The token as a string to use in the HTTP header "Authorization: Bearer <token>" line for the
     * REST requests.
     * @throws TokenAuthenticationException when the instance has not been initialized.
     */
    @Override
    public String getToken() throws TokenAuthenticationException {
        long now = now();
        if(now < refreshOn_)
            return token_;
        else if (now < expiresOn_ - 5) // 5 second window allowing for network delays...
            return getTokenWithGrantType(GT_REFRESH);
        else {
            reset();
            return getTokenWithGrantType(userPass_ ? GT_PASSWORD : GT_CLIENT_CREDS);
        }
    }

    private void checkCurl() throws TokenAuthenticationException {
        if(doCheck_)
            if(!curl_.endsWith("curl") || curl_.contains("home") || curl_.contains("tmp") || curl_.contains("opt") ||
                    !(new File(curl_)).exists())
                throw new TokenAuthenticationException("The 'curl' command specified by the passed configuration is " +
                        "suspicious or missing: " + curl_);
    }

    private String getTokenWithGrantType(String grant) throws TokenAuthenticationException {
        Map<String,String> nameValuePairs = new HashMap<>();
        nameValuePairs.put(REQ_REALM, realm_);
        nameValuePairs.put(REQ_GRANT_TYPE, grant);
        nameValuePairs.put(REQ_CLIENT_ID, clientId_);
        if(userPass_) {
            nameValuePairs.put(REQ_USERNAME, username_);
            nameValuePairs.put(REQ_PASSWORD, password_);
        } else
            nameValuePairs.put(REQ_CLIENT_SECRET, clientSecret_);
        try {
            String responseBody = makePOSTRequest(serverUrl_, nameValuePairs);
            parseResponse(responseBody);
            return token_;
        } catch(IOException | InterruptedException e) {
            log_.exception(e, "Failed to get a new/refreshed token");
            throw new TokenAuthenticationException("Failed to get OAuth2 token: " + e.getMessage());
        }
    }

    private String makePOSTRequest(String uri, Map<String,String> params)
            throws IOException, InterruptedException, TokenAuthenticationException {
        List<String> command = new ArrayList<>();
        command.add(curl_);
        command.add("-s");
        for(Map.Entry<String,String> entry: params.entrySet()) {
            command.add("-d");
            command.add(String.format("%s=%s", entry.getKey(), entry.getValue()));
        }
        command.add(uri);
        Process curl = getProcess(command);
        curl.waitFor();
        if(curl.exitValue() != 0)
            throw new TokenAuthenticationException(String.format("Failed to get token using curl: %d", curl.exitValue()));
        try (Reader reader = new InputStreamReader(curl.getInputStream(), StandardCharsets.UTF_8)) {
            StringWriter writer = new StringWriter();
            IOUtils.copy(reader, writer);
            return writer.toString();
        }
    }

    Process getProcess(List<String> command) throws IOException { // For testing...
        return new ProcessBuilder(command).start();
    }

    long now() { return Instant.now().getEpochSecond(); } // Package-private for unit testing...

    private String getRequiredArgumentValue(Map<String,String> arguments, String key)
            throws TokenAuthenticationException {
        if(arguments.getOrDefault(key, null) == null)
            throw new TokenAuthenticationException(String.format("Argument '%s' is missing or null but is required",
                    key));
        return arguments.get(key);
    }

    private void reset() {
        expiresOn_ = 0L;
        refreshOn_ = 0L;
        token_ = null;
        refreshToken_ = null;
    }

    private void parseResponse(String body) throws TokenAuthenticationException {
        try {
            PropertyMap map = parser_.fromString(body.trim()).getAsMap(); // Cannot throw a NullPointerException...
            if(!map.getStringOrDefault("token_type", "missing").equals("bearer"))
                throw new TokenAuthenticationException("Wrong type of token returned or it was missing from " +
                        "the response.");
            token_ = map.getStringOrDefault("access_token", null);
            refreshToken_ = map.getStringOrDefault("refresh_token", null);
            expiresOn_ = Instant.now().getEpochSecond() + map.getIntOrDefault("expires_in", 31_557_600);
            refreshOn_ = expiresOn_ / 2L;
        } catch(ConfigIOParseException e) {
            throw new TokenAuthenticationException("Bad JSON received from token server.");
        }
    }

            boolean doCheck_ = true;
    private Logger log_;
    private String serverUrl_;
    private String curl_;
    private String realm_;
    private String clientId_ = null;
    private String clientSecret_ = null;
    private String username_ = null;
    private String password_ = null;
    private boolean userPass_;
    protected String token_ = null;
    protected String refreshToken_ = null;
    protected long expiresOn_ = 0L;
    protected long refreshOn_ = 0L;

    private static       ConfigIO parser_ = ConfigIOFactory.getInstance("json");

    private static final String GT_PASSWORD = "password";
    private static final String GT_CLIENT_CREDS = "client_credentials";
    private static final String GT_REFRESH = "refresh_token";

    private static final String REQ_REALM = "realm";
    private static final String REQ_USERNAME = "username";
    private static final String REQ_PASSWORD = "password";
    private static final String REQ_CLIENT_ID = "client_id";
    private static final String REQ_CLIENT_SECRET = "client_secret";
    private static final String REQ_GRANT_TYPE = "grant_type";

    private static final String ARG_SERVER_URL = "tokenServer";
    private static final String ARG_REALM = "realm";
    private static final String ARG_CLIENT_ID = "clientId";
    private static final String ARG_CLIENT_SECRET = "clientSecret";
    private static final String ARG_USERNAME = "username";
    private static final String ARG_PASSWORD = "password";

    private static final String REALM_VAR = "{{REALM}}";
}
