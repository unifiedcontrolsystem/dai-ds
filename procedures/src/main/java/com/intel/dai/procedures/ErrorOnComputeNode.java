// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.time.Instant;

import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a ComputeNode transitions to "Error" state.
 * NOTE: We only change those that are currently Active into Error
 *
 *  Input parameter:
 *      String  sLctn           = string containing a fully qualified ComputeNode location that should be set in error.
 *      String  sReqAdapterType = Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId  = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *
 *  Sample invocation:
 *      echo "Exec ErrorOnComputeNode R00-CH3-CB7-PM3-CN1;" | sqlcmd
 *          Set an individual specified ComputeNode into Error
 *
 */

public class ErrorOnComputeNode extends ComputeNodeCommon {

    public final SQLStmt selectComputeNode = new SQLStmt("SELECT * FROM ComputeNode WHERE Lctn=? Order By Lctn;");

    public final SQLStmt insertComputeNodeHistory = new SQLStmt(
            "INSERT INTO ComputeNode_History " +
            "(Lctn, SequenceNumber, State, HostName, BootImageId, IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp, WlmNodeState) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );

    public final SQLStmt updateComputeNode = new SQLStmt("UPDATE ComputeNode SET State=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE Lctn=?;");



    long putSingleComputeNodeIntoError(String sLctn, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectComputeNode, EXPECT_ONE_ROW, sLctn);
        VoltTable[] aComputeNodeData = voltExecuteSQL();
        if (aComputeNodeData[0].getRowCount() == 0) {
            throw new VoltAbortException("ErrorOnComputeNode::putSingleComputeNodeIntoError - there is no entry in the ComputeNode table for the specified ComputeNode (" + sLctn + ") - " +
                                         "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }
        // Get the current record in the "active" table's LastChgTimestamp (in micro-seconds since epoch).
        aComputeNodeData[0].advanceRow();
        long lCurRecsLastChgTimestampTsInMicroSecs = aComputeNodeData[0].getTimestampAsLong("LastChgTimestamp");

        // Get this transactions timestamp in microsecs.
        Instant ts = Instant.now();
        long lThisTransactionsTsInMicrosecs = (ts.getEpochSecond() * 1_000_000) + (ts.getNano() / 1_000);

        //----------------------------------------------------------------------
        // Ensure that we aren't updating this row with the exact same LastChgTimestamp as currently exists in the table.
        //----------------------------------------------------------------------
        // Check & see if this timestamp is the same as the LastChgTimestamp on the current record (in the ComputeNode table).
        if (lThisTransactionsTsInMicrosecs <= lCurRecsLastChgTimestampTsInMicroSecs)
        {
            lThisTransactionsTsInMicrosecs = lCurRecsLastChgTimestampTsInMicroSecs + 1;
            lThisTransactionsTsInMicrosecs = ensureHaveUniqueComputeNodeLastChgTimestamp(sLctn, lThisTransactionsTsInMicrosecs, lCurRecsLastChgTimestampTsInMicroSecs);
        }

        //----------------------------------------------------------------------
        // Insert a "history" record for these updates into the history table (start with pre-change values then overlay with these changes).
        //----------------------------------------------------------------------
        voltQueueSQL(insertComputeNodeHistory
                    ,aComputeNodeData[0].getString("Lctn")
                    ,aComputeNodeData[0].getLong("SequenceNumber")
                    ,"E"                                            // State = Error
                    ,aComputeNodeData[0].getString("HostName")
                    ,aComputeNodeData[0].getString("BootImageId")
                    ,aComputeNodeData[0].getString("IpAddr")
                    ,aComputeNodeData[0].getString("MacAddr")
                    ,aComputeNodeData[0].getString("BmcIpAddr")
                    ,aComputeNodeData[0].getString("BmcMacAddr")
                    ,aComputeNodeData[0].getString("BmcHostName")
                    ,lThisTransactionsTsInMicrosecs                 // Get CURRENT_TIMESTAMP or NOW
                    ,lThisTransactionsTsInMicrosecs                 // LastChgTimestamp
                    ,sReqAdapterType
                    ,lReqWorkItemId
                    ,aComputeNodeData[0].getString("Owner")
                    ,aComputeNodeData[0].getString("Aggregator")
                    ,aComputeNodeData[0].getTimestampAsTimestamp("InventoryTimestamp")
                    ,aComputeNodeData[0].getString("WlmNodeState")
                    );

        //--------------------------------------------------
        // Update the object's values.
        //--------------------------------------------------
        voltQueueSQL(updateComputeNode, "E", lThisTransactionsTsInMicrosecs, lThisTransactionsTsInMicrosecs, sReqAdapterType, lReqWorkItemId, sLctn);
        voltExecuteSQL();
        return 0;
    }


    public long run(String sLctn, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //
        // switch(sLctn.length()) {
        //     case LCTN_COMPUTE_NODE_LENGTH :     // ComputeNode     was specified (Rdd-CH3-CB7-PM3-CN1)
        //         putSingleComputeNodeIntoError(sLctn, sReqAdapterType, lReqWorkItemId);
        //         break;
        //
        //     case LCTN_CHASSIS_LENGTH :          // Chassis         was specified (Rdd-CH3)
        //     case LCTN_RACK_LENGTH :             // Rack            was specified (Rdd)
        //         //System.out.println("\n\nErrorOnComputeNode - user specified an entire component rather than a single ComputeNode (" + sLctn + ")");
        //         // Get the list of "appropriate" ComputeNodes that exist w/i the specified component from the database.
        //         // (the appropriate node's are those with a state that it makes sense to transition from, to Error, e.g., transition from Active to Error.
        //         //  Those that are deemed inappropriate will simply not be changed.)
        //         voltQueueSQL(selectChildComputeNodesThatAreActive, sLctn+"%");
        //         VoltTable[] aComputeNodeData = voltExecuteSQL();
        //
        //         // Loop through the returned list of ComputeNodes, marking them as being in Error.
        //         for (int cntr = 0; cntr < aComputeNodeData[0].getRowCount(); ++cntr) {
        //             // Actually mark this ComputeNode into Error.
        //             putSingleComputeNodeIntoError(aComputeNodeData[0].fetchRow(cntr).getString("Lctn"), sReqAdapterType, lReqWorkItemId);
        //         }
        //         break;
        //
        //     default :
        //         throw new VoltAbortException("ErrorOnComputeNode - an unsupported location string was specified (" + sLctn + ") - " +
        //                                      "ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        // }

        return putSingleComputeNodeIntoError(sLctn, sReqAdapterType, lReqWorkItemId);
    }
}