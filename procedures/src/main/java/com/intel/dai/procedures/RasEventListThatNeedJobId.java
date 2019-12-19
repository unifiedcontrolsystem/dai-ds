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
 */

public class RasEventListThatNeedJobId extends VoltProcedure {

	public final SQLStmt selectRasEventsSql = new SQLStmt(
		"SELECT Id, EventType, Lctn, LastChgTimestamp FROM RasEvent WHERE ((JobID = '?') AND (Lctn IS NOT NULL) AND (DbUpdatedTimestamp <= ?)) Order By EventType, Id;"
	);

	public VoltTable run(long lEndTsInMicroSecs) throws VoltAbortException {
		voltQueueSQL(selectRasEventsSql, lEndTsInMicroSecs);
		VoltTable[] aListOfRasEvents = voltExecuteSQL(true);
		return aListOfRasEvents[0];
	}
}
