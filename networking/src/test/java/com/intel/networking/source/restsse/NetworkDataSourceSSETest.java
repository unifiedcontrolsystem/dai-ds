package com.intel.networking.source.restsse;

import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.networking.restserver.RESTServer;
import com.intel.networking.restserver.RESTServerException;
import com.intel.networking.restserver.RESTServerFactory;
import com.intel.networking.restserver.RouteObject;
import com.intel.networking.source.NetworkDataSource;
import com.intel.networking.source.NetworkDataSourceFactory;
import org.junit.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class NetworkDataSourceSSETest {
    public static class MockRESTServer extends RESTServer {
        public MockRESTServer(Logger log) throws RESTServerException {
            super(log);
        }

        @Override
        public void ssePublish(String subject, String data, String id) throws RESTServerException {
            if(publishException_) throw new RESTServerException("TEST");
            payload_ = data;
            id_ = id;
        }

        @Override
        protected void startServer() throws RESTServerException {
        }

        @Override
        protected void stopServer() throws RESTServerException {
            if(stopException_) throw new RESTServerException("TEST");
        }

        @Override
        protected void addInternalRouteUrl(RouteObject route) throws RESTServerException {
        }

        @Override
        protected void addInternalRouteMethod(RouteObject route) throws RESTServerException {
        }

        @Override
        protected void removeInternalRouteUrl(RouteObject route) throws RESTServerException {
        }

        @Override
        protected void removeInternalRouteMethod(RouteObject route) throws RESTServerException {
        }

        static boolean stopException_ = false;
        static String payload_ = null;
        static String id_ = null;
        static boolean publishException_ = false;
        static boolean startException_ = false;
    }

    @BeforeClass
    public static void setUpClass() throws RESTServerException, NetworkDataSourceFactory.FactoryException {
        LoggerFactory.getInstance("TEST", "TEST", "console");
        RESTServerFactory.addImplementation("test", MockRESTServer.class);
        source_ = NetworkDataSourceFactory.createInstance(mock(Logger.class), "sse", args_);
    }

    @AfterClass
    public static void tearDownClass() throws IOException {
        source_.close();
        RESTServerFactory.removeImplementation("test");
    }

    @Before
    public void setUp() {
        MockRESTServer.stopException_ = false;
        MockRESTServer.payload_ = null;
        MockRESTServer.id_ = null;
        MockRESTServer.publishException_ = false;
        MockRESTServer.startException_ = false;
    }

    @Test(expected = RuntimeException.class)
    public void ctorNegative() throws Exception {
        MockRESTServer.startException_ = true;
        new NetworkDataSourceSSE(mock(Logger.class), args_);
    }

    @Test
    public void connect() {
        source_.connect(null);
    }

    @Test
    public void setLogger() {
        source_.setLogger(mock(Logger.class));
        source_.setLogger(null);
    }

    @Test
    public void getProviderName() {
        assertEquals("sse", source_.getProviderName());
    }

    @Test
    public void sendMessage() {
        source_.sendMessage("subject", "{}");
        assertEquals("{}", MockRESTServer.payload_);
        source_.sendMessage("subject", "{\"sse_id\":\"id_number\"}");
        assertEquals("{\"sse_id\":\"id_number\"}", MockRESTServer.payload_);
        assertEquals("id_number", MockRESTServer.id_);
        source_.sendMessage("subject", "TEXT");
        source_.sendMessage("subject", "[0,1,2,3]");
    }

    @Test
    public void sendMessageNegative() {
        MockRESTServer.publishException_ = true;
        source_.sendMessage("subject", "{}");
    }

    private static Map<String,String> args_ = new HashMap<String,String>() {{
        put("implementation", "test");
    }};
    private static NetworkDataSource source_;
}
