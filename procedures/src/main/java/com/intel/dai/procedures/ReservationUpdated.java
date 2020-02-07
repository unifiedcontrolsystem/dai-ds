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

    public final SQLStmt selectReservationSql = new SQLStmt("SELECT * FROM WlmReservation_History WHERE ReservationName=? AND LastChgTimestamp<=? Order By LastChgTimestamp DESC Limit 1;");

    public long run(String sReservationName, String sUsers, String sNodes, long lStartTsInMicroSecs, long lEndTsInMicroSecs, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException
    {
        //--------------------------------------------------
        // Grab the current reservation's information for the reservation table so they are available for use creating the new history record.
        //--------------------------------------------------
        voltQueueSQL(selectReservationSql, sReservationName, lTsInMicroSecs);
        VoltTable[] aReservationData = voltExecuteSQL();

        // Ensure that we found the rest of the reservation's data.
        if (aReservationData[0].getRowCount() == 0) {
            throw new VoltAbortException("ReservationUpdated - could not find a corresponding reservation for the 'update this reservation', the query returned no rows - "
                    +"ReservationName='" + sReservationName + "', lTsInMicroSecs=" + lTsInMicroSecs + "!");
        }

        aReservationData[0].advanceRow();

        if (sUsers == null)
            sUsers = aReservationData[0].getString("Users");

        if (sNodes == null)
            sNodes = aReservationData[0].getString("Nodes");

        if (lStartTsInMicroSecs == 0L)
            lStartTsInMicroSecs = aReservationData[0].getTimestampAsLong("StartTimestamp");
        
        //---------------------------------------------------------------------
        // Insert this information into the WlmReservation_History table.
        //---------------------------------------------------------------------
        voltQueueSQL(insertHistorySql
                    ,sReservationName
                    ,sUsers
                    ,sNodes
                    ,lStartTsInMicroSecs        // StartTimestamp
                    ,aReservationData[0].getTimestampAsLong("EndTimestamp")          // EndTimestamp
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
