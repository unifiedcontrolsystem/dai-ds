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
import com.intel.dai.exceptions.DataStoreException;
import org.voltdb.types.*;

import java.io.IOException;
import java.util.*;
import java.time.Instant;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VoltDbJobsTest {

    private class VoltDbJobsMock extends VoltDbJobs {

        VoltDbJobsMock(Logger log, String[] servers) { super(log, servers); initialize(); }
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
                new VoltTable.ColumnInfo("JobId", VoltType.STRING),
                new VoltTable.ColumnInfo("WlmJobStarted", VoltType.STRING),
                new VoltTable.ColumnInfo("WlmJobStartTime", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("WlmJobEndTime", VoltType.TIMESTAMP),
                new VoltTable.ColumnInfo("WlmJobWorkDir", VoltType.STRING),
                new VoltTable.ColumnInfo("WlmJobCompleted", VoltType.STRING),
                new VoltTable.ColumnInfo("WlmJobState", VoltType.STRING));
        TimestampType now = new TimestampType(Date.from(Instant.now()));
        TimestampType later = new TimestampType(Date.from(Instant.now().plusSeconds(86400)));
        t.addRow("1", "T", now, later, "/home", "F", "Running");

        voltArray[0] = t;
        when(response_.getResults()).thenReturn(voltArray);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
    }

    @Test
    public void addNodeEntryToInternalCachedJobs() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);

        VoltDbJobs jobs = new VoltDbJobsMock(log_, null);
        String[] nodes = new String[1];
        jobs.addNodeEntryToInternalCachedJobs(nodes, "1", 5500000L);
    }

    @Test
    public void startJobinternal() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);

        VoltDbJobs jobs = new VoltDbJobsMock(log_, null);
        assertEquals ((String) jobs.startJobinternal("1", 5500000L).get("JobId"), "1");
    }

    @Test
    public void startJob() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);

        VoltDbJobs jobs = new VoltDbJobsMock(log_, null);
        jobs.startJob("1", "test", "s1", 128, null, "user", 5500000L, "WLM", 0);
    }

    @Test
    public void completeJobInternal() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);

        VoltDbJobs jobs = new VoltDbJobsMock(log_, null);
        assertEquals ((String) jobs.completeJobInternal("1", "/home", "Running", 6600000L, 5500000L).get("JobId"), "1");
    }

    @Test
    public void terminateJob() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);

        VoltDbJobs jobs = new VoltDbJobsMock(log_, null);
        jobs.terminateJob("1", "info", 0, "/home", "Done", 5500000L, "WLM", 0);
    }

    @Test
    public void terminateJobInternal() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);

        VoltDbJobs jobs = new VoltDbJobsMock(log_, null);
        jobs.terminateJobInternal(5500000L, 5500000L, "1");
    }

    @Test
    public void removeJobInternal() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);

        VoltDbJobs jobs = new VoltDbJobsMock(log_, null);
        jobs.removeJobInternal("1", 5500000L);
    }

    @Test
    public void checkStaleDataInternal() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);

        VoltDbJobs jobs = new VoltDbJobsMock(log_, null);
        jobs.checkStaleDataInternal(5500000L);
    }
}