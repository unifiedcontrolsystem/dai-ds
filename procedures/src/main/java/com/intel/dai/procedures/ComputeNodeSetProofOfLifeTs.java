// Copyright (C) 2020-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a node needs its proof of life timestamp changed.
 * Note: this processing does not reflect this update in the ComputeNode_History table, this was done deliberately (see NOTE: below for additional info)!
 *
 *  Input parameter:
 *      String  sNodeLctn                = string containing the Lctn of the node
 *      long    lNodeNewPolTsInMicroSecs = new proof of life timestamp for the node specified above
 *      long    lTsInMicroSecs           = time that this node's proof of life timestamp changed
 *      String  sReqAdapterType          = type of adapter that requested this stored procedure (PROVISIONER)
 *      long    lReqWorkItemId           = work item id that the requesting adapter was performing when it requested this stored procedure (-1 is used when there is no work item yet associated with this change)
 *
 *  Return value:
 *      0L = Everything completed fine
 */

public class ComputeNodeSetProofOfLifeTs extends ComputeNodeCommon {
    public final SQLStmt selectNode = new SQLStmt("SELECT * FROM ComputeNode WHERE Lctn=?;");
    public final SQLStmt updateNode = new SQLStmt("UPDATE ComputeNode SET ProofOfLifeTimestamp=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE Lctn=?;");


    public long run(String sNodeLctn, long lNodeNewPolTsInMicroSecs, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //----------------------------------------------------------------------
        // Grab the current record for this Lctn out of the "active" ComputeNode table.
        //      This information is used for determining whether the "new" record is indeed more recent than the record already in the table,
        //      which is necessary as "time is not a stream", and we can get records out of order (from a strict timestamp of occurrence point of view).
        //----------------------------------------------------------------------
        voltQueueSQL(selectNode, EXPECT_ZERO_OR_ONE_ROW, sNodeLctn);
        VoltTable[] aNodeData = voltExecuteSQL();
        if (aNodeData[0].getRowCount() == 0) {
            throw new VoltAbortException("ComputeNodeSetProofOfLifeTs - there is no entry in the ComputeNode table for the specified " +
                                         "node lctn(" + sNodeLctn + ") - ReqAdapterType=" + sReqAdapterType + ", ReqWorkItemId=" + lReqWorkItemId + "!");
        }
        // Get the current record in the "active" table's LastChgTimestamp (in micro-seconds since epoch).
        aNodeData[0].advanceRow();
        long lCurRecsLastChgTimestampTsInMicroSecs = aNodeData[0].getTimestampAsTimestamp("LastChgTimestamp").getTime();

        //----------------------------------------------------------------------
        // Ensure that we aren't updating this row with the exact same LastChgTimestamp (or an older value) that currently exists in the table.
        //----------------------------------------------------------------------
        // Check & see if this timestamp is the same or older than the LastChgTimestamp on the current record (in the ComputeNode table).
        if (lTsInMicroSecs <= lCurRecsLastChgTimestampTsInMicroSecs)
        {
            lTsInMicroSecs = lCurRecsLastChgTimestampTsInMicroSecs + 1;
            lTsInMicroSecs = ensureHaveUniqueComputeNodeLastChgTimestamp(sNodeLctn, lTsInMicroSecs, lCurRecsLastChgTimestampTsInMicroSecs);
        }

        //----------------------------------------------------------------------
        // NOTE: we are NOT creating an entry in the ComputeNode_History table for this update of this ComputeNode's ProofOfLife value.
        //       The reason is that we only need this value needs to be current in the ComputeNode table, we do not need it current in the ComputeNode_History table.
        //       We consider this update to be very similar to the WorkItem WorkingResults value (happen frequently with no reason to be saved away in the history table).
        //       - This choice to not update the History table was made in a deliberate manner, other than a very few specialized cases the history table should always
        //         reflect all updates done to the "active" table.
        //----------------------------------------------------------------------

        //----------------------------------------------------------------------
        // Update the record for this Lctn in the "active" table.
        //----------------------------------------------------------------------
        voltQueueSQL(updateNode, lNodeNewPolTsInMicroSecs, this.getTransactionTime(), lTsInMicroSecs, sReqAdapterType, lReqWorkItemId, sNodeLctn);
        voltExecuteSQL(true);

        return 0L;
    }
}
