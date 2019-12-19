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
        when(eventsLog.checkRasEventType(anyString())).thenReturn(false);
        RasEventApi eventApi = new RasEventApi(eventsLog);
        eventApi.createRasEvent(data);
    }

    @Test(expected = ProviderException.class)
    public void noRasEventTypes() throws ProviderException, PropertyNotExpectedType {
        Map<String, String> param = new HashMap<>();
        EventsLog eventsLog = Mockito.mock(EventsLog.class);
        when(eventsLog.checkRasEventType(anyString())).thenReturn(true);
        doNothing().when(eventsLog).createRasEvent(any());
        when(eventsLog.listAllRasEventTypes(param)).thenReturn(null);
        RasEventApi eventApi = new RasEventApi(eventsLog);
        eventApi.getRasEventTypes(param);
    }

    @Test(expected = ProviderException.class)
    public void emptyRasEventTypes() throws ProviderException, PropertyNotExpectedType {
        Map<String, String> param = new HashMap<>();
        EventsLog eventsLog = Mockito.mock(EventsLog.class);
        when(eventsLog.checkRasEventType(anyString())).thenReturn(true);
        doNothing().when(eventsLog).createRasEvent(any());
        when(eventsLog.listAllRasEventTypes(param)).thenReturn(new PropertyArray());
        RasEventApi eventApi = new RasEventApi(eventsLog);
        eventApi.getRasEventTypes(param);
    }

    @Test
    public void listRasEventTypes() throws ProviderException, PropertyNotExpectedType {
        EventsLog eventsLog = Mockito.mock(EventsLog.class);
        Map<String, String> param = new HashMap<>();
        when(eventsLog.checkRasEventType(anyString())).thenReturn(true);
        doNothing().when(eventsLog).createRasEvent(any());
        when(eventsLog.listAllRasEventTypes(param)).thenReturn(getRasEventTypes());
        RasEventApi eventApi = new RasEventApi(eventsLog);
        assertEquals("{schema=[{unit=string, data=severity, heading=severity}, " +
                "{unit=string, data=msg, heading=msg}, {unit=string, data=descriptivename, " +
                "heading=descriptivename}, {unit=string, data=controloperation, " +
                "heading=controloperation}], result-data-lines=1, result-status-code=0, " +
                "data=[[FATAL, FATAL event, Test event, null]], result-data-columns=4}",
                eventApi.getRasEventTypes(param).toString());
    }

    private PropertyArray getRasEventTypes() {
        PropertyArray data = new PropertyArray();
        PropertyMap eventType = new PropertyMap();
        eventType.put("severity", "FATAL");
        eventType.put("msg", "FATAL event");
        eventType.put("descriptivename", "Test event");
        eventType.put("controloperation", null);
        data.add(eventType);
        return data;
    }

    private Map<String, String> data = new HashMap<>();
}
