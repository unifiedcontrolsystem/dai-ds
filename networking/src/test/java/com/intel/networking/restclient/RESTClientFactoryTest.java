package com.intel.networking.restclient;

import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;

import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class RESTClientFactoryTest {
    static class Impl extends RESTClient {
        public Impl(Logger log) { super(log); }
        @Override
        protected void doSSERequest(RequestInfo request, ResponseCallback callback, SSEEvent eventsCallback) {}
        @Override
        protected BlockingResult doRESTRequest(RequestInfo request) {
            return new BlockingResult(-1, null, request);
        }
    }

    static class BadImpl extends RESTClient {
        public BadImpl(Logger log, Object other) { super(log); }
        @Override
        protected void doSSERequest(RequestInfo request, ResponseCallback callback, SSEEvent eventsCallback) {}
        @Override
        protected BlockingResult doRESTRequest(RequestInfo request) {
            return new BlockingResult(-1, null, request);
        }
    }

    @Before
    public void setUp() throws Exception {
        RESTClientFactory.clearAllImplementations();
    }

    @Test(expected = RESTClientException.class)
    public void getInstance() throws Exception {
        assertNull(RESTClientFactory.getInstance("red", mock(Logger.class)));
        RESTClientFactory.implementations_.put("mock1", Impl.class);
        assertNotNull(RESTClientFactory.getInstance("mock1", mock(Logger.class)));
        RESTClientFactory.implementations_.put("red", BadImpl.class);
        RESTClientFactory.getInstance("red", mock(Logger.class));
    }

    @Test(expected = RESTClientException.class)
    public void addImplementation() throws Exception {
        assertNull(RESTClientFactory.addImplementation("mock1", Impl.class));
        assertNotNull(RESTClientFactory.addImplementation("mock1", Impl.class));
        RESTClientFactory.addImplementation("mock2", BadImpl.class);
    }

    @Test
    public void removeImplementation() throws Exception {
        assertNull(RESTClientFactory.addImplementation("mock1", Impl.class));
        assertNotNull(RESTClientFactory.removeImplementation("mock1"));
        assertNull(RESTClientFactory.removeImplementation("mock1"));
    }

    @Test
    public void getImplementationNames() {
        RESTClientFactory.implementations_.put("red", null);
        RESTClientFactory.implementations_.put("green", null);
        RESTClientFactory.implementations_.put("blue", null);
        Collection<String> names = RESTClientFactory.getImplementationNames();
        assertEquals(3, names.size());
        assertTrue(names.contains("red"));
        assertTrue(names.contains("green"));
        assertTrue(names.contains("blue"));
    }
}
