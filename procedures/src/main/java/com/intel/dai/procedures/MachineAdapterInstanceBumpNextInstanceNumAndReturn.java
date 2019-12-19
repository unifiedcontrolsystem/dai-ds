// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Get the next NumStartedInstances value from the MachineAdapterInstance table (bump the value in the db and return that value to caller).
 *
 *  Input parameter:
 *      String  sAdapterType    = string specifying which type of adapter instance is needed
 *      String  sSnLctn         = string specifying which DAI Mgr should start the new adapter instance
 */

public class MachineAdapterInstanceBumpNextInstanceNumAndReturn extends VoltProcedure {

    public final SQLStmt getAdapterInvocationInfoForSnlctn  = new SQLStmt("SELECT * FROM MachineAdapterInstance WHERE (ADAPTERTYPE=? AND SnLctn=?) ORDER BY AdapterType, SnLctn;");
    public final SQLStmt updateNumStartedInstancesForSnlctn = new SQLStmt("UPDATE MachineAdapterInstance SET NumStartedInstances=? WHERE (ADAPTERTYPE=? AND SnLctn=?);");
    public final SQLStmt insertMachineAdapterInstanceHistorySql = new SQLStmt("INSERT INTO " +
            "MachineAdapterInstance_History(SnLctn, AdapterType, NumInitialInstances, NumStartedInstances, Invocation, LogFile, DbUpdatedTimestamp) " +
            "VALUES(?,?,?,?,?,?,?)");


    public long run(String sAdapterType, String sSnLctn) throws VoltAbortException {
        // Get the adapter invocation information for the specified type of adapter and which service node it needs to run on (information may be different depending on where it needs to run).
        voltQueueSQL(getAdapterInvocationInfoForSnlctn, EXPECT_ONE_ROW, sAdapterType, sSnLctn);
        VoltTable[] aAdapterInvocationData = voltExecuteSQL();
        aAdapterInvocationData[0].advanceRow();

        // Bump the NumStartedInstances value.
        long lNewNumStartedInstances = aAdapterInvocationData[0].getLong("NumStartedInstances") + 1L;
        voltQueueSQL(updateNumStartedInstancesForSnlctn, lNewNumStartedInstances, sAdapterType, sSnLctn);

        voltQueueSQL(insertMachineAdapterInstanceHistorySql,
                sSnLctn,
                sAdapterType,
                aAdapterInvocationData[0].getLong("NumInitialInstances"),
                lNewNumStartedInstances,
                aAdapterInvocationData[0].getString("Invocation"),
                aAdapterInvocationData[0].getString("LogFile"),
                this.getTransactionTime());

        voltExecuteSQL(true);   // Note: true indicates that this is the final batch of sql stmts for this procedure.

        // Return the information to the caller.
        return lNewNumStartedInstances;
    }
}
