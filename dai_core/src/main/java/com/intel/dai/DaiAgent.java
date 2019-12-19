// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.exceptions.AdapterException;
import com.intel.dai.exceptions.DaiException;
import com.intel.logging.Logger;
import com.intel.dai.dsapi.WorkQueue;

import java.util.Map;

/**
 * DAI agent: handles the main processing flow and part of the DAI protocol for adapters, allowing adapters to focus on
 * their specific work items.  The main processing flow includes: grabbing the next available work item for the adapter,
 * forwarding work items to the adapter, and performing work item upkeep (re queueing zombie work items, checking and
 * notifying about hung work items) to keep operations running for the adapter.
 *
 * Adapters only require to implement the DaiAgentRole interface and they can be passed to a DaiAgent to run.
 */
public class DaiAgent {
    public DaiAgent(DaiAgentRole role, IAdapter adapter, DataStoreFactory dsFactory, Logger log) {
        this.log = log;
        this.adapter = adapter;
        workQueueApi = adapter.workQueue();
        agentRole = role;
    }

    public void run() throws DaiException {
        log.info("starting...");

        while (!adapter.adapterShuttingDown()) {
            boolean gotWorkItem = grabNextAvailableWorkItem();

            if (gotWorkItem) {
                performWork();
            }

            sleepBeforeNextWorkItem();
        }

        log.info("Shutting down...");
        agentRole.shutDown();
    }

    private void performWork() throws DaiException {
        long workItemId = workQueueApi.workItemId();
        String workToBeDone = workQueueApi.workToBeDone();
        Map<String, String> params = workQueueApi.getClientParameters();

        try {
            log.info("Received a work request: WorkItemId=%d, WorkToBeDone=%s, parameters=%s", workItemId, workToBeDone,
                    params != null ? params.toString() : "");
            agentRole.performWork(workToBeDone, workItemId, params);
        } catch (AdapterException ex) {
            log.exception(ex, "An error occurred while processing work item: WorkItemId=%d, WorkToBeDone=%s, " +
                            "parameters=%s", workItemId, workToBeDone, params != null ? params.toString() : "");
            throw new DaiException("Error occurred while processing work item: " + workQueueApi.workToBeDone(), ex);
        }
    }

    private boolean grabNextAvailableWorkItem() throws DaiException {
        try {
            log.debug("Grabbing next available work item: adapter type: %s, adapter ID: %d, base work item ID: %d",
                    adapter.adapterType(), adapter.adapterId(), workQueueApi.baseWorkItemId());
            return workQueueApi.grabNextAvailWorkItem();
        } catch (Exception ex) {
            log.exception(ex, "An error occurred while attempting to grab a work item");
            throw new DaiException("An error occurred while attempting to grab a work item", ex);
        }
    }

    private void sleepBeforeNextWorkItem() {
        if (workQueueApi.amtTimeToWait() > 0) {
            try {
                Thread.sleep(Math.min(workQueueApi.amtTimeToWait(), 5) * 100);
            } catch (InterruptedException ex) {
                log.warn("Interrupted while sleeping");
            }
        }
    }

    private IAdapter adapter;
    private Logger log;
    private DaiAgentRole agentRole;
    private WorkQueue workQueueApi;
}
