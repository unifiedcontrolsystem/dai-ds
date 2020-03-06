package com.intel.dai.eventsim;

import com.intel.config_io.ConfigIOFactory;
import com.intel.logging.Logger;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.networking.restclient.RESTClientException;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class EventSimAppTest {

    @Test
    public void validateRoutes() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.source_ = mock(NetworkObject.class);
        eventSimApiTest.executeRoutes(eventSimApiTest);
    }

    @Test
    public void startServer() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.source_ = mock(NetworkObject.class);
        eventSimApiTest.startServer();
    }

    @Test
    public void stopServer() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.source_ = mock(NetworkObject.class);
        eventSimApiTest.stopServer();
    }

    @Test
    public void serverStatus() {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.source_ = mock(NetworkObject.class);
        eventSimApiTest.serverStatus();
    }

    @Test
    public void noPriorSubscription_DoSubscription() throws SimulatorException, ResultOutputException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");

        Map<String, String> input_parameters = new HashMap<>();
        input_parameters.put("Subscriber", "test1");
        input_parameters.put("Url", "http://test1.com");
        PropertyMap output = new PropertyMap();
        output.put("Url", "http://test1.com");
        output.put("Subscriber", "test1");
        output.put("ID", "1");

        eventSimApiTest.source_ = mock(NetworkObject.class);
        doNothing().when(eventSimApiTest.source_).register(anyString(), anyString(), anyMap());
        when(eventSimApiTest.source_.getSubscription(anyString(), anyString())).thenReturn(output);
        assertEquals("{\"Subscriber\":\"test1\",\"ID\":\"1\",\"Url\":\"http:\\/\\/test1.com\"}", eventSimApiTest.subscribeStateChangeNotifications(input_parameters));
    }

    @Test(expected = SimulatorException.class)
    public void withPriorSubscription_DoSubscription() throws SimulatorException, ResultOutputException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");

        Map<String, String> input_parameters = new HashMap<>();
        input_parameters.put("Subscriber", "test1");
        input_parameters.put("Url", "http://test1.com");

        eventSimApiTest.source_ = mock(NetworkObject.class);
        doThrow(SimulatorException.class).when(eventSimApiTest.source_).register(anyString(), anyString(), anyMap());
        eventSimApiTest.subscribeStateChangeNotifications(input_parameters);
    }

    @Test
    public void removeAllSubscriptions() {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);

        Map<String, String> input_parameters = new HashMap<>();

        eventSimApiTest.source_ = mock(NetworkObject.class);
        when(eventSimApiTest.source_.unRegisterAll()).thenReturn(true);
        assertEquals("", eventSimApiTest.unsubscribeAllStateChangeNotifications(input_parameters));
    }

    @Test
    public void fetchSubscriptionForGivenId() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");

        Map<String, String> input_parameters = new HashMap<>();
        input_parameters.put("sub_component", "1");

        PropertyMap output = new PropertyMap();
        output.put("Url", "http://test1.com");
        output.put("Subscriber", "test1");
        output.put("ID", "1");

        eventSimApiTest.source_ = mock(NetworkObject.class);
        when(eventSimApiTest.source_.getSubscriptionForId(anyLong())).thenReturn(output);
        assertEquals("{\"Subscriber\":\"test1\",\"ID\":\"1\",\"Url\":\"http:\\/\\/test1.com\"}", eventSimApiTest.getSubscriptionDetailForId(input_parameters));
    }

    @Test(expected = SimulatorException.class)
    public void fetchSubscriptionForInvalidId() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");

        Map<String, String> input_parameters = new HashMap<>();
        input_parameters.put("sub_component", "2");

        PropertyMap output = new PropertyMap();
        output.put("Url", "http://test1.com");
        output.put("Subscriber", "test1");
        output.put("ID", "1");

        eventSimApiTest.source_ = mock(NetworkObject.class);
        when(eventSimApiTest.source_.getSubscriptionForId(1)).thenReturn(output);
        when(eventSimApiTest.source_.getSubscriptionForId(2)).thenReturn(null);
        eventSimApiTest.getSubscriptionDetailForId(input_parameters);
    }

    @Test
    public void removeSubscriptionForId() throws RESTClientException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);

        Map<String, String> input_parameters = new HashMap<>();
        input_parameters.put("sub_component", "1");

        eventSimApiTest.source_ = mock(NetworkObject.class);
        when(eventSimApiTest.source_.unRegisterId(anyLong())).thenReturn(true);
        assertEquals("", eventSimApiTest.unsubscribeStateChangeNotifications(input_parameters));
    }

    @Test
    public void fetchAllSubscriptions() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");

        Map<String, String> input_parameters = new HashMap<>();
        PropertyMap output = new PropertyMap();
        output.put("Url", "http://test1.com");
        output.put("Subscriber", "test1");
        output.put("ID", "1");

        eventSimApiTest.source_ = mock(NetworkObject.class);
        when(eventSimApiTest.source_.getAllSubscriptions()).thenReturn(output);
        assertEquals("{\"Subscriber\":\"test1\",\"ID\":\"1\",\"Url\":\"http:\\/\\/test1.com\"}", eventSimApiTest.getAllSubscriptionDetails(input_parameters));
    }

    @Test(expected = SimulatorException.class)
    public void fetchAllSubscriptions_ButNoSubscriptions() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");

        Map<String, String> input_parameters = new HashMap<>();

        eventSimApiTest.source_ = mock(NetworkObject.class);
        when(eventSimApiTest.source_.getAllSubscriptions()).thenReturn(null);
        eventSimApiTest.getAllSubscriptionDetails(input_parameters);
    }

    @Test
    public void generateRasEvents() throws SimulatorException, PropertyNotExpectedType, ConversionException, RESTClientException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location", "test");
        parameters.put("count", "1");
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");
        eventSimApiTest.eventSimEngine =  mock(SimulatorEngine.class);
       doNothing().when(eventSimApiTest.eventSimEngine).publishRasEvents("test", null , "1", null);
        assertEquals("{\"Status\":\"F\",\"Result\":\"Success\"}", eventSimApiTest.generatRasEvents(parameters));
    }

    @Test
    public void generateRasEventsWithException() throws RESTClientException, SimulatorException, PropertyNotExpectedType, ConversionException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location", "test");
        parameters.put("count", "1");
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");
        eventSimApiTest.eventSimEngine =  mock(SimulatorEngine.class);
        doThrow(new RuntimeException("test exception")).when(eventSimApiTest.eventSimEngine).publishRasEvents("test", null , "1", null);
        assertEquals("{\"Status\":\"E\",\"Result\":\"Error: test exception\"}", eventSimApiTest.generatRasEvents(parameters));
    }

    @Test
    public void generateSensorEvents() throws SimulatorException, PropertyNotExpectedType, ConversionException, RESTClientException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location", "test");
        parameters.put("count", "1");
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");
        eventSimApiTest.eventSimEngine =  mock(SimulatorEngine.class);
        doNothing().when(eventSimApiTest.eventSimEngine).publishSensorEvents("test", null , "1", null);
        assertEquals("{\"Status\":\"F\",\"Result\":\"Success\"}", eventSimApiTest.generateEnvEvents(parameters));
    }

    @Test
    public void generateSensorEventsWithException() throws RESTClientException, SimulatorException, PropertyNotExpectedType, ConversionException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location", "test");
        parameters.put("count", "1");
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");
        eventSimApiTest.eventSimEngine =  mock(SimulatorEngine.class);
        doThrow(new RuntimeException("test exception")).when(eventSimApiTest.eventSimEngine).publishSensorEvents("test", null , "1", null);
        assertEquals("{\"Status\":\"E\",\"Result\":\"Error: test exception\"}", eventSimApiTest.generateEnvEvents(parameters));
    }

    @Test
    public void generateBootEvents() throws SimulatorException, PropertyNotExpectedType, ConversionException, RESTClientException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location", "test");
        parameters.put("count", "1");
        parameters.put("sub_component", "all");
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");
        eventSimApiTest.eventSimEngine =  mock(SimulatorEngine.class);
        doNothing().when(eventSimApiTest.eventSimEngine).publishBootEvents("test", "0" , "false");
        assertEquals("{\"Status\":\"F\",\"Result\":\"Success\"}", eventSimApiTest.generateBootEvents(parameters));
    }

    @Test
    public void generateBootOffEvents() throws SimulatorException, PropertyNotExpectedType, ConversionException, RESTClientException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location", "test");
        parameters.put("count", "1");
        parameters.put("sub_component", "off");
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");
        eventSimApiTest.eventSimEngine =  mock(SimulatorEngine.class);
        doNothing().when(eventSimApiTest.eventSimEngine).publishBootOffEvents("test", "false");
        assertEquals("{\"Status\":\"F\",\"Result\":\"Success\"}", eventSimApiTest.generateBootEvents(parameters));
    }

    @Test
    public void generateBootOnEvents() throws SimulatorException, PropertyNotExpectedType, ConversionException, RESTClientException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location", "test");
        parameters.put("count", "1");
        parameters.put("sub_component", "on");
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");
        eventSimApiTest.eventSimEngine =  mock(SimulatorEngine.class);
        doNothing().when(eventSimApiTest.eventSimEngine).publishBootOnEvents("test", "0" , "false");
        assertEquals("{\"Status\":\"F\",\"Result\":\"Success\"}", eventSimApiTest.generateBootEvents(parameters));
    }

    @Test
    public void generateBootReadyEvents() throws SimulatorException, PropertyNotExpectedType, ConversionException, RESTClientException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location", "test");
        parameters.put("count", "1");
        parameters.put("sub_component", "ready");
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");
        eventSimApiTest.eventSimEngine =  mock(SimulatorEngine.class);
        doNothing().when(eventSimApiTest.eventSimEngine).publishBootReadyEvents("test","false");
        assertEquals("{\"Status\":\"F\",\"Result\":\"Success\"}", eventSimApiTest.generateBootEvents(parameters));
    }

    @Test
    public void generateBootEventsWithException() throws RESTClientException, SimulatorException, PropertyNotExpectedType, ConversionException {
        Map<String, String> parameters = new HashMap<>();
        parameters.put("location", "test");
        parameters.put("probability", "0");
        parameters.put("sub_component", "all");
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp(log);
        eventSimApiTest.jsonParser_ = ConfigIOFactory.getInstance("json");
        eventSimApiTest.eventSimEngine =  mock(SimulatorEngine.class);
        doThrow(new RuntimeException("test exception")).when(eventSimApiTest.eventSimEngine).publishBootEvents("test", "0" , null);
        assertEquals("{\"Status\":\"E\",\"Result\":\"Error: test exception\"}", eventSimApiTest.generateBootEvents(parameters));
    }
}