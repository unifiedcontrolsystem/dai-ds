// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary in the InternalJobInfo table when a job has completed.
 * Note: this method must handle the fact that in this environment, time is NOT a stream, i.e., records may show up that are out of time order.
 *
 *  Input parameter:
 *      String  sJobId              = string containing the WLM job id
 *      String  sWorkDir            = string containing the job's working directory
 *      String  sWlmJobState        = string containing the job's WLM Job State.
 *      long    lEndTsInMicroSecs   = time this job ended in units of micro-seconds since the epoch
 *      long    lStartTsInMicroSecs = this is the time that this job completion record thinks that the job started, in units of micro-seconds since the epoch
 *
 *  Return value:
 *      VoltTable aJobInfo = VoltTable containing all of the JobInfo for this job (after updating it to reflect the fact that the job has completed)
 */

public class InternalJobInfoJobCompleted extends VoltProcedure {

    public final SQLStmt selectJobInfoBetween = new SQLStmt(
                    "SELECT * FROM InternalJobInfo WHERE JobId=? AND (WlmJobStartTime BETWEEN ? AND ?);"
    );
    public final SQLStmt selectJobInfo = new SQLStmt(
                    "SELECT * FROM InternalJobInfo WHERE JobId=? AND WlmJobStartTime=?;"
    );

    public final SQLStmt insertJobInfo = new SQLStmt(
                    "INSERT INTO InternalJobInfo " +
                    "(JobId, WlmJobStarted, WlmJobStartTime, WlmJobEndTime, WlmJobWorkDir, WlmJobCompleted, WlmJobState) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?);"
    );

    public final SQLStmt updateJobInfo = new SQLStmt("UPDATE InternalJobInfo  SET WlmJobEndTime=?, WlmJobWorkDir=?, WlmJobCompleted=?, WlmJobState=?  WHERE JobId=? AND WlmJobStartTime=?;");



    public VoltTable run(String sJobId, String sWorkDir, String sWlmJobState, long lEndTsInMicroSecs, long lStartTsInMicroSecs) throws VoltAbortException {

        // Determine what time to use when bounding the search for the job (using job start time)
        // Note: an existing record should only be from the job started record which has a job start timestamp down to millisecond granularity,
        //  BUT this for a job completed record which has timestamps only down to second granularity
        //  so we need to add 1 second of time to the specified time (plus a little more for a fudge factor).
        long lSearchForTsEndBoundInMicroSecs = lStartTsInMicroSecs + (1500 * 1000L);

        // Check & see if there is already a row containing the JobInfo for the specified job.
        voltQueueSQL(selectJobInfoBetween, EXPECT_ZERO_OR_ONE_ROW, sJobId, lStartTsInMicroSecs, lSearchForTsEndBoundInMicroSecs);
        VoltTable[] aJobInfo = voltExecuteSQL();

        long lJobsStartTsInMicroSecs = -1L;
        // Make the changes to the job's JobInfo.
        if (aJobInfo[0].getRowCount() == 0) {
            // the job's JobInfo does not yet exist.
            // Set the timestamp to use when rereading the newly updated JobInfo.
            lJobsStartTsInMicroSecs = lStartTsInMicroSecs;
            // Put a new JobInfo row into the table for this JobId.
            voltQueueSQL(insertJobInfo
                        ,sJobId                     // JobId
                        ,"F"                        // WlmJobStarted is false - we have not yet seen the job's actual start record
                        ,lJobsStartTsInMicroSecs    // WlmJobStartTime - since we have not yet seen the job's actual start record, go ahead and use the job start time from this record for now.
                        ,lEndTsInMicroSecs          // WlmJobEndTime
                        ,sWorkDir                   // WlmJobWorkDir information
                        ,"T"                        // WlmJobCompleted is true
                        ,sWlmJobState               // WlmJobState information
                        );
        }
        else {
            // the job's JobInfo does already exist.
            // Set the timestamp to use when rereading the newly updated JobInfo.
            aJobInfo[0].advanceRow();
            lJobsStartTsInMicroSecs = aJobInfo[0].getTimestampAsTimestamp("WlmJobStartTime").getTime();
            // Update the existing JobInfo with the fact that the job has now completed.
            // Note: we do NOT want to overlay the job start time with the value in this job completion record (use the original start time).
            voltQueueSQL(updateJobInfo
                        ,lEndTsInMicroSecs  // WlmJobEndTime
                        // note: we do NOT want to overlay the job start time with the value in this job completion record (use the original start time)
                        ,sWorkDir           // WlmJobWorkDir information
                        ,"T"                // WlmJobCompleted is true
                        ,sWlmJobState       // WlmJobState information
                        ,sJobId             // JobId
                        ,lJobsStartTsInMicroSecs
                        );
        }
        voltExecuteSQL();

        // Reread the newly updated JobInfo (so we have a VoltTable with the information in it to return to the caller).
        voltQueueSQL(selectJobInfo, EXPECT_ONE_ROW, sJobId, lJobsStartTsInMicroSecs);
        aJobInfo = voltExecuteSQL(true);

        // Return the JobInfo to the caller in the form of a VoltTable.
        return aJobInfo[0];
    }
}