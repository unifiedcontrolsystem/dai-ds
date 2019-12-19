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
 * - that contains the Lctn, State, and BootImageId information for each of the nodes in the specified node list that are NOT booted with the specified BootImageId
 * (one row in the VoltTable for each node).
 */

public class ComputeNodesFromListWithoutThisBootImageId extends VoltProcedure {

    public final SQLStmt select = new SQLStmt("SELECT Lctn, State, BootImageId FROM ComputeNode WHERE ((Lctn IN ?) AND (BootImageId != ?));");


    public VoltTable run(String[] aNodeLctns, String sBootImageId) throws VoltAbortException {

        voltQueueSQL(select, aNodeLctns, sBootImageId);
        VoltTable[] aVt = voltExecuteSQL();

        return aVt[0];
    }
}
