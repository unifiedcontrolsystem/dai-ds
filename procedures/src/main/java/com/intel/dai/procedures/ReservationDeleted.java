// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a WLM Reservation is explicitly deleted.
 *
 *  Input parameter:
 *      String sReservationName     = Reservation's name
 *      long   lDeleteTsInMicroSecs = Time this reservation was deleted in units of micro-seconds since the epoch (utc)
 *      String sReqAdapterType      = Type of adapter that requested this stored procedure
 *      long   lReqWorkItemId       = Work Item Id that the requesting adapter was performing when it requested this stored procedure
 *
 */

public class ReservationDeleted extends VoltProcedure {

    public final SQLStmt selectReservationSql = new SQLStmt("SELECT * FROM WlmReservation_History WHERE ReservationName=? AND LastChgTimestamp<=? Order By LastChgTimestamp DESC Limit 1;");

    public final SQLStmt insertHistorySql = new SQLStmt(
                    "INSERT INTO WlmReservation_History " +
                    "(ReservationName, Users, Nodes, StartTimestamp, EndTimestamp, DeletedTimestamp, LastChgTimestamp, DbUpdatedTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );



    public long run(String sReservationName, long lDeleteTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException
    {
        //--------------------------------------------------
        // Grab the current reservation's information for the reservation table so they are available for use creating the new history record.
        //--------------------------------------------------
        voltQueueSQL(selectReservationSql, sReservationName, lDeleteTsInMicroSecs);
        VoltTable[] aReservationData = voltExecuteSQL();

        // Ensure that we found the rest of the reservation's data.
        if (aReservationData[0].getRowCount() == 0) {
            throw new VoltAbortException("ReservationDeleted - could not find a corresponding reservation for the 'delete this reservation', the query returned no rows - "
                                        +"ReservationName='" + sReservationName + "', lDeleteTsInMicroSecs=" + lDeleteTsInMicroSecs + "!");
        }

        aReservationData[0].advanceRow();

        // Insert a new row into the reservation table, indicating that this reservation was delete.
        voltQueueSQL(insertHistorySql
                    ,sReservationName                                                   // ReservationName
                    ,aReservationData[0].getString("Users")
                    ,aReservationData[0].getString("Nodes")
                    ,aReservationData[0].getTimestampAsTimestamp("StartTimestamp")
                    ,aReservationData[0].getTimestampAsTimestamp("EndTimestamp")
                    ,lDeleteTsInMicroSecs                                               // DeletedTimestamp
                    ,lDeleteTsInMicroSecs                                               // LastChgTimestamp
                    ,this.getTransactionTime()                                          // DbUpdatedTimestamp
                    ,sReqAdapterType                                                    // LastChgAdapterType
                    ,lReqWorkItemId                                                     // LastChgWorkItemId
                    );
        voltExecuteSQL(true);
        return 0;
    }
}