package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.AdapterOperations;
import com.intel.dai.dsapi.BootState;
import com.intel.dai.dsapi.RasEventLog;
import com.intel.dai.exceptions.AdapterException;
import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.VoltTableRow;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcedureCallback;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class VoltDbAdapterOperationsTest {
    static class MockVoltDbAdapterOperations extends VoltDbAdapterOperations {
        public MockVoltDbAdapterOperations(Logger logger, String[] servers, AdapterInformation adapter, Client client) {
            super(logger, servers, adapter);
            client_ = client;
        }

        @Override
        protected void createRas() {
            ras_ = mock(VoltDbRasEventLog.class);
            super.createRas();
        }

        @Override
        protected void createClient() {
            VoltDbClient.voltClient = client_;
            super.createClient();
        }

        @Override
        protected void createWorkQueue() {
            workQueue_ = mock(VoltDbWorkQueue.class);
            super.createWorkQueue();
        }

        Client client_;
    }

    private ClientResponse setUpBasicNodeInfoResponse1() {
        VoltTable[] table = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                        new VoltTable.ColumnInfo("Hostname", VoltType.STRING),
                        new VoltTable.ColumnInfo("SequenceNumber", VoltType.BIGINT),
                        new VoltTable.ColumnInfo("Owner", VoltType.STRING)
                }, 4)
        };
        table[0].addRow("location", "hostname", 12L, "W");
        table[0].addRow("locationNot", "hostnameNot", 12L, "G");
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response.getResults()).thenReturn(table);
        return response;
    }

    private ClientResponse setUpBasicNodeInfoResponse2() {
        VoltTable[] table = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                        new VoltTable.ColumnInfo("Hostname", VoltType.STRING),
                        new VoltTable.ColumnInfo("SequenceNumber", VoltType.BIGINT),
                        new VoltTable.ColumnInfo("Owner", VoltType.STRING)
                }, 4)
        };
        table[0].addRow("location2", "hostname2", 12L, "G");
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response.getResults()).thenReturn(table);
        return response;
    }

    @Before
    public void setUp() throws Exception {
        String[] servers = new String[] { "127.0.0.1" };
        response_ = mock(ClientResponse.class);
        response2_ = mock(ClientResponse.class);
        client_ = mock(Client.class);
        when(client_.callProcedure(anyString(), anyString(), anyString(), anyLong())).thenReturn(response_);
        when(client_.callProcedure(anyString(), anyString(), anyLong(), anyString(), anyLong())).thenReturn(response_);
        VoltDbClient.voltClient = client_;
        table_ = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("Id", VoltType.BIGINT)
                }, 1)
        };
        table2_ = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo[] {
                        new VoltTable.ColumnInfo("DescriptiveName", VoltType.STRING),
                        new VoltTable.ColumnInfo("EventType", VoltType.STRING)
                }, 2)
        };
        table2_[0].addRow("Name", "Type");
        when(response2_.getResults()).thenReturn(table2_);
        when(response2_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(client_.callProcedure(anyString(), anyString())).thenReturn(response2_);
        operations_ = new VoltDbAdapterOperations(mock(Logger.class), servers,
                new AdapterInformation("TEST", "TEST", "here", "myName", 0L));
    }

    @Test
    public void getAdapterInstancesAdapterId() throws Exception {
        table_[0].addRow((Long)0L);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(table_);
        assertEquals(0L, operations_.getAdapterInstancesAdapterId());
        assertEquals(0L, operations_.getAdapterInstancesAdapterId());
    }

    @Test
    public void getAdapterInstancesAdapterId2() throws Exception {
        VoltDbAdapterOperations operations = new MockVoltDbAdapterOperations(mock(Logger.class),
                new String[] {"127.0.0.1"}, new AdapterInformation("TEST", "TEST", "here", "myName", 0L), client_);
        table_[0].addRow((Long)0L);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(table_);
        assertEquals(0L, operations.getAdapterInstancesAdapterId());
    }

    @Test
    public void getAdapterInstancesAdapterIdNoRow() throws Exception {
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(table_);
        assertEquals(-1L, operations_.getAdapterInstancesAdapterId());
    }

    @Test(expected = RuntimeException.class)
    public void getAdapterInstancesAdapterIdFailed() throws Exception {
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response_.getResults()).thenReturn(table_);
        operations_.getAdapterInstancesAdapterId();
    }

    @Test(expected = IOException.class)
    public void getAdapterInstancesAdapterIdVoltFailure() throws Exception {
        when(client_.callProcedure(anyString(), anyString(), anyString(), anyLong())).thenThrow(IOException.class);
        operations_.getAdapterInstancesAdapterId();
    }

    @Test
    public void registerAdapter() throws Exception {
        table_[0].addRow((Long)0L);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(table_);
        operations_.registerAdapter();
    }

    @Test(expected = RuntimeException.class)
    public void registerAdapterException() throws Exception {
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        operations_.registerAdapter();
    }

    @Test
    public void shutdownAdapter() throws Exception {
        VoltDbAdapterOperations operations = new MockVoltDbAdapterOperations(mock(Logger.class),
                new String[] {"127.0.0.1"}, new AdapterInformation("TEST", "TEST", "here", "myName", 0L), client_);
        table_[0].addRow((Long)0L);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(table_);
        operations.registerAdapter();
        when(client_.callProcedure(anyString(), anyString(), anyLong(), anyString())).thenReturn(response_);
        assertEquals(0, operations.shutdownAdapter());
    }

    @Test
    public void shutdownAdapterAbnormal() throws Exception {
        VoltDbAdapterOperations operations = new MockVoltDbAdapterOperations(mock(Logger.class),
                new String[] {"127.0.0.1"}, new AdapterInformation("TEST", "TEST", "here", "myName", 0L), client_);
        table_[0].addRow((Long)0L);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(table_);
        operations.registerAdapter();
        when(client_.callProcedure(anyString(), anyString(), anyLong(), anyString())).thenReturn(response_);
        assertEquals(1, operations.shutdownAdapter(new Exception("CAUSE")));
    }

    @Test(expected = AdapterException.class)
    public void shutdownAdapterException1() throws Exception {
        VoltDbAdapterOperations operations = new MockVoltDbAdapterOperations(mock(Logger.class),
                new String[] {"127.0.0.1"}, new AdapterInformation("TEST", "TEST", "here", "myName", 0L), client_);
        table_[0].addRow((Long)0L);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(table_);
        operations.registerAdapter();
        when(client_.callProcedure(anyString(), anyString(), anyLong(), anyString())).thenThrow(IOException.class);
        operations.shutdownAdapter();
    }

    @Test(expected = RuntimeException.class)
    public void shutdownAdapterException2() throws Exception {
        VoltDbAdapterOperations operations = new MockVoltDbAdapterOperations(mock(Logger.class),
                new String[] {"127.0.0.1"}, new AdapterInformation("TEST", "TEST", "here", "myName", 0L), client_);
        table_[0].addRow((Long)0L);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(table_);
        operations.registerAdapter();
        when(client_.callProcedure(anyString(), anyString(), anyLong(), anyString())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        operations.shutdownAdapter();
    }

    @Test(expected = RuntimeException.class)
    public void shutdownAdapterException3() throws Exception {
        VoltDbAdapterOperations operations = new MockVoltDbAdapterOperations(mock(Logger.class),
                new String[] {"127.0.0.1"}, new AdapterInformation("TEST", "TEST", "here", "myName", 0L), client_);
        table_[0].addRow((Long)0L);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(table_);
        operations.registerAdapter();
        when(client_.callProcedure(anyString(), anyString(), anyLong(), anyString())).thenReturn(response_);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS, ClientResponse.OPERATIONAL_FAILURE);
        operations.shutdownAdapter();
    }

    @Test(expected = AdapterException.class)
    public void shutdownAdapterException5() throws Exception {
        VoltDbAdapterOperations operations = new MockVoltDbAdapterOperations(mock(Logger.class),
                new String[] {"127.0.0.1"}, new AdapterInformation("TEST", "TEST", "here", "myName", 0L), client_);
        table_[0].addRow((Long)0L);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        when(response_.getResults()).thenReturn(table_);
        operations.registerAdapter();
        when(client_.callProcedure(anyString(), anyString(), anyLong(), anyString(), anyLong())).
                thenThrow(IOException.class);
        operations.shutdownAdapter();
    }

    @Test
    public void informWlmAboutActiveNode() throws Exception {
        ClientResponse response1 = setUpBasicNodeInfoResponse1();
        ClientResponse response2 = setUpBasicNodeInfoResponse2();

        when(client_.callProcedure("ComputeNodeBasicInformation")).thenReturn(response1);
        when(client_.callProcedure("ServiceNodeBasicInformation")).thenReturn(response2);

        VoltDbAdapterOperations operations = new MockVoltDbAdapterOperations(mock(Logger.class),
                new String[] { "127.0.0.1" },
                new AdapterInformation("TEST", "TEST", "here", "myName", 0L), client_);
        operations.markNodeState(BootState.NODE_OFFLINE, "location", System.currentTimeMillis() * 1000, true);
        operations.markNodeState(BootState.NODE_BOOTING, "location", System.currentTimeMillis() * 1000, true);
        operations.markNodeState(BootState.NODE_ONLINE, "location", System.currentTimeMillis() * 1000, true);
        operations.markNodeState(BootState.NODE_ONLINE, "location2", System.currentTimeMillis() * 1000, true);
        operations.markNodeState(BootState.NODE_ONLINE, "locationNot", System.currentTimeMillis() * 1000, true);
    }

    @Test
    public void informWlmAboutActiveNodeNegative1() throws Exception {
        ClientResponse response1 = setUpBasicNodeInfoResponse1();
        ClientResponse response2 = setUpBasicNodeInfoResponse2();

        when(client_.callProcedure("ComputeNodeBasicInformation")).thenReturn(response1);
        when(client_.callProcedure("ServiceNodeBasicInformation")).thenReturn(response2);
        when(client_.callProcedure(any(ProcedureCallback.class), anyString(), anyString(), anyString(),
                anyLong(), anyString(), anyLong())).thenThrow(IOException.class);

        VoltDbAdapterOperations operations = new MockVoltDbAdapterOperations(mock(Logger.class),
                new String[] { "127.0.0.1" },
                new AdapterInformation("TEST", "TEST", "here", "myName", 0L), client_);
        operations.markNodeState(BootState.NODE_OFFLINE, "location", System.currentTimeMillis() * 1000, true);
        operations.markNodeState(BootState.NODE_BOOTING, "location", System.currentTimeMillis() * 1000, true);
        operations.markNodeState(BootState.NODE_ONLINE, "location", System.currentTimeMillis() * 1000, true);
        operations.markNodeState(BootState.NODE_ONLINE, "location2", System.currentTimeMillis() * 1000, true);
    }

    @Test
    public void clientCallback() throws Exception {
        AdapterInformation adapter = new AdapterInformation("TEST", "TEST", "here", "myName", 0L);
        ProcedureCallback callback = new VoltDbAdapterOperations.CallbackForNodeStateChange(adapter,
                mock(RasEventLog.class),"location", "ProcedureName");
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(response.getStatusString()).thenReturn("no entry in the ComputeNode table");
        callback.clientCallback(response);
        when(response.getStatusString()).thenReturn("no entry in the ServiceNode table");
        callback.clientCallback(response);
        when(response.getStatusString()).thenReturn("");
        callback.clientCallback(response);
        when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
        callback.clientCallback(response);
    }

    AdapterOperations operations_;
    Client client_;
    ClientResponse response_;
    ClientResponse response2_;
    VoltTable[] table_;
    VoltTable[] table2_;
}
