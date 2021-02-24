// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Arrays;
import java.util.BitSet;
import static java.lang.Math.toIntExact;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary to record the information when an Adapter starts.
 *
 *  Input parameter:
 *      String sReqAdapterType  = Type of adapter that requested this stored procedure
 *      long   lReqWorkItemId   = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *      String sLctn            = Location string that this adapter instance was started on/for
 *      long   lPid             = Process id of this adapter instance
 *
 *  Returns: long lUniqueAdapterId = The adapter id for the adapter that was just "started"
 */

public class AdapterStarted extends VoltProcedure {


    public final SQLStmt insertAdapterSql = new SQLStmt(
            "INSERT INTO Adapter (Id, AdapterType, SconRank, State, DbUpdatedTimestamp, LastChgAdapterType, LastChgWorkItemId, Lctn, Pid) " +
            "VALUES (?, ?, ?, 'A', CURRENT_TIMESTAMP, ?, ?, ?, ?);"
    );

    public final SQLStmt insertAdapterHistorySql = new SQLStmt(
            "INSERT INTO Adapter_History (Id, AdapterType, SconRank, State, DbUpdatedTimestamp, LastChgAdapterType, LastChgWorkItemId, Lctn, Pid) " +
            "VALUES (?, ?, ?, 'A', CURRENT_TIMESTAMP, ?, ?, ?, ?);"
    );

    public final SQLStmt selectUniqueIdSql = new SQLStmt("SELECT NextValue FROM UniqueValues WHERE Entity = ? Order By Entity;");
    public final SQLStmt updateUniqueIdSql = new SQLStmt("UPDATE UniqueValues SET NextValue = NextValue + 1, DbUpdatedTimestamp = ? WHERE Entity = ?;");
    public final SQLStmt insertUniqueIdSql = new SQLStmt("INSERT INTO UniqueValues (Entity, NextValue, DbUpdatedTimestamp) VALUES (?, ?, ?);");



    public long run(String sReqAdapterType, long lReqWorkItemId, String sLctn, long lPid) throws VoltAbortException {
        //---------------------------------------------------------------------
        // Ensure that the specified adapter type is a valid adapter type
        // - when adding a new adapter type also add to the WorkItemQueue.java stored procedure.
        //---------------------------------------------------------------------
        switch(sReqAdapterType.toUpperCase()) {
            case "WLM":
            case "PROVISIONER":
            case "RAS":
            case "ONLINE_TIER":
            case "NEARLINE_TIER":
            case "MONITOR":
            case "RM_RTE":
            case "FM":
            case "UI":
            case "CONTROL":
            case "DIAGNOSTICS":
            case "DAI_MGR":
            case "SERVICE":
            case "INVENTORY":
            case "POWER_MANAGER":
            case "INITIALIZATION":
            case "ALERT_MGR":
            case "FABRICCRIT":
            case "FABRICPERF":
            case "FABRIC":
                break;
            default:
                throw new VoltAbortException("AdapterStarted - can't start adapter because an invalid AdapterType was specified (" + sReqAdapterType + ")!");
        }

        //--------------------------------------------------
        // Generate a unique adapter id for this new adapter.
        //--------------------------------------------------
        final String Entity = "Adapter".toUpperCase();
        // Get the current "next unique id" for the specified entity.
        voltQueueSQL(selectUniqueIdSql, EXPECT_ZERO_OR_ONE_ROW, Entity);
        VoltTable[] uniqueId = voltExecuteSQL();
        // Check and see if there is a matching record for the specified entity
        if (uniqueId[0].getRowCount() == 0) {
            // No matching record for the specified entity - add a new row for the specified entity
            voltQueueSQL(insertUniqueIdSql, Entity, 1, this.getTransactionTime());
            voltExecuteSQL();
            // Now redo the above query (to get the current "next unique id" for the specified entity)
            voltQueueSQL(selectUniqueIdSql, EXPECT_ONE_ROW, Entity);
            uniqueId = voltExecuteSQL();
        }
        // Save away the generated unique adapter id.
        long lUniqueAdapterId = uniqueId[0].asScalarLong();
        // Bump the current "next unique id" to generate the next "next unique id" for the specified entity.
        voltQueueSQL(updateUniqueIdSql, EXPECT_ONE_ROW, this.getTransactionTime(), Entity);

        // Temporarily use the new AdapterId also as the Scon rank - until we figure out what to do with and how to assign scon ranks.
        long lAdapterSconRank = lUniqueAdapterId;

        //---------------------------------------------------------------------
        // Insert a new row into the Adapter table for this adapter
        //---------------------------------------------------------------------
        voltQueueSQL(insertAdapterSql, lUniqueAdapterId, sReqAdapterType, lAdapterSconRank, sReqAdapterType, lReqWorkItemId, sLctn, lPid);

        //---------------------------------------------------------------------
        // Also insert a copy of this information into the History table.
        //---------------------------------------------------------------------
        voltQueueSQL(insertAdapterHistorySql, lUniqueAdapterId, sReqAdapterType, lAdapterSconRank, sReqAdapterType, lReqWorkItemId, sLctn, lPid);
        voltExecuteSQL(true);

        // Return this new adapter's id to the caller.
        return lUniqueAdapterId;
    }
}
