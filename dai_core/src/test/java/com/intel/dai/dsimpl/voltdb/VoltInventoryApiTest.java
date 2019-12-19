package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.InventoryApi;
import com.intel.dai.dsapi.NodeInformation;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VoltInventoryApiTest {
    class MockVoltInventoryApi extends VoltInventoryApi {
        public MockVoltInventoryApi() {
            super(mock(Logger.class), adapter_, new String[] {"127.0.0.1"});
            initialize();
        }

        @Override protected Client getClient() { return client_; }
        @Override protected NodeInformation getNodeInformation() { return info_; }
    }

    @Before
    public void setUp() throws Exception {
        client_ = mock(Client.class);
        api_ = new MockVoltInventoryApi();
        info_ = mock(NodeInformation.class);
        when(info_.isComputeNodeLocation("location1")).thenReturn(true);
        when(info_.isComputeNodeLocation("location2")).thenReturn(false);
        when(info_.isServiceNodeLocation("location1")).thenReturn(false);
        when(info_.isServiceNodeLocation("location2")).thenReturn(true);
        when(info_.isComputeNodeLocation("location3")).thenReturn(false);
        when(info_.isServiceNodeLocation("location3")).thenReturn(false);

        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
        VoltTable table = new VoltTable(
                new VoltTable.ColumnInfo("InventoryInfo", VoltType.STRING)
        );
        table.addRow("Inventory Stuff");
        when(response.getResults()).thenReturn(new VoltTable[] { table });
        when(client_.callProcedure(anyString(), anyString())).thenReturn(response);
    }

    @Test
    public void getNodesInvInfoFromDb() throws DataStoreException {
        assertEquals("Inventory Stuff", api_.getNodesInvInfoFromDb("location1"));
        assertEquals("Inventory Stuff", api_.getNodesInvInfoFromDb("location2"));
    }

    @Test(expected = DataStoreException.class)
    public void getNodesInvInfoFromDbNegative1() throws DataStoreException {
        api_.getNodesInvInfoFromDb("location3");
    }

    @Test(expected = RuntimeException.class)
    public void getNodesInvInfoFromDbNegative2() throws DataStoreException, IOException, ProcCallException {
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(ClientResponse.OPERATIONAL_FAILURE);
        when(client_.callProcedure(anyString(), anyString())).thenReturn(response);
        api_.getNodesInvInfoFromDb("location1");
    }

    @Test
    public void getNodesInvInfoFromDbNegative3() throws DataStoreException, IOException, ProcCallException {
        ClientResponse response = mock(ClientResponse.class);
        when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
        VoltTable[] empty = new VoltTable[] {
                new VoltTable(new VoltTable.ColumnInfo("InventoryInfo", VoltType.STRING))
        };
        when(response.getResults()).thenReturn(empty);
        when(client_.callProcedure(anyString(), anyString())).thenReturn(response);
        assertNull(api_.getNodesInvInfoFromDb("location1"));
    }

    @Test(expected = DataStoreException.class)
    public void getNodesInvInfoFromDbNegative4() throws DataStoreException, IOException, ProcCallException {
        when(client_.callProcedure(anyString(), anyString())).thenThrow(ProcCallException.class);
        api_.getNodesInvInfoFromDb("location1");
    }

    private static final AdapterInformation adapter_ =
            new AdapterInformation("TestType", "TestAdapter", "master", "smw", 24L);
    private Client client_;
    private InventoryApi api_;
    private NodeInformation info_;
}
