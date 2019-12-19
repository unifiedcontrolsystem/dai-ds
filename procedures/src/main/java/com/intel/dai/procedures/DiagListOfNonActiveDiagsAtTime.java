// Copyright (C) 2017-2018 Intel Corporation
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
 * This stored procedure is "Temporary" for use during the DAI Prototype, during the prototype we are using VoltDB to represent the entire data store not just Tier1.
 * The product implementation SHOULD NOT use this, but rather should be getting the data out of Tier2 (Postgres) probably utilizing Distinct On feature
 * for optimizing performance of this function!
 *
 */

public class DiagListOfNonActiveDiagsAtTime extends VoltProcedure {

    public final SQLStmt selectWithTimeSql = new SQLStmt(
        "SELECT * FROM Diag WHERE EndTimestamp<=? Order By DiagId DESC;"
    );


    public VoltTable run(TimestampType sEndingTimestamp) throws VoltAbortException {
        // Ending time was specified
        if (sEndingTimestamp != null)
            voltQueueSQL(selectWithTimeSql, sEndingTimestamp);
        // Ending time was NOT specified - use current time
        else
            voltQueueSQL(selectWithTimeSql, this.getTransactionTime());

        return voltExecuteSQL()[0];
    }
}
