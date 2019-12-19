// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Arrays;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a work item is done.
 *
 *  Input parameter:
 *     String sWorkingAdapterType = The type of adapter that handles this type of work item.
 *     long   lWorkItemId         = The work item id that identifes the work item that is done.
 *
 *  Returns:
 *      long 0  (this method does not return results - WorkItemFinishedResults are used to get the work item results)
 *
 *
 * echo "Exec AdapterStarted WLM, 1;" | sqlcmd
 * echo "Exec AdapterStarted ONLINE_TIER, 2;" | sqlcmd
 * echo "Select * from Adapter Order By Id;" | sqlcmd
 * echo "Exec WorkItemQueue Session, ONLINE_TIER, SessionAllocate, ParmsGoHere, T, WLM, 40000000000;" | sqlcmd
 * echo "Exec WorkItemFindAndOwn ONLINE_TIER, 11;" | sqlcmd
 * echo "Exec WorkItemFinished ONLINE_TIER, 40000000001, Results;" | sqlcmd
 * echo "Exec WorkItemDone ONLINE_TIER, 40000000001;" | sqlcmd
 * echo "Exec AdapterTerminated ONLINE_TIER, 11;" | sqlcmd
 * echo "Exec AdapterTerminated WLM, 10;" | sqlcmd
 *
 *
 *  Sample invocation:
 *      echo "Exec WorkItemDone ONLINE_TIER, 40000000001;" | sqlcmd
 *          Transitions a work item to done.
 *              Verifies that the specified work item is currently in Finished state
 *              Save "current" values for this entry into the WorkItem_History table (before making any other changes)
 *              Deletes the specified work item row out of the active WorkItem table (since item is now done)
 *              Insert an additional row into the WorkItem_History table, indicating that this item is done
 *                  Set DbUpdatedTimestamp to have the current timestamp
 *                  Set State to Done
 *                  Set EndTimestamp to have the current timestamp
 *              Returns the Results field for this work item
 */

public class WorkItemDone extends VoltProcedure {

    final String selectWorkItem = "SELECT " +
        "Queue, WorkToBeDone, Parameters, NotifyWhenFinished, State, RequestingWorkItemId, RequestingAdapterType, WorkingAdapterId, WorkingResults, Results, StartTimestamp, DbUpdatedTimestamp " +
        "FROM WorkItem WHERE WorkingAdapterType = ? AND Id = ? Order By WorkingAdapterType, Id;";
    public final SQLStmt selectWorkItemSql = new SQLStmt(selectWorkItem);


    public final SQLStmt insertDoneWorkItemIntoHistorySql = new SQLStmt(
        "INSERT INTO WorkItem_History " +
        "(Queue, WorkingAdapterType, Id, WorkToBeDone, Parameters, NotifyWhenFinished, State, RequestingWorkItemId, RequestingAdapterType, WorkingAdapterId, WorkingResults, Results, StartTimestamp, DbUpdatedTimestamp, EndTimestamp, RowInsertedIntoHistory) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 'T');"
    );


    final String deleteWorkItem = "DELETE FROM WorkItem WHERE WorkingAdapterType = ? AND Id = ?;";
    public final SQLStmt deleteWorkItemSql = new SQLStmt(deleteWorkItem);



    public long run(String sWorkingAdapterType, long lWorkItemId) throws VoltAbortException {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectWorkItemSql, EXPECT_ONE_ROW, sWorkingAdapterType, lWorkItemId);
        VoltTable[] aWorkItemData = voltExecuteSQL();
        aWorkItemData[0].advanceRow();

        // Ensure that this WorkItem is currently in Finished state (this procedure can only transition into Done state from one of the Finished state).
        if (!aWorkItemData[0].getString("State").equals("F") &&  // F = Finished successfully
            !aWorkItemData[0].getString("State").equals("E")) {  // E = Finished Due To Error
            throw new VoltAbortException("WorkItemDone - unable to change WorkItem " + lWorkItemId + " to Done state due to incompatible State value (" + aWorkItemData[0].getString("State") + ")");
        }

        //---------------------------------------------------------------------
        // Delete the work item's row out of the active WorkItem table (as it is now Done)
        //---------------------------------------------------------------------
        voltQueueSQL(deleteWorkItemSql, EXPECT_ONE_ROW, sWorkingAdapterType, lWorkItemId);

        //--------------------------------------------------
        // Insert the Done record for this work item into the history table
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
                     aWorkItemData[0].getString("Results"),
                     aWorkItemData[0].getTimestampAsTimestamp("StartTimestamp")
                    );
        voltExecuteSQL(true);
        return 0;
    }
}