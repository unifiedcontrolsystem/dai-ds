// Copyright (C) 2018 Intel Corporation
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
 * Temporary method being used by the demo GUI to directly query VoltDB - allows the specification of LIMIT and OFFSET parms.
 */

public class RasEventListByLimit extends VoltProcedure {

    public final SQLStmt selectRasEvents = new SQLStmt(
        "SELECT RasEvent.EventType, RasEvent.LastChgTimestamp, RasEvent.DbUpdatedTimestamp, RasMetaData.Severity, RasEvent.Lctn, RasEvent.JobId, RasEvent.ControlOperation, RasMetaData.Msg, RasEvent.InstanceData FROM RasEvent " +
            "INNER JOIN RasMetaData on RasEvent.EventType=RasMetaData.EventType " +
        "ORDER BY RasEvent.DbUpdatedTimestamp ASC, EventType, Id " +
        "LIMIT ? OFFSET ?;"
    );

    public VoltTable run(long lLimitNumber, long lOffsetNumber) throws VoltAbortException {
        voltQueueSQL(selectRasEvents, lLimitNumber, lOffsetNumber);
        VoltTable[] aListOfRasEvents = voltExecuteSQL(true);
        return aListOfRasEvents[0];
    }
}
