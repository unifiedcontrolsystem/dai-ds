// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 *
 */

public class ComputeNodeCommon extends VoltProcedure {

    public final SQLStmt selectComputeNodeHistoryWithThisTimestamp = new SQLStmt("SELECT Lctn FROM ComputeNode_History WHERE Lctn=? AND LastChgTimestamp=?;");
    public final SQLStmt selectNode = new SQLStmt("SELECT * FROM ComputeNode WHERE Lctn=?;");
    public final SQLStmt selectNodeHistoryWithPrecedingTs = new SQLStmt("SELECT * FROM ComputeNode_History WHERE (Lctn=? AND LastChgTimestamp<?) ORDER BY LastChgTimestamp DESC LIMIT 1;");

    public VoltTable[] getComputeNode(String sNodeLctn) {
        voltQueueSQL(selectNode, EXPECT_ZERO_OR_ONE_ROW, sNodeLctn);
        return voltExecuteSQL();
    }

    public VoltTable[] getComputeNodeFromHistoryWithPrecedingTime(String sNodeLctn, long lTsInMicroSecs) {
        voltQueueSQL(selectNodeHistoryWithPrecedingTs, sNodeLctn, lTsInMicroSecs);
        return voltExecuteSQL();
    }


    //----------------------------------------------------------------------
    // Ensure that the we have a unique timestamp that we can use for inserting a record into the ComputeNode_History table
    // (it is unique meaning that no other history record for this specified lctn is already using it in the LastChgTimestamp column).
    // - needed as we want the records to have unique timestamps for that field.
    //----------------------------------------------------------------------
    public long ensureHaveUniqueComputeNodeLastChgTimestamp(String sNodeLctn, long lNewRecordsTsInMicroSecs, long lCurRecordsTsInMicroSecs)
    {
        //----------------------------------------------------------------------
        // Ensure that we aren't inserting multiple records into history with the same timestamp (down to millisecond granularity).
        //----------------------------------------------------------------------
        ///System.out.println("ensureHaveUniqueComputeNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - On Entry");
        boolean bPhase2IsBad = true;
        long lCntr = 0L;
        while (bPhase2IsBad) {
            if (lCntr > 0)
                System.out.println("ensureHaveUniqueComputeNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - Doing a Recheck!");
            //--------
            // Phase 1 - make sure that this new timestamp is not the same as the current db value from the ComputeNode table.
            //--------
            // Check & see if this timestamp is the same as the timestamp on the current record (in the ComputeNode table).
            if ( lNewRecordsTsInMicroSecs == lCurRecordsTsInMicroSecs )
            {
                // these 2 records have the same timestamp - bump the number of microseconds for this new record.
                ++lNewRecordsTsInMicroSecs;  // bump by 1 microsecond.
                System.out.println("ensureHaveUniqueComputeNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - Bumped in Phase1!");
            }
            //--------
            // Phase 2 - make sure that this new timestamp is not the same as an already existing record in the ComputeNode_History table.
            //--------
            // Query and see if this timestamp is already in the history table for this specific lctn.
            voltQueueSQL(selectComputeNodeHistoryWithThisTimestamp, EXPECT_ZERO_OR_ONE_ROW, sNodeLctn, lNewRecordsTsInMicroSecs);
            VoltTable[] aComputeNodeHistoryWithThisTimestamp = voltExecuteSQL();
            if (aComputeNodeHistoryWithThisTimestamp[0].getRowCount() > 0) {
                // there is already an existing record in the ComputeNode_History table for this lctn that has this timestamp.
                // Bump the number of microseconds for this new record.
                ++lNewRecordsTsInMicroSecs;  // bump by 1 microsecond.
                System.out.println("ensureHaveUniqueComputeNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - Bumped in Phase2!");
            }
            else
                // there was not a match in the history table for this lctn with this timestamp - good to go.
                bPhase2IsBad = false;  // indicate that this timestamp is good.

            ++lCntr;
        }   // ensure no constraint violation due to multiple records with the same timestamp.
        ///System.out.println("ensureHaveUniqueComputeNodeLastChgTimestamp - " + sNodeLctn + " - lNewRecordsTsInMicroSecs=" + lNewRecordsTsInMicroSecs + " - LastChgTimestamp=" + lCurRecordsTsInMicroSecs + " - After both Phases OK");

        return lNewRecordsTsInMicroSecs;  // return a timestamp that is unique.
    }   // End ensureHaveUniqueComputeNodeLastChgTimestamp(String sNodeLctn, long lNewRecordsTsInMicroSecs, long lCurRecordsTsInMicroSecs)

}
