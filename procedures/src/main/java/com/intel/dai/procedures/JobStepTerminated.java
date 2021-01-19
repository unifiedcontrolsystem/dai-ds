// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a JobStep terminates.
 *
 *  Input parameter:
 *      String  sJobId              = Job's id
 *      String  sJobStepId          = JobStep's id
 *      int     iExitStatus         = JobStep's exit status
 *      String  sJobStepName        = JobStep's name/executable
 *      String  sWlmJobStepState    = JobStep's WLM state
 *      long    lEndTsInMicroSecs   = Time this JobStep terminated in the form of number of microseconds since the epoch
 *      String  sReqAdapterType     = Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId      = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *
 */

public class JobStepTerminated extends JobStepCommon {

    public final SQLStmt selectJobStepSql = new SQLStmt("SELECT * FROM JobStep WHERE JobId=? AND JobStepId=?;");

    public final SQLStmt deleteJobStepSql = new SQLStmt("DELETE FROM JobStep WHERE JobId=? AND JobStepId=?;");

    public final SQLStmt insertJobStepHistorySql = new SQLStmt(
                    "INSERT INTO JobStep_History " +
                    "(JobId, JobStepId, State, NumNodes, Nodes, NumProcessesPerNode, Executable, InitialWorkingDir, Arguments, EnvironmentVars, MpiMapping, StartTimestamp, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, " +
                    " EndTimestamp, ExitStatus, WlmJobStepState) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );



    public long run(String sJobId, String sJobStepId, int iExitStatus, String sJobStepName, String sWlmJobStepState, long lEndTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId)
                throws VoltAbortException
    {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectJobStepSql, EXPECT_ZERO_OR_ONE_ROW, sJobId, sJobStepId);
        VoltTable[] aJobStepData = voltExecuteSQL();
        if (aJobStepData[0].getRowCount() == 0) {
            throw new VoltAbortException("JobStepTerminated - there is no entry in the JobStep table for the specified JobStep (JobId=" + sJobId + ", JobStepId=" + sJobStepId + ") - " +
                                         "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }
        aJobStepData[0].advanceRow();

        //----------------------------------------------------------------------
        // Ensure that we aren't inserting multiple records into history with the same timestamp.
        //----------------------------------------------------------------------
        lEndTsInMicroSecs = ensureHaveUniqueJobStepLastChgTimestamp(sJobId, sJobStepId,
                                                                    lEndTsInMicroSecs, aJobStepData[0].getTimestampAsTimestamp("LastChgTimestamp").getTime());

        //--------------------------------------------------
        // Delete JobStep out of the active JobStep table (we already have a copy in aJobStepData).
        //--------------------------------------------------
        voltQueueSQL(deleteJobStepSql, EXPECT_ONE_ROW, sJobId, sJobStepId);

        //--------------------------------------------------
        // Insert a new row into the JobStep history table, indicating that this JobStep has terminated
        // (State = Terminated)
        //--------------------------------------------------
        if (sJobStepName == null)
            sJobStepName = aJobStepData[0].getString("Executable");
        voltQueueSQL(insertJobStepHistorySql
                    ,aJobStepData[0].getString("JobId")                         // Job's id
                    ,aJobStepData[0].getString("JobStepId")                     // JobStep's id
                    ,"T"                                                        // JobStep's State, T = Terminated
                    ,aJobStepData[0].getLong("NumNodes")                        // Number of Nodes
                    ,aJobStepData[0].getVarbinary("Nodes")                      // Nodes
                    ,aJobStepData[0].getLong("NumProcessesPerNode")             // NumProcessesPerNode
                    ,sJobStepName                                               // Executable
                    ,aJobStepData[0].getString("InitialWorkingDir")             // InitialWorkingDir
                    ,aJobStepData[0].getString("Arguments")                     // Arguments
                    ,aJobStepData[0].getString("EnvironmentVars")               // EnvironmentVars
                    ,aJobStepData[0].getString("MpiMapping")                    // MpiMapping
                    ,aJobStepData[0].getTimestampAsTimestamp("StartTimestamp")  // JobStep's started timestamp
                    ,this.getTransactionTime()                                  // DbUpdatedTimestamp
                    ,lEndTsInMicroSecs                                          // LastChgTimestamp
                    ,sReqAdapterType                                            // Requesting Adapter type
                    ,lReqWorkItemId                                             // Requesting Work Item id
                    ,lEndTsInMicroSecs                                          // EndTimestamp
                    ,iExitStatus                                                // ExitStatus
                    ,sWlmJobStepState                                           // JobStep's WLM state
                    );
        voltExecuteSQL(true);
        return 0;
    }
}