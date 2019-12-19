// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Arrays;
import org.voltdb.*;
import java.sql.Timestamp;
import java.util.ArrayList;


/**
 * Handle the database processing that is necessary to clean up any "stale data entries" in the InternalJobInfo table
 *     - Grab a copy of the volt table with the entries that are going to be deleted
 *     - Actually delete those entries
 *
 *  Input parameter:
 *     long lMillisecs  = Type of adapter this is (used for determining which adapters to look for)
 *
 *  Returns:
 *      VoltTable aStaleDataEntries[0] = the list of stale data entries that were deleted
 *
 */

public class InternalJobInfoCheckForStaleData extends VoltProcedure {

    public final SQLStmt listOfStaleDataEntries = new SQLStmt(
        "SELECT JobId, WlmJobStarted, WlmJobStartTime, WlmJobEndTime, WlmJobCompleted FROM InternalJobInfo " +
        "WHERE WlmJobStarted='T' AND WlmJobCompleted='F' AND WlmJobStartTime=WlmJobEndTime AND WlmJobStartTime<=? " +
        "Order By JobId, WlmJobStartTime;"
    );

    public final SQLStmt deleteStaleDataEntries = new SQLStmt("DELETE FROM InternalJobInfo WHERE WlmJobStarted='T' AND WlmJobCompleted='F' AND WlmJobStartTime=WlmJobEndTime AND WlmJobStartTime<=?;");



    public VoltTable run(long lMicroSecs) throws VoltAbortException {
        //---------------------------------------------------------------------
        // Get the list of "stale data entries" (so they can be returned to the caller)
        // - These entries can occur in the case of the scenario in which jobs are canceled without ever really starting  E.g.,
        //      /var/hit/data/slurmaccounting/slurmctld.log;sched;_slurm_rpc_allocate_resources;JobId=1956 NodeList=(null) usec=279
        //      /var/hit/data/slurmaccounting/slurmctld.log;_slurm_rpc_kill_job2;REQUEST_KILL_JOB;job 1956 uid 60000
        //      /var/hit/data/slurmaccounting/slurmctld.log;_slurm_rpc_kill_job2;REQUEST_KILL_JOB;job 1956 uid 60000
        //---------------------------------------------------------------------
        voltQueueSQL(listOfStaleDataEntries, lMicroSecs);
        VoltTable[] aStaleDataEntries = voltExecuteSQL();

        //----------------------------------------------------------------------
        // Cleanup / Delete the stale data entries
        //----------------------------------------------------------------------
        voltQueueSQL(deleteStaleDataEntries, lMicroSecs);
        voltExecuteSQL(true);

        // Returns the list of stale data entries that were deleted to the caller.
        return aStaleDataEntries[0];
    }
}
