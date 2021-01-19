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

public class TempStoredProcToTestTimeouts extends VoltProcedure {

    public final SQLStmt selectReservationsOnlyEndTimeSql = new SQLStmt(
        "SELECT ReservationName, Users, Nodes, StartTimestamp, EndTimestamp, DeletedTimestamp, LastChgTimestamp FROM WlmReservation_History " +
        "WHERE DbUpdatedTimestamp<=? " +
        "ORDER BY DbUpdatedTimestamp DESC, ReservationName, Users;"
    );



    public long run() throws VoltAbortException {


        System.out.println("TempStoredProcToTestTimeouts - starting to sleep for 130 seconds");

        try { Thread.sleep(310 * 1000L); } catch (Exception e) {}

        System.out.println("TempStoredProcToTestTimeouts - finished sleeping for 130 seconds");



        return 0L;
    }
}
