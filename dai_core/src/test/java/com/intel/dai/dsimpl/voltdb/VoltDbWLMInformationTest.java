// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import org.voltdb.types.*;

import java.io.IOException;
import java.util.*;
import java.time.Instant;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VoltDbWLMInformationTest {
    private class MockVoltDbWLMInformation extends VoltDbWLMInformation {

        MockVoltDbWLMInformation(Logger log, String[] servers) { super(log, servers); }
        @Override protected Client getClient() { return client_; }
    }

    private Client client_;
    private ClientResponse response_;
    private Logger log_ = mock(Logger.class);

    @Before
    public void setUp() {
        client_ = mock(Client.class);
        response_ = mock(ClientResponse.class);
        VoltTable[] voltArray = new VoltTable[1];
        VoltTable t = new VoltTable(
                new VoltTable.ColumnInfo("Users", VoltType.STRING),
                new VoltTable.ColumnInfo("Nodes", VoltType.STRING),
                new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("EndTimestamp", VoltType.TIMESTAMP));
        TimestampType now = new TimestampType(Date.from(Instant.now()));
        TimestampType later = new TimestampType(Date.from(Instant.now().plusSeconds(86400)));
        t.addRow("user", "location1", now, later);

        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
    }

    @Test
    public void getUsersForActiveReservation() throws IOException, ProcCallException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);
        VoltDbWLMInformation wlminfo = new MockVoltDbWLMInformation(log_, null);
        wlminfo.initialize();
        assertEquals (wlminfo.getUsersForActiveReservation("location1").get("location1"), "user");
    }

}
