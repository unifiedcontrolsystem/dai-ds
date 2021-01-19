// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.dsapi.WorkQueue;
import com.intel.dai.exceptions.AdapterException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.voltdb.client.ProcCallException;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AdapterNearlineTierTest {
    static class MockAdapterNearlineTier extends AdapterNearlineTier {
        MockAdapterNearlineTier() throws IOException, TimeoutException {
            super(mock(Logger.class));
            workQueue_ = mock(WorkQueue.class);
            this.workQueue = workQueue_;
            try {
                when(adapter.setUpAdapter(anyString(), anyString())).thenReturn(workQueue_);
            } catch(AdapterException e) {

            }
        }

        @Override
        public long receiveDataFromDataMover() throws InterruptedException, IOException, ProcCallException, TimeoutException {
            return 0;
        }

        @Override
        protected void initializeAdapter() throws IOException, TimeoutException {
            adapter = mock(IAdapter.class);
        }

        private WorkQueue workQueue_;
    }


    @Before
    public void setUp() {
    }

    @Test
    public void construction() throws Exception {
        AdapterNearlineTier nearline = new MockAdapterNearlineTier();
    }

    @Test
    public void mainFlow1() throws Exception {
        AdapterNearlineTier nearline = new MockAdapterNearlineTier();
        when(nearline.adapter.adapterShuttingDown()).thenReturn(false,false, false, false, true);
        when(nearline.workQueue.grabNextAvailWorkItem()).thenReturn(true, true, false);
        when(nearline.workQueue.workToBeDone()).thenReturn("DataReceiver", "DataReceiver", "");
        when(nearline.workQueue.getClientParameters(ArgumentMatchers.anyString())).thenReturn(new String[] {
                "IntvlBtwnPurgesMs=60", "AddtlTimeToKeepMovedDataBeforePurgeMs=300", "unknown=0"});
        when(nearline.workQueue.amtTimeToWait()).thenReturn(0, 1);
        nearline.mainProcessingFlow(new String[] {"127.0.0.1"});
    }

    @Test
    public void mainFlow2() throws Exception {
        AdapterNearlineTier nearline = new MockAdapterNearlineTier();
        when(nearline.adapter.adapterShuttingDown()).thenReturn(false);
        when(nearline.workQueue.grabNextAvailWorkItem()).thenThrow(new RuntimeException());
        nearline.mainProcessingFlow(new String[] {});
    }
}
