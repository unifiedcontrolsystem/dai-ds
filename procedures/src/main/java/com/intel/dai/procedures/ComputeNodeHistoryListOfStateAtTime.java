// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.TimeZone;
import java.text.SimpleDateFormat;
import org.voltdb.types.TimestampType;


/**
 * This stored procedure is "Temporary" for use during the DAI Prototype, during the prototype we are using VoltDB to represent the entire data store not just Tier1.
 * The product implementation SHOULD NOT use this, but rather should be getting the data out of Tier2 (Postgres) probably utilizing Distinct On feature
 * for optimizing performance of this function!
 *
 *
 *  Flows
 *      1)  If EndingTimestamp IS null - then just return the states for each lctn in descending order from the ComputeNode table (not the history table)
 *          a)  The entries will consist of 1 string
 *              The very first returned string will be the current time (in UTC) - this value can then be used for deltas from this point on.
 *              All the rest of the entries will be the CURRENT state for each of the lctns in the machine (using the ComputeNode table).
 *          b)  There will be exactly 1 + number of machine compute node lctns.
 *          c)  The states will be returned in descending lctn order.
 *      2)  If EndingTimestamp is not null and StartingTimestamp IS null
 *          a)  The entries will consist of 1 string
 *              - The Lctn's state
 *          b)  The entries will be for each lctn's record with the "newest" timestamp that is <= the specified ending timestamp (using the ComputeNode_History table).
 *          c)  There will be at most one entry for each lctn in the machine
 *          d)  The states will be returned in descending lctn order.
 *      3)  If EndingTimestamp is not null and StartingTimestamp is not null
 *          a)  The entries will consist of 2 strings
 *              - The lctn
 *              - The Lctn's state
 *          b)  The entries will be for each lctn's record with the "newest" timestamp that are >= the specified starting timestamp AND <= the specified ending timestamp (using the ComputeNode_History table).
 *          c)  There will be at most one entry for each lctn in the machine.
 *          d)  The states will be returned in descending lctn AND timestamp order.
 *
 *  echo "execute ComputeNodeHistoryListOfStateAtTime '2017-03-15 19:15:06.377002', '2017-03-15 19:15:06.377001';" | sqlcmd
 *  echo "execute ComputeNodeHistoryListOfStateAtTime '2017-03-15 19:15:06.377002', null;" | sqlcmd
 *  echo "execute ComputeNodeHistoryListOfStateAtTime '2017-03-15 19:15:06.377002', '';" | sqlcmd
 *
 */

public class ComputeNodeHistoryListOfStateAtTime extends VoltProcedure {

    public final SQLStmt selectCurrentNodeStatesSql = new SQLStmt(
        "SELECT State from ComputeNode Order By SequenceNumber;"
    );

    public final SQLStmt selectNodeStatesOnlyEndTimeSql = new SQLStmt(
        // "SELECT Lctn, State from ComputeNode_History WHERE LastChgTimestamp<=? " + "UNION ALL " +
        // "SELECT Lctn, State from ComputeNode " +    "WHERE LastChgTimestamp<=? " +
        // "Order By Lctn, LastChgTimestamp DESC;"
        "SELECT Lctn, SequenceNumber, State from ComputeNode_History WHERE LastChgTimestamp<=? Order By SequenceNumber, LastChgTimestamp DESC;"
    );

    public final SQLStmt selectNodeStatesBothEndAndStartTimeSql = new SQLStmt(
        // "SELECT Lctn, State from ComputeNode_History WHERE LastChgTimestamp<=? AND LastChgTimestamp>=? " + "UNION ALL " +
        // "SELECT Lctn, State from ComputeNode " +    "WHERE LastChgTimestamp<=? AND LastChgTimestamp>=? " +
        // "Order By Lctn, LastChgTimestamp DESC;"
        "SELECT Lctn, SequenceNumber, State from ComputeNode_History WHERE LastChgTimestamp<=? AND LastChgTimestamp>=? Order By SequenceNumber, LastChgTimestamp DESC;"
    );



    public VoltTable run(TimestampType sEndingTimestamp, TimestampType sStartingTimestamp) throws VoltAbortException {

        // Check & see if the caller specified an ending timestamp.
        boolean bEndingTsSpecified = false;  // initialize that the ending timestamp was NOT specified.
        if (sEndingTimestamp != null)
            bEndingTsSpecified = true;

        // Check & see if the caller specified a starting timestamp.
        boolean bStartingTsSpecified = false;  // initialize that the starting timestamp was NOT specified.
        if (sStartingTimestamp != null)
            bStartingTsSpecified = true;

        // Create the VoltTable that will be returned to the caller.
        VoltTable vtReturnToCaller;

        //----------------------------------------------------------------------
        // Ending time was NOT specified
        //----------------------------------------------------------------------
        if (bEndingTsSpecified == false) {
            //------------------------------------------------------------------
            // Get the list of CURRENT states for the nodes in the machine (use the ComputeNode table, not the ComputeNode_History table).
            //------------------------------------------------------------------
            // Queue up the pertinent sql query.
            voltQueueSQL(selectCurrentNodeStatesSql);
            // Since the user wants the current state of each node in the machine - return just a single string.
            vtReturnToCaller = new VoltTable(new VoltTable.ColumnInfo("State", VoltType.STRING));
        }
        //----------------------------------------------------------------------
        // Both Ending and Starting time were specified
        //----------------------------------------------------------------------
        else if ((bEndingTsSpecified) && (bStartingTsSpecified)) {
            //----------------------------------------------------------------------
            // Get the list of all the node states that occurred ON OR BETWEEN the specified ending and starting timestamps.
            //  NOTE: This procedure should only be used for the DAI Prototype,
            //        the real product implementation should be done using the history in Tier 2 and should utilize the DISTINCT ON feature of Postgres,
            //        rather than the brute force method utilized here!!!
            //----------------------------------------------------------------------
            // Queue up the pertinent sql query.
            voltQueueSQL(selectNodeStatesBothEndAndStartTimeSql,
                         sEndingTimestamp,
                         sStartingTimestamp);
            // Since the user just wants a subset of updates for the specified timestamps it may result in only a subset of the lctns being returned - return both SequenceNumber and State.
            vtReturnToCaller = new VoltTable(new VoltTable.ColumnInfo("SequenceNumber", VoltType.INTEGER),
                                             new VoltTable.ColumnInfo("State",  VoltType.STRING));
        }
        //----------------------------------------------------------------------
        // Only Ending time was specified
        //----------------------------------------------------------------------
        else {
            //----------------------------------------------------------------------
            // Get the list of all the node states that occurred ON OR BEFORE the specified ending timestamp.
            //  NOTE: This procedure should only be used for the DAI Prototype,
            //        the real product implementation should be done using the history in Tier 2 and should utilize the DISTINCT ON feature of Postgres,
            //        rather than the brute force method utilized here!!!
            //----------------------------------------------------------------------
            // Queue up the pertinent sql query.
            voltQueueSQL(selectNodeStatesOnlyEndTimeSql,
                         sEndingTimestamp);
            // Since the user wants all the data up to the specified ending timestamp, it will result in a row for each Lctn - return just the State in descending Lctn order.
            vtReturnToCaller = new VoltTable(new VoltTable.ColumnInfo("State", VoltType.STRING));
        }
        VoltTable[] aListOfNodeAndStates = voltExecuteSQL(true);


        //--------------------------------------------------
        // Build up the information that we want to return to the caller as a VoltTable[].
        //--------------------------------------------------
            // Add the current timestamp as the first entry of the table - so caller has it available for future delta invocations.
            SimpleDateFormat sqlDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS000");
            sqlDateFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // This line cause timestamps formatted by this SimpleDateFormat to be converted into UTC time zone
        if ((bEndingTsSpecified) && (bStartingTsSpecified)) {
            vtReturnToCaller.addRow(-1L, sqlDateFormat.format(this.getTransactionTime()));
        }
        else {
            vtReturnToCaller.addRow( sqlDateFormat.format(this.getTransactionTime()) );
        }

        if (bEndingTsSpecified == false) {
            // ending time was not specified - return the list of current state of every node in the machine, so just return the current state in Lctn/SequenceNumber order.
            // Loop through copying the data into the return VoltTable.
            for (int iRowCntr = 0; iRowCntr < aListOfNodeAndStates[0].getRowCount(); ++iRowCntr) {
                aListOfNodeAndStates[0].advanceRow();
                vtReturnToCaller.addRow( aListOfNodeAndStates[0].getString(0) );  // State.
            }
        }
        else {
            // ending time was specified.
            // Loop through the data that we got back from the query, keeping just the first record for each distinct Lctn/SequenceNumber.
            int iPrevSeqNum = -99999;
            for (int iRowCntr = 0; iRowCntr < aListOfNodeAndStates[0].getRowCount(); ++iRowCntr) {
                aListOfNodeAndStates[0].advanceRow();
                int    iNodeSeqNum = (int)aListOfNodeAndStates[0].getLong(1);
                String sNodeState  = aListOfNodeAndStates[0].getString(2);
                // Check & see if this is a new Lctn/SequenceNumber (that we hadn't seen yet)
                if (iPrevSeqNum != iNodeSeqNum) {
                    // since this is a new Lctn/SequenceNumber, the order by clause on the select guarantees that this is the state we want to return for this Lctn/SequenceNumber.
                    // Add the appropriate data to the volt table.
                    if ((bEndingTsSpecified) && (bStartingTsSpecified)){
                        // both ending time and starting time were specified so we may only get a subset of the total list of nodes, so return both SequenceNumber and State.
                        vtReturnToCaller.addRow(iNodeSeqNum, sNodeState);
                    }
                    else {
                        // only ending time was specified so we will return an entry for each node.
                        vtReturnToCaller.addRow(sNodeState);
                    }
                    // Save this SequenceNumber as the "previous SequenceNumber", so we skip all the rest of the states for this Lctn/SequenceNumber.
                    iPrevSeqNum = iNodeSeqNum;
                }
            }
        }

        //----------------------------------------------------------------------
        // Returns the information to the caller.
        //----------------------------------------------------------------------
        return vtReturnToCaller;
    }
}
