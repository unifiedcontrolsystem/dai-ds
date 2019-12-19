// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Arrays;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary to "finish" a specified work item.
 *
 *  Input parameter:
 *     String sWorkingAdapterType = The type of adapter that handles this type of work item.
 *     long   lWorkItemId         = The work item id that identifes the work item that is finished.
 *     string sWorkItemResults    = The results for this work item, may consist of JSON data.
 *
 *
 *  Sample invocation:
 *      echo "Exec WorkItemFinished ONLINE_TIER, 40000000001, Results;" | sqlcmd
 *          Updates the specified work item with the specified results and updates the work item's state as appropriate.
 *              Verifies that the specified work item is currently in Working state
 *              Save "current" values for this entry into the WorkItem_History table (before changing the WorkItem record to reflect these changes)
 *              Updates include:
 *                  - If requester chose NotifyWhenFinished = F
 *                      - Deletes the specified work item row out of the active WorkItem table (since item is now done)
 *                      - Set Results to have the specified results
 *                      - Set WorkingResults to NULL (to remove any intermediate results, since the final results have now been put in Results)
 *                      - Set DbUpdatedTimestamp to have the current timestamp
 *                      - Set State to Done
 *                      - Set EndTimestamp to have the current timestamp
 *                      - Inserts a new row containing these changes into the WorkItem_History table
 *                      - No need to wait for requester to do anything more on this!
 *                  - Else if requester chose NotifyWhenFinished = T
 *                      - Set Results to have the specified results
 *                      - Set WorkingResults to NULL (to remove any intermediate results, since the final results have now been put in Results)
 *                      - Set DbUpdatedTimestamp to have the current timestamp
 *                      - Set State to Finished
 *                      - Update the active work item in the WorkItem table
 */

public class WorkItemFinished extends VoltProcedure {


    final String selectWorkItem = "SELECT " +
        "Queue, WorkToBeDone, Parameters, NotifyWhenFinished, State, RequestingWorkItemId, RequestingAdapterType, WorkingAdapterId, WorkingResults, Results, StartTimestamp, DbUpdatedTimestamp " +
        "FROM WorkItem WHERE WorkingAdapterType = ? AND Id = ? Order By WorkingAdapterType, Id;";
    public final SQLStmt selectWorkItemSql = new SQLStmt(selectWorkItem);


    public final SQLStmt insertWorkItemHistorySql = new SQLStmt(
        "INSERT INTO WorkItem_History " +
        "(Queue, WorkingAdapterType, Id, WorkToBeDone, Parameters, NotifyWhenFinished, State, RequestingWorkItemId, RequestingAdapterType, WorkingAdapterId, WorkingResults, Results, StartTimestamp, DbUpdatedTimestamp, RowInsertedIntoHistory) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'T');"
    );

    public final SQLStmt insertDoneWorkItemIntoHistorySql = new SQLStmt(
        "INSERT INTO WorkItem_History " +
        "(Queue, WorkingAdapterType, Id, WorkToBeDone, Parameters, NotifyWhenFinished, State, RequestingWorkItemId, RequestingAdapterType, WorkingAdapterId, WorkingResults, Results, StartTimestamp, DbUpdatedTimestamp, EndTimestamp, RowInsertedIntoHistory) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'T');"
    );


    final String deleteWorkItem = "DELETE FROM WorkItem WHERE WorkingAdapterType = ? AND Id = ?;";
    public final SQLStmt deleteWorkItemSql = new SQLStmt(deleteWorkItem);


    public final SQLStmt updateFinishedWorkItemSql = new SQLStmt("UPDATE WorkItem SET Results = ?, WorkingResults = NULL, DbUpdatedTimestamp = CURRENT_TIMESTAMP, State = ? WHERE WorkingAdapterType = ? AND Id = ?;");



    public long run(String sWorkingAdapterType, long lWorkItemId, String sWorkItemResults) throws VoltAbortException {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectWorkItemSql, EXPECT_ONE_ROW, sWorkingAdapterType, lWorkItemId);
        VoltTable[] aWorkItemData = voltExecuteSQL();
        aWorkItemData[0].advanceRow();

        // Ensure that this WorkItem is currently in Working state (can only transition into Finished state from Working state).
        if (!aWorkItemData[0].getString("State").equals("W")) {
            throw new VoltAbortException("WorkItemFinished - unable to change WorkItem " + lWorkItemId + " to Finished state due to incompatible State value (" + aWorkItemData[0].getString("State") + ")");
        }

        //---------------------------------------------------------------------
        // Perform appropriate changes to the work item based on whether or not the requester wanted to be notified when this work item finished.
        //---------------------------------------------------------------------
        if (aWorkItemData[0].getString("NotifyWhenFinished").equals("F")) {
            // Requester does NOT want to be notified when this work item finishes
            //--------------------------------------------------
            // Delete the work item's row out of the active WorkItem table (as it is now Done)
            //--------------------------------------------------
            voltQueueSQL(deleteWorkItemSql, EXPECT_ONE_ROW, sWorkingAdapterType, lWorkItemId);
            //--------------------------------------------------
            // Insert the Done record for this work item into the history table (no need to go to Finished state first)
            //--------------------------------------------------
            voltQueueSQL(insertDoneWorkItemIntoHistorySql,
                         aWorkItemData[0].getString("Queue"),
                         sWorkingAdapterType,
                         lWorkItemId,
                         aWorkItemData[0].getString("WorkToBeDone"),
                         aWorkItemData[0].getString("Parameters"),
                         aWorkItemData[0].getString("NotifyWhenFinished"),
                         "D",                                                           // State = Done
                         aWorkItemData[0].getLong("RequestingWorkItemId"),
                         aWorkItemData[0].getString("RequestingAdapterType"),
                         aWorkItemData[0].getLong("WorkingAdapterId"),
                         sWorkItemResults,
                         aWorkItemData[0].getTimestampAsTimestamp("StartTimestamp")
                        );
        }
        else {
            // requester does want to be notified when this work item finishes
            //------------------------------------------------------------------
            // Update the work item with the appropriate information.
            //------------------------------------------------------------------
            voltQueueSQL(updateFinishedWorkItemSql, EXPECT_ONE_ROW, sWorkItemResults, "F", sWorkingAdapterType, lWorkItemId);

            //----------------------------------------------------------------------
            // Insert a "history" record for these updates into the history table (start with pre-change values then overlay with these changes).
            //----------------------------------------------------------------------
            voltQueueSQL(insertWorkItemHistorySql,
                         aWorkItemData[0].getString("Queue"),
                         sWorkingAdapterType,
                         lWorkItemId,
                         aWorkItemData[0].getString("WorkToBeDone"),
                         aWorkItemData[0].getString("Parameters"),
                         aWorkItemData[0].getString("NotifyWhenFinished"),
                         "F",                                                           // State = Finished
                         aWorkItemData[0].getLong("RequestingWorkItemId"),
                         aWorkItemData[0].getString("RequestingAdapterType"),
                         aWorkItemData[0].getLong("WorkingAdapterId"),
                         null,                                                          // WorkingResults
                         sWorkItemResults,
                         aWorkItemData[0].getTimestampAsTimestamp("StartTimestamp"),
                         this.getTransactionTime()                                      // Get CURRENT_TIMESTAMP or NOW
                        );
        }

        voltExecuteSQL(true);

        return 0;
    }

}