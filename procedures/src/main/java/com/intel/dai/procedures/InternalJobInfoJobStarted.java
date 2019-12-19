// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary in the InternalJobInfo table when a job has started.
 * Note: this method must handle the fact that in this environment, time is NOT a stream, i.e., records may show up that are out of time order.
 *
 *  Input parameter:
 *      String  sJobId              = string containing the WLM job id
 *      long    lStartTsInMicroSecs = time this job started in units of micro-seconds since the epoch
 *
 *  Return value:
 *      VoltTable aJobInfo = VoltTable containing all of the JobInfo for this job (after updating it to reflect the fact that the job has started)
 */

public class InternalJobInfoJobStarted extends VoltProcedure {

    public final SQLStmt selectJobInfoBetween = new SQLStmt("SELECT * FROM InternalJobInfo WHERE JobId=? AND (WlmJobStartTime BETWEEN ? AND ?);");
    public final SQLStmt selectJobInfo        = new SQLStmt("SELECT * FROM InternalJobInfo WHERE JobId=? AND WlmJobStartTime=?;");

    public final SQLStmt insertJobInfo = new SQLStmt(
                    "INSERT INTO InternalJobInfo " +
                    "(JobId, WlmJobStarted, WlmJobStartTime, WlmJobEndTime, WlmJobWorkDir, WlmJobCompleted, WlmJobState) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?);"
    );

    public final SQLStmt updateJobInfo = new SQLStmt("UPDATE InternalJobInfo SET WlmJobStarted=?, WlmJobStartTime=? WHERE JobId=? AND WlmJobStartTime=?;");



    public VoltTable run(String sJobId, long lStartTsInMicroSecs) throws VoltAbortException {

        // Determine what start time we should use when looking for this job in the InternalJobInfo table.
        // Note: an existing record should only be from the job completion record which has job timestamps only down to second granularity,
        //  so we need to take this difference into account.
        long lSearchForTsStartLowerBoundInMicroSecs = lStartTsInMicroSecs - (2 * 1000000L);  // go up to 2 seconds earlier when looking for an existing row in the table.
        long lSearchForTsStartUpperBoundInMicroSecs = lStartTsInMicroSecs + (1 * 1000000L);  // go up to 1 seconds later when looking for an existing row in the table.

        // Check & see if there is already a row containing the JobInfo for the specified job.
        voltQueueSQL(selectJobInfoBetween, EXPECT_ZERO_OR_ONE_ROW, sJobId, lSearchForTsStartLowerBoundInMicroSecs, lSearchForTsStartUpperBoundInMicroSecs);
        VoltTable[] aJobInfo = voltExecuteSQL();

        // Update the job's JobInfo.
        if (aJobInfo[0].getRowCount() == 0) {
            // the job's JobInfo does not yet exist.
            // Put a new JobInfo row into the table for this JobId.
            voltQueueSQL(insertJobInfo
                        ,sJobId                 // JobId
                        ,"T"                    // WlmJobStarted is true
                        ,lStartTsInMicroSecs    // WlmJobStartTime - use the start timestamp specified on this call.
                        ,null                   // WlmJobEndTime has not yet been filled in
                        ,null                   // WlmJobWorkDir has not yet been filled in
                        ,"F"                    // WlmJobCompleted is false
                        ,null                   // WlmJobState information
                        );
        }
        else {
            // the job's JobInfo does already exist.
            // Get the original job started timestamp (in the row before these changes occur).
            aJobInfo[0].advanceRow();
            long lRowsOriginalStartTsInMicroSecs = aJobInfo[0].getTimestampAsTimestamp("WlmJobStartTime").getTime();
            // Update the existing JobInfo with the fact that the job has now started.
            voltQueueSQL(updateJobInfo
                        ,"T"                             // WlmJobStarted is true
                        ,lStartTsInMicroSecs             // WlmJobStartTime - go ahead and overlay the job start time with the actual job start time as specified on this call.
                        ,sJobId                          // JobId
                        ,lRowsOriginalStartTsInMicroSecs // Row's original start timestamp before this update.
                        );
        }
        voltExecuteSQL();

        // Reread the newly updated JobInfo (so we have a VoltTable with the information in it to return to the caller).
        voltQueueSQL(selectJobInfo, EXPECT_ONE_ROW, sJobId, lStartTsInMicroSecs);  // now using lStartTsInMicroSecs as we just updated the start time in the row.
        aJobInfo = voltExecuteSQL(true);

        // Return the JobInfo to the caller in the form of a VoltTable.
        return aJobInfo[0];
    }
}