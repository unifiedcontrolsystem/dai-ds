// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a JOB has been requeued / is being rerun, this takes care of cleaning up any "active" JobSteps!
 *
 *  Input parameter:
 *      String  sJobId              = Job's id
 *      long    lEndTsInMicroSecs   = Time this JobStep terminated in the form of number of microseconds since the epoch
 *      String  sReqAdapterType     = Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId      = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *  Returns:
 *      long    = the number of JobSteps that were "cleaned up" for the specified job.
 */

public class JobStepCleanup extends JobStepCommon {

    public final SQLStmt selectJobStepSql = new SQLStmt("SELECT * FROM JobStep WHERE JobId=?;");

    public final SQLStmt deleteJobStepSql = new SQLStmt("DELETE FROM JobStep WHERE JobId=?;");

    public final SQLStmt insertJobStepHistorySql = new SQLStmt(
                    "INSERT INTO JobStep_History " +
                    "(JobId, JobStepId, State, NumNodes, Nodes, NumProcessesPerNode, Executable, InitialWorkingDir, Arguments, EnvironmentVars, MpiMapping, StartTimestamp, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, " +
                    " EndTimestamp, ExitStatus, WlmJobStepState) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );



    public long run(String sJobId, long lEndTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectJobStepSql, sJobId);
        VoltTable[] aJobStepData = voltExecuteSQL();
        if (aJobStepData[0].getRowCount() == 0) {
            return 0;
        }

        //----------------------------------------------------------------------
        // Loop through the saved JobStep data and insert the appropriate records into the JobStep History table (indicating that the JobStep has terminated).
        //----------------------------------------------------------------------
        for (int iRowCntr = 0; iRowCntr < aJobStepData[0].getRowCount(); ++iRowCntr) {
            // Grab this JobStep's data.
            aJobStepData[0].advanceRow();

            //----------------------------------------------------------------------
            // Ensure that we aren't inserting multiple records into history with the same timestamp.
            //----------------------------------------------------------------------
            lEndTsInMicroSecs = ensureHaveUniqueJobStepLastChgTimestamp(sJobId, aJobStepData[0].getString("JobStepId"),
                                                                        lEndTsInMicroSecs, aJobStepData[0].getTimestampAsTimestamp("LastChgTimestamp").getTime());

            //--------------------------------------------------
            // Insert a new row into the JobStep history table, indicating that this JobStep has terminated
            // (State = Terminated)
            //--------------------------------------------------
            voltQueueSQL(insertJobStepHistorySql
                        ,aJobStepData[0].getString("JobId")                         // Job's id
                        ,aJobStepData[0].getString("JobStepId")                     // JobStep's id
                        ,"T"                                                        // JobStep's State, T = Terminated
                        ,aJobStepData[0].getLong("NumNodes")                        // Number of Nodes
                        ,aJobStepData[0].getVarbinary("Nodes")                      // Nodes
                        ,aJobStepData[0].getLong("NumProcessesPerNode")             // NumProcessesPerNode
                        ,aJobStepData[0].getString("Executable")                    // Executable
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
                        ,null                                                       // ExitStatus is not currently available
                        ,aJobStepData[0].getString("WlmJobStepState")               // JobStep's WLM state
                        );

        }  // Loop through all the records we got back from the query.

        //--------------------------------------------------
        // Delete the JobStep(s) out of the active JobStep table (we have their data saved in aJobStepData)
        //--------------------------------------------------
        voltQueueSQL(deleteJobStepSql, sJobId);

        voltExecuteSQL(true);
        return aJobStepData[0].getRowCount();  // return the number of JobStep's that were "cleaned up".
    }
}