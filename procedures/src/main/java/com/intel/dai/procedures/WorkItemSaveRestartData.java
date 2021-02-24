// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Arrays;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary to store data necessary to be able to restart this work item in the event of a failure of the adapter "working on" this work item.
 *
 *  Input parameter:
 *      String  sWorkingAdapterType         = The type of adapter that handles this type of work item.
 *      long    lWorkItemId                 = The work item id that identifies the work item that the restart data should be saved for.
 *      String  sWorkingResults             = The intermediate working results for this work item, may consist of JSON data.
 *      byte    flagInsertRowIntoHistory    = Flag indicating whether we should insert OR update a history record for the value of this work item's db row when updating the workitem for this particular invocation!
 *                                              - 1 means to insert another workitem history record, rather than doing an updated of the existing workitem's history record - this is the "usual" flow
 *                                              - 0 means to update this workitem's history record, rather than doing an insert of another workitem history record - this is "unusual" (only used when a workitem is updating its working results fields very often)
 *                                              Note: the only reason to not do an insert is when the updates are very numerous and very frequent
 *                                              (e.g., when updating work item entry for MonitorLogFile work item's - these save the timestamp of the last "handled" log message, no need for all the intermediary history of those timestamps)
 *      long lTsInMicroSecs                 = is the timestamp value (in units of microsecs since epoch) that should be used for the DbUpdatedTimestamp field
 *                                              -  0L means that we should use the current timestamp at the time of the db operation - this is "usual" flow
 *                                              - !0L means use specified value during the db operation - this is "UNusual"
 */

public class WorkItemSaveRestartData extends VoltProcedure {


    public final SQLStmt selectWorkItemSql = new SQLStmt(
        "SELECT Queue, WorkToBeDone, Parameters, NotifyWhenFinished, State, RequestingWorkItemId, RequestingAdapterType, WorkingAdapterId, WorkingResults, Results, StartTimestamp, DbUpdatedTimestamp " +
        "FROM WorkItem WHERE WorkingAdapterType = ? AND Id = ? Order By WorkingAdapterType, Id;"
    );

    public final SQLStmt insertWorkItemHistorySql = new SQLStmt(
        "INSERT INTO WorkItem_History " +
        "(Queue, WorkingAdapterType, Id, WorkToBeDone, Parameters, NotifyWhenFinished, State, RequestingWorkItemId, RequestingAdapterType, WorkingAdapterId, WorkingResults, Results, StartTimestamp, DbUpdatedTimestamp, RowInsertedIntoHistory) " +
        "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'T');"
    );

    public final SQLStmt updateWorkItemSql = new SQLStmt(
        "UPDATE WorkItem " +
        "SET WorkingResults=?, DbUpdatedTimestamp=? " +
        "WHERE WorkingAdapterType=? AND Id=?;");

    public final SQLStmt updateWorkItemHistorySql = new SQLStmt(
        "UPDATE WorkItem_History " +
        "SET WorkingResults=?, DbUpdatedTimestamp=?, RowInsertedIntoHistory='F' " +
        "WHERE WorkingAdapterType=? AND Id=? AND WorkingAdapterId=? AND State='W' AND WorkingResults IS NOT NULL;"
    );

    public long run(String sWorkingAdapterType, long lWorkItemId, String sWorkingResults, byte flagInsertRowIntoHistory, long lTsInMicroSecs) throws VoltAbortException {
        // Fill in the current timestamp if it was not specified.
        if (lTsInMicroSecs == 0L)
            lTsInMicroSecs = this.getTransactionTime().getTime() * 1000L;  // get current time in micro-seconds since epoch

        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectWorkItemSql, EXPECT_ONE_ROW, sWorkingAdapterType, lWorkItemId);
        VoltTable[] aWorkItemData = voltExecuteSQL();
        aWorkItemData[0].advanceRow();

        // Ensure that this WorkItem is currently in Working state (can only generate intermediate results while working on a work item).
        if (!aWorkItemData[0].getString("State").equals("W")) {
            throw new VoltAbortException("WorkItemSaveRestartData - unable to save WorkItem " + lWorkItemId + "'s restart data due to incompatible State value (" + aWorkItemData[0].getString("State") + ")");
        }

        //----------------------------------------------------------------------
        // Update the work item record with the new WorkingResults value (also updates the DbUpdatedTimestamp value).
        //----------------------------------------------------------------------
        voltQueueSQL(updateWorkItemSql, EXPECT_ONE_ROW,
                     sWorkingResults, lTsInMicroSecs,
                     sWorkingAdapterType, lWorkItemId);

        //----------------------------------------------------------------------
        // Do the appropriate action in the WorkItem_History table.
        //----------------------------------------------------------------------
        if (flagInsertRowIntoHistory == 1) {
            // insert workitem history record - this is the typical flow.
            //------------------------------------------------------------------
            // Insert a new work item history record into the history table, rather than updating the existing work item history record with the new WorkingResults value
            // (start with pre-change values then overlay those values with the new WorkingResults value and an updated DbUpdatedTimestamp value).
            //------------------------------------------------------------------
            voltQueueSQL(insertWorkItemHistorySql
                        ,aWorkItemData[0].getString("Queue")
                        ,sWorkingAdapterType                                        // WorkingAdapterType
                        ,lWorkItemId                                                // Id
                        ,aWorkItemData[0].getString("WorkToBeDone")
                        ,aWorkItemData[0].getString("Parameters")
                        ,aWorkItemData[0].getString("NotifyWhenFinished")
                        ,aWorkItemData[0].getString("State")
                        ,aWorkItemData[0].getLong("RequestingWorkItemId")
                        ,aWorkItemData[0].getString("RequestingAdapterType")
                        ,aWorkItemData[0].getLong("WorkingAdapterId")
                        ,sWorkingResults                                            // WorkingResults
                        ,aWorkItemData[0].getString("Results")
                        ,aWorkItemData[0].getTimestampAsTimestamp("StartTimestamp")
                        ,lTsInMicroSecs                                             // DbUpdatedTimestamp
                        );
        }   // insert workitem history record - this is the typical flow.
        else {
            // update workitem history record - this is the "unusual" flow.
            String sCurRecsWorkingResults = aWorkItemData[0].getString("WorkingResults");
            if ((sCurRecsWorkingResults == null) || (aWorkItemData[0].wasNull())) {
                // since no proof of life has been put into working results yet, we want to insert a record into history this time anyway
                // as otherwise there is a chance that the workitem history record that transitioned from queued to working may not be moved to
                // Tier2 (depending on the timing of the DataMover cycle) - this should only occur once per work item so it is not a big impact to data move.
                voltQueueSQL(insertWorkItemHistorySql
                            ,aWorkItemData[0].getString("Queue")
                            ,sWorkingAdapterType                                        // WorkingAdapterType
                            ,lWorkItemId                                                // Id
                            ,aWorkItemData[0].getString("WorkToBeDone")
                            ,aWorkItemData[0].getString("Parameters")
                            ,aWorkItemData[0].getString("NotifyWhenFinished")
                            ,aWorkItemData[0].getString("State")
                            ,aWorkItemData[0].getLong("RequestingWorkItemId")
                            ,aWorkItemData[0].getString("RequestingAdapterType")
                            ,aWorkItemData[0].getLong("WorkingAdapterId")
                            ,sWorkingResults                                            // WorkingResults
                            ,aWorkItemData[0].getString("Results")
                            ,aWorkItemData[0].getTimestampAsTimestamp("StartTimestamp")
                            ,lTsInMicroSecs                                             // DbUpdatedTimestamp
                            );
            }
            else {
                // the currently active work item record already has a filled in WorkingResults field so it is ok to just update the workitem's history record.
                //----------------------------------------------------------------------
                // Update the existing work item "history" record with the new WorkingResults value (and the DbUpdatedTimestamp value), rather than inserting a new work item history record into the table.
                //----------------------------------------------------------------------
                voltQueueSQL(updateWorkItemHistorySql, EXPECT_ONE_ROW,
                             sWorkingResults, lTsInMicroSecs,
                             sWorkingAdapterType, lWorkItemId, aWorkItemData[0].getLong("WorkingAdapterId"));
            }


        }   // update workitem history record - this is the "unusual" flow.

        voltExecuteSQL(true);

        return 0;
    }

}