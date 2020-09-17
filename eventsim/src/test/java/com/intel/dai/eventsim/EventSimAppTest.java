package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
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
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);
        eventSimApiTest.source_ = mock(NetworkObject.class);
        eventSimApiTest.executeRoutes(eventSimApiTest);
    }

    @Test
    public void startServer() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);
        eventSimApiTest.source_ = mock(NetworkObject.class);
        eventSimApiTest.startServer();
    }

    @Test
    public void stopServer() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);
        eventSimApiTest.source_ = mock(NetworkObject.class);
        eventSimApiTest.stopServer();
    }

    @Test
    public void serverStatus() {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);
        eventSimApiTest.source_ = mock(NetworkObject.class);
        eventSimApiTest.serverStatus();
    }

    @Test
    public void noPriorSubscription_DoSubscription() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);
        //eventSimApiTest.parser_ = ConfigIOFactory.getInstance("json");

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
    public void withPriorSubscription_DoSubscription() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);
        ///eventSimApiTest.parser_ = ConfigIOFactory.getInstance("json");

        Map<String, String> input_parameters = new HashMap<>();
        input_parameters.put("Subscriber", "test1");
        input_parameters.put("Url", "http://test1.com");

        eventSimApiTest.source_ = mock(NetworkObject.class);
        doThrow(SimulatorException.class).when(eventSimApiTest.source_).register(anyString(), anyString(), anyMap());
        eventSimApiTest.subscribeStateChangeNotifications(input_parameters);
    }

    @Test
    public void removeAllSubscriptions() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);

        Map<String, String> input_parameters = new HashMap<>();

        eventSimApiTest.source_ = mock(NetworkObject.class);
        when(eventSimApiTest.source_.unRegisterAll()).thenReturn(true);
        assertEquals("", eventSimApiTest.unsubscribeAllStateChangeNotifications(input_parameters));
    }

    @Test
    public void fetchSubscriptionForGivenId() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);
        //eventSimApiTest.parser_ = ConfigIOFactory.getInstance("json");

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
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);
        //eventSimApiTest.parser_ = ConfigIOFactory.getInstance("json");
//
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
    public void removeSubscriptionForId() {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);

        Map<String, String> input_parameters = new HashMap<>();
        input_parameters.put("sub_component", "1");

        eventSimApiTest.source_ = mock(NetworkObject.class);
        when(eventSimApiTest.source_.unRegisterId(anyLong())).thenReturn(true);
        assertEquals("", eventSimApiTest.unsubscribeStateChangeNotifications(input_parameters));
    }

    @Test
    public void fetchAllSubscriptions() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);
        //eventSimApiTest.parser_ = ConfigIOFactory.getInstance("json");

        Map<String, String> input_parameters = new HashMap<>();
        PropertyMap output = new PropertyMap();
        output.put("Url", "http://test1.com");
        output.put("Subscriber", "test1");
        output.put("ID", "1");

        eventSimApiTest.source_ = mock(NetworkObject.class);
        EventSimApp.log_ = mock(Logger.class);
        when(eventSimApiTest.source_.getAllSubscriptions()).thenReturn(output);
        assertEquals("{\"Subscriber\":\"test1\",\"ID\":\"1\",\"Url\":\"http:\\/\\/test1.com\"}", eventSimApiTest.getAllSubscriptionDetails(input_parameters));
    }

    @Test(expected = SimulatorException.class)
    public void fetchAllSubscriptions_ButNoSubscriptions() throws SimulatorException {
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);
        //eventSimApiTest.parser_ = ConfigIOFactory.getInstance("json");

        Map<String, String> input_parameters = new HashMap<>();

        eventSimApiTest.source_ = mock(NetworkObject.class);
        when(eventSimApiTest.source_.getAllSubscriptions()).thenReturn(null);
        eventSimApiTest.getAllSubscriptionDetails(input_parameters);
    }

/*    @Test
    public void fetchRandomSeed() {
        Map<String, String> parameters = new HashMap<>();
        Logger log = mock(Logger.class);
        EventSimApp eventSimApiTest = new EventSimApp("", "", log);
        EventSimApp.log_ = mock(Logger.class);
        //eventSimApiTest.parser_ = ConfigIOFactory.getInstance("json");
        eventSimApiTest.eventSimEngine_ =  mock(SimulatorEngine.class);
        when(eventSimApiTest.eventSimEngine_.getRandomizationSeed()).thenReturn("123");
        assertEquals("{\"Status\":\"F\",\"Result\":\"123\"}", eventSimApiTest.getRandomizationSeed(parameters));
    }*/
}