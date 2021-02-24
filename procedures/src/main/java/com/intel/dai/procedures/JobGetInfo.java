// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.voltdb.*;

/**
 * Get the job information for the specified JobId.
 *  - Will first attempt to get the information out of the active job table (Job),
 *    if there is no entry in the active table we will then grab the information
 *    from the "latest" entry in the Job_History table.
 *
 *  Input parameter:
 *      sJobId - id of the job that we want the information for
 *
 *  Returns:
 *      VoltTable - containing the job information (if any)
 *
 */

public class JobGetInfo extends VoltProcedure {

    public final SQLStmt selectInfoForActiveJob = new SQLStmt("SELECT * FROM Job WHERE JobId=?;");

    public final SQLStmt selectInfoForHistoricalJob = new SQLStmt(
        "SELECT * FROM Job_History a WHERE JobId=? AND LastChgTimestamp=(SELECT MAX(LastChgTimestamp) FROM Job_History b WHERE b.JobId=?)"
    );


    public VoltTable run(String sJobId) throws VoltAbortException {
        // Get the job's info from the active job table.
        voltQueueSQL(selectInfoForActiveJob, EXPECT_ZERO_OR_ONE_ROW, sJobId);
        VoltTable[] jobInfo = voltExecuteSQL();

        if (jobInfo[0].getRowCount() == 0) {
            // there was no information in the active job table for this JobId.
            // Get the job information out of the Job_History table.
            voltQueueSQL(selectInfoForHistoricalJob, sJobId, sJobId);
            jobInfo = voltExecuteSQL(true);
        }

        return jobInfo[0];
    }
}