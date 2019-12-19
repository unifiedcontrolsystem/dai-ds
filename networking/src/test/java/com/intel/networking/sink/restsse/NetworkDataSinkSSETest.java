package com.intel.networking.sink.restsse;

import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.NetworkException;
import com.intel.networking.restclient.*;
import com.intel.networking.sink.NetworkDataSink;
import com.intel.networking.sink.NetworkDataSinkFactory;
import org.junit.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class NetworkDataSinkSSETest {
    public static class PositiveBuilder implements SSERequestBuilder {
        public PositiveBuilder() {}

        @Override
        public String buildRequest(Collection<String> eventTypes, Map<String, String> builderSpecific) {
            return "{}";
        }
    }

    public static class MissingCtor implements SSERequestBuilder {
        public MissingCtor(int size) {}

        @Override
        public String buildRequest(Collection<String> eventTypes, Map<String, String> builderSpecific) {
            return "{}";
        }
    }

    public static class BadCtor implements SSERequestBuilder {
        private BadCtor() {}

        @Override
        public String buildRequest(Collection<String> eventTypes, Map<String, String> builderSpecific) {
            return "{}";
        }
    }

    public static class MockRESTClient extends RESTClient {
        public MockRESTClient(Logger log) {
            super(log);
        }

        @Override
        protected void doSSERequest(RequestInfo request, ResponseCallback callback, SSEEvent eventsCallback) {
            callback.responseCallback(200, "{}", request);
            callback_ = eventsCallback;
        }

        @Override
        protected BlockingResult doRESTRequest(RequestInfo request) {
            return new BlockingResult(200, "{}", request);
        }

        static void fakePublish(String subject, String payload, String id) {
            callback_.event(subject, payload, id);
        }

        static SSEEvent callback_;
    }

    private void sink(String subject, String payload) {
        subject_ = subject;
        payload_ = payload;
    }

    @BeforeClass
    public static void setUpClass() throws RESTClientException {
        LoggerFactory.getInstance("TEST", "TEST", "console");
        RESTClientFactory.addImplementation("test", MockRESTClient.class);
    }

    @AfterClass
    public static void tearDownClass() {
        RESTClientFactory.removeImplementation("test");
    }

    @Before
    public void setUp() throws Exception {
        client_ = NetworkDataSinkFactory.createInstance(mock(Logger.class), "sse", args_);
        assertNotNull(client_);
        client_.startListening();
    }

    @After
    public void tearDown() throws Exception {
        client_.stopListening();
        client_ = null;
    }

    @Test
    public void clearSubjects() {
        client_.clearSubjects();
    }

    @Test
    public void setMonitoringSubject() {
        client_.setMonitoringSubject(null);
        client_.setMonitoringSubject("   ");
        client_.setMonitoringSubject("subject1");
    }

    @Test
    public void setMonitoringSubjects() {
        client_.setMonitoringSubjects(null);
        client_.setMonitoringSubjects(new ArrayList<>() {{ add("subject2"); }});
    }

    @Test
    public void setConnectionInfo() {
        client_.setConnectionInfo(null);
        client_.setConnectionInfo("http://127.0.0.1:8080/myevents");
    }

    @Test
    public void setCallbackDelegate() {
        client_.setCallbackDelegate(null);
    }

    @Test
    public void isListening() {
        assertTrue(client_.isListening());
    }

    @Test
    public void setLogger() {
        client_.setLogger(null);
        client_.setLogger(mock(Logger.class));
    }

    @Test
    public void getProviderName() {
        assertEquals("sse", client_.getProviderName());
    }

    @Test
    public void eventing() {
        MockRESTClient.fakePublish("mySubject", "{}", "some_id");
        client_.setCallbackDelegate(this::sink);
        MockRESTClient.fakePublish("mySubject", "{}", "some_id");
        assertEquals("mySubject", subject_);
        assertEquals("{\"sse_id\":\"some_id\"}", payload_);
        MockRESTClient.fakePublish("mySubject", "{}", null);
        assertEquals("{}", payload_);
        MockRESTClient.fakePublish("mySubject", "TEST", "real_id");
        assertEquals("TEST", payload_);
        MockRESTClient.fakePublish("mySubject", "[0,1,2]", "real_id");
        assertEquals("[0,1,2]", payload_);
        client_.stopListening();
        assertFalse(client_.isListening());
        MockRESTClient.fakePublish("mySubject", "[0,1,2]", null);
    }

    @Test
    public void nullSubjects() throws Exception {
        Map<String,String> args = new HashMap<>() {{
            put("implementation", "test");
            put("subjects", null);
        }};
        NetworkDataSink local = NetworkDataSinkFactory.createInstance(mock(Logger.class), "sse", args);
    }

    @Test
    public void startListening() throws Exception {
        NetworkDataSink local = NetworkDataSinkFactory.createInstance(mock(Logger.class), "sse", args_);
        assertNotNull(local);
        local.startListening();
        local.startListening();
        local.stopListening();
    }

    @Test
    public void missingRESTClientImplementation() throws Exception {
        Map<String,String> args = new HashMap<>() {{
            put("implementation", "test2");
        }};
        NetworkDataSink local = NetworkDataSinkFactory.createInstance(mock(Logger.class), "sse", args);
        assertNotNull(local);
        local.startListening();
        assertFalse(local.isListening());
    }

    @Test
    public void createBuilder() throws Exception {
        Map<String,String> args = new HashMap<>() {{
            put("requestBuilder", "com.intel.networking.sink.restsse.NetworkDataSinkSSETest$PositiveBuilder");
        }};
        NetworkDataSink local = NetworkDataSinkFactory.createInstance(mock(Logger.class), "sse", args);
        assertNotNull(local);
    }

    @Test(expected = NetworkException.class)
    public void createBuilderMissingClass() throws Exception {
        Map<String,String> args = new HashMap<>() {{
            put("requestBuilder", "com.intel.networking.sink.restsse.NetworkDataSinkSSETest$Unknown");
        }};
        NetworkDataSinkFactory.createInstance(mock(Logger.class), "sse", args);
    }

    @Test(expected = NetworkException.class)
    public void createBuilderMissingConstructor() throws Exception {
        Map<String,String> args = new HashMap<>() {{
            put("requestBuilder", "com.intel.networking.sink.restsse.NetworkDataSinkSSETest$MissingCtor");
        }};
        NetworkDataSinkFactory.createInstance(mock(Logger.class), "sse", args);
    }

    @Test(expected = NetworkException.class)
    public void createBuilderPrivateConstructor() throws Exception {
        Map<String,String> args = new HashMap<>() {{
            put("requestBuilder", "com.intel.networking.sink.restsse.NetworkDataSinkSSETest$BadCtor");
        }};
        NetworkDataSinkFactory.createInstance(mock(Logger.class), "sse", args);
    }

    private NetworkDataSink client_;
    private Map<String,String> args_ = new HashMap<>() {{
        put("implementation", "test");
        put("subjects", "s1,s2,s3");
    }};
    private String subject_;
    private String payload_;
}
