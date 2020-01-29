// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Arrays;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary to indicate that a work item has "finished due to an error".
 *  This happens when there is nothing more that the working adapter can due for this work item, so it is finished BUT it did not finish successfully!
 *  Rather it finished due to an error, or possibly finished with an error...
 *
 *  Input parameter:
 *     String sWorkingAdapterType = The type of adapter that handles this type of work item.
 *     long   lWorkItemId         = The work item id that identifes the work item that is finished.
 *     string sWorkItemResults    = The results for this work item, may consist of JSON data.
 *
 *
 */

public class WorkItemFinishedDueToError extends VoltProcedure {


    static final String selectWorkItem = "SELECT " +
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


    static final String deleteWorkItem = "DELETE FROM WorkItem WHERE WorkingAdapterType = ? AND Id = ?;";
    public final SQLStmt deleteWorkItemSql = new SQLStmt(deleteWorkItem);


    public final SQLStmt updateFinishedDueToErrorWorkItemSql = new SQLStmt("UPDATE WorkItem SET Results = ?, WorkingResults = NULL, DbUpdatedTimestamp = CURRENT_TIMESTAMP, State = ? WHERE WorkingAdapterType = ? AND Id = ?;");



    public long run(String sWorkingAdapterType, long lWorkItemId, String sWorkItemResults) throws VoltAbortException {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectWorkItemSql, EXPECT_ONE_ROW, sWorkingAdapterType, lWorkItemId);
        VoltTable[] aWorkItemData = voltExecuteSQL();
        aWorkItemData[0].advanceRow();

        // Ensure that this WorkItem is currently in Working state (can only transition into Finished state from Working state).
        if (!aWorkItemData[0].getString("State").equals("W")) {
            throw new VoltAbortException("WorkItemFinishedDueToError - unable to change WorkItem " + lWorkItemId + " to FinishedDueToError state due to incompatible State value (" + aWorkItemData[0].getString("State") + ")");
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

            //---------------------------------------------------------------------
            // Since this invocation finished due to an error, do an extra step (over the normal finish processing flow) and
            // also put the FinishedDueToError record in the history table (so that the information that this occurred is explicitly available)!
            //---------------------------------------------------------------------
            voltQueueSQL(insertWorkItemHistorySql,
                         aWorkItemData[0].getString("Queue"),
                         sWorkingAdapterType,
                         lWorkItemId,
                         aWorkItemData[0].getString("WorkToBeDone"),
                         aWorkItemData[0].getString("Parameters"),
                         aWorkItemData[0].getString("NotifyWhenFinished"),
                         "E",  // State = FinishedDueToError
                         aWorkItemData[0].getLong("RequestingWorkItemId"),
                         aWorkItemData[0].getString("RequestingAdapterType"),
                         aWorkItemData[0].getLong("WorkingAdapterId"),
                         aWorkItemData[0].getString("WorkingResults"),
                         sWorkItemResults,
                         aWorkItemData[0].getTimestampAsTimestamp("StartTimestamp"),
                         this.getTransactionTime()  // Get CURRENT_TIMESTAMP or NOW
                        );

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
                         "D",  // State = Done
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
            voltQueueSQL(updateFinishedDueToErrorWorkItemSql, EXPECT_ONE_ROW, sWorkItemResults, "E", sWorkingAdapterType, lWorkItemId);

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
                         "E",   // State = Finished due to Error
                         aWorkItemData[0].getLong("RequestingWorkItemId"),
                         aWorkItemData[0].getString("RequestingAdapterType"),
                         aWorkItemData[0].getLong("WorkingAdapterId"),
                         null,  // WorkingResults
                         sWorkItemResults,
                         aWorkItemData[0].getTimestampAsTimestamp("StartTimestamp"),
                         this.getTransactionTime()  // Get CURRENT_TIMESTAMP or NOW
                        );
        }

        voltExecuteSQL(true);

        return 0;
    }

}