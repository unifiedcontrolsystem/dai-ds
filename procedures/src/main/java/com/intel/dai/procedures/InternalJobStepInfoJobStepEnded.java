// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary in the InternalJobStepInfo table when a JobStep has ended.
 * Note: this method must handle the fact that in this environment, time is NOT a stream, i.e., records may show up that are out of time order.
 *
 *  Input parameter:
 *      String  sJobId              = string containing the WLM job id
 *      String  sJobStepId          = string containing the WLM JobStep id
 *      long    lEndTsInMicroSecs   = time this JobStep ended in units of micro-seconds since the epoch
 *
 *  Return value:
 *      VoltTable aJobStepInfo = VoltTable containing all of the JobStepInfo for this JobStep (after updating it to reflect the fact that the JobStep has ended)
 */

public class InternalJobStepInfoJobStepEnded extends VoltProcedure {

    public final SQLStmt selectJobStepInfo = new SQLStmt("SELECT * FROM InternalJobStepInfo WHERE (JobId=? AND JobStepId=?);");

    public final SQLStmt insertJobStepInfo = new SQLStmt(
                    "INSERT INTO InternalJobStepInfo " +
                    "(JobId, JobStepId, WlmJobStepStarted, WlmJobStepEndTime, WlmJobStepEnded) " +
                    "VALUES (?, ?, ?, ?, ?);"
    );

    public final SQLStmt updateJobStepInfo = new SQLStmt("UPDATE InternalJobStepInfo SET WlmJobStepEndTime=?, WlmJobStepEnded=? WHERE (JobId=? AND JobStepId=?);");



    public VoltTable run(String sJobId, String sJobStepId, long lEndTsInMicroSecs) throws VoltAbortException {
        // Check & see if there is already a row containing the JobStepInfo for the specified JobStepId.
        voltQueueSQL(selectJobStepInfo, EXPECT_ZERO_OR_ONE_ROW, sJobId, sJobStepId);
        VoltTable[] aJobStepInfo = voltExecuteSQL();

        // Update the job's JobStepInfo.
        if (aJobStepInfo[0].getRowCount() == 0) {
            // the job's JobStepInfo does not yet exist.
            // Put a new JobStepInfo row into the table for this JobStepId.
            voltQueueSQL(insertJobStepInfo
                        ,sJobId             // JobId
                        ,sJobStepId         // JobStepId
                        ,"F"                // WlmJobStepStarted is false
                        ,lEndTsInMicroSecs  // WlmJobStepEndTime information
                        ,"T"                // WlmJobStepEnded is true
                        );
        }
        else {
            // the job's JobStepInfo does already exist.
            // Update the existing JobStepInfo with the fact that the JobStep has now ended.
            voltQueueSQL(updateJobStepInfo
                        ,lEndTsInMicroSecs  // WlmJobStepEndTime information
                        ,"T"                // WlmJobStepEnded is true
                        ,sJobId             // JobId
                        ,sJobStepId         // JobStepId
                        );
        }
        voltExecuteSQL();

        // Reread the newly updated JobStepInfo (so we have a VoltTable with the information in it to return to the caller).
        voltQueueSQL(selectJobStepInfo, EXPECT_ONE_ROW, sJobId, sJobStepId);
        aJobStepInfo = voltExecuteSQL(true);

        // Return the JobStepInfo to the caller in the form of a VoltTable.
        return aJobStepInfo[0];
    }
}