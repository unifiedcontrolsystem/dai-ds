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
 * This stored procedure returns an array of VoltTables:
 * - the first contains the details for the list of RasEvents which have specified that UCS should check for an associated WLM Job ID
 * - the second contains the maximum value for the LastChgTimestamp column in the above-mentioned list of RasEvents
 */
public class RasEventListThatNeedJobId extends VoltProcedure {
	public final SQLStmt selectRasEventsSql = new SQLStmt(
			"SELECT Id, EventType, Lctn, LastChgTimestamp  FROM RasEvent WHERE ((JobID = '?') AND (Lctn IS NOT NULL) AND (DbUpdatedTimestamp <= ?)) Order By EventType, Id;"
	);

	public final SQLStmt selectRasEventsMaxSql = new SQLStmt(
			"SELECT MAX(LastChgTimestamp) FROM RasEvent WHERE ((JobID = '?') AND (Lctn IS NOT NULL) AND (DbUpdatedTimestamp <= ?)) Order By EventType, Id;"
	);

	public VoltTable[] run(long lEndTsInMicroSecs) throws VoltAbortException {
		// Get the list of RAS events that we need to try and find any associated job id.
		voltQueueSQL(selectRasEventsSql, lEndTsInMicroSecs);

		// Get the maximum LastChgTimestamp for the above-mentioned list of RAS events.
		voltQueueSQL(selectRasEventsMaxSql, lEndTsInMicroSecs);

		VoltTable[] aVt = voltExecuteSQL(true);
		return aVt;
	}
}
