// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/**
 *  This stored procedure returns an array of VoltTables:
 *      the first contains the list of compute nodes that are being serviced (compute nodes which have an Owner = 'S')
 *      the second contains the list of service nodes that are being serviced (service nodes which have an Owner = 'S')
 */
public class NodeListBeingServiced extends VoltProcedure {

    public final SQLStmt selectComputeNodesSql = new SQLStmt(
        "SELECT Lctn FROM ComputeNode WHERE OWNER='S';"
    );

    public final SQLStmt selectServiceNodesSql = new SQLStmt(
        "SELECT Lctn FROM ServiceNode WHERE OWNER='S';"
    );

    public VoltTable[] run() throws VoltAbortException {
        voltQueueSQL(selectComputeNodesSql);
        voltQueueSQL(selectServiceNodesSql);
        VoltTable[] aVt = voltExecuteSQL(true);
        return aVt;
    }
}
