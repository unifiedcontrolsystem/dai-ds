// Copyright (C) 2018 Intel Corporation
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
 * This stored procedure returns a single VoltTable:
 * - that contains the Lctn, State information for each of the nodes in the specified node list
 * (one row in the VoltTable for each node).
 */

public class ComputeNodeStates extends VoltProcedure {

    public final SQLStmt select = new SQLStmt("SELECT Lctn, State FROM ComputeNode WHERE Lctn IN ? ORDER BY Lctn;");

    public VoltTable run(String[] aNodeLctns) throws VoltAbortException {
        voltQueueSQL(select, (Object) aNodeLctns);
        VoltTable[] aVt = voltExecuteSQL();
        return aVt[0];
    }
}
