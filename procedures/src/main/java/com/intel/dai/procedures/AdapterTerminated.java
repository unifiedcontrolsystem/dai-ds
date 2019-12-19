// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Arrays;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when an adapter goes away / terminates.
 *
 *  Input parameter:
 *      String sAdapterType     = The type of adapter that is being terminated.
 *      long   lAdapterId       = The adapter id of the adapter that is being terminated.
 *      String sReqAdapterType  = The type of adapter that requested the specified adapter be terminated.
 *      long   lReqWorkItemId   = Work Item Id that the requesting adapter was performing when it requested this adapter be terminated.
 */

public class AdapterTerminated extends VoltProcedure {

    public final SQLStmt selectAdapterSql = new SQLStmt("SELECT * FROM Adapter WHERE AdapterType = ? AND Id = ? Order By AdapterType, Id;");

    public final SQLStmt insertTerminatedAdapterIntoHistorySql = new SQLStmt(
            "INSERT INTO Adapter_History " +
            "(Id, AdapterType, SconRank, State, DbUpdatedTimestamp, LastChgAdapterType, LastChgWorkItemId, Lctn, Pid) " +
            "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP, ?, ?, ?, ?);"
    );

    public final SQLStmt deleteAdapterSql = new SQLStmt("DELETE FROM Adapter WHERE AdapterType = ? AND Id = ?;");



    public long run(String sAdapterType, long lAdapterId, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException {
        //--------------------------------------------------
        // Grab the object's pre-change values so they are available for use creating the history record.
        //--------------------------------------------------
        voltQueueSQL(selectAdapterSql, EXPECT_ZERO_OR_ONE_ROW, sAdapterType, lAdapterId);
        VoltTable[] aAdapterData = voltExecuteSQL();

        // Short-circuit if the adapter instance has already been removed
        // (this could happen when the adapter instance cleaned up after itself correctly, but then the DaiMgr detected that this adapter instance ended and also calls this procedure).
        if (aAdapterData[0].getRowCount() == 0)
            return 0;  // no row to delete.

        aAdapterData[0].advanceRow();

        //---------------------------------------------------------------------
        // Delete the adapter's row out of the active Adapter table (as it is now Done)
        //---------------------------------------------------------------------
        voltQueueSQL(deleteAdapterSql, EXPECT_ONE_ROW, sAdapterType, lAdapterId);

        //--------------------------------------------------
        // Insert the Terminated record for this adapter into the history table
        //--------------------------------------------------
        voltQueueSQL(insertTerminatedAdapterIntoHistorySql
                    ,lAdapterId                             // Id of the adapter being terminated
                    ,sAdapterType                           // Adapter type of the adapter being terminated
                    ,aAdapterData[0].getLong("SconRank")
                    ,"T"                                    // State = Terminated
                    ,sReqAdapterType                        // LastChgAdapterType
                    ,lReqWorkItemId                         // LastChgWorkItemId
                    ,aAdapterData[0].getString("Lctn")
                    ,aAdapterData[0].getLong("Pid")
                    );
        voltExecuteSQL(true);

        return 1;   // deleted 1 row.
    }

}