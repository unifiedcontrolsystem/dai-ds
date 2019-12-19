// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a job terminates.
 *
 *  Input parameter:
 *      String  sJobId              = Job's ID
 *      String  sAcctInfo           = Job's accounting information
 *      int     iExitStatus         = Job's exit status
 *      String  sWorkDir            = Job's initial working directory
 *      String  sWlmJobState        = Job's WLM state
 *      long    lEndTsInMicroSecs   = Time this job terminated in units of micro-seconds since the epoch
 *      String  sReqAdapterType     = Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId      = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 */

public class JobTerminated extends JobCommon {

    public final SQLStmt selectJobSql = new SQLStmt("SELECT * FROM Job WHERE JobId=?;");

    public final SQLStmt deleteJobSql = new SQLStmt("DELETE FROM Job WHERE JobId=?;");

    public final SQLStmt insertJobHistorySql = new SQLStmt(
                    "INSERT INTO Job_History " +
                    "(JobId, JobName, State, Bsn, NumNodes, Nodes, PowerCap, UserName, Executable, InitialWorkingDir, Arguments, EnvironmentVars, StartTimestamp, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, " +
                    " EndTimestamp, ExitStatus, JobAcctInfo, PowerUsed, WlmJobState) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );



    public long run(String sJobId, String sAcctInfo, int iExitStatus, String sWorkDir, String sWlmJobState,
                    long lEndTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException
    {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectJobSql, EXPECT_ZERO_OR_ONE_ROW, sJobId);
        VoltTable[] aJobData = voltExecuteSQL();

        // Check & see if this is the special flow that is used when a job has been canceled without ever having actually started.
        if (aJobData[0].getRowCount() == 0) {
            //------------------------------------------------------------------
            // This is special flow that is used when a job has been canceled without ever having the job actually being started.
            //------------------------------------------------------------------
            // Just insert a row for this job in the job history table indicating that the job was terminated.
            voltQueueSQL(insertJobHistorySql
                        ,sJobId
                        ,null                           // JobName
                        ,"T"                            // State - T = Terminated
                        ,null                           // BSN
                        ,0                              // number of nodes
                        ,new byte[]{ (byte)(0) }        // nodes
                        ,0                              // power cap
                        ,null                           // UserName
                        ,null                           // executable
                        ,null                           // InitialWorkingDir
                        ,null                           // arguments
                        ,null                           // environmental variables
                        ,lEndTsInMicroSecs              // StartTimestamp - since we never got a job started message, just the end time for this start time too!
                        ,this.getTransactionTime()      // DbUpdatedTimestamp
                        ,lEndTsInMicroSecs              // LastChgTimestamp
                        ,sReqAdapterType
                        ,lReqWorkItemId
                        ,lEndTsInMicroSecs              // EndTimestamp
                        ,iExitStatus
                        ,sAcctInfo
                        ,0                              // Power Used (not currently available)
                        ,sWlmJobState                   // Job's WLM state
                        );
            voltExecuteSQL(true);
            return 0L;
        }   // special flow when job has been canceled w/o ever having actually started.

        aJobData[0].advanceRow();

        //----------------------------------------------------------------------
        // Ensure that we aren't inserting multiple records into history with the same timestamp.
        //----------------------------------------------------------------------
        lEndTsInMicroSecs = ensureHaveUniqueJobLastChgTimestamp(sJobId, lEndTsInMicroSecs, aJobData[0].getTimestampAsTimestamp("LastChgTimestamp").getTime());

        //--------------------------------------------------
        // Delete job out of the active job table (we have its data in aJobData structure).
        //--------------------------------------------------
        voltQueueSQL(deleteJobSql, EXPECT_ONE_ROW, sJobId);

        //--------------------------------------------------
        // Insert a new row into the job history table, indicating that this job has terminated
        // (State = Terminated, include ExitStatus, ErrorText, PowerUsed).
        //--------------------------------------------------
        // Determine if we should use the specified parameters for the history record or instead use the values that were in the active job entry.
        if (sWorkDir == null)
            sWorkDir = aJobData[0].getString("InitialWorkingDir");
        // Actually insert the job termination record into the Job_History table.
        voltQueueSQL(insertJobHistorySql
                    ,aJobData[0].getString("JobId")
                    ,aJobData[0].getString("JobName")                       // JobName
                    ,"T"                                                    // State - T = Terminated
                    ,aJobData[0].getString("Bsn")
                    ,aJobData[0].getLong("NumNodes")
                    ,aJobData[0].getVarbinary("Nodes")
                    ,aJobData[0].getLong("PowerCap")
                    ,aJobData[0].getString("UserName")                      // UserName
                    ,aJobData[0].getString("Executable")
                    ,sWorkDir                                               // InitialWorkingDir
                    ,aJobData[0].getString("Arguments")
                    ,aJobData[0].getString("EnvironmentVars")
                    ,aJobData[0].getTimestampAsTimestamp("StartTimestamp")
                    ,this.getTransactionTime()                              // DbUpdatedTimestamp
                    ,lEndTsInMicroSecs                                      // LastChgTimestamp
                    ,sReqAdapterType
                    ,lReqWorkItemId
                    ,lEndTsInMicroSecs                                      // EndTimestamp
                    ,iExitStatus
                    ,sAcctInfo
                    ,0                                                      // Power Used (not currently available)
                    ,sWlmJobState                                           // Job's WLM state
                    );
        voltExecuteSQL(true);
        return 0;
    }
}