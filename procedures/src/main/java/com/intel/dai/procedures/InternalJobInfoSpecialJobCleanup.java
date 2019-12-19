// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Go through the JobInfo table and mark the row for the specified job such that WlmJobStarted is set to 'T'
 * AND
 * Also return the updated JobInfo as a VoltTable.
 *
 *  Input parameter:
 *      String  sJobId              = string containing the WLM job id
 *      long    lEndTsInMicroSecs   = time this Job ended in units of micro-seconds since the epoch
 *      long    lStartTsInMicroSecs = this is the time that this job cleanup record thinks that the job started, in units of micro-seconds since the epoch
 *
 *  Return value:
 *      VoltTable aJobInfo = VoltTable containing the current JobInfo values for the specified job (after updating it).
 */

public class InternalJobInfoSpecialJobCleanup extends VoltProcedure {

    public final SQLStmt selectJobInfo = new SQLStmt("SELECT * FROM InternalJobInfo WHERE JobId=? AND WlmJobStartTime=?;");
    public final SQLStmt selectJobInfoChkForExistingJobStartedNotCompleted = new SQLStmt(
                    "SELECT * FROM InternalJobInfo WHERE JobId=?               AND " +
                                                        "WlmJobStarted='T'     AND " +
                                                        "WlmJobCompleted='F'   AND " +
                                                        "WlmJobEndTime IS NULL AND " +
                                                        "WlmJobStartTime<=?;"
    );

    public final SQLStmt insertJobInfo = new SQLStmt(
                    "INSERT INTO InternalJobInfo " +
                    "(JobId, WlmJobStarted, WlmJobStartTime, WlmJobEndTime, WlmJobWorkDir, WlmJobCompleted, WlmJobState) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?);"
    );

    public final SQLStmt updateJobInfo = new SQLStmt("UPDATE InternalJobInfo SET WlmJobStarted=? WHERE JobId=? AND WlmJobStartTime=?;");



    public VoltTable run(String sJobId, long lEndTsInMicroSecs, long lStartTsInMicroSecs) throws VoltAbortException
    {
        //----------------------------------------------------------------------
        // Check & see if we have the special situation in which a job with an already seen job completion record has this record occur
        // (this is used for cleaning up inflight jobs that are canceled without any other explicit message in logs).
        // - E.g., the case of a queued job being canceled without even ever having been started, but it does has an accounting / job completion message.
        //----------------------------------------------------------------------
        // Determine what start time we should use when looking for this job.
        // Note: an existing record should only be from the job completion record which has job timestamps only down to second granularity,
        //  so we need to truncate the timestamp we search with to only have second granularity too.
        long lSearchForTsInMicroSecs = (lStartTsInMicroSecs / 1000000L) * 1000000L;

        // Get the JobInfo values for the specified job in the JobInfo table.
        voltQueueSQL(selectJobInfo, EXPECT_ZERO_OR_ONE_ROW, sJobId, lSearchForTsInMicroSecs);
        VoltTable[] aJobInfo = voltExecuteSQL();

        long lJobsStartTsInMicroSecs = -1L;
        if (aJobInfo[0].getRowCount() > 0) {
            // this is the special situation and the job's JobInfo entry does already exist.
            // Set the timestamp to use when rereading the newly updated JobInfo.
            lJobsStartTsInMicroSecs = lSearchForTsInMicroSecs;
            // Update the existing JobInfo with the fact that the job has now started.
            // Note: we do NOT want to overlay the job start time with the value in this job completion record (use the original start time).
            voltQueueSQL(updateJobInfo
                        ,"T"                        // WlmJobStarted is true
                        // note: we do NOT want to overlay the job start time with the value in this record (just leave the start time that is already in this row)
                        ,sJobId                     // JobId
                        ,lJobsStartTsInMicroSecs    // row's job start time
                        );
        }
        else {
            // this is not the special situation and the job's JobInfo entry does already exist.
            //------------------------------------------------------------------
            // Check and see if there is an existing JobInfo entry for this JobId that indicates the job has started but not completed.
            //------------------------------------------------------------------
            voltQueueSQL(selectJobInfoChkForExistingJobStartedNotCompleted, EXPECT_ZERO_OR_ONE_ROW, sJobId, lStartTsInMicroSecs);
            aJobInfo = voltExecuteSQL();
            if (aJobInfo[0].getRowCount() > 0) {
                // there is an existing JobInfo entry for this JobId that indicates that the job has started but not completed.
                // Note: there is no need to change anything in the JobInfo table - an entry is already there, etc.
                // Return the JobInfo to the caller in the form of a VoltTable.
                return aJobInfo[0];
            }
            else {
                //--------------------------------------------------------------
                // This is the special situation but the job's JobInfo entry does not yet exist.
                //--------------------------------------------------------------
                // Set the timestamp to use when rereading the newly updated JobInfo.
                lJobsStartTsInMicroSecs = lStartTsInMicroSecs;
                // Put a new JobInfo row into the table for this JobId.
                voltQueueSQL(insertJobInfo
                            ,sJobId                     // JobId
                            ,"T"                        // WlmJobStarted is true
                            ,lJobsStartTsInMicroSecs    // WlmJobStartTime - since we have not yet seen the job's actual start record, go ahead and use the job start time from this record for now.
                            ,lEndTsInMicroSecs          // WlmJobEndTime
                            ,null                       // WlmJobWorkDir has not yet been filled in
                            ,"F"                        // WlmJobCompleted is false
                            ,null                       // WlmJobState information
                            );
            }
        }
        voltExecuteSQL();

        // Reread the newly updated/inserted JobInfo (so we have a VoltTable with the information in it to return to the caller).
        voltQueueSQL(selectJobInfo, EXPECT_ONE_ROW, sJobId, lJobsStartTsInMicroSecs);
        aJobInfo = voltExecuteSQL(true);

        // Return the JobInfo to the caller in the form of a VoltTable.
        return aJobInfo[0];
    }
}
