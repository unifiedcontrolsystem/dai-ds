// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.*;
import static java.lang.Math.toIntExact;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a JobStep is started.
 *
 *  Input parameter:
 *      String  sJobId              = Job's id
 *      String  sJobStepId          = JobStep's id
 *      String  sJobStepExecutable  = JobStep's executable
 *      int     iNumNodes           = Number of nodes that are allocated to this job
 *      byte[]  baThisJobsNodes     = Byte array that contains the BitSet containing the information on the compute nodes that are "allocated" to this job
 *      long    lStartTsInMicroSecs = Time this JobStep started in the form of number of microseconds since the epoch
 *      String  sReqAdapterType     = Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId      = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *
 */

public class JobStepStarted extends VoltProcedure {

    public final SQLStmt insertJobStepSql = new SQLStmt(
                    "INSERT INTO JobStep " +
                    "(JobId, JobStepId, State, NumNodes, Nodes, NumProcessesPerNode, Executable, InitialWorkingDir, Arguments, EnvironmentVars, MpiMapping, StartTimestamp, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );

    public final SQLStmt insertJobStepHistorySql = new SQLStmt(
                    "INSERT INTO JobStep_History " +
                    "(JobId, JobStepId, State, NumNodes, Nodes, NumProcessesPerNode, Executable, InitialWorkingDir, Arguments, EnvironmentVars, MpiMapping, StartTimestamp, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, " +
                    " EndTimestamp, ExitStatus, WlmJobStepState) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );



    public long run(String sJobId, String sJobStepId, String sJobStepExecutable, int iNumNodes, byte[] baThisJobsNodes, long lStartTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //---------------------------------------------------------------------
        // Insert this information into the JobStep table.
        //---------------------------------------------------------------------
        voltQueueSQL(insertJobStepSql
                    ,sJobId                     // Job's id
                    ,sJobStepId                 // JobStep's id
                    ,"S"                        // JobStep's state, S = Started
                    ,iNumNodes                  // NumNodes
                    ,baThisJobsNodes            // Nodes
                    ,null                       // NumProcessesPerNode
                    ,sJobStepExecutable         // Executable
                    ,null                       // InitialWorkingDir
                    ,null                       // Arguments
                    ,null                       // EnvironmentVars
                    ,null                       // MpiMapping
                    ,lStartTsInMicroSecs        // StartTimestamp
                    ,this.getTransactionTime()  // DbUpdatedTimestamp
                    ,lStartTsInMicroSecs        // LastChgTimestamp
                    ,sReqAdapterType            // Requesting Adapter type
                    ,lReqWorkItemId             // Requesting Work Item id
                    );

        //---------------------------------------------------------------------
        // Also insert this information into the JobStep_History table.
        //---------------------------------------------------------------------
        voltQueueSQL(insertJobStepHistorySql
                    ,sJobId                     // Job's id
                    ,sJobStepId                 // JobStep's id
                    ,"S"                        // JobStep's state, S = Started
                    ,iNumNodes                  // NumNodes
                    ,baThisJobsNodes            // Nodes
                    ,null                       // NumProcessesPerNode
                    ,sJobStepExecutable         // Executable
                    ,null                       // InitialWorkingDir
                    ,null                       // Arguments
                    ,null                       // EnvironmentVars
                    ,null                       // MpiMapping
                    ,lStartTsInMicroSecs        // StartTimestamp
                    ,this.getTransactionTime()  // DbUpdatedTimestamp
                    ,lStartTsInMicroSecs        // LastChgTimestamp
                    ,sReqAdapterType            // Requesting Adapter type
                    ,lReqWorkItemId             // Requesting Work Item id
                    ,null                       // EndTimestamp
                    ,null                       // ExitStatus
                    ,null                       // JobStep's WLM state
                    );

        voltExecuteSQL(true);
        return 0;
    }
}