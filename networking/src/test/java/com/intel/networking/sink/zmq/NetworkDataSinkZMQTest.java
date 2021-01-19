package com.intel.networking.sink.zmq;

//*** THESE TEST FREQUENTLY CAUSE OutOfMemory Exceptions!!! ***//
//import com.intel.logging.Logger;
//import com.intel.logging.LoggerFactory;
//import com.intel.networking.sink.NetworkDataSinkDelegate;
//import com.intel.networking.sink.rabbitmq.NetworkDataSinkRabbitMQ;
//import com.intel.networking.sink.rabbitmq.NetworkDataSinkRabbitMQTest;
//import com.rabbitmq.client.AMQP;
//import com.rabbitmq.client.Channel;
//import com.rabbitmq.client.Connection;
//import com.rabbitmq.client.ConnectionFactory;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.BeforeClass;
//import org.junit.Test;
//import org.zeromq.ZSocket;
//
//import java.io.IOException;
//import java.util.*;
//
//import static org.junit.Assert.*;
//import static org.junit.Assert.fail;
//import static org.mockito.ArgumentMatchers.anyString;
//import static org.mockito.Mockito.mock;
//import static org.mockito.Mockito.when;
//
//public class NetworkDataSinkZMQTest implements NetworkDataSinkDelegate {
//    private NetworkDataSinkZMQTest.MockNetworkDataSinkZMQ instance_;
//    private ZSocket subscriber_;
//    private HashSet<String> subjects_ = new HashSet<>();
//
//    private static class MockNetworkDataSinkZMQ extends NetworkDataSinkZMQ {
//        public MockNetworkDataSinkZMQ(Map<String, String> args) {
//            super(mock(Logger.class), args);
//        }
//
//        @Override
//        ZSocket createSocketZMQ(){
//            ZSocket socket =  mock(ZSocket.class);
//            when(socket.connect(anyString())).thenReturn(true);
//            return socket;
//        }
//
//    }
//
//    @Override
//    public void processIncomingData(String subject, String payload) {
//    }
//
//    @Before
//    public void setUp() throws Exception {
//        Map<String,String> sinkArgs = new HashMap<>();
//        sinkArgs.put("uri", "tcp://127.0.0.1:5401");
//        sinkArgs.put("subjects", "env,evt,avg");
//        subscriber_ = mock(ZSocket.class);
//        String subject_ = "env,evt,avg";
//        String connectionString_ = "tcp://127.0.0.1:5401";
//        for (String topic: subject_.split(",")) {
//            subscriber_.subscribe(topic);
//        }
//        for (String connection: connectionString_.split(",")) {
//            subscriber_.connect(connection);
//        }
//        instance_ = new MockNetworkDataSinkZMQ(sinkArgs);
//    }
//
//    @After
//    public void tearDown() {
//        instance_.stopListening();
//        instance_ = null;
//    }
//
//    @Test
//    public void ctor() {
//        Map<String,String> sinkArgs = new HashMap<>();
//        sinkArgs.put("uri", "tcp://127.0.0.1:5401");
//        sinkArgs.put("subjects", "env,evt,avg");
//        MockNetworkDataSinkZMQ sink = new MockNetworkDataSinkZMQ(sinkArgs);
//        sink.setCallbackDelegate(this);
//        sink.setConnectionInfo("tcp://127.0.0.1:5401");
//        sink.error(new RuntimeException("Some exception!"));
//        sink.setLogger(mock(Logger.class));
//        assertFalse(sink.isListening());
//        sink.startListening();
//        sink.startListening();
//        assertTrue(sink.isListening());
//        sink.error(new RuntimeException("Some exception!"));
//        sink.stopListening();
//        sink.setMonitoringSubject("evt");
//        String[] subjects = "env,avg,evt".split(",");
//        Collections.addAll(subjects_, subjects);
//        sink.setMonitoringSubjects(subjects_);
//        sink.clearSubjects();
//        sink.setLogger(null);
//        sink.error(new RuntimeException("Some exception!"));
//        assertEquals("zmq", sink.getProviderName());
//    }
//
//    @Test
//    public void ctorNegative() {
//        NetworkDataSinkZMQTest.MockNetworkDataSinkZMQ sink;
//        try {
//            Map<String,String> sinkArgs = null;
//            sink = new NetworkDataSinkZMQTest.MockNetworkDataSinkZMQ(sinkArgs);
//            fail("Zero length test");
//        } catch(IllegalArgumentException e) {
//            // Pass...
//        }
//        try {
//            Map<String,String> sinkArgs = new HashMap<>();
//            sink = new NetworkDataSinkZMQTest.MockNetworkDataSinkZMQ(sinkArgs);
//            fail("Zero length test");
//        } catch(IllegalArgumentException e) {
//            // Pass...
//        }
//        try {
//            Map<String,String> sinkArgs = new HashMap<>();
//            sinkArgs.put("uri", null);
//            sink = new NetworkDataSinkZMQTest.MockNetworkDataSinkZMQ(sinkArgs);
//            fail("null first value test");
//        } catch(IllegalArgumentException e) {
//            // Pass...
//        }
//        try {
//            Map<String,String> sinkArgs = new HashMap<>();
//            sinkArgs.put("uri", "  \n \t ");
//            sink = new NetworkDataSinkZMQTest.MockNetworkDataSinkZMQ(sinkArgs);
//            fail("empty string first value test");
//        } catch(IllegalArgumentException e) {
//            // Pass...
//        }
//        try {
//            Map<String,String> sinkArgs = new HashMap<>();
//            sinkArgs.put("uri", "amqp://127.0.0.1.:5401");
//            sink = new NetworkDataSinkZMQTest.MockNetworkDataSinkZMQ(sinkArgs);
//            fail("missing subjects key");
//        } catch(IllegalArgumentException e) {
//            // Pass...
//        }
//    }
//}
