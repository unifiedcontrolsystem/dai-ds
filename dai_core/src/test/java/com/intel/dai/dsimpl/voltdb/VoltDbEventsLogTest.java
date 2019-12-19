package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.RasEventLog;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyNotExpectedType;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;


import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyVararg;
import static org.mockito.Mockito.*;

public class VoltDbEventsLogTest {

    public class VoltDbEventsLogMock extends VoltDbEventsLog {

        public VoltDbEventsLogMock() {
            super(new String[]{"localhost"}, "test", "test", mock(Logger.class));
            initialize();
        }

        @Override
        void initializeVoltClient(String[] servers) {
            voltClient = _voltClient;
        }

        @Override
        void loadRasEventLog(String[] server, String name, String type) {
            _raseventlog = mockrasEventLog;
        }

        void setEventTypeMap(String key, String value) { mEventTypeToRasDescName.put(key, value); }
        void setDscrpNameMap(String key, String value) {
            mRasDescNameToEventTypeMap.put(key, value);
        }
    }

    @Before
    public void setup() {
        _voltClient = mock(Client.class);
        response_ = mock(ClientResponse.class);
        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("EVENTTYPE", VoltType.STRING),
                new VoltTable.ColumnInfo("DESCRIPTIVENAME", VoltType.STRING),
                new VoltTable.ColumnInfo("SEVERITY", VoltType.STRING),
                new VoltTable.ColumnInfo("CATEGORY", VoltType.STRING),
                new VoltTable.ColumnInfo("COMPONENT", VoltType.STRING),
                new VoltTable.ColumnInfo("CONTROLOPERATION", VoltType.STRING),
                new VoltTable.ColumnInfo("MSG", VoltType.STRING),
                new VoltTable.ColumnInfo("DBUPDATEDTIMESTAMP", VoltType.TIMESTAMP));
        t.addRow("0001000005", "RasGenAdapterAbend", "FATAL", "Adapter", "AdapterGeneric", null, "Adapter finished abnormally", System.currentTimeMillis() * 1000L);
        t.addRow("0001000006", "RasGenAdapterMyCallbackForHouseKeepingNoRtrnValueFailed", "WARNING", "Adapter", "AdapterGeneric", null, "Adapter finished abnormally", null);
        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
    }

    @Test
    public void loadEventDescMapsPositive() throws IOException, ProcCallException {
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        VoltDbEventsLogMock mock = new VoltDbEventsLogMock();
        mock.loadRasEvtTypeDescName();
    }

    @Test (expected = RuntimeException.class)
    public void throwExceptionWhileLoadingMap() throws IOException, ProcCallException {
        doThrow(new IOException("Unable to retrieve RAS meta data from the data store")).when(_voltClient).callProcedure(anyString(), anyString());
        VoltDbEventsLogMock mock = new VoltDbEventsLogMock();
        mock.loadRasEvtTypeDescName();
    }

    @Test (expected = RuntimeException.class)
    public void responseStatusError() throws IOException, ProcCallException {
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.UNEXPECTED_FAILURE);
        VoltDbEventsLogMock mock = new VoltDbEventsLogMock();
        mock.loadRasEvtTypeDescName();
    }

    @Test
    public void checRasEventTypePositive() throws IOException, ProcCallException {
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        VoltDbEventsLogMock mock = new VoltDbEventsLogMock();
        mock.setEventTypeMap("test1","value1" );
        boolean exists = mock.checkRasEventType("test1");
        assertTrue(exists);
    }

    @Test
    public void checRasEventTypeNegative() throws IOException, ProcCallException {
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        VoltDbEventsLogMock mock = new VoltDbEventsLogMock();
        mock.setEventTypeMap("test1","value1" );
        boolean exists = mock.checkRasEventType("test2");
        assertFalse(exists);
    }

    @Test
    public void checRasDescNamePositive() throws IOException, ProcCallException {
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        VoltDbEventsLogMock mock = new VoltDbEventsLogMock();
        mock.setDscrpNameMap("test1","value1" );
        boolean exists = mock.checkDescriptiveName("test1");
        assertTrue(exists);
    }

    @Test
    public void checRasDescNameNegative() throws IOException, ProcCallException {
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        VoltDbEventsLogMock mock = new VoltDbEventsLogMock();
        mock.setDscrpNameMap("test1","value1" );
        boolean exists = mock.checkDescriptiveName("test2");
        assertFalse(exists);
    }

    @Test
    public void getAllEventTypesPositive() throws IOException, ProcCallException, PropertyNotExpectedType {
        HashMap<String, String> paramters = new HashMap<>();
        paramters.put("limit", "1");
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        VoltDbEventsLogMock mock = new VoltDbEventsLogMock();
        PropertyArray result = mock.listAllRasEventTypes(paramters);
        String output = result.getMap(0).getString("eventtype");
        assertEquals("0001000005", output);
    }

    @Test (expected = RuntimeException.class)
    public void getAllEventTypesException() throws IOException, ProcCallException {
        HashMap<String, String> paramters = new HashMap<>();
        paramters.put("limit", "1");
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        doThrow(new IOException("Error occurred while retrieving the data.")).when(_voltClient).callProcedure(anyString(), anyString(), anyString(), anyString());
        VoltDbEventsLogMock mock = new VoltDbEventsLogMock();
        mock.listAllRasEventTypes(paramters);
    }

    @Test
    public void createRasEventWithEffectedJob() throws IOException, ProcCallException {
        mockrasEventLog = mock(RasEventLog.class);
        HashMap<String, String> param = new HashMap<>();
        param.put("eventtype", "0001000005");
        param.put("location", "node00");
        param.put("instancedata", "test");
        param.put("jobid", "1");
        param.put("checkForAffectedJob", "false");
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        Mockito.doNothing().when(mockrasEventLog).logRasEventWithEffectedJob(anyString(), anyString(), anyString(), anyString(), anyLong(), anyString(), anyLong());
        VoltDbEventsLogMock mockEvents = new VoltDbEventsLogMock();
        mockEvents.createRasEvent(param);
    }

    @Test
    public void createRasEventWithNoEffectedJob() throws IOException, ProcCallException {
        mockrasEventLog = mock(RasEventLog.class);
        HashMap<String, String> param = new HashMap<>();
        param.put("eventtype", "0001000005");
        param.put("location", "node00");
        param.put("instancedata", "test");
        param.put("jobid", null);
        param.put("checkForAffectedJob", "false");
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        Mockito.doNothing().when(mockrasEventLog).logRasEventNoEffectedJob(anyString(), anyString(), anyString(), anyLong(), anyString(), anyLong());
        VoltDbEventsLogMock mockEvents = new VoltDbEventsLogMock();
        mockEvents.createRasEvent(param);
    }

    @Test
    public void createRasEvenCheckForEffectedJob() throws IOException, ProcCallException {
        mockrasEventLog = mock(RasEventLog.class);
        HashMap<String, String> param = new HashMap<>();
        param.put("eventtype", "0001000005");
        param.put("location", "node00");
        param.put("instancedata", "test");
        param.put("jobid", null);
        param.put("checkForAffectedJob", "true");
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        Mockito.doNothing().when(mockrasEventLog).logRasEventCheckForEffectedJob(anyString(), anyString(), anyString(), anyLong(), anyString(), anyLong());
        VoltDbEventsLogMock mockEvents = new VoltDbEventsLogMock();
        mockEvents.createRasEvent(param);
    }

    @Test
    public void createRasEventWithNoEffectedJob_NoLocation() throws IOException, ProcCallException {
        mockrasEventLog = mock(RasEventLog.class);
        HashMap<String, String> param = new HashMap<>();
        param.put("eventtype", "0001000005");
        param.put("location", null);
        param.put("instancedata", "test");
        param.put("jobid", null);
        param.put("checkForAffectedJob", "false");
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        Mockito.doNothing().when(mockrasEventLog).logRasEventNoEffectedJob(anyString(), anyString(), anyString(), anyLong(), anyString(), anyLong());
        VoltDbEventsLogMock mockEvents = new VoltDbEventsLogMock();
        mockEvents.createRasEvent(param);
    }

    @Test
    public void createRasEventWithNoEffectedJob_EmptyJob() throws IOException, ProcCallException {
        mockrasEventLog = mock(RasEventLog.class);
        HashMap<String, String> param = new HashMap<>();
        param.put("eventtype", "0001000005");
        param.put("location", "node00");
        param.put("instancedata", "test");
        param.put("jobid", "");
        param.put("checkForAffectedJob", "true");
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        Mockito.doNothing().when(mockrasEventLog).logRasEventNoEffectedJob(anyString(), anyString(), anyString(), anyLong(), anyString(), anyLong());
        VoltDbEventsLogMock mockEvents = new VoltDbEventsLogMock();
        mockEvents.createRasEvent(param);
    }

    @Test
    public void createRasEventWithNoEffectedJob_EmptyJob2() throws IOException, ProcCallException {
        mockrasEventLog = mock(RasEventLog.class);
        HashMap<String, String> param = new HashMap<>();
        param.put("eventtype", "0001000005");
        param.put("location", "node00");
        param.put("instancedata", "test");
        param.put("jobid", "");
        param.put("checkForAffectedJob", "false");
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        Mockito.doNothing().when(mockrasEventLog).logRasEventNoEffectedJob(anyString(), anyString(), anyString(), anyLong(), anyString(), anyLong());
        VoltDbEventsLogMock mockEvents = new VoltDbEventsLogMock();
        mockEvents.createRasEvent(param);
    }

    @Test
    public void createRasEventWithNoEffectedJob_EmptyLocation() throws IOException, ProcCallException {
        mockrasEventLog = mock(RasEventLog.class);
        HashMap<String, String> param = new HashMap<>();
        param.put("eventtype", "0001000005");
        param.put("location", "");
        param.put("instancedata", "test");
        param.put("jobid", null);
        param.put("checkForAffectedJob", "false");
        when(_voltClient.callProcedure(anyString(), any())).thenReturn(response_);
        Mockito.doNothing().when(mockrasEventLog).logRasEventNoEffectedJob(anyString(), anyString(), anyString(), anyLong(), anyString(), anyLong());
        VoltDbEventsLogMock mockEvents = new VoltDbEventsLogMock();
        mockEvents.createRasEvent(param);
    }

    private Client _voltClient;
    private ClientResponse response_;
    RasEventLog mockrasEventLog;
}




