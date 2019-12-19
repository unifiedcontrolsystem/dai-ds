// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a job is started for populating the InternalCachedJobs table.
 *  Input parameter:
 *      String[] aNodeLctns          = array of Lctn strings representing the compute nodes associated with the specified job id
 *      String  sJobId              = JobId telling us which job this entry is for
 *      long    lStartTsInMicroSecs = Time this job was created in units of micro-seconds since the epoch
 */

public class InternalCachedJobsAddNodeEntry extends VoltProcedure {

    public final SQLStmt InternalCachedJobSql = new SQLStmt(
                    "INSERT INTO InternalCachedJobs " +
                    "(NodeLctn, JobId, StartTimestamp, DbUpdatedTimestamp, EndTimestamp) " +
                    "VALUES (?, ?, ?, ?, ?);"
    );

    public long run(String[] aNodeLctns, String sJobId, long lStartTsInMicroSecs) throws VoltAbortException {
        // Loop through each lctn associated with this job.
        for (String sNodeLctn: aNodeLctns) {
	        // Insert a new job row into the InternalCachedJobs table.
	        voltQueueSQL(InternalCachedJobSql
	                    ,sNodeLctn                  // node lctn
	                    ,sJobId                     // job id
	                    ,lStartTsInMicroSecs        // startTimestamp
	                    ,this.getTransactionTime()  // DbUpdatedTimestamp - use current timestamp
	                    ,null                       // EndTimestamp - set this field to NULL
	                    );
        }
        voltExecuteSQL(true);
        return 0;
    }
}
