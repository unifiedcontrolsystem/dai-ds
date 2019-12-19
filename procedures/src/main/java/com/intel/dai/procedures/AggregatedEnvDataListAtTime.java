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
import org.voltdb.types.TimestampType;


/**
 * This stored procedure is "Temporary" for use during the DAI Prototype, during the prototype we are using VoltDB to represent the entire data store not just Tier1.
 * The product implementation SHOULD NOT use this, but rather should be getting the data out of Tier2 (Postgres) probably utilizing its 'Distinct On feature'
 * for optimizing performance of this function!
 */

public class AggregatedEnvDataListAtTime extends VoltProcedure {

    public final SQLStmt selectOnlyEndTimeSql = new SQLStmt(
        "SELECT * FROM Tier2_AggregatedEnvData WHERE (Timestamp<=?) ORDER BY Lctn, Timestamp DESC;"
    );

    public final SQLStmt selectBothEndAndStartTimeSql = new SQLStmt(
        "SELECT * FROM Tier2_AggregatedEnvData WHERE (Timestamp<=? AND Timestamp>=?) ORDER BY Lctn, Timestamp DESC;"
    );


    public VoltTable run(TimestampType sEndingTimestamp, TimestampType sStartingTimestamp) throws VoltAbortException {
        // Check & see if the caller specified an ending timestamp.
        boolean bEndingTsSpecified = (sEndingTimestamp != null);

        // Check & see if the caller specified a starting timestamp.
        boolean bStartingTsSpecified = (sStartingTimestamp != null);

        //----------------------------------------------------------------------
        // Starting Time was specified
        //----------------------------------------------------------------------
        if (bStartingTsSpecified) {
            if (bEndingTsSpecified)
                voltQueueSQL(selectBothEndAndStartTimeSql,
                             sEndingTimestamp,
                             sStartingTimestamp);

            else
                voltQueueSQL(selectBothEndAndStartTimeSql,
                             this.getTransactionTime(),  // use current timestamp
                             sStartingTimestamp);
        }
        //----------------------------------------------------------------------
        // Starting Time was NOT specified
        //----------------------------------------------------------------------
        else {
            if (bEndingTsSpecified)
                voltQueueSQL(selectOnlyEndTimeSql, sEndingTimestamp);
            else
                voltQueueSQL(selectOnlyEndTimeSql, this.getTransactionTime());  // use current timestamp
        }

        VoltTable[] aListOfAggEnvData = voltExecuteSQL(true);

        //----------------------------------------------------------------------
        // Returns the information to the caller.
        //----------------------------------------------------------------------
        return aListOfAggEnvData[0];
    }
}
