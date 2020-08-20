// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.authentication

import com.intel.logging.Logger
import org.keycloak.admin.client.Keycloak
import org.keycloak.admin.client.KeycloakBuilder
import org.keycloak.admin.client.token.TokenManager
import org.keycloak.representations.AccessTokenResponse
import spock.lang.Specification

class KeycloakTokenAuthenticationSpec extends Specification {
    def builder_ = Mock(KeycloakBuilder)
    def cloak_ = Mock(Keycloak)
    def manager_ = Mock(TokenManager)
    def response_ = Mock(AccessTokenResponse)
    def logger_ = Mock(Logger)
    def now_ = 0L
    def token_
    class UnderTest extends KeycloakTokenAuthentication {
        @Override KeycloakBuilder getBuilder() { return builder_ }
        @Override long now() { return now_ }
    }

    def underTest_
    def arguments_
    void setup() {
        arguments_ = [
                "tokenServer":"https://localhost:8080/auth",
                "clientId":"testClientId",
                "clientSecret":"some_secret",
                "realm":"null",
                "username":"null",
                "password":"null"
        ]
        builder_.grantType(_) >> builder_
        builder_.serverUrl(_) >> builder_
        builder_.realm(_) >> builder_
        builder_.clientId(_) >> builder_
        builder_.clientSecret(_) >> builder_
        builder_.username(_) >> builder_
        builder_.password(_) >> builder_
        builder_.build() >> cloak_
        cloak_.tokenManager() >> manager_
        manager_.getAccessToken() >> response_
        manager_.refreshToken() >> response_
        response_.getExpiresIn() >> 60
        response_.getRefreshExpiresIn() >> 120
        token_ = "newToken"
        response_.getToken() >> { token_ }

        underTest_ = new UnderTest()
    }

    def "Test initialize with negative inputs"() {
        when:
        underTest_.initialize(logger, args)
        then:
        def e = thrown(except)
        e.cause == null
        where:
        logger       | args | except
        Mock(Logger) | null | IllegalArgumentException
        Mock(Logger) | [:]  | TokenAuthenticationException
        null         | null | IllegalArgumentException
    }

    def "Test initialize with varying arguments"() {
        arguments_."realm" = realm
        arguments_."username" = user
        arguments_."password" = pass
        when: underTest_.initialize(logger_, arguments_)
        then: thrown(TokenAuthenticationException)
        where:
        realm   | user   | pass
        null    | null   | "passwd"
        null    | "name" | null
//        "realm" | "name" | "passwd"
    }

    def "Test getToken without initializing"() {
        when:
        underTest_.getToken()
        then:
        def e = thrown(TokenAuthenticationException)
        e.cause == null
    }

    def "Test getToken"() {
        underTest_.initialize(logger_, arguments_)
        token_ = (token == "SKIP")?"newToken":token
        underTest_.token_ = (time == 1L)?null:"newToken"
        now_ = time
        underTest_.expiresOn_ = expire
        underTest_.refreshOn_ = refresh
        expect: underTest_.getToken() == retVal
        where:
        time | expire | refresh | token           || retVal
        1L   | 0L     | 0L      | "SKIP"          || "newToken"
        31L  | 60L    | 120L    | "SKIP"          || "newToken"
        91L  | 60L    | 120L    | "SKIP"          || "newToken"
        121L | 60L    | 120L    | "brandNewToken" || "brandNewToken"
    }

    def "Test the getBuilder method"() {
        KeycloakTokenAuthentication instance = new KeycloakTokenAuthentication()
        expect: instance.getBuilder() != null
    }

    def "Test the now method"() {
        KeycloakTokenAuthentication instance = new KeycloakTokenAuthentication()
        expect: instance.now() > 0L
    }
}
