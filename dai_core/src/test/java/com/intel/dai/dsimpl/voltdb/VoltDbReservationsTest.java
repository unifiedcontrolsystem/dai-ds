// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import com.intel.logging.Logger;
import org.junit.Before;
import org.junit.Test;
import org.voltdb.client.Client;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ProcCallException;
import com.intel.dai.exceptions.DataStoreException;

import java.io.IOException;
import java.util.*;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class VoltDbReservationsTest {

    private class VoltDbReservationsMock extends VoltDbReservations {

        VoltDbReservationsMock(Logger log, String[] servers) { super(log, servers); initialize(); }
        @Override protected Client getClient() { return client_; }

    }

    private Client client_;
    private ClientResponse response_;
    private Logger log_ = mock(Logger.class);

    @Before
    public void setUp() {
        client_ = mock(Client.class);
        response_ = mock(ClientResponse.class);
        when(response_.getStatus()).thenReturn(ClientResponse.SUCCESS);
    }

    @Test
    public void createReservation() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);

        VoltDbReservations reservations = new VoltDbReservationsMock(log_, null);
        reservations.createReservation("res1", "user", "c01",  5500000L, 6600000L, 5500000L, "WLM", 0);
    }

    @Test
    public void updateReservation() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);

        VoltDbReservations reservations = new VoltDbReservationsMock(log_, null);
        reservations.updateReservation("res1", "user", "c01",  5500000L, 0L, 5500000L, "WLM", 0);
    }

    @Test
    public void deleteReservation() throws IOException, ProcCallException, DataStoreException {
        when(client_.callProcedure(anyString(), any())).thenReturn(response_);

        VoltDbReservations reservations = new VoltDbReservationsMock(log_, null);
        reservations.deleteReservation("res1", 5500000L, "WLM", 0);
    }

}