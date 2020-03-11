// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.populate;

import com.intel.dai.dsapi.DbStatusApi;
import com.intel.dai.dsapi.DbStatusEnum;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.Before;

import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsapi.DataLoaderApi;
import com.intel.logging.Logger;
import com.intel.dai.exceptions.DataStoreException;
import org.voltdb.client.Client;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

public class OnlineTierDataLoaderTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @Before
    public void setUp() throws Exception {
        mockDsFactory_ = mock(DataStoreFactory.class);
        Logger mockLog = mock(Logger.class);
        servers_ = "localhost";
        manifestFile_ = "SystemManifest.json";
        machineConfigFile_ = "MachineConfig.json";
        rasMetaDataFile_ = "RasEventMetaData.json";
        mockApi_ = mock(DataLoaderApi.class);
        mockStatusApi_ = mock(DbStatusApi.class);
        mockLoader_ = mock(DefaultOnlineTierDataLoader.class);

        dataLoader_ = new OnlineTierDataLoader(mockDsFactory_, servers_, manifestFile_, machineConfigFile_,
                rasMetaDataFile_, mockLog);
        dataLoader_.setDefaultLoader(mockLoader_);
        when(mockDsFactory_.createDbStatusApi(any())).
                thenReturn(mockStatusApi_);
        when(mockStatusApi_.getStatus()).thenReturn(DbStatusEnum.SCHEMA_LOADED);
        System.setProperty("com.intel.dai.populate.OnlineTierDataLoader.timeout", "1"); // One second timeout.
    }

    @Test
    public void populatesFromConfigWhenNearlineTierNotValid() throws Exception {
        dataLoader_.client = mock(Client.class);
        when(mockDsFactory_.createDataLoaderApi()).thenReturn(mockApi_);
        when(mockApi_.isNearlineTierValid()).thenReturn(false); // Load from config
        when(mockLoader_
                .doPopulate(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(0);

        int rc = dataLoader_.populateOnlineTier();

        // Data must NOT have been populated from Nearline tier
        verify(mockApi_, times(0)).populateOnlineTierFromNearlineTier();
        // Data must have been populated from config. Check that correct parameters were specified.
        verify(mockLoader_, times(1))
                .doPopulate(servers_, manifestFile_, machineConfigFile_, rasMetaDataFile_);
        assertEquals(0, rc);
    }

    @Test
    public void handlesDataLoaderPopulateException() throws Exception {
        when(mockDsFactory_.createDataLoaderApi()).thenReturn(mockApi_);
        when(mockApi_.isNearlineTierValid()).thenReturn(true); // Load from Nearline tier
        when(mockLoader_
                .doPopulate(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(0);
        doThrow(new DataStoreException("Ooops!")).when(mockApi_).populateOnlineTierFromNearlineTier();

        int rc = dataLoader_.populateOnlineTier();

        assertEquals(1, rc);
    }

    @Test
    public void handlesDataLoaderNearlineCheckException() throws Exception {
        when(mockDsFactory_.createDataLoaderApi()).thenReturn(mockApi_);
        when(mockApi_.isNearlineTierValid()).thenThrow(new DataStoreException("Ooops!"));
        when(mockLoader_
                .doPopulate(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(0);

        int rc = dataLoader_.populateOnlineTier();

        assertEquals(1, rc);
    }

    @Test
    public void handlesDefaultDataLoaderError() throws Exception {
        when(mockDsFactory_.createDataLoaderApi()).thenReturn(mockApi_);
        when(mockApi_.isNearlineTierValid()).thenReturn(false); // Load from config
        when(mockLoader_
                .doPopulate(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(1); // Error

        int rc = dataLoader_.populateOnlineTier();

        assertEquals(1, rc);
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest1() throws Exception {
        when(mockStatusApi_.getStatus()).thenReturn(DbStatusEnum.SCHEMA_MISSING);
        assertFalse(dataLoader_.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest2() throws Exception {
        when(mockStatusApi_.getStatus()).thenReturn(DbStatusEnum.SCHEMA_LOADING);
        assertFalse(dataLoader_.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest3() throws Exception {
        when(mockStatusApi_.getStatus()).thenReturn(DbStatusEnum.SCHEMA_ERROR);
        assertFalse(dataLoader_.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest4() throws Exception {
        when(mockStatusApi_.getStatus()).thenReturn(DbStatusEnum.SCHEMA_LOADED);
        assertTrue(dataLoader_.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest5() throws Exception {
        when(mockStatusApi_.getStatus()).thenReturn(DbStatusEnum.POPULATE_DATA_STARTED);
        assertFalse(dataLoader_.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest6() throws Exception {
        when(mockStatusApi_.getStatus()).thenReturn(DbStatusEnum.POPULATE_DATA_ERROR);
        assertFalse(dataLoader_.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierTest7() throws Exception {
        when(mockStatusApi_.getStatus()).thenReturn(DbStatusEnum.POPULATE_DATA_COMPLETED);
        assertTrue(dataLoader_.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierNegativeTest1() throws Exception {
        when(mockStatusApi_.getStatus()).thenThrow(DataStoreException.class);
        assertFalse(dataLoader_.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void checkInitialStatusOfOnlineTierNegativeTest2() throws Exception {
        when(mockDsFactory_.createDbStatusApi(any())).thenThrow(DataStoreException.class);
        assertFalse(dataLoader_.checkInitialStatusOfOnlineTier());
    }

    @Test
    public void populateOnlineTierNegativeTest1() throws Exception {
        when(mockDsFactory_.createDbStatusApi(any())).thenThrow(DataStoreException.class);
        assertEquals(1, dataLoader_.populateOnlineTier());
    }

    @Test
    public void populateOnlineTierNegativeTest2() throws Exception {
        when(mockDsFactory_.createDbStatusApi(any())).
                thenReturn(mockStatusApi_).
                thenThrow(DataStoreException.class);
        assertEquals(1, dataLoader_.populateOnlineTier());
    }

    @Test
    public void populateOnlineTierNegativeTest3() throws Exception {
        when(mockDsFactory_.createDataLoaderApi()).thenThrow(DataStoreException.class);
        assertEquals(1, dataLoader_.populateOnlineTier());
    }

    @Test
    public void populateOnlineTierNegativeTest4() throws Exception {
        when(mockDsFactory_.createDataLoaderApi()).thenReturn(mockApi_);
        doThrow(DataStoreException.class).when(mockStatusApi_).setDataPopulationStarting();
        doThrow(DataStoreException.class).when(mockStatusApi_).setDataPopulationFailed(anyString());
        assertEquals(1, dataLoader_.populateOnlineTier());
    }

    @Test
    public void makeExceptionDetailsText() {
        DataStoreException exception = new DataStoreException("TEST", new IllegalArgumentException("CAUSE"));
        String result = OnlineTierDataLoader.makeExceptionDetailsText(exception);
        assertTrue(result.contains("TEST"));
        assertTrue(result.contains("CAUSE"));
    }

//    @Test
//    @Ignore
//    public void populateOnlineTierWhenAlreadyDone() throws Exception {
//        when(mockStatusApi_.getStatus()).thenReturn(DbStatusEnum.POPULATE_DATA_COMPLETED);
//        assertEquals(0, dataLoader_.populateOnlineTier());
//    }

    private DataStoreFactory mockDsFactory_;
    private String servers_;
    private String manifestFile_;
    private String machineConfigFile_;
    private String rasMetaDataFile_;
    private DataLoaderApi mockApi_;
    private DbStatusApi mockStatusApi_;
    private DefaultOnlineTierDataLoader mockLoader_;
    private OnlineTierDataLoader dataLoader_;
}