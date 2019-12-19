// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.*;
import static java.lang.Math.toIntExact;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a job is started.
 * NOTE:
 *
 *  Input parameter:
 *      String sJobId               = JobId
 *      String sJobName             = Wlm's job name
 *      String sBsn                 = Batch Service Node that started this job (i.e., FEN, Login node, Mother Superior node)
 *      int    iNumNodes            = Number of nodes that are allocated to this job
 *      byte[] baThisJobsNodes      = Byte array that contains the BitSet containing the information on the compute nodes that are "allocated" to this job
 *      String sUserName            = Username for this job
 *      long   lStartTsInMicroSecs  = Time this job was created in units of micro-seconds since the epoch
 *      String sReqAdapterType      = Type of adapter that requested this stored procedure
 *      long   lReqWorkItemId       = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *
 */

public class JobStarted extends VoltProcedure {

    public final SQLStmt insertJobSql = new SQLStmt(
                    "INSERT INTO Job " +
                    "(JobId, JobName, State, Bsn, NumNodes, Nodes, PowerCap, UserName, Executable, InitialWorkingDir, Arguments, EnvironmentVars, StartTimestamp, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL, ?, ?, ?, ?, ?);"
    );
    public final SQLStmt insertJobHistorySql = new SQLStmt(
                    "INSERT INTO Job_History " +
                    "(JobId, JobName, State, Bsn, NumNodes, Nodes, PowerCap, UserName, Executable, InitialWorkingDir, Arguments, EnvironmentVars, StartTimestamp, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, " +
                    " EndTimestamp, ExitStatus, JobAcctInfo, PowerUsed, WlmJobState) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL, ?, ?, ?, ?, ?, NULL, NULL, NULL, NULL, NULL);"
    );

    public long run(String sJobId, String sJobName, String sBsn, int iNumNodes, byte[] baThisJobsNodes,
                    String sUserName, long lStartTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //---------------------------------------------------------------------
        // Insert this information into the Job_History table.
        //---------------------------------------------------------------------
        voltQueueSQL(insertJobHistorySql
                    ,sJobId
                    ,sJobName
                    ,"S"                        // State - S = Started
                    ,sBsn
                    ,iNumNodes
                    ,baThisJobsNodes
                    ,0                          // PowerCap
                    ,sUserName
                    ,lStartTsInMicroSecs        // StartTimestamp
                    ,this.getTransactionTime()  // DbUpdatedTimestamp
                    ,lStartTsInMicroSecs        // LastChgTimestamp
                    ,sReqAdapterType
                    ,lReqWorkItemId
                    );

        //---------------------------------------------------------------------
        // Also insert a new job row into the Job table.
        //---------------------------------------------------------------------
        voltQueueSQL(insertJobSql
                    ,sJobId
                    ,sJobName
                    ,"S"                        // State - S = Started
                    ,sBsn
                    ,iNumNodes
                    ,baThisJobsNodes
                    ,0                          // PowerCap
                    ,sUserName
                    ,lStartTsInMicroSecs        // StartTimestamp
                    ,this.getTransactionTime()  // DbUpdatedTimestamp
                    ,lStartTsInMicroSecs        // LastChgTimestamp
                    ,sReqAdapterType
                    ,lReqWorkItemId
                    );

        voltExecuteSQL(true);
        return 0;
    }
}