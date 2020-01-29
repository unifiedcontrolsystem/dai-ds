// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.authentication;

import com.intel.logging.Logger;

import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.AccessTokenResponse;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Implementation of TokenAuthentication using Keycloak libraries. Supported arguments in the passes map are:
 *
 * tokenServer  - (Req.) The URL string to get the token from.
 * clientId     - (Req.) The keycloak client Id to use for access.
 * clientSecret - (Req.) The keycloak client secret to use for access.
 * realm        - (Opt.) If required by the client Id this must be the realm of the client ID when it was created.
 * username     - (Opt.) If performing username/password authentication to the keycloak server this must be set.
 * password     - (Opt.) If performing username/password authentication to the keycloak server this must be set.
 */
public class KeycloakTokenAuthentication implements TokenAuthentication {
    public KeycloakTokenAuthentication() {} // Required for dynamic creation...

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
        KeycloakBuilder builder = getBuilder()
                .clientId(getRequiredArgumentValue(arguments, "clientId"))
                .clientSecret(getRequiredArgumentValue(arguments, "clientSecret"))
                .serverUrl(getRequiredArgumentValue(arguments, "tokenServer"))
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS);
        supportRealm(arguments, builder);
        supportUsernamePassword(arguments, builder);
        log_.info("Creating an OAuth Token Bearer class for interface %s",
                TokenAuthentication.class.getCanonicalName());
        cloak_ = builder.build();
        manager_ = cloak_.tokenManager();
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
        if(cloak_ == null)
            throw new TokenAuthenticationException("The object has not been initialized!");
        return getTokenString();
    }

    private String getTokenString() {
        long now = now();
        AccessTokenResponse response = null;
        if(now >= refreshOn_) {
            log_.info("Getting a new OAuth Bearer Token...");
            response = manager_.getAccessToken();
        } else if(now >= expiresOn_) {
            log_.info("Refreshing an OAuth Bearer Token...");
            response = manager_.refreshToken();
        }
        return getNewTokenStringFromResponse(now, response);
    }

    private String getNewTokenStringFromResponse(long now, AccessTokenResponse response) {
        if(response != null) {
            refreshOn_ = now + response.getRefreshExpiresIn() - 5; // 5 second margin of error...
            expiresOn_ = now + response.getExpiresIn() - 5; // 5 second margin of error...
            token_ = response.getToken();
        }
        return token_;
    }

    KeycloakBuilder getBuilder() { return KeycloakBuilder.builder(); } // Package-private for testing...

    long now() { return Instant.now().getEpochSecond(); } // Package-private for testing...

    private String getRequiredArgumentValue(Map<String,String> arguments, String key)
            throws TokenAuthenticationException {
        if(arguments.getOrDefault(key, null) == null)
            throw new TokenAuthenticationException(String.format("Argument '%s' is missing or null but is required",
                    key));
        return arguments.get(key);
    }

    private void supportUsernamePassword(Map<String, String> arguments, KeycloakBuilder builder) {
        String username = arguments.getOrDefault("username", null);
        String password = arguments.getOrDefault("password", null);
        if(username != null && password != null) {
            builder.grantType(OAuth2Constants.PASSWORD)
                    .username(username)
                    .password(password);
        }
    }

    private void supportRealm(Map<String,String> arguments, KeycloakBuilder builder) {
        String realm = arguments.getOrDefault("realm", null);
        if(realm != null)
            builder.realm(realm);
    }

    private Logger log_;
    private Keycloak cloak_ = null;
    private TokenManager manager_ = null;
    protected String token_ = null;
    protected long expiresOn_ = 0L;
    protected long refreshOn_ = 0L;
}
