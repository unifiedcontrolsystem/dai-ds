// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Get the information from the active adapter instance table (Adapter) using the specified type of adapter, the lctn of the service node it is running on, and possibly the pid of that adapter instance.
 *
 *  Input parameter:
 *      String  sAdaptersType = string specifying which type of adapter instance is needed
 *      String  sAdaptersLctn = string specifying the location of the service node that we are interested in
 *      long    lAdaptersPid  = optional parameter specifying the pid of the adapter instance that we are interested in
 *                  <0 = give me a list of all adapter instances with the specified adapter type and adapter lctn
 *                 >=0 = give me the information for the single adapter instance with the specified adapter type, adapter lctn, and with the specified pid
 */

public class AdapterInfoUsingTypeLctnPid extends VoltProcedure {

    public final SQLStmt getAdapterInvocationInfoNoPid  = new SQLStmt("SELECT * FROM Adapter WHERE (ADAPTERTYPE=? AND Lctn=?) ORDER BY AdapterType, Lctn, Pid;");
    public final SQLStmt getAdapterInvocationInfoForPid = new SQLStmt("SELECT * FROM Adapter WHERE (ADAPTERTYPE=? AND Lctn=? AND Pid=?) ORDER BY AdapterType, Lctn, Pid;");


    public VoltTable run(String sAdaptersType, String sAdaptersLctn, long lAdaptersPid) throws VoltAbortException {
        // Get the active adapter information for the specified type of adapter and which service node it is running on and if specified its process id.
        if (lAdaptersPid < 0) {
            // not interested in a specific pid - return the whole list of adapters of the specified type and running on the specified service node.
            voltQueueSQL(getAdapterInvocationInfoNoPid, sAdaptersType, sAdaptersLctn);
        }
        else {
            // interested in a single adapter instance with the specified type of adapter and which service node it is running on and its process id.
            voltQueueSQL(getAdapterInvocationInfoForPid, EXPECT_ZERO_OR_ONE_ROW, sAdaptersType, sAdaptersLctn, lAdaptersPid);
        }
        VoltTable[] aAdapterInfo = voltExecuteSQL(true);
        // Return the information to the caller.
        return aAdapterInfo[0];
    }
}
