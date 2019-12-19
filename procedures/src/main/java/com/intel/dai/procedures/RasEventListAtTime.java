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
 */

public class RasEventListAtTime extends VoltProcedure {

    public final SQLStmt selectRasEventsOnlyEndTimeSql = new SQLStmt(
        "SELECT RasEvent.EventType, RasEvent.LastChgTimestamp, RasEvent.DbUpdatedTimestamp, RasMetaData.Severity, RasEvent.Lctn, RasEvent.JobId, RasEvent.ControlOperation, RasMetaData.Msg, RasEvent.InstanceData FROM RasEvent " +
            "INNER JOIN RasMetaData on RasEvent.EventType=RasMetaData.EventType " +
        "WHERE RasEvent.DbUpdatedTimestamp<=? " +
        "ORDER BY RasEvent.DbUpdatedTimestamp DESC, EventType, Id LIMIT 10000;"  // ToDo: Horrible hack to limit to 10,000 rows until the UI Adapter available and GUI is switched to use Tier2 for fetching its data (Tier2 RasEvent table will have a unique event counter for paging).
    );

    public final SQLStmt selectRasEventsBothEndAndStartTimeSql = new SQLStmt(
        "SELECT RasEvent.EventType, RasEvent.LastChgTimestamp, RasEvent.DbUpdatedTimestamp, RasMetaData.Severity, RasEvent.Lctn, RasEvent.JobId, RasEvent.ControlOperation, RasMetaData.Msg, RasEvent.InstanceData FROM RasEvent " +
            "INNER JOIN RasMetaData on RasEvent.EventType=RasMetaData.EventType " +
        "WHERE RasEvent.DbUpdatedTimestamp<=? AND RasEvent.DbUpdatedTimestamp>=? " +
        "ORDER BY RasEvent.DbUpdatedTimestamp DESC, EventType, Id LIMIT 10000;"  // ToDo: Horrible hack to limit to 10,000 rows until the UI Adapter available and GUI is switched to use Tier2 for fetching its data (Tier2 RasEvent table will have a unique event counter for paging).
    );



    public VoltTable run(TimestampType sEndingTimestamp, TimestampType sStartingTimestamp) throws VoltAbortException {

        // Check & see if the caller specified an ending timestamp.
        boolean bEndingTsSpecified = false;  // initialize that the ending timestamp was NOT specified.
        if (sEndingTimestamp != null)
            bEndingTsSpecified = true;

        // Check & see if the caller specified a starting timestamp.
        boolean bStartingTsSpecified = false;  // initialize that the starting timestamp was NOT specified.
        if (sStartingTimestamp != null)
            bStartingTsSpecified = true;


        //----------------------------------------------------------------------
        // If Starting Time was specified
        //----------------------------------------------------------------------
        if (bStartingTsSpecified) {
            if (bEndingTsSpecified)
                voltQueueSQL(selectRasEventsBothEndAndStartTimeSql,
                             sEndingTimestamp,
                             sStartingTimestamp);

            else
                voltQueueSQL(selectRasEventsBothEndAndStartTimeSql,
                             this.getTransactionTime(),  // use current timestamp
                             sStartingTimestamp);
        }
        //----------------------------------------------------------------------
        // If Starting Time was NOT specified
        //----------------------------------------------------------------------
        else {
            if (bEndingTsSpecified)
                voltQueueSQL(selectRasEventsOnlyEndTimeSql, sEndingTimestamp);
            else
                voltQueueSQL(selectRasEventsOnlyEndTimeSql, this.getTransactionTime());  // use current timestamp
        }

        VoltTable[] aListOfRasEvents = voltExecuteSQL(true);

        //----------------------------------------------------------------------
        // Returns the information to the caller.
        //----------------------------------------------------------------------
        return aListOfRasEvents[0];
    }
}
