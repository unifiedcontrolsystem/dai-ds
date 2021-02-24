// Copyright (C) 2020-2020 Intel Corporation
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
 * This stored procedure returns an array of VoltTables:
 * - the first contains the details for the list of RasEvents that are not yet finished (need the RAS adapter to do work on them)
 * - the second contains the maximum value for the LastChgTimestamp column in the above-mentioned list of RasEvents
 */
public class RasEventListThatNeedToBeDone extends VoltProcedure {

    //--------------------------------------------------------------------------
    // These sql stmts are optimized versions which avoids use of OR constructs (as Volt is unable to optimize over OR).
    // - this query is equivalent to
    //      SELECT DescriptiveName, Id, Lctn, LastChgTimestamp, ControlOperation FROM RasEvent WHERE ((Done!='Y') AND ((DbUpdatedTimestamp <= ?) OR ((JobId IS NULL) OR (JobId != '?')))) Order By DbUpdatedTimestamp ASC;
    // Note: DO NOT change (Done!='Y') to be (Done='N')!!!  There is a specialized partial index which also is part of this optimization!!!
    //--------------------------------------------------------------------------
    public final SQLStmt selectUnfinishedRasEvents = new SQLStmt (
        "SELECT DescriptiveName, Id, Lctn, JobId, ControlOperation, LastChgTimestamp FROM RasEvent WHERE ((Done!='Y') AND (NOT(NOT(DbUpdatedTimestamp<=?) AND NOT(NOT(JobId='?'))))) ORDER BY DbUpdatedTimestamp ASC;"
    );
    public final SQLStmt selectUnfinishedRasEventsMaxLastChgTs = new SQLStmt (
        "SELECT MAX(LastChgTimestamp) FROM RasEvent WHERE ((Done!='Y') AND (NOT(NOT(DbUpdatedTimestamp<=?) AND NOT(NOT(JobId='?')))));"
    );



    public VoltTable[] run(long lEndTsInMicroSecs) throws VoltAbortException {
        // Get the list of RAS events that are not yet finished.
        voltQueueSQL(selectUnfinishedRasEvents, lEndTsInMicroSecs);

        // Get the maximum LastChgTimestamp for the above-mentioned list of RAS events.
        voltQueueSQL(selectUnfinishedRasEventsMaxLastChgTs, lEndTsInMicroSecs);

        VoltTable[] aVt = voltExecuteSQL(true);
        return aVt;
    }
}
