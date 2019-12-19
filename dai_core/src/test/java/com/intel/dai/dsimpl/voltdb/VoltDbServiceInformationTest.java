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

public class VoltDbServiceInformationTest {
    private class MockVoltDbServiceInformation extends VoltDbServiceInformation {

        MockVoltDbServiceInformation(Logger log, String[] servers) { super(log, servers); }
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
                new VoltTable.ColumnInfo("ServiceOperationId", VoltType.BIGINT),
                new VoltTable.ColumnInfo("Lctn", VoltType.STRING),
                new VoltTable.ColumnInfo("TypeOfServiceOperation", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStartedService", VoltType.STRING),
                new VoltTable.ColumnInfo("UserStoppedService", VoltType.STRING),
                new VoltTable.ColumnInfo("State", VoltType.STRING),
                new VoltTable.ColumnInfo("Status", VoltType.STRING),
                new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("StopTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("StartRemarks", VoltType.STRING),
                new VoltTable.ColumnInfo("StopRemarks", VoltType.STRING),
                new VoltTable.ColumnInfo("DbUpdatedTimestamp", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("LogFile", VoltType.STRING));
        TimestampType now = new TimestampType(Date.from(Instant.now()));
        t.addRow(1, "location1", "Exclusive", "user", null, "O", "A", now, null, null, null, now, "Log");

        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
    }

    @Test
    public void getServiceOperationInfo() throws IOException, ProcCallException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);
        VoltDbServiceInformation serviceinfo = new MockVoltDbServiceInformation(log_, null);
        serviceinfo.initialize();
        assertEquals ((String) serviceinfo.getServiceOperationInfo("location1").get("TypeOfServiceOperation"), "Exclusive");
    }

}
