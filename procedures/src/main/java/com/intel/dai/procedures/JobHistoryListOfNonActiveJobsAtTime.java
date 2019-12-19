// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.voltdb.*;
import java.lang.*;
import java.util.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import org.voltdb.types.TimestampType;

/**
 * This stored procedure is "Temporary" for use during the DAI Prototype, during the prototype we are using VoltDB to represent the entire data store not just Tier1.
 * The product implementation SHOULD NOT use this, but rather should be getting the data out of Tier2 (Postgres) probably utilizing Distinct On feature
 * for optimizing performance of this function!
 */

public class JobHistoryListOfNonActiveJobsAtTime extends VoltProcedure {

    public final SQLStmt selectJobsWithTimeSql = new SQLStmt(
        "SELECT JobId, JobName, State, Bsn, NumNodes, Nodes, UserName, StartTimestamp, EndTimestamp, ExitStatus, JobAcctInfo, WlmJobState FROM Job_History WHERE DbUpdatedTimestamp>? Order By JobId DESC, EndTimestamp DESC, LastChgTimestamp DESC, DbUpdatedTimestamp DESC;"
    );


    //-------------------------------------------------------------------------
    // This method goes through the specified list of Nodes represented as a BitSet and returns a list of their node ranks (0 - 100,000) separated by spaces.
    //-------------------------------------------------------------------------
    public String getListNodeRanks(byte[] baThisJobsNodes) {
        // Reconstitute the specified byte array representation of a BitSet back into a BitSet representing this job's compute nodes.
        BitSet bsThisJobsNodes = BitSet.valueOf( baThisJobsNodes );

        boolean bFirstNode = true;
        StringBuilder sbTemp = new StringBuilder();
        // Loop through any bits that are "on" and add its corresponding location string.
        for (int i = bsThisJobsNodes.nextSetBit(0); i >= 0; i = bsThisJobsNodes.nextSetBit(i+1)) {
            // Add the node's rank to the string that will be returned to the caller.
            if (bFirstNode == false) {
                sbTemp.append( " " + Integer.toString(i) );
            }
            else {
                bFirstNode = false;
                sbTemp.append( Integer.toString(i) );
            }
        }
        return sbTemp.toString();
    }   // End getListNodeRanks(byte[] baThisJobsNodes)



    public VoltTable run(TimestampType sBeginningTimestamp) throws VoltAbortException {

        // Check & see if the caller specified a beginning timestamp.
        boolean bBeginningTsSpecified = false;  // initialize that the beginning timestamp was NOT specified.
        if (sBeginningTimestamp != null)
            bBeginningTsSpecified = true;

        // Create the VoltTable that will be returned to the caller.
        VoltTable vtReturnToCaller;
        vtReturnToCaller = new VoltTable(new VoltTable.ColumnInfo("JobId",          VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("JobName",        VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("State",          VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("Bsn",            VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("UserName",       VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("StartTimestamp", VoltType.TIMESTAMP)
                                        ,new VoltTable.ColumnInfo("EndTimestamp",   VoltType.TIMESTAMP)
                                        ,new VoltTable.ColumnInfo("ExitStatus",     VoltType.BIGINT)
                                        ,new VoltTable.ColumnInfo("NumNodes",       VoltType.BIGINT)
                                        ,new VoltTable.ColumnInfo("Nodes",          VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("JobAcctInfo",    VoltType.STRING)
                                        ,new VoltTable.ColumnInfo("WlmJobState",    VoltType.STRING)
                                        );

        //----------------------------------------------------------------------
        // Beginning time was specified
        //----------------------------------------------------------------------
        if (bBeginningTsSpecified == true)
            voltQueueSQL(selectJobsWithTimeSql, sBeginningTimestamp);
        //----------------------------------------------------------------------
        // Beginning time was NOT specified - use beginning of the epoch.
        //----------------------------------------------------------------------
        else
            voltQueueSQL(selectJobsWithTimeSql, 0L);
        VoltTable[] aListOfJobs = voltExecuteSQL();


        //--------------------------------------------------
        // Build up the information that we want to return to the caller as a VoltTable[].
        //--------------------------------------------------
        // Loop through the data that we got back from the query, keeping just the first record for each distinct JobId.
        String sPrevJobId = "";
        for (int iRowCntr = 0; iRowCntr < aListOfJobs[0].getRowCount(); ++iRowCntr) {
            aListOfJobs[0].advanceRow();
            String sJobId    = aListOfJobs[0].getString("JobId");
            String sJobState = aListOfJobs[0].getString("State");
            // Check & see if this is a new JobId
            if (sPrevJobId.equals(sJobId) == false) {
                // Save this JobId as the "previous JobId", so we skip all the rest of the states for this JobId.
                sPrevJobId = sJobId;
                // Check & see if the JobState from this record is NOT S (S = Started/active).
                if (sJobState.equals("S") == false) {
                    // since this is a new JobId with a non-active job state, the order by clause on the select guarantees that this is the info that we want to return for this JobId.
                    // Expand the node list into a single string with space separated node representations.
                    String sListNodeRanks = getListNodeRanks(aListOfJobs[0].getVarbinary("Nodes"));
                    // Add the appropriate data to the volt table.
                    vtReturnToCaller.addRow(sJobId
                                           ,aListOfJobs[0].getString("JobName")
                                           ,sJobState
                                           ,aListOfJobs[0].getString("Bsn")
                                           ,aListOfJobs[0].getString("UserName")
                                           ,aListOfJobs[0].getTimestampAsTimestamp("StartTimestamp")
                                           ,aListOfJobs[0].getTimestampAsTimestamp("EndTimestamp")
                                           ,aListOfJobs[0].getLong("ExitStatus")
                                           ,aListOfJobs[0].getLong("NumNodes")
                                           ,sListNodeRanks  // String containing node ranks separated by spaces
                                           ,aListOfJobs[0].getString("JobAcctInfo")
                                           ,aListOfJobs[0].getString("WlmJobState")
                                           );
                }  // Job state is not active.
            }  // this is the first record for a new job id.
        }  // loop through all the records we got back from the query.

        //----------------------------------------------------------------------
        // Returns the information to the caller.
        //----------------------------------------------------------------------
        return vtReturnToCaller;
    }
}
