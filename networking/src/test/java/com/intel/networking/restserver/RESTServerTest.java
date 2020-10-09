package com.intel.networking.restserver;

import com.intel.logging.Logger;
import com.intel.networking.HttpMethod;
import com.intel.properties.PropertyDocument;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class RESTServerTest {
    static class MockRESTServerImpl extends RESTServer {

        public MockRESTServerImpl(Logger log) throws RESTServerException {
            super(log);
        }

        @Override
        public void ssePublish(String subject, String data, String id) {
        }

        @Override
        protected void startServer() throws RESTServerException {
            startServerCalled++;
            running_.set(true);
        }

        @Override
        protected void stopServer() throws RESTServerException {
            if(stopServerThrows)
                throw new RESTServerException("TESTING...");
            stopServerCalled++;
            running_.set(false);
        }

        @Override
        protected void addInternalRouteUrl(RouteObject route) throws RESTServerException {
            addUrlCalled++;
        }

        @Override
        protected void addInternalRouteMethod(RouteObject route) throws RESTServerException {
            addMethodCalled++;
        }

        @Override
        protected void removeInternalRouteUrl(RouteObject route) throws RESTServerException {
            removeUrlCalled++;
        }

        @Override
        protected void removeInternalRouteMethod(RouteObject route) throws RESTServerException {
            removeMethodCalled++;
        }

        RouteObject match(String path, HttpMethod method) {
            return matchUrlPathAndMethod(path, method);
        }

        Logger getLogger() { return log_; }

        int startServerCalled = 0;
        int stopServerCalled = 0;
        int addMethodCalled = 0;
        int addUrlCalled = 0;
        int removeMethodCalled = 0;
        int removeUrlCalled = 0;
        boolean stopServerThrows = false;
    }

    static class MockTranslator extends ResponseTranslator {
        @Override
        public PropertyMap makeError(int code, String message, URI uri, String method, Throwable cause) {
            return new PropertyMap();
        }

        @Override
        public PropertyMap makeResponse(PropertyDocument payload) {
            return new PropertyMap();
        }
    }

    static class MockTranslator2 extends RequestTranslator {
        @Override
        public Set<String> getSSESubjects(PropertyMap bodyMap) {
            return new HashSet<>();
        }
    }

    @Before
    public void setUp() throws RESTServerException {
        server_ = new MockRESTServerImpl(mock(Logger.class));
        server_.setPort(1024);
    }

    void callback(Request request, Response response) {}

    @Test
    public void setLogger() {
        assertNotNull(server_.getLogger());
    }

    @Test(expected = RESTServerException.class)
    public void setLoggerNegative() throws RESTServerException {
        server_.setLogger(null);
    }

    @Test
    public void getPort() {
        assertEquals(1024, server_.getPort());
    }

    @Test(expected = RESTServerException.class)
    public void setPortNegative1() throws RESTServerException {
        server_.setPort(80);
    }

    @Test(expected = RESTServerException.class)
    public void setPortNegative2() throws RESTServerException {
        server_.setPort(70000);
    }

    @Test(expected = RESTServerException.class)
    public void setPortNegative3() throws RESTServerException {
        server_.start();
        server_.setPort(1024);
    }

    @Test
    public void getAddress() {
        assertEquals("*", server_.getAddress());
    }

    @Test(expected = RESTServerException.class)
    public void setAddressNegative1() throws RESTServerException {
        server_.setAddress(null);
    }

    @Test(expected = RESTServerException.class)
    public void setAddressNegative2() throws RESTServerException {
        server_.setAddress("  \n\t\r  ");
    }

    @Test(expected = RESTServerException.class)
    public void setAddressNegative3() throws RESTServerException {
        server_.start();
        server_.setAddress("*");
    }

    @Test
    public void start() throws RESTServerException {
        assertEquals(0, server_.startServerCalled);
        server_.start();
        assertEquals(1, server_.startServerCalled);
        server_.start();
        assertEquals(1, server_.startServerCalled);
    }

    @Test
    public void stop() throws Exception {
        assertEquals(0, server_.stopServerCalled);
        server_.stop();
        assertEquals(0, server_.stopServerCalled);
        server_.start();
        server_.stop();
        assertEquals(1, server_.stopServerCalled);
        server_.close();
        assertEquals(1, server_.stopServerCalled);
    }

    @Test
    public void closeNegative() throws Exception {
        server_.start();
        server_.stopServerThrows = true;
        server_.close();
    }

    @Test
    public void isRunning() throws RESTServerException {
        assertFalse(server_.isRunning());
        server_.start();
        assertTrue(server_.isRunning());
        server_.stop();
        assertFalse(server_.isRunning());
    }

    @Test
    public void addHandler1() throws RESTServerException {
        server_.addHandler("/", HttpMethod.GET, this::callback);
    }

    @Test
    public void addHandler2() throws RESTServerException {
        server_.addHandler("/", HttpMethod.GET, this::callback);
        server_.addHandler("/", HttpMethod.POST, this::callback);
    }

    @Test
    public void addHandlerNegative1() throws RESTServerException {
        server_.addHandler("/", HttpMethod.GET, this::callback);
        server_.addHandler("/", HttpMethod.GET, this::callback);
    }

    @Test
    public void addHandlerNegative2() throws RESTServerException {
        server_.addHandler("/", HttpMethod.GET, this::callback);
        server_.addHandler("/*", HttpMethod.GET, this::callback);
    }

    @Test
    public void addSSE1() throws RESTServerException {
        Collection<String> eventTypes = new HashSet<String>() {{
            add("type1");
            add("type2");
        }};
        server_.addSSEHandler("/", eventTypes);
    }

    @Test
    public void addSSE2() throws RESTServerException {
        server_.addHandler("/", HttpMethod.GET, this::callback);
    }

    @Test(expected = RESTServerException.class)
    public void addSSENegative1() throws RESTServerException {
        server_.addSSEHandler("/", null);
        server_.addSSEHandler("/", null);
    }

    @Test(expected = RESTServerException.class)
    public void addSSENegative2() throws RESTServerException {
        server_.addSSEHandler("/*", null);
    }

    @Test
    public void removeHandler() throws RESTServerException {
        server_.addHandler("/", HttpMethod.GET, this::callback);
        server_.addHandler("/", HttpMethod.POST, this::callback);
        assertNotNull(server_.getHandler("/", HttpMethod.GET));
        server_.removeHandler("/", HttpMethod.POST);
        server_.removeHandler("/", HttpMethod.GET);
        assertNull(server_.getHandler("/", HttpMethod.GET));
        assertNull(server_.getHandler("/", HttpMethod.POST));
    }

    @Test(expected = RESTServerException.class)
    public void removeHandlerNegative1() throws RESTServerException {
        server_.removeHandler("/", HttpMethod.GET);
    }

    @Test(expected = RESTServerException.class)
    public void removeHandlerNegative2() throws RESTServerException {
        server_.addHandler("/", HttpMethod.POST, this::callback);
        server_.removeHandler("/", HttpMethod.GET);
    }

    @Test
    public void getHandler() throws RESTServerException {
        server_.addHandler("/", HttpMethod.GET, this::callback);
        assertNotNull(server_.getHandler("/", HttpMethod.GET));
        assertNull(server_.getHandler("/", HttpMethod.POST));
        assertNull(server_.getHandler("/fred", HttpMethod.GET));
    }

    @Test
    public void getEventTypes() throws Exception {
        Collection<String> eventTypes = new HashSet<String>() {{
            add("type1");
            add("type2");
        }};
        server_.addHandler("/", HttpMethod.GET, this::callback);
        Collection<String> result = server_.getEventTypesFromPath("/");
        assertNull(result);
        server_.removeHandler("/", HttpMethod.GET);
        server_.addSSEHandler("/", eventTypes);
        result = server_.getEventTypesFromPath("/");
        assertEquals(eventTypes, result);
        result = server_.getEventTypesFromPath("/api2");
        assertNull(result);
    }

    @Test
    public void setResponseTranslator() {
        server_.setResponseTranslator(new MockTranslator());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setResponseTranslatorNegative() {
        server_.setResponseTranslator(null);
    }

    @Test
    public void setRequestTranslator() {
        server_.setRequestTranslator(new MockTranslator2());
    }

    @Test(expected = IllegalArgumentException.class)
    public void setRequestTranslatorNegative() {
        server_.setRequestTranslator(null);
    }

    @Test
    public void matchUrlPathAndMethod() throws Exception {
        server_.addHandler("/api", HttpMethod.GET, this::callback);
        server_.addHandler("/api/*", HttpMethod.GET, this::callback);
        server_.addHandler("/api/func1", HttpMethod.GET, this::callback);
        server_.addHandler("/api/func1/*", HttpMethod.GET, this::callback);
        server_.addHandler("/api/func1", HttpMethod.POST, this::callback);
        server_.addHandler("/api/func2/sub1", HttpMethod.GET, this::callback);
        server_.addHandler("/api/func2/sub1/*", HttpMethod.GET, this::callback);
        server_.addHandler("/api/func2/sub2", HttpMethod.POST, this::callback);
        RouteObject route = server_.match("/api/method1", HttpMethod.GET);
        assertNotNull(route);
        assertEquals("/api/", route.url);
        route = server_.match("/api/func3", HttpMethod.GET);
        assertNotNull(route);
        assertEquals("/api/", route.url);
        route = server_.match("/api2/func3", HttpMethod.GET);
        assertNull(route);
        route = server_.match("/api/func1", HttpMethod.POST);
        assertNotNull(route);
        assertEquals("/api/func1", route.url);
        route = server_.match("/api/func1/sub0", HttpMethod.POST);
        assertNull(route);
    }

    private MockRESTServerImpl server_;
}
