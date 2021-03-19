// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import com.intel.config_io.ConfigIOParseException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.dai.dsapi.WorkQueue;

import org.voltdb.client.*;
import java.lang.*;
import java.util.*;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * AdapterOnlineTier for the VoltDB database.
 *
 * Parms:
 *  List of the db node names, so that this client connects to each of them.
 *  E.g., dbServer1, dbServer2, ...
 *
 * Example invocation:
 *      java AdapterOnlineTier 10.254.89.8  (or java AdapterOnlineTier  - this will default to using localhost)
 */
public abstract class AdapterOnlineTier {
    IAdapter adapter;
    Logger log_;
    WorkQueue workQueue;
    SyncAdapterShutdownHandler shutdownHandler;
    String rabbitMQHost = "localhost";

    // Constructor
    AdapterOnlineTier(Logger logger) throws IOException, TimeoutException {
        log_ = logger;
        shutdownHandler = new SyncAdapterShutdownHandler(log_);
        initializeAdapter();
        mAmtTimeBetweenCheckingForDataToPurgeInMillis =  3600000L;  // check once an hour.
        mAmtTimeToKeepMovedDataBeforePurgingInMillis  = 86400000L;  // keep data for one day even after the data has been moved to Tier2.
        // Set up the list of tables that we will be purging from.
        // (when changing this list be sure to check the constructor for Adapter objects in Adapter.java and ALSO the method DataMoverGetListOfRecsToMove.java!!!)
        mTablesToBePurgedSet = new HashSet<String>();
        mTablesToBePurgedSet.add("Machine_History");            // Index 0
        mTablesToBePurgedSet.add("Job_History");
        mTablesToBePurgedSet.add("JobStep_History");
        mTablesToBePurgedSet.add("Rack_History");
        mTablesToBePurgedSet.add("Chassis_History");
        mTablesToBePurgedSet.add("ComputeNode_History");
        mTablesToBePurgedSet.add("ServiceNode_History");
        mTablesToBePurgedSet.add("ServiceOperation_History");
        mTablesToBePurgedSet.add("Replacement_History");
        mTablesToBePurgedSet.add("NonNodeHw_History");          // Index 9
        mTablesToBePurgedSet.add("RasEvent");
        mTablesToBePurgedSet.add("WorkItem_History");
        mTablesToBePurgedSet.add("Adapter_History");
        mTablesToBePurgedSet.add("BootImage_History");
        mTablesToBePurgedSet.add("Switch_History");
        mTablesToBePurgedSet.add("FabricTopology_History");
        mTablesToBePurgedSet.add("Lustre_History");
        /*mTablesToBePurgedSet.add("RasMetaData");*/    // Note: The RasMetaData table should never be purged, it is being included here (commented out) for completeness, so it matches DataMoverGetListOfRecsToMove, etc.
        mTablesToBePurgedSet.add("WlmReservation_History");
        mTablesToBePurgedSet.add("Diag_History");
        mTablesToBePurgedSet.add("MachineAdapterInstance_History");
        /*mTablesToBePurgedSet.add("UcsConfigValue");*/ // Note: The UcsConfigValue table should never be purged, it is being included here (commented out) for completeness, so it matches DataMoverGetListOfRecsToMove, etc.
        /*mTablesToBePurgedSet.add("UniqueValues");*/   // Note: The UniqueValues table should never be purged, it is being included here (commented out) for completeness, so it matches DataMoverGetListOfRecsToMove, etc.
        /*mTablesToBePurgedSet.add("Diag_Tools");*/     // Note: The Diag_Tools table should never be purged, it is being included here (commented out) for completeness, so it matches DataMoverGetListOfRecsToMove, etc.
        /*mTablesToBePurgedSet.add("Diag_List");*/      // Note: The Diag_List table should never be purged, it is being included here (commented out) for completeness, so it matches DataMoverGetListOfRecsToMove, etc.
        mTablesToBePurgedSet.add("DiagResults");
        mTablesToBePurgedSet.add("NodeInventory_History");      // Index 26
        mTablesToBePurgedSet.add("NonNodeHwInventory_History"); // Index 27
        /*mTablesToBePurgedSet.add("Constraint");*/     // Note: The Constraint table should never be purged, it is being included here (commented out) for completeness, so it matches DataMoverGetListOfRecsToMove, etc.
        mTablesToBePurgedSet.add("Dimm_History");               // Index 29
        mTablesToBePurgedSet.add("Processor_History");          // Index 30
        mTablesToBePurgedSet.add("Accelerator_History");        // Index 31
        mTablesToBePurgedSet.add("Hfi_History");                // Index 32
        mTablesToBePurgedSet.add("RawHWInventory_History");     // Index 33
    }   // ctor

    void initializeAdapter() throws IOException, TimeoutException {
        adapter = AdapterSingletonFactory.getAdapter();
    }

    // Member data
    private HashSet<String> mTablesToBePurgedSet;               // set of Tier1 tables that should be purged from.
    private long mAmtTimeBetweenCheckingForDataToPurgeInMillis; // amount of time (in millisecs) between performing checks for data to purge, e.g., 1 hour * 60 minutes * 60 secs * 1000 millisecs = 3,600,000 millisecs
    private long mAmtTimeToKeepMovedDataBeforePurgingInMillis;  // amount of additional time (in millisecs) that we should retain the data in Tier1 tables AFTER it has been moved to Tier2, e.g., 24 hours * 60 minutes * 60 seconds * 1000 millisecs = 86,400,000 millisecs.

    // Class methods
    final HashSet<String> setOfTablesToBePurged()          { return mTablesToBePurgedSet; }
    final long timeBetweenCheckingForDataToPurgeInMillis() { return mAmtTimeBetweenCheckingForDataToPurgeInMillis; }
    final long timeToKeepMovedDataBeforePurgingInMillis()  { return mAmtTimeToKeepMovedDataBeforePurgingInMillis; }

    //---------------------------------------------------------
    // Handle moving the historical data from Tier1 to Tier2.
    // Note 1: this is also the mechanism that supports the ability for subscriptions to data store changes.
    // Note 2: this work item is different than most in that it runs for the length of time that this adapter instance is active.
    //          It does not start and then finish, it starts and stays active handling any data that needs to move from Tier1 to Tier2.
    //---------------------------------------------------------
    public abstract long sendDataMoverData() throws IOException, TimeoutException, InterruptedException, ProcCallException, java.text.ParseException, ConfigIOParseException;

    //---------------------------------------------------------
    // Handle purging of data from appropriate Tier1 tables.
    // - Can't purge data if it has not yet been moved to Tier2 (need to be sure that a copy persists elsewhere before purging it from Tier1)
    // - Can only purge from historical tables (e.g., ComputeNode_History table), can't purge from current/active tables (e.g., ComputeNode table)
    // - Need to keep data in Tier1 for an interval of time (lTimeToKeepMovedDataBeforePurgingInMillis) even after it has been moved to Tier2.
    // Parms:
    //      long lTimeOfLastMovedTier1DataInMillis          - the timestamp (in number of millisecs) of the last historical data that has been moved to Tier2.
    //      long lTimeToKeepMovedDataBeforePurgingInMillis  - the number of millisecs that we want to also retain the data in Tier1 even AFTER it has been moved to Tier2.
    //---------------------------------------------------------
    public abstract long handlePurgingData(long lTimeOfLastMovedTier1DataInMillis, long lTimeToKeepMovedDataBeforePurgingInMillis) throws InterruptedException, IOException, ProcCallException;


    //--------------------------------------------------------------------------
    // This method handles the general processing flow for NearlineTier adapters (regardless of specific implementation, e.g. VoltDB).
    //--------------------------------------------------------------------------
    public void mainProcessingFlow(String[] args) throws IOException, TimeoutException {
        try {
            log_.info("starting");

            // Get list of VoltDb servers, location of service node this adapter is running on, and service node's hostname.
            final String DbServers  = (args.length >= 1) ? args[0] : "localhost";  // e.g., voltdbserver1,voltdbserver2,10.11.12.13
            final String SnLctn     = (args.length >= 2) ? args[1] : "UnknownLctn";
            final String SnHostname = (args.length >= 3) ? args[2] : "UnknownHostName";
            log_.info("this adapter instance is running on lctn=%s, hostname=%s, pid=%d", SnLctn, SnHostname, adapter.pid());
            // Set up the adapter instance.
            workQueue = adapter.setUpAdapter(DbServers, SnLctn, shutdownHandler);

            //-----------------------------------------------------------------
            // Main processing loop
            //-----------------------------------------------------------------
            while(adapter.adapterShuttingDown() == false) {
                // Handle any work items that have been queued for this type of adapter.
                boolean bGotWorkItem = workQueue.grabNextAvailWorkItem();
                if (bGotWorkItem == true) {
                    // did get a work item
                    String[] aWiParms = workQueue.getClientParameters(",");
                    switch(workQueue.workToBeDone()) {
                        case "DataMover":
                            processClientParams(aWiParms);
                            //---------------------------------------------------------
                            // Handle moving the historical data from Tier1 to Tier2.
                            // Note 1: this is also the mechanism that supports the ability for subscriptions to data store changes.
                            // Note 2: this work item is different than most in that it runs for the length of time that this adapter instance is active.
                            //          It does not start and then finish, it starts and stays active handling any data that needs to move from Tier1 to Tier2.
                            //---------------------------------------------------------
                            long rc = sendDataMoverData();
                            break;

                        default:
                            workQueue.handleProcessingWhenUnexpectedWorkItem();
                            break;
                    }   // end of switch - workToBeDone()
                }   // did get a work item

                // Sleep for a little bit if no work items are queued for this adapter type.
                if (workQueue.amtTimeToWait() > 0 && !adapter.adapterShuttingDown())
                    Thread.sleep( Math.min(workQueue.amtTimeToWait(), 5) * 100);
            }   // End while loop - handle any work items that have been queued for this type of adapter.

            // Complete the shutdown process and allow the Adapter teardown to complete...
            shutdownHandler.signalShutdownComplete();

            //-----------------------------------------------------------------
            // Clean up adapter table, base work item, and close connections to db.
            //-----------------------------------------------------------------
            adapter.handleMainlineAdapterCleanup(adapter.adapterAbnormalShutdown());
            return;
        }   // End try
        catch (Exception e) {
            adapter.handleMainlineAdapterException(e);
        }
    }   // End mainProcessingFlow(String[] args)

    void processClientParams(String[] aWiParms) {
        for (String sParm : aWiParms) {
            if (sParm.startsWith("IntvlBtwnPurgesMs=")) {
                mAmtTimeBetweenCheckingForDataToPurgeInMillis = Long.parseLong( sParm.substring(sParm.indexOf("=")+1) );
                log_.info("DataMover - setting AmtTimeBetweenCheckingForDataToPurgeInMillis to %d millisecs", mAmtTimeBetweenCheckingForDataToPurgeInMillis);
            }
            else if (sParm.startsWith("AddtlTimeToKeepMovedDataBeforePurgeMs=")) {
                mAmtTimeToKeepMovedDataBeforePurgingInMillis = Long.parseLong( sParm.substring(sParm.indexOf("=")+1) );
                log_.info("DataMover - setting AmtTimeToKeepMovedDataBeforePurgingInMillis to %d millisecs", mAmtTimeToKeepMovedDataBeforePurgingInMillis);
            }
            else if (sParm.startsWith("RabbitMQHost=")) {
                rabbitMQHost = sParm.substring(sParm.indexOf("=")+1).trim();
                log_.info("DataMover - setting rabbitMQHost to %s", rabbitMQHost);
            }
        }

    }

}   // End class AdapterOnlineTier