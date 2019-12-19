// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.populate;

import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;
import org.voltdb.client.Client;
import org.voltdb.client.ClientConfig;

import org.junit.Test;
import org.voltdb.client.ClientResponse;
import org.voltdb.client.ClientStatusListenerExt;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DbSchemaPopulateTest {
    private static class MockDbSchemaPopulate extends DefaultOnlineTierDataLoader {
        protected MockDbSchemaPopulate(Logger log) {
            super(log, factory_);
        }

        @Override
        Client connectToVoltDB(String servers) {
            return mock(Client.class);
        }

        @Override
        ClientResponse internalCallProcedure(String name, Object... args) throws IOException {
            VoltTable[] result = new VoltTable[1];
            result[0] = new VoltTable(
                    new VoltTable.ColumnInfo("Value", VoltType.BIGINT)
            );
            result[0].addRow(0L);
            ClientResponse response = mock(ClientResponse.class);
            when(response.getResults()).thenReturn(result);
            when(response.getStatus()).thenReturn(ClientResponse.SUCCESS);
            return response;
        }
    }

    @BeforeClass
    public static void setUpClass() {
        LoggerFactory.getInstance("TEST", "Testing", "console");
    }

    @Test
    public void testMachineConfigEntry() {
        DefaultOnlineTierDataLoader.MachineConfigEntry mce = new DefaultOnlineTierDataLoader.MachineConfigEntry("","","",
                "","", "", "", "", "");
    }

    @Test
    public void testMachineConfigWiInfo() {
        DefaultOnlineTierDataLoader.MachineConfigWiInfo mcwi = new DefaultOnlineTierDataLoader.MachineConfigWiInfo("", "",
                "", "", "");
        assertEquals("", mcwi.notifyWhenFinished());
        assertEquals("", mcwi.parms());
        assertEquals("", mcwi.queue());
        assertEquals("", mcwi.typeOfAdapter());
        assertEquals("", mcwi.work());
    }

    @Test
    public void testManifestContent() {
        DefaultOnlineTierDataLoader.ManifestContent mc = new DefaultOnlineTierDataLoader.ManifestContent("", "");
        assertEquals("", mc.definition());
        assertEquals("", mc.name());
    }

    @Test
    public void testMyClientStatusListenerExt() {
        AtomicBoolean shuttingDown = new AtomicBoolean(false);
        DefaultOnlineTierDataLoader.MyClientStatusListenerExt listen = new DefaultOnlineTierDataLoader.MyClientStatusListenerExt(mock(Logger.class), shuttingDown);
        listen.backpressure(false);
        listen.connectionLost("", 0, 0, ClientStatusListenerExt.DisconnectCause.CONNECTION_CLOSED);
        listen.lateProcedureResponse(mock(ClientResponse.class), "", 0);
        listen.uncaughtException(null, mock(ClientResponse.class), new Exception());
        shuttingDown.set(true);
        listen = new DefaultOnlineTierDataLoader.MyClientStatusListenerExt(mock(Logger.class), shuttingDown);
        listen.connectionLost("", 0, 0, ClientStatusListenerExt.DisconnectCause.CONNECTION_CLOSED);
    }

    private static final DataStoreFactory factory_ = mock(DataStoreFactory.class);
}
