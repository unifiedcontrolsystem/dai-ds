// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Get the adapter invocation information out of the MachineAdapterInstance table.
 *
 *  Input parameter:
 *      String  sAdapterType    = string specifying which type of adapter instance is needed
 *      String  sSnLctn         = string specifying which DAI Mgr should start the new adapter instance
 */

public class MachineAdapterInvocationInformation extends VoltProcedure {

    public final SQLStmt getAdapterInvocationInfoNoSnlctn  = new SQLStmt("SELECT * FROM MachineAdapterInstance WHERE (ADAPTERTYPE=?) ORDER BY AdapterType, SnLctn;");
    public final SQLStmt getAdapterInvocationInfoForSnlctn = new SQLStmt("SELECT * FROM MachineAdapterInstance WHERE (ADAPTERTYPE=? AND SnLctn=?) ORDER BY AdapterType, SnLctn;");


    public VoltTable run(String sAdapterType, String sSnLctn) throws VoltAbortException {
        // Get the adapter invocation information for the specified type of adapter and which service node it needs to run on (information may be different depending on where it needs to run).
        if ((sSnLctn == null) || (sSnLctn.isEmpty())) {
            // there is no special service node that needs to run this adapter instance.
            // Find the invocation information for this type of adapter, in this case there should be exactly one entry in the MachineAdapterInstance table.
            voltQueueSQL(getAdapterInvocationInfoNoSnlctn, EXPECT_ONE_ROW, sAdapterType);
        }
        else {
            // this adapter instance does need to run on the specified service node.
            // Find the invocation information for this type of adapter on this particular service node.
            voltQueueSQL(getAdapterInvocationInfoForSnlctn, EXPECT_ONE_ROW, sAdapterType, sSnLctn);
        }
        VoltTable[] aAdapterInvocationData = voltExecuteSQL(true);
        // Return the information to the caller.
        return aAdapterInvocationData[0];
    }
}
