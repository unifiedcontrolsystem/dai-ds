// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.authentication

import com.intel.logging.Logger
import spock.lang.Specification

import java.nio.charset.StandardCharsets

class KeycloakTokenAuthenticationSpec extends Specification {
    def process_ = Mock(Process)
    static Logger logger_
    def now_ = 0L
    def underTest_
    static Map<String,String> arguments_
    def savedParser_
    static String json1_
    static String json2_
    static String badJson1_
    static String badJson2_
    void setup() {
        savedParser_ = KeycloakTokenAuthentication.parser_
        logger_ = Mock(Logger)
        arguments_ = [
                "tokenServer":"https://localhost:8080/auth/{{REALM}}",
                "clientId":"testClientId",
                "clientSecret":"some_secret",
                "realm":"realm",
                "username":null,
                "password":null,
                "curl":"/usr/bin/curl"
        ]
        json1_ = """{"token_type":"bearer","access_token":"newToken","refresh_token":"refreshToken","expires_on":31557600}"""
        json2_ = """{"token_type":"bearer","access_token":"brandNewToken","refresh_token":"refreshToken2","expires_on":31557600}"""
        badJson1_ = """{"token_type":"bearer","access_token:"brandNewToken","refresh_token":"refreshToken2","expires_on":31557600}"""
        badJson2_ = """{"token_type":"other","access_token":"brandNewToken","refresh_token":"refreshToken2","expires_on":31557600}"""
        underTest_ = new UnderTest()
    }

    void cleanup() {
        KeycloakTokenAuthentication.parser_ = savedParser_
    }

    def "Test getProcess"() {
        KeycloakTokenAuthentication auth = new KeycloakTokenAuthentication()
        Process process = auth.getProcess(new ArrayList<String>() {{ add("/bin/ls") }})
        expect: process != null
    }

    def "Test now"() {
        KeycloakTokenAuthentication auth = new KeycloakTokenAuthentication()
        long now = auth.now()
        expect: now > 0L
    }

    def "Test reset"() {
        KeycloakTokenAuthentication auth = new KeycloakTokenAuthentication()
        auth.expiresOn_ = 100L
        auth.reset()
        expect: auth.expiresOn_ == 0L
    }

    def "Test initialize negative 1"() {
        KeycloakTokenAuthentication ut = new KeycloakTokenAuthentication()
        when: ut.initialize(LOG, ARGS)
        then: thrown(EXCEPT)
        where:
        LOG     | ARGS       || EXCEPT
        null    | arguments_ || IllegalArgumentException.class
        logger_ | null       || IllegalArgumentException.class
    }

    def "Test initialize negative 2"() {
        KeycloakTokenAuthentication ut = new KeycloakTokenAuthentication()
        ut.doCheck_ = true
        arguments_."curl" = CURL
        if(SKIP_REALM)
            arguments_.remove("realm")
        when: ut.initialize(LOG, arguments_)
        then: thrown(EXCEPT)
        where:
        LOG     | CURL                  | SKIP_REALM || EXCEPT
        logger_ | "/usr/bin/cUrl"       | false      || TokenAuthenticationException.class
        logger_ | "/opt/bin/curl"       | false      || TokenAuthenticationException.class
        logger_ | "/tmp/curl"           | false      || TokenAuthenticationException.class
        logger_ | "/home/user/bin/curl" | false      || TokenAuthenticationException.class
        logger_ | "/usr/local/bin/curl" | false      || TokenAuthenticationException.class
        logger_ | "/usr/bin/curl"       | true       || TokenAuthenticationException.class
    }

    def "Test ctor negative"() {
        KeycloakTokenAuthentication.parser_ = null
        when: new KeycloakTokenAuthentication()
        then: thrown(NullPointerException)
    }

    def "Test initialize 1"() {
        KeycloakTokenAuthentication ut = new KeycloakTokenAuthentication()
        ut.doCheck_ = false
        ut.initialize(logger_, arguments_)
        expect: ut.username_ == null
        and:    ut.password_ == null
        and:    ut.realm_ == "realm"
        and:    ut.clientId_ == "testClientId"
        and:    ut.clientSecret_ == "some_secret"
        and:    ut.serverUrl_ == "https://localhost:8080/auth/realm"
        and:    !ut.userPass_
    }

    def "Test initialize 2"() {
        arguments_."clientSecret" = null
        arguments_."username" = "user"
        arguments_."password" = "pass"
        KeycloakTokenAuthentication ut = new KeycloakTokenAuthentication()
        ut.doCheck_ = false
        ut.initialize(logger_, arguments_)
        expect: ut.username_ == "user"
        and:    ut.password_ == "pass"
        and:    ut.realm_ == "realm"
        and:    ut.clientId_ == "testClientId"
        and:    ut.clientSecret_ == null
        and:    ut.serverUrl_ == "https://localhost:8080/auth/realm"
        and:    ut.userPass_
    }

    def "Test getToken"() {
        if(retVal == "brandNewToken") {
            arguments_."username" = "user"
            arguments_."password" = "pass"
        }
        underTest_.initialize(logger_, arguments_)
        process_.exitValue() >> 0
        process_.getInputStream() >> new ByteArrayInputStream(((String)json).getBytes(StandardCharsets.UTF_8))
        now_ = time
        underTest_.token_ = token
        underTest_.expiresOn_ = expire
        underTest_.refreshOn_ = refresh
        expect: underTest_.getToken() == retVal
        where:
        time | refresh | expire | token      | json   || retVal
        1L   | 0L      | 0L     | null       | json1_ || "newToken"
        31L  | 60L     | 120L   | "newToken" | json1_ || "newToken"
        91L  | 60L     | 120L   | "newToken" | json1_ || "newToken"
        121L | 60L     | 120L   | "newToken" | json2_ || "brandNewToken"
    }

    def "Test getToken negative"() {
        underTest_.initialize(logger_, arguments_)
        process_.exitValue() >> EXIT
        process_.getInputStream() >> new ByteArrayInputStream(((String)JSON).getBytes(StandardCharsets.UTF_8))
        when: underTest_.getToken()
        then: thrown(EXCEPT)
        where:
        EXIT | JSON      || EXCEPT
        1    | badJson1_ || TokenAuthenticationException.class
        0    | badJson1_ || TokenAuthenticationException.class
        0    | badJson2_ || TokenAuthenticationException.class
    }

    def "Test getToken negative 2"() {
        underTest_.initialize(logger_, arguments_)
        process_.exitValue() >> 0
        process_.getInputStream() >> { throw new IOException("TEST") }
        when: underTest_.getToken()
        then: thrown(TokenAuthenticationException)
        where:
        EXIT | JSON      || EXCEPT
        1    | badJson1_ || TokenAuthenticationException.class
        0    | badJson1_ || TokenAuthenticationException.class
        0    | badJson2_ || TokenAuthenticationException.class
    }

    class UnderTest extends KeycloakTokenAuthentication {
        @Override void initialize(Logger logger, Map<String, String> arguments) throws TokenAuthenticationException {
            super.doCheck_ = false
            super.initialize(logger, arguments)
        }
        @Override Process getProcess(List<String> command) { return process_ }
        @Override long now() { return now_ }
    }
}
