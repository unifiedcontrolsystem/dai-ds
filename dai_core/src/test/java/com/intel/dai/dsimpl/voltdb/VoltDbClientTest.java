// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.voltdb;

import org.junit.Test;
import org.voltdb.client.ClientResponse;

import static org.junit.Assert.*;

public class VoltDbClientTest {

    @Test
    public void initializeVoltDbClient() {
        VoltDbClient.initializeVoltDbClient(new String[] {"localhost"});
    }

    @Test
    public void initializeVoltDbClientNoServers() {
        VoltDbClient.initializeVoltDbClient(null);
    }

    @Test
    public void getVoltClientInstance() {
        try {
            VoltDbClient.voltClient = null;
            VoltDbClient.getVoltClientInstance();
            fail("test failed as voltclient is marked null before getting an instance of it");
        } catch(Exception e) {

        }
    }

    @Test
    public void statusByteAsString() {
        assertEquals(VoltDbClient.statusByteAsString(ClientResponse.USER_ABORT), "USER_ABORT");
        assertEquals(VoltDbClient.statusByteAsString(ClientResponse.CONNECTION_LOST), "CONNECTION_LOST");
        assertEquals(VoltDbClient.statusByteAsString(ClientResponse.CONNECTION_TIMEOUT), "CONNECTION_TIMEOUT");
        assertEquals(VoltDbClient.statusByteAsString(ClientResponse.GRACEFUL_FAILURE), "GRACEFUL_FAILURE");
        assertEquals(VoltDbClient.statusByteAsString(ClientResponse.RESPONSE_UNKNOWN), "RESPONSE_UNKNOWN");
        assertEquals(VoltDbClient.statusByteAsString(ClientResponse.UNEXPECTED_FAILURE), "UNEXPECTED_FAILURE");
        assertEquals(VoltDbClient.statusByteAsString(ClientResponse.SUCCESS), "SUCCESS");
        assertEquals(VoltDbClient.statusByteAsString((byte)-128), "-128");

    }
}