// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl;

import com.intel.dai.AdapterInformation;
import com.intel.dai.dsapi.*;

import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.voltdb.client.Client;

import java.sql.Connection;

import static junit.framework.TestCase.assertNotNull;
import static org.mockito.Mockito.mock;

public class DataStoreFactoryImplTest {
    static class MockDataStoreFactoryImpl extends DataStoreFactoryImpl {
        MockDataStoreFactoryImpl(String[] args) {
            super(args, mock(Logger.class));
        }

        @Override
        protected Connection createTier1Connection() throws DataStoreException {
            return mock(Connection.class);
        }

        @Override
        protected Connection createTier2Connection() throws DataStoreException {
            return mock(Connection.class);
        }
    }

    @Before
    public void setUp() {
        String[] args = {"server1,server2"};
        factory = new MockDataStoreFactoryImpl(args);
    }

    @Test
    public void canCreateInventorySnapshotApi() {
        InventorySnapshot api = factory.createInventorySnapshotApi();
        Assert.assertNotEquals(null, api);
    }

    @Test
    public void createDbStatusApiTest() throws DataStoreException {
        DbStatusApi api = factory.createDbStatusApi(mock(Client.class));
        assertNotNull(api);
    }

    @Test
    public void createDataLoaderApiTest() throws DataStoreException {
        DataLoaderApi api = factory.createDataLoaderApi();
        assertNotNull(api);
    }

    @Test
    public void createAdapterOperations() throws Exception {
        AdapterOperations operations = factory.createAdapterOperations(new AdapterInformation("TEST", "TEST",
                "here", "myName", 0L));
    }

    private DataStoreFactoryImpl factory;
}
