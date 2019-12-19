package com.intel.networking.sink.http_callback

import com.intel.authentication.TokenAuthentication
import com.intel.authentication.TokenAuthenticationException
import com.intel.config_io.ConfigIO
import com.intel.config_io.ConfigIOFactory
import com.intel.logging.Logger
import com.intel.networking.HttpMethod
import com.intel.networking.NetworkException
import com.intel.networking.restclient.BlockingResult
import com.intel.networking.restclient.RESTClient
import com.intel.networking.restclient.RESTClientFactory
import com.intel.networking.restclient.RequestInfo
import com.intel.networking.restclient.ResponseCallback
import com.intel.networking.restclient.SSEEvent
import com.intel.networking.restserver.RESTServer
import com.intel.networking.restserver.RESTServerFactory
import com.intel.networking.restserver.Request
import com.intel.networking.restserver.Response
import com.intel.networking.sink.NetworkDataSinkDelegate
import com.intel.properties.PropertyMap
import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean

class NetworkDataSinkHttpCallbackSpec extends Specification implements NetworkDataSinkDelegate {
    def logger_ = Mock(Logger)
    def args_ = [
            connectAddress    : "127.0.0.1",
            connectPort       : "12345",
            bindAddress       : "127.0.0.1",
            bindPort          : "54321",
            urlPath           : "/apis/subs",
            subjects          : "stateChange",
            requestBuilder    : TestSubscriptionRequestBuilder.class.getCanonicalName(),
            responseParser    : TestSubscriptionResponseParser.class.getCanonicalName(),
            subscriberName    : "test_id",
            requestType       : "POST",
            deleteType        : "DELETE",
            connectTimeout    : "600",
            "use-ssl"         : "true",
            tokenAuthProvider : TestTokenAuthentication.class.getCanonicalName(),
            tokenServer       : "",
            realm             : "",
            clientId          : "",
            clientSecret      : "",
            username          : "",
            password          : ""
    ]
    def underTest_
    void setup() {
        NetworkDataSinkHttpCallback.HTTP_IMPLEMENTATION = "test";
        RESTServer server = Mock(RESTServer)
        server.running_ = new AtomicBoolean(false)
        server.routes_ = new TreeMap()
        server.log_ = logger_
        RESTServerFactory.singletons_.put("test", server)
        RESTClientFactory.addImplementation("test", TestRESTClient.class)
        RESTClient client = Mock(RESTClient)
        client.log_ = logger_
        underTest_ = new NetworkDataSinkHttpCallback(logger_, args_)
        underTest_.initialize()
    }

    void cleanup() {
        NetworkDataSinkHttpCallback.HTTP_IMPLEMENTATION = "jdk11";
        RESTServerFactory.singletons_.remove("test")
        RESTClientFactory.removeImplementation("test")
        RESTServerFactory.singletons_.remove("test")
    }

    def "Test Bas ctor input #1"() {
        when: new NetworkDataSinkHttpCallback(logger_, [:])
        then:  thrown(IllegalArgumentException)
    }

    def "Test initialize with 'connectTimeout'"() {
        expect: underTest_.connectionTimeoutSeconds_ == 600
    }

    def "Test initialize with no 'connectTimeout'"() {
        args_.put("use-ssl", "false")
        args_.put("urlPath", args_.get("urlPath").substring(1))
        args_.remove("connectTimeout")
        def ut = new NetworkDataSinkHttpCallback(logger_, args_)
        ut.initialize()
        expect: ut.connectionTimeoutSeconds_ == 300
    }

    def "Test initialize with no 'subjects'"() {
        args_.put("subjects", null)
        def ut = new NetworkDataSinkHttpCallback(logger_, args_)
        ut.initialize()
        expect: ut.subject_ == null
    }

    def "Test initialize with bad 'requestBuilder'"() {
        given:
        args_.put("requestBuilder", "com.intel.missing.RequestBuilder")
        def ut = new NetworkDataSinkHttpCallback(logger_, args_)

        when:
        ut.initialize()

        then: thrown(NetworkException)
    }

    def "Test initialize with bad 'responseParser'"() {
        given:
        args_.put("responseParser", "com.intel.missing.ResponseParser")
        def ut = new NetworkDataSinkHttpCallback(logger_, args_)

        when:
        ut.initialize()

        then: thrown(NetworkException)
    }

    def "Test initialize with bad 'tokenProvider'"() {
        given:
        args_.put("tokenAuthProvider", "com.intel.missing.TokenProvider")
        def ut = new NetworkDataSinkHttpCallback(logger_, args_)

        when:
        ut.initialize()

        then: thrown(NetworkException)
    }

    def "Test initialize with missing 'tokenProvider'"() {
        args_.remove("tokenAuthProvider")
        def ut = new NetworkDataSinkHttpCallback(logger_, args_)
        ut.initialize()
        expect: ut.tokenProvider_ == null
    }

    def "Test a Bad Token Provider #1"() {
        given:
        args_.put("tokenAuthProvider", BadTokenProvider1.class.getCanonicalName())
        def ut = new NetworkDataSinkHttpCallback(logger_, args_)
        when: ut.initialize()
        then: thrown(NetworkException)
    }

    def "Test a Bad Token Provider #2"() {
        given:
        args_.put("tokenAuthProvider", BadTokenProvider2.class.getCanonicalName())
        def ut = new NetworkDataSinkHttpCallback(logger_, args_)
        when: ut.initialize()
        then: thrown(NetworkException)
    }

    def "Test a Bad Token Provider #3"() {
        given:
        args_.put("tokenAuthProvider", BadTokenProvider3.class.getCanonicalName())
        def ut = new NetworkDataSinkHttpCallback(logger_, args_)
        when: ut.initialize()
        then: thrown(NetworkException)
    }

    def "Test a Bad Token Provider #4"() {
        given:
        args_.put("tokenAuthProvider", BadTokenProvider4.class.getCanonicalName())
        def ut = new NetworkDataSinkHttpCallback(logger_, args_)
        when: ut.initialize()
        then: thrown(NetworkException)
    }

    def "Test ClearSubjects"() {
        underTest_.clearSubjects()
        expect: underTest_.subject_ == null
    }

    def "Test SetMonitoringSubject"() {
        underTest_.clearSubjects()
        underTest_.setMonitoringSubject("stateChange")
        expect: underTest_.subject_ == "stateChange"
    }

    def "Test SetMonitoringSubjects"() {
        given:
        underTest_.clearSubjects()
        when: underTest_.setMonitoringSubjects(["stateChange"])
        then: thrown(NetworkException)
    }

    def "Test SetConnectionInfo"() {
        when: underTest_.setConnectionInfo("string")
        then: thrown(UnsupportedOperationException)
    }

    @Override
    void processIncomingData(String subject, String message) {}

    def "Test SetCallbackDelegate"() {
        underTest_.setCallbackDelegate(this)
        underTest_.setCallbackDelegate(null)
        expect: underTest_.incomingMessageCallback_ == this
    }

    def "Test SetLogger"() {
        underTest_.setLogger(logger_)
        underTest_.setLogger(null)
        expect: underTest_.log_ == logger_
    }

    def "Test GetProviderName"() {
        expect: underTest_.getProviderName() == "http_callback"
    }

    def "Test logOnly"() {
        underTest_.logOnly("", "")
        expect: true
    }

    def "Test StartListening"() {
        TestRESTClient.codes_.add(500)
        underTest_.startListening()
        underTest_.startListening()
        expect: underTest_.isListening()
    }

    def "Test StartListening Negative"() {
        given:
        TestRESTClient.codes_.add(500)
        TestRESTClient.codes_.add(500)
        TestRESTClient.codes_.add(500)
        when:
        underTest_.startListening()
        then: thrown(NetworkException)
    }

    def "Test StopListening"() {
        TestRESTClient.codes_.add(200)
        TestRESTClient.codes_.add(500)
        underTest_.startListening()
        underTest_.stopListening()
        underTest_.stopListening()
        expect: !underTest_.isListening()
    }

    def "Test ProcessHttpMessage"() {
        Request request = Mock(Request)
        Response response = Mock(Response)
        underTest_.processHttpMessage(request, response);
        expect: true
    }
}

class TestSubscriptionRequestBuilder implements SubscriptionRequestBuilder {
    TestSubscriptionRequestBuilder() {}

    @Override
    String buildRequest(Collection<String> subjects, String subscriberID, String callbackUrl) {
        return "{}";
    }
}

class TestSubscriptionResponseParser implements SubscriptionResponseParser {
    TestSubscriptionResponseParser() {}

    @Override
    URI parseResponse(String message, String subscriptionUri) {
        return URI.create(subscriptionUri + "/42");
    }
}

class TestTokenAuthentication implements TokenAuthentication {
    TestTokenAuthentication() {}

    @Override
    void initialize(Logger logger, Map<String, String> arguments) throws TokenAuthenticationException {
    }

    @Override
    String getToken() throws TokenAuthenticationException {
        return "token";
    }
}

class BadTokenProvider1 implements TokenAuthentication {
    BadTokenProvider1(String arg) {}

    @Override
    void initialize(Logger logger, Map<String, String> arguments) throws TokenAuthenticationException { }

    @Override
    String getToken() throws TokenAuthenticationException { return null }
}

class BadTokenProvider2 implements TokenAuthentication {
    private BadTokenProvider2() {}

    @Override
    void initialize(Logger logger, Map<String, String> arguments) throws TokenAuthenticationException { }

    @Override
    String getToken() throws TokenAuthenticationException { return null }
}

class BadTokenProvider3 implements TokenAuthentication {
    BadTokenProvider3() { throw new InstantiationException() }

    @Override
    void initialize(Logger logger, Map<String, String> arguments) throws TokenAuthenticationException { }

    @Override
    String getToken() throws TokenAuthenticationException { return null }
}

class BadTokenProvider4 implements TokenAuthentication {
    BadTokenProvider4() { }

    @Override
    void initialize(Logger logger, Map<String, String> arguments) throws TokenAuthenticationException {
        throw new TokenAuthenticationException("TEST")
    }

    @Override
    String getToken() throws TokenAuthenticationException { return null }
}

class TestRESTClient extends RESTClient {
    @Override protected void doSSERequest(RequestInfo request, ResponseCallback callback, SSEEvent eventsCallback) {}

    @Override protected BlockingResult doRESTRequest(RequestInfo request) {
        if(request.method() == HttpMethod.POST) {
            ConfigIO parser = ConfigIOFactory.getInstance("json")
            PropertyMap map = parser.fromString(request.body()).getAsMap()
            map.put("ID", "42");
            return new BlockingResult(getCode(), parser.toString(map), request)
        } else {
            return new BlockingResult(getCode(), "", request)
        }
    }

    private int getCode() {
        if(codes_.size() > 0)
            return codes_.poll()
        else
            return RETURN_CODE
    }

    TestRESTClient(Logger log) { super(log) }

    static int RETURN_CODE = 200
    static Queue<Integer> codes_ = new LinkedList<>()
}
