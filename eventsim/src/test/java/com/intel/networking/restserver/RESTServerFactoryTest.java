package com.intel.networking.restserver;

import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class RESTServerFactoryTest {
    static class MockRESTServerImpl extends RESTServer {
        public MockRESTServerImpl(Logger log) throws RESTServerException { super(log); }
        @Override public void ssePublish(String subject, String data, String id) { }
        @Override protected void startServer() throws RESTServerException { }
        @Override protected void stopServer() throws RESTServerException { }
        @Override protected void addInternalRouteUrl(RouteObject route) throws RESTServerException { }
        @Override protected void addInternalRouteMethod(RouteObject route) throws RESTServerException { }
        @Override protected void removeInternalRouteUrl(RouteObject route) throws RESTServerException { }
        @Override protected void removeInternalRouteMethod(RouteObject route) throws RESTServerException { }
    }
    static class FailedRESTServerImpl extends RESTServer {
        public FailedRESTServerImpl(Logger log) throws RESTServerException {
            super(log); throw new RESTServerException();
        }
        @Override public void ssePublish(String subject, String data, String id) { }
        @Override protected void startServer() throws RESTServerException { }
        @Override protected void stopServer() throws RESTServerException { }
        @Override protected void addInternalRouteUrl(RouteObject route) throws RESTServerException { }
        @Override protected void addInternalRouteMethod(RouteObject route) throws RESTServerException { }
        @Override protected void removeInternalRouteUrl(RouteObject route) throws RESTServerException { }
        @Override protected void removeInternalRouteMethod(RouteObject route) throws RESTServerException { }
    }
    static class BadRESTServerImpl extends RESTServer {
        public BadRESTServerImpl(Logger log, int port) throws RESTServerException
        { super(log); setPort(port); }

        @Override public void ssePublish(String subject, String data, String id) { }
        @Override protected void startServer() throws RESTServerException { }
        @Override protected void stopServer() throws RESTServerException { }
        @Override protected void addInternalRouteUrl(RouteObject route) throws RESTServerException { }
        @Override protected void addInternalRouteMethod(RouteObject route) throws RESTServerException { }
        @Override protected void removeInternalRouteUrl(RouteObject route) throws RESTServerException { }
        @Override protected void removeInternalRouteMethod(RouteObject route) throws RESTServerException { }
    }

    @Before
    public void setUp() throws Exception {
        RESTServerFactory.clearAllImplementations();
    }

    @Test
    public void getInstance() throws RESTServerException {
        RESTServerFactory.addImplementation("mock", MockRESTServerImpl.class);
        assertNotNull(RESTServerFactory.getInstance("mock", mock(Logger.class)));
    }

    @Test
    public void getSingletonInstance() throws RESTServerException {
        RESTServerFactory.addImplementation("mock", MockRESTServerImpl.class);
        RESTServer server = RESTServerFactory.getSingletonInstance("mock", mock(Logger.class));
        assertNotNull(server);
        assertEquals(server, RESTServerFactory.getSingletonInstance("mock", mock(Logger.class)));
    }

    @Test(expected = RESTServerException.class)
    public void getInstanceNegative() throws RESTServerException {
        RESTServerFactory.addImplementation("fail", FailedRESTServerImpl.class);
        RESTServerFactory.getInstance("fail", mock(Logger.class));
    }

    @Test(expected = RESTServerException.class)
    public void addImplementationNegative() throws RESTServerException {
        RESTServerFactory.addImplementation("bad", BadRESTServerImpl.class);
    }

    @Test
    public void removeImplementation() throws RESTServerException {
        RESTServerFactory.addImplementation("mock", MockRESTServerImpl.class);
        assertNotNull(RESTServerFactory.getInstance("mock", mock(Logger.class)));
        RESTServerFactory.removeImplementation("mock");
        assertNull(RESTServerFactory.getInstance("mock", mock(Logger.class)));
    }

    @Test
    public void getImplmentationNames() throws RESTServerException {
        RESTServerFactory.addImplementation("mock", MockRESTServerImpl.class);
        assertEquals(1, RESTServerFactory.getImplementationNames().size());
        assertTrue(RESTServerFactory.getImplementationNames().contains("mock"));
    }
}
