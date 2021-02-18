package com.intel.dai.eventsim;

import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class EventSimAppTest {

    @Before
    public void setup() throws SimulatorException {
        log_ = mock(Logger.class);
        eventSimApiTest_ = new EventSimApp("voltdb_server", "server_config_file", log_);
        eventSimApiTest_.network_ = mock(NetworkObject.class);

        input_parameters = new HashMap<>();
        input_parameters.put("Subscriber", "test1");
        input_parameters.put("Url", "http://test1.com");

        output = new PropertyMap();
        output.put("Url", "http://test1.com");
        output.put("Subscriber", "test1");
        output.put("ID", "1");
    }

    @Test
    public void validateRoutes() throws SimulatorException {
        eventSimApiTest_.executeRoutes(eventSimApiTest_);
    }

    @Test
    public void startAndStopServer() throws SimulatorException {
        eventSimApiTest_.startServer();
        eventSimApiTest_.serverStatus();
        eventSimApiTest_.stopServer();
    }

    @Test
    public void noPriorSubscription_DoSubscription() throws SimulatorException {
        doNothing().when(eventSimApiTest_.network_).register(anyString(), anyString(), anyMap());
        when(eventSimApiTest_.network_.getSubscription(anyString(), anyString())).thenReturn(output);
        assertEquals("{\"Subscriber\":\"test1\",\"ID\":\"1\",\"Url\":\"http:\\/\\/test1.com\"}", eventSimApiTest_.subscribeStateChangeNotifications(input_parameters));
    }

    @Test(expected = SimulatorException.class)
    public void withPriorSubscription_DoSubscription() throws SimulatorException {
        doThrow(SimulatorException.class).when(eventSimApiTest_.network_).register(anyString(), anyString(), anyMap());
        eventSimApiTest_.subscribeStateChangeNotifications(input_parameters);
    }

    @Test
    public void removeAllSubscriptions() {
        when(eventSimApiTest_.network_.unRegisterAll()).thenReturn(true);
        assertEquals("", eventSimApiTest_.unsubscribeAllStateChangeNotifications(new HashMap<>()));
    }

    @Test
    public void fetchSubscriptionForGivenId() throws SimulatorException {
        input_parameters.put("sub_component", "1");
        when(eventSimApiTest_.network_.getSubscriptionForId(anyLong())).thenReturn(output);
        assertEquals("{\"Subscriber\":\"test1\",\"ID\":\"1\",\"Url\":\"http:\\/\\/test1.com\"}", eventSimApiTest_.getSubscriptionDetailForId(input_parameters));
    }

    @Test(expected = SimulatorException.class)
    public void fetchSubscriptionForInvalidId() throws SimulatorException {
        input_parameters.put("sub_component", "2");
        when(eventSimApiTest_.network_.getSubscriptionForId(1)).thenReturn(output);
        when(eventSimApiTest_.network_.getSubscriptionForId(2)).thenReturn(null);
        eventSimApiTest_.getSubscriptionDetailForId(input_parameters);
    }

    @Test
    public void removeSubscriptionForId() {
        input_parameters.put("sub_component", "1");
        when(eventSimApiTest_.network_.unRegisterId(anyLong())).thenReturn(true);
        assertEquals("", eventSimApiTest_.unsubscribeStateChangeNotifications(input_parameters));
    }

    @Test
    public void fetchAllSubscriptions() throws SimulatorException {
        EventSimApp.log_ = mock(Logger.class);
        when(eventSimApiTest_.network_.getAllSubscriptions()).thenReturn(output);
        assertEquals("{\"Subscriber\":\"test1\",\"ID\":\"1\",\"Url\":\"http:\\/\\/test1.com\"}", eventSimApiTest_.getAllSubscriptionDetails(input_parameters));
    }

    @Test(expected = SimulatorException.class)
    public void fetchAllSubscriptions_ButNoSubscriptions() throws SimulatorException {
        eventSimApiTest_.network_ = mock(NetworkObject.class);
        when(eventSimApiTest_.network_.getAllSubscriptions()).thenReturn(null);
        eventSimApiTest_.getAllSubscriptionDetails(new HashMap<>());
    }

    private Logger log_;
    private EventSimApp eventSimApiTest_;
    private Map<String, String> input_parameters;
    private PropertyMap output;
}