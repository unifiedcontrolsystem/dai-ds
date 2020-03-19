// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.exceptions.AdapterException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.perflogging.BenchmarkHelper;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdapterOnlineTierTest {
    class MockAdapterOnlineTier extends AdapterOnlineTier {
        MockAdapterOnlineTier() throws IOException, TimeoutException {
            super(mock(Logger.class));
            initializeAdapter();
            workQueue_ = mock(WorkQueue.class);
            this.workQueue = workQueue_;
            try {
                when(adapter.setUpAdapter(anyString(), anyString(), any())).thenReturn(workQueue);
            } catch(AdapterException e) { /**/ }
        }

        @Override
        void initializeAdapter() {
            adapter = mock(IAdapter.class);
        }

        @Override
        public long sendDataMoverData() throws IOException, TimeoutException, InterruptedException, ProcCallException, ParseException, ConfigIOParseException {
            return 0;
        }

        @Override
        public long handlePurgingData(long lTimeOfLastMovedTier1DataInMillis, long lTimeToKeepMovedDataBeforePurgingInMillis) throws InterruptedException, IOException, ProcCallException {
            return 0;
        }

        private WorkQueue workQueue_;
    }

    @Before
    public void setUp() {
    }

    @Test
    public void basicTests() throws Exception {
        AdapterOnlineTier online = new MockAdapterOnlineTier();
        assertEquals(26, online.setOfTablesToBePurged().size());
        assertEquals(3600000L, online.timeBetweenCheckingForDataToPurgeInMillis());
        assertEquals(86400000L, online.timeToKeepMovedDataBeforePurgingInMillis());
    }

    @Test
    public void mainFlow1() throws Exception {
        AdapterOnlineTier online = new MockAdapterOnlineTier();
        when(online.adapter.adapterShuttingDown()).thenReturn(false,false, false, false, true);
        when(online.workQueue.grabNextAvailWorkItem()).thenReturn(true, true, false);
        when(online.workQueue.workToBeDone()).thenReturn("DataMover", "DataMover", "");
        when(online.workQueue.getClientParameters(ArgumentMatchers.anyString())).thenReturn(new String[] {
                "IntvlBtwnPurgesMs=60", "AddtlTimeToKeepMovedDataBeforePurgeMs=300", "unknown=0"});
        when(online.workQueue.amtTimeToWait()).thenReturn(0, 1);
        online.mainProcessingFlow(new String[] {"127.0.0.1"}, mock(BenchmarkHelper.class));
    }

    @Test
    public void mainFlow2() throws Exception {
        AdapterOnlineTier online = new MockAdapterOnlineTier();
        when(online.adapter.adapterShuttingDown()).thenReturn(false);
        when(online.workQueue.grabNextAvailWorkItem()).thenThrow(new RuntimeException());
        online.mainProcessingFlow(new String[] {}, mock(BenchmarkHelper.class));
    }
}
