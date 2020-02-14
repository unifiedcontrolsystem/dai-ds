// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.IAdapter;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.*;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class VoltDbRasEventLogTest {
    class VoltDbRasEventLogMock extends VoltDbRasEventLog {
        VoltDbRasEventLogMock() throws DataStoreException {
            super(new String[] {"localhost"}, adapter_, mock(Logger.class));
            initialize();
        }
        @Override protected Client initializeVoltClient(String[] servers) { return client_; }
    }

    @Before
    public void setUp() throws IOException, ProcCallException {
        client_ = mock(Client.class);
        adapter_ = mock(IAdapter.class);
        when(adapter_.adapterType()).thenReturn("TestType");
        when(adapter_.adapterName()).thenReturn("TestName");
        response_ = mock(ClientResponse.class);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        VoltTable table = new VoltTable(
                new VoltTable.ColumnInfo("EventType", VoltType.STRING),
                new VoltTable.ColumnInfo("DescriptiveName", VoltType.STRING));
        table.addRow("0000000000", "RasTestEvent");
        table.addRow(null, "RasTestEventNull");
        table.addRow("", "RasTestEventEmpty");
        table.addRow("0001000013", "");
        when(response_.getResults()).thenReturn(new VoltTable[] { table });
        when(client_.callProcedure(eq("@AdHoc"), anyString())).thenReturn(response_);
    }

    @Test
    public void constructor1() throws DataStoreException {
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
    }

    @Test
    public void constructor2() throws DataStoreException {
        VoltDbRasEventLog eventLog = new VoltDbRasEventLog(new String[] {"localhost"}, adapter_.adapterType(),
                adapter_.adapterName(), mock(Logger.class));
    }

    @Test(expected = RuntimeException.class)
    public void loadRasMetaDataNegative1() throws Exception {
        when(client_.callProcedure(eq("@AdHoc"), anyString())).thenThrow(ProcCallException.class);
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
    }

    @Test(expected = RuntimeException.class)
    public void loadRasMetaDataNegative2() throws Exception {
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
    }

    @Test
    public void getRasEventType() throws Exception {
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
        assertEquals("0000000000", eventLog.getRasEventType("RasTestEvent", 9999L));
    }

    @Test
    public void getRasEventTypeNegative1() throws Exception {
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
        assertEquals("0001000013", eventLog.getRasEventType("RasTestEventNull", 9999L));
    }

    @Test
    public void getRasEventTypeNegative2() throws Exception {
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
        assertEquals("0001000013", eventLog.getRasEventType("RasTestEventEmpty", 9999L));
    }

    @Test
    public void logRasEventNoEffectedJobNegative() throws Exception {
        doThrow(IOException.class).when(client_).callProcedure(any(ProcedureCallback.class),
                anyString(), anyString(), anyString(), anyString(), eq(null), anyLong(), anyString(), anyLong());
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
        eventLog.logRasEventNoEffectedJob("0001000013", "InstanceData", "Location", 0L, adapter_.adapterType(), 9999L);
    }

    @Test
    public void logRasEventSyncNoEffectedJob() throws Exception {
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
        eventLog.logRasEventSyncNoEffectedJob("0001000013", "InstanceData", "Location", 0L, adapter_.adapterType(),
                9999L);
    }

    @Test
    public void logRasEventSyncNoEffectedJobNegative() throws Exception {
        doThrow(IOException.class).when(client_).callProcedure(anyString(), anyString(), anyString(), anyString(),
                eq(null), anyLong(), anyString(), anyLong());
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
        eventLog.logRasEventSyncNoEffectedJob("0001000013", "InstanceData", "Location", 0L, adapter_.adapterType(), 9999L);
    }

    @Test
    public void logRasEventWithEffectedJob() throws Exception {
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
        eventLog.logRasEventWithEffectedJob("0001000013", "InstanceData", "Location", "JobId", 0L, adapter_.adapterType(),
                9999L);
    }

    @Test
    public void logRasEventWithEffectedJobNegative() throws Exception {
        doThrow(IOException.class).when(client_).callProcedure(any(ProcedureCallback.class), anyString(), anyString(),
                anyString(), anyString(), anyString(), anyLong(), anyString(), anyLong());
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
        eventLog.logRasEventWithEffectedJob("0001000013", "InstanceData", "Location", "JobId", 0L,
                adapter_.adapterType(), 9999L);
    }

    @Test
    public void logRasEventCheckForEffectedJob() throws Exception {
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
        eventLog.logRasEventCheckForEffectedJob("0001000013", "InstanceData", "Location", 0L, adapter_.adapterType(),
                9999L);
    }

    @Test
    public void logRasEventCheckForEffectedJobNegative() throws Exception {
        doThrow(IOException.class).when(client_).callProcedure(any(ProcedureCallback.class), anyString(), anyString(),
                anyString(), anyString(), eq("?"), anyLong(), anyString(), anyLong());
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
        eventLog.logRasEventCheckForEffectedJob("0001000013", "InstanceData", "Location", 0L, adapter_.adapterType(),
                9999L);
    }

    @Test
    public void logRasEventCheckForEffectedJobNegative2() throws Exception {
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
        eventLog.logRasEventCheckForEffectedJob("0001000013", "InstanceData", "", 0L, adapter_.adapterType(),
                9999L);
    }

    @Test
    public void logRasEventCheckForEffectedJobNegative3() throws Exception {
        VoltDbRasEventLog eventLog = new VoltDbRasEventLogMock();
        eventLog.logRasEventCheckForEffectedJob("0001000013", "InstanceData", null, 0L, adapter_.adapterType(),
                9999L);
    }

    private Client client_;
    private IAdapter adapter_;
    private ClientResponse response_;
}
