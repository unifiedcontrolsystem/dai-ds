// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.*;
import org.voltdb.*;
import java.sql.Timestamp;


/**
 * Handle the database processing that is necessary to find any zombie work items (zombie work items are work items that are "owned" by an adapter that is no longer alive)
 *
 *  Input parameter:
 *     String  sWorkingAdapterType  = Type of adapter this is (used for determining which adapters to look for)
 *
 *  Returns:
 *      VoltTable    vtReturnToCaller = Contains the information on any requeued work items.
 */

public class WorkItemRequeueZombies extends VoltProcedure {


    public final SQLStmt listActiveAdapters = new SQLStmt("SELECT AdapterType, Id FROM Adapter WHERE State = 'A' Order By AdapterType;");


    public final SQLStmt listActiveWorkItems = new SQLStmt(
        "SELECT * FROM WorkItem WHERE State='W' Order By WorkingAdapterType, Id;"
    );


    public final SQLStmt insertWorkItemHistory = new SQLStmt(
        "INSERT INTO WorkItem_History " +
        "(Queue, WorkingAdapterType, Id, WorkToBeDone, Parameters, NotifyWhenFinished, State, RequestingWorkItemId, RequestingAdapterType, WorkingAdapterId, WorkingResults, Results, StartTimestamp, DbUpdatedTimestamp, RowInsertedIntoHistory) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'T');"
    );


    public final SQLStmt requeueWorkItem = new SQLStmt(
        "UPDATE WorkItem SET State = ?, WorkingAdapterId = NULL, DbUpdatedTimestamp = CURRENT_TIMESTAMP WHERE WorkingAdapterType = ? AND Id = ?;"
    );



    public VoltTable run(String sWorkingAdapterType) throws VoltAbortException {
        long lNumRequeuedWorkItems = 0;

        //---------------------------------------------------------------------
        // Get the list of currently active adapters from the Adapter table.
        //---------------------------------------------------------------------
        voltQueueSQL(listActiveAdapters);
        VoltTable[] aActiveAdapters = voltExecuteSQL();
        // Loop through each of the active adapters - building a string of the list of active adapter ids of the specified adapter type
        Map<String, ArrayList<Long>> mapAdaptertypeToArraylistOfActiveAdapters = new HashMap<String, ArrayList<Long>>();
        //System.out.println("\n\nWorkItemRequeueZombies - Looping through VoltTable of active Adapters: number of Adapters=" + aActiveAdapters[0].getRowCount());
        for (int iRowCntr = 0; iRowCntr < aActiveAdapters[0].getRowCount(); ++iRowCntr) {
            aActiveAdapters[0].advanceRow();

            String sAdapterTypeUc = aActiveAdapters[0].getString("AdapterType").toUpperCase();
            long   lAdapterId     = aActiveAdapters[0].getLong("Id");
            //System.out.println("WorkItemRequeueZombies - " + iRowCntr + ")  AdapterType=" + sAdapterTypeUc + ", AdapterId=" + lAdapterId);

            // Update the array list of active adapter ids for this type of adapter.
            ArrayList<Long> alActiveAdapOfThisType = mapAdaptertypeToArraylistOfActiveAdapters.get(sAdapterTypeUc);
            if (alActiveAdapOfThisType == null) {
                // no array list for this working adapter type yet.
                ArrayList<Long> alNewArrayList = new ArrayList<Long>();
                alNewArrayList.add(lAdapterId);
                //System.out.println("WorkItemRequeueZombies - putting entry into the map - " + iRowCntr + ")  AdapterType=" + sAdapterTypeUc + ", alNewArrayList=" + alNewArrayList.toString());
                mapAdaptertypeToArraylistOfActiveAdapters.put(sAdapterTypeUc, alNewArrayList);
            }
            else {
                // the array list does already exist.
                alActiveAdapOfThisType.add(lAdapterId);
                //System.out.println("WorkItemRequeueZombies - adding to the ArrayList for an existing map entry - " + iRowCntr + ")  AdapterType=" + sAdapterTypeUc + ", alNewArrayList=" + alActiveAdapOfThisType.toString());
            }
        }
        //// Loop through all the entries in the map of active adapter types.
        //System.out.println("\nWorkItemRequeueZombies - Displaying map entries for active adapters: ");
        //for (Map.Entry<String, ArrayList<Long>> pairKeyAndValue : mapAdaptertypeToArraylistOfActiveAdapters.entrySet()) {
        //    // iterate over the entries in the map.
        //    //System.out.println("WorkItemRequeueZombies - Active adapter map entry: " + pairKeyAndValue.getKey() + ", " + pairKeyAndValue.getValue().toString());
        //}

        //---------------------------------------------------------------------
        // Requeue any work items that are zombies
        // (zombie work items are work items that are "owned" by an adapter instance that is no longer alive)
        //---------------------------------------------------------------------
        // Get the list of "active" work items so we can compare that against the list of "alive" adapter instances.
        voltQueueSQL(listActiveWorkItems);
        VoltTable[] aActiveWorkItems = voltExecuteSQL();
        // Loop through each of the "active" work items - compare its data against the list of "alive" adapter instances
        // so we can find any zombies (work items that appear to be being worked on by a "dead" adapter instance).
        //System.out.println("\nWorkItemRequeueZombies - Looping through VoltTable of active WorkItems: number of WorkItems=" + aActiveWorkItems[0].getRowCount());
        // Create the VoltTable that will be returned to the caller - it will contain the info for each re-queued workitem.
        VoltTable vtReturnToCaller = new VoltTable(new VoltTable.ColumnInfo("WorkitemId",                   VoltType.BIGINT)
                                                  ,new VoltTable.ColumnInfo("WorkitemWorkingAdapterType",   VoltType.STRING)
                                                  ,new VoltTable.ColumnInfo("WorkitemWorkingAdapterId",     VoltType.BIGINT)
                                                  ,new VoltTable.ColumnInfo("WorkitemWorkToBeDone",         VoltType.STRING)
                        );
        for (int iWrkItmCntr = 0; iWrkItmCntr < aActiveWorkItems[0].getRowCount(); ++iWrkItmCntr) {
            aActiveWorkItems[0].advanceRow();
            //------------------------------------------------------------------
            // Check & see if there is an alive adapter that matches this WorkItem's WorkingAdapterType and WorkingAdapterId.
            //------------------------------------------------------------------
            // Get pertinent info out of this active work item.
            String sWiWorkingAdapterType = aActiveWorkItems[0].getString("WorkingAdapterType");
            long   lWiWorkingAdapterId   = aActiveWorkItems[0].getLong("WorkingAdapterId");
            long   lWiId                 = aActiveWorkItems[0].getLong("Id");
            // Get the array list that has the list of active adapters for the specified type of adapter.
            ArrayList<Long> alTempArraylistOfActiveAdapters = mapAdaptertypeToArraylistOfActiveAdapters.get(sWiWorkingAdapterType);
            if (alTempArraylistOfActiveAdapters != null) {
                // Check & see if the work items "owning" adapter is still active.
                if (alTempArraylistOfActiveAdapters.contains(lWiWorkingAdapterId)) {
                    // this WorkItem's adapter is still alive - nothing more to do for this work item, skip to the next work item.
                    //System.out.println("WorkItemRequeueZombies - the map entry for " + sWiWorkingAdapterType + " did     contain WorkingAdapterId of " + lWiWorkingAdapterId + " - WorkItem = " + lWiId + ", WorkingAdapterType = " + sWiWorkingAdapterType + ", WorkingAdapterId = " + lWiWorkingAdapterId);
                    continue;
                }
                else {
                    //System.out.println("WorkItemRequeueZombies - the map entry for " + sWiWorkingAdapterType + " did NOT contain WorkingAdapterId of " + lWiWorkingAdapterId + " - WorkItem = " + lWiId + ", WorkingAdapterType =" + sWiWorkingAdapterType + ", WorkingAdapterId = " + lWiWorkingAdapterId);
                }
            }
            else {
                //System.out.println("WorkItemRequeueZombies - there was NO map entry for " + sWiWorkingAdapterType + " so it could not contain WorkingAdapterId of " + lWiWorkingAdapterId + " - WorkItem = " + lWiId + ", WorkingAdapterType =" + sWiWorkingAdapterType + ", WorkingAdapterId = " + lWiWorkingAdapterId);
            }

            //------------------------------------------------------------------
            // The WorkItem's adapter is NOT alive - Requeue the WorkItem!
            //------------------------------------------------------------------
            //System.out.println("WorkItemRequeueZombies - this work items adapter is DEAD - WorkItem = " + lWiId + ", WorkingAdapterType =" + sWiWorkingAdapterType + ", WorkingAdapterId = " + lWiWorkingAdapterId);
            // Insert the requeued WorkItem record into the history table (start with pre-change values then overlay with these changes).
            voltQueueSQL(insertWorkItemHistory
                        ,aActiveWorkItems[0].getString("Queue")
                        ,sWiWorkingAdapterType                                  // WorkingAdapterType
                        ,lWiId                                                  // WorkItemId
                        ,aActiveWorkItems[0].getString("WorkToBeDone")
                        ,aActiveWorkItems[0].getString("Parameters")
                        ,aActiveWorkItems[0].getString("NotifyWhenFinished")
                        ,"R"                                                    // State = Requeued
                        ,aActiveWorkItems[0].getLong("RequestingWorkItemId")
                        ,aActiveWorkItems[0].getString("RequestingAdapterType")
                        ,null                                                   // WorkingAdapterId
                        ,aActiveWorkItems[0].getString("WorkingResults")
                        ,aActiveWorkItems[0].getString("Results")
                        ,aActiveWorkItems[0].getTimestampAsTimestamp("StartTimestamp")
                        ,this.getTransactionTime()                              // Get CURRENT_TIMESTAMP or NOW
                        );
            // Requeue the WorkItem - update the WorkItem's values in the active WorkItem table.
            voltQueueSQL(requeueWorkItem, "R", sWiWorkingAdapterType, lWiId);
            // Add the info on this requeued work item into the VoltTable that will be returned to the caller.
            vtReturnToCaller.addRow(lWiId, sWiWorkingAdapterType, lWiWorkingAdapterId, aActiveWorkItems[0].getString("WorkToBeDone"));
            ++lNumRequeuedWorkItems;
        }
        voltExecuteSQL(true);

        // Returns the number of work items that we requeued due to dead adapters.
        //System.out.println("WorkItemRequeueZombies - requeued " + lNumRequeuedWorkItems + " work items!");
        return vtReturnToCaller;
    }
}
