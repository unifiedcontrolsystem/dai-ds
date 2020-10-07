package com.intel.networking.restserver.java11;

import com.intel.logging.Logger;
import com.intel.networking.restserver.RESTServer;
import com.intel.networking.restserver.Request;
import com.intel.networking.restserver.Response;
import com.sun.net.httpserver.HttpExchange;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class SSEConnectionManagerTest {
    private void callback(Request request, Response response) {}

    @BeforeClass
    public static void setUpClass() {
        System.setProperty("SSEConnectionManager.pingInterval", "1");
    }

    @AfterClass
    public static void tearDownClass() {
        System.setProperty("SSEConnectionManager.pingInterval", "90");
    }

    @Before
    public void setUp() throws Exception {
        types_ = new ArrayList<String>() {{
            add("type1");
            add("type2");
            add("type3");
        }};
        server_ = mock(RESTServer.class);
        manager_ = new SSEConnectionManager(server_, mock(Logger.class));
        when(server_.getEventTypesFromPath(anyString())).thenReturn(new ArrayList<String>() {{
            add("type2");
        }});
        exchange_ = mock(HttpExchange.class);
        when(exchange_.getRequestURI()).thenReturn(URI.create("http://localhost:12345/path"));
        stream_ = mock(OutputStream.class);
        when(exchange_.getResponseBody()).thenReturn(stream_);
        when(exchange_.getRemoteAddress()).thenReturn(remote_);
    }

    @Test
    public void close() throws Exception {
        manager_.close();
    }

    @Test
    public void addConnection() throws Exception {
        manager_.addConnection(exchange_, null);
        manager_.addConnection(exchange_, null);
        Thread.sleep(1500);
        manager_.close();
    }

    @Test
    public void publishToConnections() {
        manager_.addConnection(exchange_, types_);
        manager_.publishToConnections("type1", "Some Data", null);
        when(server_.getEventTypesFromPath(anyString())).thenReturn(null);
        manager_.publishToConnections("type2", "Some Data", null);
        manager_.publishToConnections("type2", "Some Data", "1");
        manager_.publishToConnections("type2", "Some Data", "   ");
        manager_.publishToConnections("type9", "Some Data", null);
    }

    @Test
    public void publishToConnectionsBroken() throws Exception {
        manager_.addConnection(exchange_, types_);
        doThrow(IOException.class).when(stream_).write(any(byte[].class));
        manager_.publishToConnections("type1", "Some Data", null);
        Thread.sleep(1500);
        manager_.publishToConnections("type2", "Some Data", null);
        manager_.publishToConnections("type9", "Some Data", null);
    }

    @Test
    public void publishToConnectionsPingBroken() throws Exception {
        manager_.addConnection(exchange_, types_);
        doThrow(IOException.class).when(stream_).write(any(byte[].class));
        Thread.sleep(1500);
    }

    @Test
    public void sseConnectionObject() {
        new SSEConnectionManager.SSEConnection(mock(HttpExchange.class), null);
    }

    @Test
    public void checkForWantedEventTypes() {
        assertTrue(manager_.checkForWantedEventTypes("anything", null));
        assertTrue(manager_.checkForWantedEventTypes("anything", new ArrayList<String>()));
        assertTrue(manager_.checkForWantedEventTypes("anything", new ArrayList<String>() {{ add("anything"); }}));
        assertFalse(manager_.checkForWantedEventTypes("anything", new ArrayList<String>() {{ add("nothing"); }}));
    }

    @Test
    public void checkForPossibleEventTypes() {
        when(server_.getEventTypesFromPath(anyString())).thenReturn(null);
        assertTrue(manager_.checkForPossibleEventTypes("anything", "/path"));
        when(server_.getEventTypesFromPath(anyString())).thenReturn(new ArrayList<String>());
        assertTrue(manager_.checkForPossibleEventTypes("anything", "/path"));
        when(server_.getEventTypesFromPath(anyString())).thenReturn(new ArrayList<String>(){{ add("anything"); }});
        assertTrue(manager_.checkForPossibleEventTypes("anything", "/path"));
        assertFalse(manager_.checkForPossibleEventTypes("nothing", "/path"));
    }

    private SSEConnectionManager manager_;
    private RESTServer server_;
    private HttpExchange exchange_;
    private Collection<String> types_;
    private OutputStream stream_;
    private InetSocketAddress remote_ = new InetSocketAddress("localhost", 34562);
}
