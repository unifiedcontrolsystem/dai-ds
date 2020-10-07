// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import com.intel.dai.dsapi.NodeIpAndBmcIp;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyVararg;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class VoltDbNodeInformationTest {
    private class MockVoltDbNodeInformation extends VoltDbNodeInformation {
        MockVoltDbNodeInformation(Logger log, String[] servers) { super(log, servers); initialize(); }
        @Override protected Client getClient() { return client_; }
    }

    private Client client_;
    private Logger log_ = mock(Logger.class);
    private VoltDbNodeInformation nodeInfo_;

    private final Map<String,VoltType> types_ = new HashMap<String,VoltType>() {{
        put("SequenceNumber", VoltType.BIGINT);
    }};

    private VoltType determineType(String name) {
        return types_.getOrDefault(name, VoltType.STRING);
    }

    private VoltTable[] infoToVoltTable(List<String> columnNames, List<List<?>> rows) {
        VoltTable.ColumnInfo[] columns = new VoltTable.ColumnInfo[columnNames.size()];
        int i = 0;
        for(String columnName: columnNames)
            columns[i++] = new VoltTable.ColumnInfo(columnName, determineType(columnName));
        VoltTable table = new VoltTable(columns);
        for(List<?> row: rows)
            table.addRow(row.toArray());
        return new VoltTable[] { table };
    }

    @Before
    public void setUp() throws IOException, ProcCallException {
        client_ = mock(Client.class);

        List<String> allColumnNames = new ArrayList<String>() {{
            add("Lctn");
            add("HostName");
            add("State");
            add("SequenceNumber");
            add("IpAddr");
            add("BmcIpAddr");
            add("Aggregator");
        }};
        List<List<?>> computeNodeTable = new ArrayList<List<?>>() {{
            add(new ArrayList<Object>(
                    Arrays.asList("location1", "name1", "A", 100L, "10.0.0.10", "10.1.0.10", "service1")));
            add(new ArrayList<Object>(
                    Arrays.asList("location2", "name2", "N", 200L, "10.0.0.11", "10.1.0.11", "service2")));
            add(new ArrayList<Object>(
                    Arrays.asList("location3", "name3", "A", 300L, "10.0.0.12", "10.1.0.12", "service3")));
        }};
        List<List<?>> serviceNodeTable = new ArrayList<List<?>>() {{
            add(new ArrayList<Object>(
                    Arrays.asList("master", "smw", "A", 1L, "10.192.0.2", "10.193.0.2", null)));
            add(new ArrayList<Object>(
                    Arrays.asList("service1", "sname1", "A", 10L, "10.192.0.10", "10.193.0.10", "master")));
            add(new ArrayList<Object>(
                    Arrays.asList("service2", "sname2", "N", 20L, "10.192.0.11", "10.193.0.11", "master")));
            add(new ArrayList<Object>(
                    Arrays.asList("service3", "sname3", "A", 30L, "10.192.0.12", "10.193.0.12", "master")));
        }};

        doAnswer(invocation -> {
            ClientResponse response = mock(ClientResponse.class);
            when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
            when(response.getResults()).thenReturn(infoToVoltTable(allColumnNames, computeNodeTable));
            return response;
        }).when(client_).callProcedure(eq("ComputeNodeListLctnAndHostname"));
        doAnswer(invocation -> {
            ClientResponse response = mock(ClientResponse.class);
            when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
            when(response.getResults()).thenReturn(infoToVoltTable(allColumnNames, serviceNodeTable));
            return response;
        }).when(client_).callProcedure("ServiceNodeListLctnAndHostname");
        doAnswer(invocation -> {
            ClientResponse response = mock(ClientResponse.class);
            when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
            when(response.getResults()).thenReturn(infoToVoltTable(allColumnNames, computeNodeTable));
            return response;
        }).when(client_).callProcedure("ComputeNodeListLctnAndSeqNum");
        doAnswer(invocation -> {
            ClientResponse response = mock(ClientResponse.class);
            when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
            when(response.getResults()).thenReturn(infoToVoltTable(allColumnNames, computeNodeTable));
            return response;
        }).when(client_).callProcedure("ComputeNodesList");
        doAnswer(invocation -> {
            ClientResponse response = mock(ClientResponse.class);
            when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
            when(response.getResults()).thenReturn(infoToVoltTable(allColumnNames, serviceNodeTable));
            return response;
        }).when(client_).callProcedure("ServiceNodesList");

        new MockVoltDbNodeInformation(log_, new String[] {"127.0.0.1"});
        nodeInfo_ = new MockVoltDbNodeInformation(log_, null);
    }

    @Test
    public void getComputeNodeState() throws DataStoreException, IOException, ProcCallException {
        doAnswer(invocation ->  {
            ClientResponse response = mock(ClientResponse.class);
            when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
            List<Object> oneRow = new ArrayList<Object>(Arrays.asList("location1", "A"));
            when(response.getResults()).thenReturn(infoToVoltTable(Arrays.asList("Lctn", "State"),
                    new ArrayList<List<?>>() {{ add(oneRow); }}));
            return response;
        }).when(client_).callProcedure(eq("ComputeNodeState"), eq("location1"));
        doAnswer(invocation ->  {
            ClientResponse response = mock(ClientResponse.class);
            when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
            List<Object> oneRow = new ArrayList<Object>(Arrays.asList("location2", "N"));
            when(response.getResults()).thenReturn(infoToVoltTable(Arrays.asList("Lctn", "State"),
                    new ArrayList<List<?>>() {{ add(oneRow); }}));
            return response;
        }).when(client_).callProcedure(eq("ComputeNodeState"), eq("location2"));
        assertEquals("A", nodeInfo_.getComputeNodeState("location1"));
        assertEquals("N", nodeInfo_.getComputeNodeState("location2"));
    }

    @Test(expected = DataStoreException.class)
    public void getComputeNodeStateNegative() throws DataStoreException, IOException, ProcCallException {
        when(client_.callProcedure(eq("ComputeNodeState"), anyString())).thenThrow(ProcCallException.class);
        nodeInfo_.getComputeNodeState("location1");
    }

    @Test
    public void getComputeXFromYMap() throws DataStoreException {
        Map<String,String> map = nodeInfo_.getComputeHostnameFromLocationMap();
        assertEquals(3, map.size());
        assertEquals("name1", map.get("location1"));
        assertEquals("name2", map.get("location2"));
        assertEquals("name3", map.get("location3"));
        map = nodeInfo_.getComputeNodeLocationFromHostnameMap();
        assertEquals(3, map.size());
        assertEquals("location1", map.get("name1"));
        assertEquals("location2", map.get("name2"));
        assertEquals("location3", map.get("name3"));
    }

    @Test(expected = DataStoreException.class)
    public void getComputeXFromYMapNegative() throws DataStoreException, IOException, ProcCallException {
        when(client_.callProcedure(eq("ComputeNodeListLctnAndHostname"))).thenThrow(ProcCallException.class);
        nodeInfo_.getComputeHostnameFromLocationMap();
    }

    @Test
    public void getServiceXFromYMap() throws DataStoreException {
        Map<String,String> map = nodeInfo_.getServiceHostnameFromLocationMap();
        assertEquals(4, map.size());
        assertEquals("sname1", map.get("service1"));
        assertEquals("sname2", map.get("service2"));
        assertEquals("sname3", map.get("service3"));
        map = nodeInfo_.getServiceNodeLocationFromHostnameMap();
        assertEquals(4, map.size());
        assertEquals("service1", map.get("sname1"));
        assertEquals("service2", map.get("sname2"));
        assertEquals("service3", map.get("sname3"));
    }

    @Test(expected = DataStoreException.class)
    public void getServiceXFromYMapNegative() throws DataStoreException, IOException, ProcCallException {
        when(client_.callProcedure(eq("ServiceNodeListLctnAndHostname"))).thenThrow(ProcCallException.class);

        nodeInfo_.getServiceHostnameFromLocationMap();
    }

    @Test
    public void getComputeNodeSequenceNumberFromLocationMap() throws DataStoreException {
        Map<String,Long> map = nodeInfo_.getComputeNodeSequenceNumberFromLocationMap();
        assertEquals(3, map.size());
        assertEquals(100L, (long)map.get("location1"));
        assertEquals(200L, (long)map.get("location2"));
        assertEquals(300L, (long)map.get("location3"));
        nodeInfo_.getComputeNodeSequenceNumberFromLocationMap();
    }

    @Test(expected = DataStoreException.class)
    public void getComputeNodeSequenceNumberFromLocationMapNegative()
            throws DataStoreException, IOException, ProcCallException {
        when(client_.callProcedure("ComputeNodeListLctnAndSeqNum")).thenThrow(ProcCallException.class);
        nodeInfo_.getComputeNodeSequenceNumberFromLocationMap();
    }

    @Test
    public void getAggregatorLocationFromNodeLocationMap() throws DataStoreException {
        Map<String,String> map = nodeInfo_.getAggregatorLocationFromNodeLocationMap();
        assertEquals(7, map.size());
        assertEquals("service1", map.get("location1"));
        assertEquals("service2", map.get("location2"));
        assertNull(map.get("master"));
        nodeInfo_.getAggregatorLocationFromNodeLocationMap();
    }

    @Test(expected = DataStoreException.class)
    public void getAggregatorLocationFromNodeLocationMapNegative()
            throws DataStoreException, IOException, ProcCallException {
        when(client_.callProcedure("ComputeNodesList")).thenThrow(ProcCallException.class);
        nodeInfo_.getAggregatorLocationFromNodeLocationMap();
    }

    @Test
    public void getNodeAndBmcIPsFromLocationMap() throws DataStoreException {
        Map<String, NodeIpAndBmcIp> map = nodeInfo_.getNodeAndBmcIPsFromLocationMap();
        assertEquals(7, map.size());
        assertEquals("10.192.0.10", map.get("service1").nodeIpAddress);
        assertEquals("10.192.0.11", map.get("service2").nodeIpAddress);
        assertEquals("10.192.0.12", map.get("service3").nodeIpAddress);
        assertEquals("10.193.0.10", map.get("service1").bmcIpAddress);
        assertEquals("10.193.0.11", map.get("service2").bmcIpAddress);
        assertEquals("10.193.0.12", map.get("service3").bmcIpAddress);
        nodeInfo_.getNodeAndBmcIPsFromLocationMap();
        map = nodeInfo_.getComputeNodeAndBmcIPsFromLocationMap();
        assertEquals(3, map.size());
        assertEquals("10.0.0.10", map.get("location1").nodeIpAddress);
        assertEquals("10.0.0.11", map.get("location2").nodeIpAddress);
        assertEquals("10.0.0.12", map.get("location3").nodeIpAddress);
        assertEquals("10.1.0.10", map.get("location1").bmcIpAddress);
        assertEquals("10.1.0.11", map.get("location2").bmcIpAddress);
        assertEquals("10.1.0.12", map.get("location3").bmcIpAddress);
        nodeInfo_.getComputeNodeAndBmcIPsFromLocationMap();
    }

    @Test(expected = DataStoreException.class)
    public void getNodeAndBmcIPsFromLocationMapNegative() throws DataStoreException, IOException, ProcCallException {
        when(client_.callProcedure("ServiceNodesList")).thenThrow(ProcCallException.class);
        Map<String, NodeIpAndBmcIp> map = nodeInfo_.getNodeAndBmcIPsFromLocationMap();
    }

    @Test
    public void otherTests() throws DataStoreException {
        assertTrue(nodeInfo_.isServiceNodeLocation("service2"));
        assertFalse(nodeInfo_.isServiceNodeLocation("location2"));
        assertFalse(nodeInfo_.isComputeNodeLocation("service2"));
        assertTrue(nodeInfo_.isComputeNodeLocation("location2"));
        assertEquals("10.192.0.2", nodeInfo_.getNodesIpAddress("master"));
        assertEquals("10.193.0.2", nodeInfo_.getNodesBmcIpAddress("master"));
    }

    @Test
    public void locationListTest() throws Exception {
        nodeInfo_.getComputeNodeLocations();
        nodeInfo_.getComputeNodeLocations();
        nodeInfo_.getServiceNodeLocations();
        nodeInfo_.getServiceNodeLocations();
        nodeInfo_.getNodeLocations();
        List<String> allNodeLocations = nodeInfo_.getNodeLocations();
        String[] golden = new String[] {
                "location1",
                "location2",
                "location3",
                "master",
                "service1",
                "service2",
                "service3"
        };
        for(int i = 0; i < 7; i++)
            assertEquals(golden[i], allNodeLocations.get(i));
    }
}
