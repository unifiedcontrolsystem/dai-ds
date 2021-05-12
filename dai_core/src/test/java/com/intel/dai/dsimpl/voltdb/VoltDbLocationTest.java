// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

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
import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VoltDbLocationTest {
    private class MockVoltDbLocation extends VoltDbLocation {

        MockVoltDbLocation(Logger log, String[] servers) { super(log, servers); initialize(); }
        @Override protected Client getClient() { return client_; }
    }

    private Client client_;
    private ClientResponse computeNodesResponse_;
    private ClientResponse serviceNodesResponse_;
    private ClientResponse emptyResponse_;
    private Logger log_ = mock(Logger.class);

    @Before
    public void setUp() {
        client_ = mock(Client.class);
        computeNodesResponse_ = mock(ClientResponse.class);
        serviceNodesResponse_ = mock(ClientResponse.class);
        emptyResponse_ = mock(ClientResponse.class);

        VoltTable[] voltArrayCompute = new VoltTable[1];
        VoltTable computeNodesTable = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("HostName", VoltType.STRING),
                new VoltTable.ColumnInfo("BmcHostName", VoltType.STRING),
                new VoltTable.ColumnInfo("IpAddr", VoltType.STRING),
                new VoltTable.ColumnInfo("MacAddr", VoltType.STRING));
        computeNodesTable.addRow("location_c01", "c01", "c01_bmc", "192.168.100.1", "00:11:22:33:44:55");
        computeNodesTable.addRow("location_c02", "c02", "c02_bmc", "192.168.100.2", "00:11:22:33:44:56");
        voltArrayCompute[0] = computeNodesTable;
        when(computeNodesResponse_.getResults()).thenReturn(voltArrayCompute);
        when(computeNodesResponse_.getStatus()).thenReturn(ClientResponse.SUCCESS);
        VoltTable[] voltArrayService = new VoltTable[1];
        VoltTable serviceNodesTable = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("HostName", VoltType.STRING),
                new VoltTable.ColumnInfo("BmcHostName", VoltType.STRING),
                new VoltTable.ColumnInfo("IpAddr", VoltType.STRING),
                new VoltTable.ColumnInfo("MacAddr", VoltType.STRING));
        serviceNodesTable.addRow("location_s01", "s01", "s01_bmc", "192.168.200.1", "00:11:22:33:44:a5");
        serviceNodesTable.addRow("location_s02", "s02", "s02_bmc", "192.168.200.2", "00:11:22:33:44:a6");
        voltArrayService[0] = serviceNodesTable;
        when(serviceNodesResponse_.getResults()).thenReturn(voltArrayService);
        when(serviceNodesResponse_.getStatus()).thenReturn(ClientResponse.SUCCESS);

        VoltTable[] voltArrayEmpty = new VoltTable[1];
        VoltTable emptyTable = new VoltTable(
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("HostName", VoltType.STRING),
                new VoltTable.ColumnInfo("BmcHostName", VoltType.STRING));
        voltArrayEmpty[0] = emptyTable;
        when(emptyResponse_.getResults()).thenReturn(voltArrayEmpty);
        when(emptyResponse_.getStatus()).thenReturn(ClientResponse.SUCCESS);
    }

    @Test
    public void getHostnameAndLocation() throws IOException, ProcCallException {
        when(client_.callProcedure(eq("ComputeNodeLocationInformation"))).thenReturn(computeNodesResponse_);
        when(client_.callProcedure(eq("ServiceNodeLocationInformation"))).thenReturn(serviceNodesResponse_);
        ClientResponse machineResponse = mock(ClientResponse.class);
        when(client_.callProcedure(eq("MachineDescription"))).thenReturn(machineResponse);
        when(machineResponse.getStatus()).thenReturn(ClientResponse.SUCCESS);
        VoltTable description = new VoltTable(new VoltTable.ColumnInfo("Description", VoltType.STRING));
        description.addRow("SystemName");
        when(machineResponse.getResults()).thenReturn(new VoltTable[] {description});
        VoltDbLocation lctn = new MockVoltDbLocation(log_, null);
        assertEquals("location_c01", lctn.getLocationFromHostname("c01"));
        assertEquals("location_c02", lctn.getLocationFromHostname("c02bmc"));
        assertEquals("location_s01", lctn.getLocationFromHostname("s01"));
        assertEquals("location_s02", lctn.getLocationFromHostname("s02bmc"));
        assertEquals("location_c01", lctn.getLocationFromIP("192.168.100.1"));
        assertEquals("location_s01", lctn.getLocationFromIP("192.168.200.1"));
        assertEquals("location_c01", lctn.getLocationFromMAC("00:11:22:33:44:55"));
        assertEquals("location_s01", lctn.getLocationFromMAC("00:11:22:33:44:a5"));
        assertEquals("", lctn.getLocationFromHostname("nonExistent"));

        assertEquals("c01", lctn.getHostnameFromLocation("location_c01"));
        assertEquals("c02", lctn.getHostnameFromLocation("location_c02"));
        assertEquals("s01", lctn.getHostnameFromLocation("location_s01"));
        assertEquals("s02", lctn.getHostnameFromLocation("location_s02"));
        assertEquals("", lctn.getHostnameFromLocation("nonExistent"));
    }

    @Test
    public void emptyHostnameAndLocationMaps() throws IOException, ProcCallException {
        when(client_.callProcedure(anyString(), any())).thenReturn(emptyResponse_);

        VoltDbLocation lctn = new MockVoltDbLocation(log_, null);
        assertEquals(lctn.getLocationFromHostname("c01"), "");
        assertEquals(lctn.getHostnameFromLocation("location_c01"), "");
    }

    @Test(expected = DataStoreException.class)
    public void voltdbClientFailed() throws IOException, ProcCallException, DataStoreException {
        ClientResponse resp = mock(ClientResponse.class);
        when(resp.getStatus()).thenReturn(ClientResponse.USER_ABORT); // -1
        when(client_.callProcedure(anyString(), any())).thenReturn(resp);
        VoltDbLocation lctn = new MockVoltDbLocation(log_, null);
        lctn.reloadCache();
    }
}
