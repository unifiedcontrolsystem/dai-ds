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

public class ReservationListAtTime extends VoltProcedure {

    public final SQLStmt selectReservationsOnlyEndTimeSql = new SQLStmt(
        "SELECT ReservationName, Users, Nodes, StartTimestamp, EndTimestamp, DeletedTimestamp, LastChgTimestamp FROM WlmReservation_History " +
        "WHERE DbUpdatedTimestamp<=? " +
        "ORDER BY DbUpdatedTimestamp DESC, ReservationName, Users;"
    );

    public final SQLStmt selectReservationsBothEndAndStartTimeSql = new SQLStmt(
        "SELECT ReservationName, Users, Nodes, StartTimestamp, EndTimestamp, DeletedTimestamp, LastChgTimestamp FROM WlmReservation_History " +
        "WHERE DbUpdatedTimestamp<=? AND DbUpdatedTimestamp>=? " +
        "ORDER BY DbUpdatedTimestamp DESC, ReservationName, Users;"
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
                voltQueueSQL(selectReservationsBothEndAndStartTimeSql,
                             sEndingTimestamp,
                             sStartingTimestamp);

            else
                voltQueueSQL(selectReservationsBothEndAndStartTimeSql,
                             this.getTransactionTime(),  // use current timestamp
                             sStartingTimestamp);
        }
        //----------------------------------------------------------------------
        // If Starting Time was NOT specified
        //----------------------------------------------------------------------
        else {
            if (bEndingTsSpecified)
                voltQueueSQL(selectReservationsOnlyEndTimeSql, sEndingTimestamp);
            else
                voltQueueSQL(selectReservationsOnlyEndTimeSql, this.getTransactionTime());  // use current timestamp
        }

        VoltTable[] aListOfReservations = voltExecuteSQL(true);

        //----------------------------------------------------------------------
        // Returns the information to the caller.
        //----------------------------------------------------------------------
        return aListOfReservations[0];
    }
}
