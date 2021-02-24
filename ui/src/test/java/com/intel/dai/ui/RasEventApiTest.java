package com.intel.dai.ui;

import com.intel.dai.dsapi.EventsLog;
import com.intel.dai.exceptions.ProviderException;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

public class RasEventApiTest {

    @Before
    public void setup() {
        data.put("eventtype", "100005");
    }

    @Test
    public void testEventsLog() throws ProviderException {
        EventsLog eventsLog = Mockito.mock(EventsLog.class);
        assertNotNull(new RasEventApi(eventsLog));
    }

    @Test(expected = ProviderException.class)
    public void testEventsLog_Negative() throws ProviderException {
        EventsLog eventsLog = null;
        assertNull(new RasEventApi(eventsLog));
    }

    @Test(expected = ProviderException.class)
    public void invalidDescriptiveName() throws ProviderException {
        EventsLog eventsLog = Mockito.mock(EventsLog.class);
        when(eventsLog.checkDescriptiveName(anyString())).thenReturn(false);
        RasEventApi eventApi = new RasEventApi(eventsLog);
        eventApi.createRasEvent(data);
    }

    private Map<String, String> data = new HashMap<>();
}
