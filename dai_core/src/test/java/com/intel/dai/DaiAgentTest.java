// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.logging.Logger;
import com.intel.dai.dsapi.WorkQueue;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.util.HashMap;

public class DaiAgentTest {
    @Before
    public void setUp() {
        mockAdapter = Mockito.mock(IAdapter.class);
        mockRole = Mockito.mock(DaiAgentRole.class);
        mockLog = Mockito.mock(Logger.class);
        mockDsFactory = Mockito.mock(DataStoreFactory.class);
        mockWorkQueueApi = Mockito.mock(WorkQueue.class);

        Mockito.when(mockDsFactory.createWorkQueue(Mockito.any(AdapterInformation.class))).thenReturn(mockWorkQueueApi);
        Mockito.when(mockAdapter.workQueue()).thenReturn(mockWorkQueueApi);

        agent = new DaiAgent(mockRole, mockAdapter, mockDsFactory, mockLog);
    }

    @Test
    public void initializesBeforeProcessingWorkItems() throws Exception {
        // Don't start the main processing loop (just perform init and setup)
        Mockito.when(mockAdapter.adapterShuttingDown()).thenReturn(true);

        agent.run();
        // Expected init and setup that the agent should perform: TODO: Behavior changed; changed "1" to "0" below.
        Mockito.verify(mockAdapter, Mockito.times(0)).registerAdapter(ArgumentMatchers.anyString());
        Mockito.verify(mockAdapter, Mockito.times(0)).addRasMetaDataIntoDataStore();
    }

    @Test
    public void iteratesUntilAbnormalShutdown() throws Exception {
        // Perform two iterations of the agent loop
        Mockito.when(mockAdapter.adapterShuttingDown())
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);
        // Don't return any work items
        Mockito.when(mockWorkQueueApi.grabNextAvailWorkItem()).thenReturn(false);
        // Don't sleep
        Mockito.when(mockWorkQueueApi.amtTimeToWait()).thenReturn(0);

        agent.run();
        // Expected behavior in agent loop:
        Mockito.verify(mockWorkQueueApi, Mockito.times(2)).grabNextAvailWorkItem();
        Mockito.verify(mockAdapter.workQueue(), Mockito.times(2)).amtTimeToWait(); // Must check if it needs to sleep
    }

    @Test
    public void performsWorkWhenWorkItemAvailable() throws Exception {
        // Perform two iterations of the agent loop
        Mockito.when(mockAdapter.adapterShuttingDown())
                .thenReturn(false)
                .thenReturn(false)
                .thenReturn(true);
        // Return a work item once
        Mockito.when(mockWorkQueueApi.grabNextAvailWorkItem())
                .thenReturn(false)
                .thenReturn(true);
        // Work to be done
        String workToBeDone = "TakeOutTrash";
        HashMap<String, String> params = new HashMap<>();
        params.put("time", "10pm");
        long workItemId = 10;
        Mockito.when(mockWorkQueueApi.workToBeDone()).thenReturn(workToBeDone);
        Mockito.when(mockWorkQueueApi.getClientParameters()).thenReturn(params);
        Mockito.when(mockWorkQueueApi.workItemId()).thenReturn(workItemId);
        // Don't sleep
        Mockito.when(mockWorkQueueApi.amtTimeToWait()).thenReturn(0);

        agent.run();
        // Should perform work only once and pass correct parameters
        Mockito.verify(mockRole, Mockito.times(1)).performWork(workToBeDone, workItemId, params);
    }

    private IAdapter mockAdapter;
    private DaiAgentRole mockRole;
    private Logger mockLog;
    private DataStoreFactory mockDsFactory;
    private WorkQueue mockWorkQueueApi;
    private DaiAgent agent;
}