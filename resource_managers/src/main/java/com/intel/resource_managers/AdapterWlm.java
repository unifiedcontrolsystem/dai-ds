// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.resource_managers;

import com.intel.dai.AdapterSingletonFactory;
import com.intel.dai.IAdapter;
import com.intel.dai.exceptions.AdapterException;
import com.intel.dai.result.Result;
import com.intel.logging.*;
import com.intel.dai.dsapi.*;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.AdapterInformation;
import org.voltdb.client.*;
import org.voltdb.VoltTable;
import java.lang.*;
import java.io.IOException;
import java.util.*;
import java.text.SimpleDateFormat;

/**
 * AdapterWlm for the VoltDB database - handles the generic functionality needed for a WLM adapter, regardless of which specific job scheduler is employed.
 */
public class AdapterWlm {
    private IAdapter adapter;
    private WorkQueue workQueue;
    private Logger log;
    private WlmProvider provider;
    private Jobs jobs;
    private EventsLog eventlog;
    private AdapterInformation baseAdapter;
    private RasEventLog raseventlog;
    long mTimeLastCheckedStaleData = 0L;  // timestamp that we last checked for stale data in wlm tables.

    // Constructor
    public AdapterWlm(AdapterInformation baseAdapter, WlmProvider provider, DataStoreFactory factory, Logger log) {

        this.baseAdapter = baseAdapter;
        AdapterSingletonFactory.initializeFactory(baseAdapter.getType(), baseAdapter.getName(), log);
        try {
            adapter = AdapterSingletonFactory.getAdapter();
        } catch (IOException e) {
            e.printStackTrace(System.err);
            throw new RuntimeException("Failed to start the base Adapter: " + e.getMessage());
        }
        this.provider = provider;
        this.log = log;
        jobs = factory.createJobApi();
        eventlog = factory.createEventsLog(baseAdapter.getName(), baseAdapter.getType());
        raseventlog = factory.createRasEventLog(baseAdapter);
        workQueue = factory.createWorkQueue(baseAdapter);
    }   // ctor

    /**
     * Start the adapter from the application. This call blocks and all signals are properly handled.
     * @return The code that will be returned to the application.  This integer can be returned by System.exit().
     */
    public int run() {
        return mainProcessingFlow();
    }

    //--------------------------------------------------------------------------
    // This method handles the general processing flow for WLM adapters (regardless of specific implementation, e.g. PBS Pro, Slurm, Cobalt).
    //--------------------------------------------------------------------------
    public int mainProcessingFlow() {
        try {
            log.info("starting");

            //-----------------------------------------------------------------
            // Main processing loop
            //-----------------------------------------------------------------
            while(baseAdapter.isShuttingDown() == false) {
                // Handle any work items that have been queued for this type of adapter.
                boolean bGotWorkItem = workQueue.grabNextAvailWorkItem();
                if (bGotWorkItem == true) {
                    // did get a work item
                    Map<String, String> aWiParms  = workQueue.getClientParameters();
                    switch(workQueue.workToBeDone()) {
                        case "HandleInputFromExternalComponent":
                            //---------------------------------------------------------
                            // Handle input coming in from the "attached" external component (e.g., PBS Pro, Slurm, Cobalt, LSF), looking for things that we are interested in / need to take action on.
                            // Note: This work item is different than most in that this one work item will run for the length of time that the Wlm component is active.
                            //          It does not start and stop, it starts and stays active processing the Wlm component's log for state changes, etc.
                            //---------------------------------------------------------
                        {
                            long rc = provider.handleInputFromExternalComponent(aWiParms);
                        }
                        break;

                        default:
                            workQueue.handleProcessingWhenUnexpectedWorkItem();
                            break;
                    }   // end of switch - workToBeDone()
                }   // did get a work item

                // Sleep for a little bit if no work items are queued for this adapter type.
                if (workQueue.amtTimeToWait() > 0)
                    Thread.sleep( Math.min(workQueue.amtTimeToWait(), 5) * 100);

                // Clean up any stale data that may have accumulated in the InternalJobInfo table.
                cleanupAnyStaleDataInTables();
            }   // End while loop - handle any work items that have been queued for this type of adapter.

            //-----------------------------------------------------------------
            // Clean up adapter table, base work item, and close connections to db.
            //-----------------------------------------------------------------
            adapter.handleMainlineAdapterCleanup(adapter.adapterAbnormalShutdown());
        }   // End try
        catch (Exception e) {
            adapter.handleMainlineAdapterException(e);
        }
        return 0;
    }


    private void finishedWorkingOnWorkItem(Result resultObj) throws IOException {
        if(resultObj.getReturnCode() != 0) {
            workQueue.finishedWorkItemDueToError(workQueue.workToBeDone(), workQueue.workItemId(),
                    resultObj.getMessage());
        }
        else {
            workQueue.finishedWorkItem(workQueue.workToBeDone(), workQueue.workItemId(),
                    resultObj.getMessage());
        }
    }


    //--------------------------------------------------------------------------
    // Clean up any stale data that may have accumulated in any of the Slurm tables.
    // - Currently the only stale data that we are aware of that may need to be periodically cleaned up is:
    //      InternalJobInfo table
    //--------------------------------------------------------------------------
    private long cleanupAnyStaleDataInTables() throws IOException, DataStoreException {
        // Member Data
        final   long StaleDataChkInterval      = 10 * 60 * 1000L;    // number of milliseconds that we want to wait between checks for "stale data".

        if ((System.currentTimeMillis() - mTimeLastCheckedStaleData) >= StaleDataChkInterval) {
            SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS000");
            sqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            log.info("cleanupAnyStaleDataInTables - checking for stale data older than %s",
                    sqlDateFormat.format(new Date( System.currentTimeMillis() - StaleDataChkInterval ))
            );

            //------------------------------------------------------------------
            // Check for stale data in InternalJobInfo table
            // - These entries can occur in the case of the scenario in which jobs are canceled without ever really starting  E.g.,
            //      /var/hit/data/slurmaccounting/slurmctld.log;sched;_slurm_rpc_allocate_resources;JobId=1956 NodeList=(null) usec=279
            //      /var/hit/data/slurmaccounting/slurmctld.log;_slurm_rpc_kill_job2;REQUEST_KILL_JOB;job 1956 uid 60000
            //      /var/hit/data/slurmaccounting/slurmctld.log;_slurm_rpc_kill_job2;REQUEST_KILL_JOB;job 1956 uid 60000
            //------------------------------------------------------------------
            HashMap<Long, String> jobInfo = jobs.checkStaleDataInternal((System.currentTimeMillis() - StaleDataChkInterval) * 1000L);
            long lNumStaleDataEntries = 0L;
            if (jobInfo != null){
                for(Map.Entry<Long, String> staleJob : jobInfo.entrySet()) {
                    String sTempMsg = staleJob.getValue();
                    log.warn("cleanupAnyStaleDataInTables - %s", sTempMsg);
                    Map<String, String> parameters = new HashMap<String, String>();
                    parameters.put("instancedata", "AdapterName=" + baseAdapter.getName() + ", " + sTempMsg);
                    parameters.put("eventtype", raseventlog.getRasEventType("RasWlmStaleDataEntries", workQueue.workItemId()));
                    eventlog.createRasEvent(parameters);

                    ++lNumStaleDataEntries;
                }
            }
            
            mTimeLastCheckedStaleData = System.currentTimeMillis();  // reset last time we checked for stale data.

            if (lNumStaleDataEntries > 0) {
                log.warn("cleanupAnyStaleDataInTables - %d stale data entries were found!", lNumStaleDataEntries);
            } else {
                log.info("cleanupAnyStaleDataInTables - %d stale data entries were found",  lNumStaleDataEntries);
            }

            return lNumStaleDataEntries;  // number of stale data entries that were found.
        }

        return -1;  // interval has not yet expired so we didn't check for any stale data.
    }   // End cleanupAnyStaleDataInTables()


}   // End class AdapterWlm
