// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.populate;

import com.intel.dai.dsapi.DbStatusApi;
import com.intel.dai.dsapi.DbStatusEnum;
import com.intel.logging.LoggerFactory;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.Before;
import org.junit.Assert;
import org.junit.Ignore;
import org.mockito.Mockito;

import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.DataLoaderApi;
import com.intel.logging.Logger;
import com.intel.dai.exceptions.DataStoreException;
import org.voltdb.client.Client;

public class OnlineTierDataLoaderTest {
    @BeforeClass
    public static void setUpClass() {
        LoggerFactory.getInstance("TEST", "TEST", "console");
    }

    @Before
    public void setUp() throws Exception {
        mockDsFactory = Mockito.mock(DataStoreFactory.class);
        Logger mockLog = Mockito.mock(Logger.class);
        servers = "localhost";
        manifestFile = "SystemManifest.json";
        machineConfigFile = "MachineConfig.json";
        rasMetaDataFile = "RasEventMetaData.json";
        mockApi = Mockito.mock(DataLoaderApi.class);
        mockStatusApi = Mockito.mock(DbStatusApi.class);
        mockLoader = Mockito.mock(DefaultOnlineTierDataLoader.class);

        dataLoader = new OnlineTierDataLoader(mockDsFactory, servers, manifestFile, machineConfigFile,
                rasMetaDataFile, mockLog);
        dataLoader.setDefaultLoader(mockLoader);
        Mockito.when(mockDsFactory.createDbStatusApi(Mockito.any())).
                thenReturn(mockStatusApi);
        Mockito.when(mockStatusApi.getStatus()).thenReturn(DbStatusEnum.SCHEMA_LOADED);
        System.setProperty("com.intel.dai.populate.OnlineTierDataLoader.timeout", "1"); // One second timeout.
    }
//
//    @Test
//    public void populatesFromNearlineTierWhenValid() throws Exception {
//        dataLoader.client = Mockito.mock(Client.class);
//        Mockito.when(mockDsFactory.createDataLoaderApi()).thenReturn(mockApi);
//        Mockito.when(mockApi.isNearlineTierValid()).thenReturn(true); // Load from Nearline tier
//        Mockito.when(mockLoader
//                .doPopulate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
//                .thenReturn(0);
//
//        int rc = dataLoader.populateOnlineTier();
//
//        // Data must have been populated from Nearline tier
//        Mockito.verify(mockApi, Mockito.times(1)).populateOnlineTierFromNearlineTier();
//        // Data must NOT have been loaded from config
//        Mockito.verify(mockLoader, Mockito.times(0))
//                .doPopulate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString());
//        Assert.assertEquals(0, rc);
//    }

    @Test
    public void populatesFromConfigWhenNearlineTierNotValid() throws Exception {
        dataLoader.client = Mockito.mock(Client.class);
        Mockito.when(mockDsFactory.createDataLoaderApi()).thenReturn(mockApi);
        Mockito.when(mockApi.isNearlineTierValid()).thenReturn(false); // Load from config
        Mockito.when(mockLoader
                .doPopulate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(0);

        int rc = dataLoader.populateOnlineTier();

        // Data must NOT have been populated from Nearline tier
        Mockito.verify(mockApi, Mockito.times(0)).populateOnlineTierFromNearlineTier();
        // Data must have been populated from config. Check that correct parameters were specified.
        Mockito.verify(mockLoader, Mockito.times(1))
                .doPopulate(servers, manifestFile, machineConfigFile, rasMetaDataFile);
        Assert.assertEquals(0, rc);
    }

    @Test
    public void handlesDataLoaderPopulateException() throws Exception {
        Mockito.when(mockDsFactory.createDataLoaderApi()).thenReturn(mockApi);
        Mockito.when(mockApi.isNearlineTierValid()).thenReturn(true); // Load from Nearline tier
        Mockito.when(mockLoader
                .doPopulate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(0);
        Mockito.doThrow(new DataStoreException("Ooops!")).when(mockApi).populateOnlineTierFromNearlineTier();

        int rc = dataLoader.populateOnlineTier();

        Assert.assertEquals(1, rc);
    }

    @Test
    public void handlesDataLoaderNearlineCheckException() throws Exception {
        Mockito.when(mockDsFactory.createDataLoaderApi()).thenReturn(mockApi);
        Mockito.when(mockApi.isNearlineTierValid()).thenThrow(new DataStoreException("Ooops!"));
        Mockito.when(mockLoader
                .doPopulate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(0);

        int rc = dataLoader.populateOnlineTier();

        Assert.assertEquals(1, rc);
    }

    @Test
    public void handlesDefaultDataLoaderError() throws Exception {
        Mockito.when(mockDsFactory.createDataLoaderApi()).thenReturn(mockApi);
        Mockito.when(mockApi.isNearlineTierValid()).thenReturn(false); // Load from config
        Mockito.when(mockLoader
                .doPopulate(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString()))
                .thenReturn(1); // Error

        int rc = dataLoader.populateOnlineTier();

        Assert.assertEquals(1, rc);
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest1() throws Exception {
        Mockito.when(mockStatusApi.getStatus()).thenReturn(DbStatusEnum.SCHEMA_MISSING);
        Assert.assertFalse(dataLoader.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest2() throws Exception {
        Mockito.when(mockStatusApi.getStatus()).thenReturn(DbStatusEnum.SCHEMA_LOADING);
        Assert.assertFalse(dataLoader.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest3() throws Exception {
        Mockito.when(mockStatusApi.getStatus()).thenReturn(DbStatusEnum.SCHEMA_ERROR);
        Assert.assertFalse(dataLoader.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest4() throws Exception {
        Mockito.when(mockStatusApi.getStatus()).thenReturn(DbStatusEnum.SCHEMA_LOADED);
        Assert.assertTrue(dataLoader.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest5() throws Exception {
        Mockito.when(mockStatusApi.getStatus()).thenReturn(DbStatusEnum.POPULATE_DATA_STARTED);
        Assert.assertFalse(dataLoader.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest6() throws Exception {
        Mockito.when(mockStatusApi.getStatus()).thenReturn(DbStatusEnum.POPULATE_DATA_ERROR);
        Assert.assertFalse(dataLoader.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest7() throws Exception {
        Mockito.when(mockStatusApi.getStatus()).thenReturn(DbStatusEnum.POPULATE_DATA_COMPLETED);
        Assert.assertTrue(dataLoader.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierNegativeTest1() throws Exception {
        Mockito.when(mockStatusApi.getStatus()).thenThrow(DataStoreException.class);
        Assert.assertFalse(dataLoader.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierNegativeTest2() throws Exception {
        Mockito.when(mockDsFactory.createDbStatusApi(Mockito.any())).thenThrow(DataStoreException.class);
        Assert.assertFalse(dataLoader.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void populateOnlineTierNegativeTest1() throws Exception {
        Mockito.when(mockDsFactory.createDbStatusApi(Mockito.any())).thenThrow(DataStoreException.class);
        Assert.assertEquals(1, dataLoader.populateOnlineTier());
    }

    @Test
    public void populateOnlineTierNegativeTest2() throws Exception {
        Mockito.when(mockDsFactory.createDbStatusApi(Mockito.any())).
                thenReturn(mockStatusApi).
                thenThrow(DataStoreException.class);
        Assert.assertEquals(1, dataLoader.populateOnlineTier());
    }

    @Test
    public void populateOnlineTierNegativeTest3() throws Exception {
        Mockito.when(mockDsFactory.createDataLoaderApi()).thenThrow(DataStoreException.class);
        Assert.assertEquals(1, dataLoader.populateOnlineTier());
    }

    @Test
    public void populateOnlineTierNegativeTest4() throws Exception {
        Mockito.when(mockDsFactory.createDataLoaderApi()).thenReturn(mockApi);
        Mockito.doThrow(DataStoreException.class).when(mockStatusApi).setDataPopulationStarting();
        Mockito.doThrow(DataStoreException.class).when(mockStatusApi).setDataPopulationFailed(Mockito.anyString());
        Assert.assertEquals(1, dataLoader.populateOnlineTier());
    }

    private DataStoreFactory mockDsFactory;
    private String servers;
    private String manifestFile;
    private String machineConfigFile;
    private String rasMetaDataFile;
    private DataLoaderApi mockApi;
    private DbStatusApi mockStatusApi;
    private DefaultOnlineTierDataLoader mockLoader;
    private OnlineTierDataLoader dataLoader;
}