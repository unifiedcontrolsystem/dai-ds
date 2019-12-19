// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import org.voltdb.types.TimestampType;


/**
 * This stored procedure is "Temporary" for use during the DAI Prototype, during the prototype we are using VoltDB to represent the entire data store not just Tier1.
 * The product implementation SHOULD NOT use this, but rather should be getting the data out of Tier2 (Postgres) probably utilizing Distinct On feature
 * for optimizing performance of this function!
 * - this method returns the list of active adapter instances and the work item each is doing at this moment (if any).
 */

public class AdapterListOfActiveAndWorkItems extends VoltProcedure {

    public final SQLStmt selectActiveAdaptersAndTheirWorkItemsSql = new SQLStmt(
        "SELECT Adapter.AdapterType, Adapter.Id, Adapter.Lctn, Adapter.Pid, WorkItem.Queue, WorkItem.Id, WorkItem.WorkingAdapterType, WorkItem.WorkingAdapterId, WorkItem.WorkToBeDone, WorkItem.State, WorkItem.WorkingResults, WorkItem.Parameters " +
        "FROM Adapter LEFT OUTER JOIN WorkItem ON Adapter.AdapterType=WorkItem.WorkingAdapterType " +
        "WHERE (WorkItem.WorkingAdapterId=Adapter.Id) ORDER BY Adapter.AdapterType, Adapter.Id, WorkItem.WorkToBeDone DESC;"
    );

    public final SQLStmt selectAdaptersAndTheirWorkItemsOnlyEndTimeSql = new SQLStmt(
        "SELECT Adapter_History.AdapterType, Adapter_History.Id, Adapter_History.Lctn, Adapter_History.Pid, " +
               "WorkItem_History.Queue, WorkItem_History.Id, WorkItem_History.WorkingAdapterType, WorkItem_History.WorkingAdapterId, WorkItem_History.WorkToBeDone, WorkItem_History.State, WorkItem_History.WorkingResults, WorkItem_History.Parameters " +
        "FROM Adapter_History LEFT OUTER JOIN WorkItem_History ON Adapter_History.AdapterType=WorkItem_History.WorkingAdapterType " +
        "WHERE (WorkItem_History.WorkingAdapterId=Adapter_History.Id AND WorkItem_History.DbUpdatedTimestamp<=?) ORDER BY Adapter_History.AdapterType, Adapter_History.Id, WorkItem_History.WorkToBeDone DESC;"
    );

    public final SQLStmt selectAdaptersAndTheirWorkItemsBothEndAndStartTimeSql = new SQLStmt(
        "SELECT Adapter_History.AdapterType, Adapter_History.Id, Adapter_History.Lctn, Adapter_History.Pid, " +
               "WorkItem_History.Queue, WorkItem_History.Id, WorkItem_History.WorkingAdapterType, WorkItem_History.WorkingAdapterId, WorkItem_History.WorkToBeDone, WorkItem_History.State, WorkItem_History.WorkingResults, WorkItem_History.Parameters " +
        "FROM Adapter_History LEFT OUTER JOIN WorkItem_History ON Adapter_History.AdapterType=WorkItem_History.WorkingAdapterType " +
        "WHERE (WorkItem_History.WorkingAdapterId=Adapter_History.Id AND WorkItem_History.DbUpdatedTimestamp<=? AND WorkItem_History.DbUpdatedTimestamp>=?) ORDER BY Adapter_History.AdapterType, Adapter_History.Id, WorkItem_History.WorkToBeDone DESC;"
    );


    public VoltTable run(TimestampType sEndingTimestamp, TimestampType sStartingTimestamp) throws VoltAbortException {

        // Check & see if the caller specified any timestamp parameters.
        boolean bEndingTsSpecified = false;   // initialize that the ending timestamp was NOT specified.
        boolean bStartingTsSpecified = false; // initialize that the starting timestamp was NOT specified.
        if (sEndingTimestamp != null) {
            // an ending timestamp was specified.
            bEndingTsSpecified = true;
            // Check & see if the caller specified a starting timestamp.
            if (sStartingTimestamp != null)
                bStartingTsSpecified = true;
        }

        // Create the VoltTable that will be returned to the caller.
        VoltTable vtReturnToCaller;

        //----------------------------------------------------------------------
        // Ending time was NOT specified
        //----------------------------------------------------------------------
        if (bEndingTsSpecified == false) {
            //------------------------------------------------------------------
            // Get the list of active adapter instances and the work items (if any) that this adapter is currently working on.
            //------------------------------------------------------------------
            // Queue up the pertinent sql query.
            voltQueueSQL(selectActiveAdaptersAndTheirWorkItemsSql);
        }
        //----------------------------------------------------------------------
        // Both Ending and Starting time were specified
        //----------------------------------------------------------------------
        else if ((bEndingTsSpecified) && (bStartingTsSpecified)) {
            //----------------------------------------------------------------------
            // Get the list of active adapter instances and the work items (if any) that occurred ON OR BETWEEN the specified ending and starting timestamps.
            //  NOTE: This procedure should only be used for the DAI Prototype,
            //        the real product implementation should be done using the history in Tier 2 and should utilize the DISTINCT ON feature of Postgres,
            //        rather than the brute force method utilized here!!!
            //----------------------------------------------------------------------
            // Queue up the pertinent sql query.
            voltQueueSQL(selectAdaptersAndTheirWorkItemsBothEndAndStartTimeSql, sEndingTimestamp, sStartingTimestamp);
        }
        //----------------------------------------------------------------------
        // Only Ending time was specified
        //----------------------------------------------------------------------
        else {
            //----------------------------------------------------------------------
            // Get the list of active adapter instances and the work items (if any) that occurred ON OR BETWEEN the specified ending and starting timestamps.
            //  NOTE: This procedure should only be used for the DAI Prototype,
            //        the real product implementation should be done using the history in Tier 2 and should utilize the DISTINCT ON feature of Postgres,
            //        rather than the brute force method utilized here!!!
            //----------------------------------------------------------------------
            // Queue up the pertinent sql query.
            voltQueueSQL(selectAdaptersAndTheirWorkItemsOnlyEndTimeSql, sEndingTimestamp);
        }
        VoltTable[] aListOfAdaptersAndWorkitems = voltExecuteSQL(true);


        //--------------------------------------------------
        // Build up the information that we want to return to the caller as a VoltTable[].
        //--------------------------------------------------
        // Construct the VoltTable of results that will be returned to the caller.
        vtReturnToCaller = new VoltTable(new VoltTable.ColumnInfo("AdapterType",                VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("AdapterId",                  VoltType.INTEGER)
                                        ,new VoltTable.ColumnInfo("AdapterLctn",                VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("AdapterPid",                 VoltType.INTEGER)
                                        ,new VoltTable.ColumnInfo("WorkItemQueue",              VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("WorkItemId",                 VoltType.INTEGER)
                                        ,new VoltTable.ColumnInfo("WorkItemWorkingAdapterType", VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("WorkItemWorkingAdapterId",   VoltType.INTEGER)
                                        ,new VoltTable.ColumnInfo("WorkItemWorkToBeDone",       VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("WorkItemState",              VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("WorkItemWorkingResults",     VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("WorkItemParameters",         VoltType.STRING)
                                        );

        // Loop through the data that we got back from the query, keeping just one record for each distinct adapter instance.
        final String BaseWorkItemWorkToBeDone  = "BaseWork";
        String sPrevAdapterType                = "";
        long   lPrevAdapterId                  = -99999L;
        String sPrevAdapterLctn                = "";
        long   lPrevAdapterPid                 = -99999L;
        String sPrevWorkItemQueue              = "";
        long   lPrevWorkItemId                 = -99999L;
        String sPrevWorkItemWorkingAdapterType = "";
        long   lPrevWorkItemWorkingAdapterId   = -99999L;
        String sPrevWorkItemWorkToBeDone       = "";
        String sPrevWorkItemState              = "";
        String sPrevWorkItemWorkingResults     = "";
        String sPrevWorkItemParameters         = "";

        for (int iRowCntr = 0; iRowCntr < aListOfAdaptersAndWorkitems[0].getRowCount(); ++iRowCntr) {
            aListOfAdaptersAndWorkitems[0].advanceRow();

            String sAdapterType          = aListOfAdaptersAndWorkitems[0].getString("AdapterType");
            long   lAdapterId            = aListOfAdaptersAndWorkitems[0].getLong(1);               // Adapter.AdapterId

            // Check & see if this is a new adapter instance (that we haven't seen yet).
            if ((lPrevAdapterId != lAdapterId) || (sPrevAdapterType.equals(sAdapterType) == false)) {
                // this is a new adapter instance.
                // Put the "previous" adapter instance's data into the VoltTable that will be returned to the caller
                // (since we got a new adapter instance's data we know that we have processed all the previous adapter instance's data).
                if (iRowCntr != 0) {
                    // Add the previous adapter instance information into the VoltTable.
                    vtReturnToCaller.addRow(sPrevAdapterType
                                           ,lPrevAdapterId
                                           ,sPrevAdapterLctn
                                           ,lPrevAdapterPid
                                           ,sPrevWorkItemQueue
                                           ,lPrevWorkItemId
                                           ,sPrevWorkItemWorkingAdapterType
                                           ,lPrevWorkItemWorkingAdapterId
                                           ,sPrevWorkItemWorkToBeDone
                                           ,sPrevWorkItemState
                                           ,sPrevWorkItemWorkingResults
                                           ,sPrevWorkItemParameters
                                           );
                }
                // Save away this new adapter instance's data (in temporary variables).
                sPrevAdapterType                = aListOfAdaptersAndWorkitems[0].getString("AdapterType");
                lPrevAdapterId                  = aListOfAdaptersAndWorkitems[0].getLong(1);                        // Adapter.Id
                sPrevAdapterLctn                = aListOfAdaptersAndWorkitems[0].getString("Lctn");
                lPrevAdapterPid                 = aListOfAdaptersAndWorkitems[0].getLong("Pid");
                sPrevWorkItemQueue              = aListOfAdaptersAndWorkitems[0].getString("Queue");
                lPrevWorkItemId                 = aListOfAdaptersAndWorkitems[0].getLong(5);                        // WorkItem.Id
                sPrevWorkItemWorkingAdapterType = aListOfAdaptersAndWorkitems[0].getString("WorkingAdapterType");
                lPrevWorkItemWorkingAdapterId   = aListOfAdaptersAndWorkitems[0].getLong("WorkingAdapterId");
                sPrevWorkItemWorkToBeDone       = aListOfAdaptersAndWorkitems[0].getString("WorkToBeDone");
                sPrevWorkItemState              = aListOfAdaptersAndWorkitems[0].getString("State");
                sPrevWorkItemWorkingResults     = aListOfAdaptersAndWorkitems[0].getString("WorkingResults");
                sPrevWorkItemParameters         = aListOfAdaptersAndWorkitems[0].getString("Parameters");
            }   // this is a new adapter instance.
            else {
                // this is a repeat adapter instance.
                // Check and see if this is an "interesting" work item or is it just the adapter instance's base work item id.
                if (!BaseWorkItemWorkToBeDone.equals( aListOfAdaptersAndWorkitems[0].getString("WorkToBeDone") )) {
                    // this is not the adapter instance's base work item, so save away this information.
                    sPrevAdapterType                = aListOfAdaptersAndWorkitems[0].getString("AdapterType");
                    lPrevAdapterId                  = aListOfAdaptersAndWorkitems[0].getLong(1);                        // Adapter.Id
                    sPrevAdapterLctn                = aListOfAdaptersAndWorkitems[0].getString("Lctn");
                    lPrevAdapterPid                 = aListOfAdaptersAndWorkitems[0].getLong("Pid");
                    sPrevWorkItemQueue              = aListOfAdaptersAndWorkitems[0].getString("Queue");
                    lPrevWorkItemId                 = aListOfAdaptersAndWorkitems[0].getLong(5);                        // WorkItem.Id
                    sPrevWorkItemWorkingAdapterType = aListOfAdaptersAndWorkitems[0].getString("WorkingAdapterType");
                    lPrevWorkItemWorkingAdapterId   = aListOfAdaptersAndWorkitems[0].getLong("WorkingAdapterId");
                    sPrevWorkItemWorkToBeDone       = aListOfAdaptersAndWorkitems[0].getString("WorkToBeDone");
                    sPrevWorkItemState              = aListOfAdaptersAndWorkitems[0].getString("State");
                    sPrevWorkItemWorkingResults     = aListOfAdaptersAndWorkitems[0].getString("WorkingResults");
                    sPrevWorkItemParameters         = aListOfAdaptersAndWorkitems[0].getString("Parameters");
                }
                else {
                    // this is the adapter instance's base work item, so do not overlay the information.
                }
            }   // this is a repeat adapter instance.
        }   // loop through the data that we got back from the query, keeping just one record for each distinct adapter instance.

        // Add the last adapter instance's information into the VoltTable (if appropriate).
        if (aListOfAdaptersAndWorkitems[0].getRowCount() > 0) {
            // Add the previous adapter instance information into the VoltTable (as we now know that we have the correct information).
            vtReturnToCaller.addRow(sPrevAdapterType
                                   ,lPrevAdapterId
                                   ,sPrevAdapterLctn
                                   ,lPrevAdapterPid
                                   ,sPrevWorkItemQueue
                                   ,lPrevWorkItemId
                                   ,sPrevWorkItemWorkingAdapterType
                                   ,lPrevWorkItemWorkingAdapterId
                                   ,sPrevWorkItemWorkToBeDone
                                   ,sPrevWorkItemState
                                   ,sPrevWorkItemWorkingResults
                                   ,sPrevWorkItemParameters
                                   );
        }

        //----------------------------------------------------------------------
        // Return the information to the caller.
        //----------------------------------------------------------------------
        return vtReturnToCaller;
    }
}
