// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a WLM Reservation is updated.
 *
 *  Input parameter:
 *      String sReservationName     = Reservation's name
 *      String sUsers               = Reservation's users
 *      String sNodes               = Reservation's nodes (WLM node names)
 *      long   lStartTsInMicroSecs  = Time this reservation starts in units of micro-seconds since the epoch (utc)
 *      long   lEndTsInMicroSecs    = Time this reservation ends   in units of micro-seconds since the epoch (utc)
 *      long   lTsInMicroSecs       = Time that this reservation event occurred
 *      String sReqAdapterType      = Type of adapter that requested this stored procedure
 *      long   lReqWorkItemId       = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *
 */

public class ReservationUpdated extends VoltProcedure {

    public final SQLStmt insertHistorySql = new SQLStmt(
                    "INSERT INTO WlmReservation_History " +
                    "(ReservationName, Users, Nodes, StartTimestamp, EndTimestamp, DeletedTimestamp, LastChgTimestamp, DbUpdatedTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );



    public long run(String sReservationName, String sUsers, String sNodes, long lStartTsInMicroSecs, long lEndTsInMicroSecs, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException
    {
        //---------------------------------------------------------------------
        // Insert this information into the WlmReservation_History table.
        //---------------------------------------------------------------------
        voltQueueSQL(insertHistorySql
                    ,sReservationName
                    ,sUsers
                    ,sNodes
                    ,lStartTsInMicroSecs        // StartTimestamp
                    ,lEndTsInMicroSecs          // EndTimestamp
                    ,null                       // DeletedTimestamp
                    ,lTsInMicroSecs             // LastChgTimestamp
                    ,this.getTransactionTime()  // DbUpdatedTimestamp
                    ,sReqAdapterType            // LastChgAdapterType
                    ,lReqWorkItemId             // LastChgWorkItemId
                    );
        voltExecuteSQL(true);
        return 0;
    }
}
