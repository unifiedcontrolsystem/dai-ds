// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.Arrays;
import java.util.BitSet;
import static java.lang.Math.toIntExact;
import org.voltdb.*;
import java.util.concurrent.atomic.AtomicLong;


/**
 * Handle the database processing that is necessary to obtain a list of "pertinent" RAS Events, pertinent are the "new" RAS events (since last time we checked) that also have non-null ControlOperation.
 * NOTE: This logic purposely excludes RAS events that currently have a JobId of "?".
 *       Such a JobId indicates that the control system needs to figure out if there was a job that was effected by this RAS event or not.
 *       After that determination is made the JobId will be changed from "?" to either NULL or the effected JobId.
 *       When the JobId filled is "fixed up" then the Ras Event's control operations will be executed (necessary in order to correctly handle job related Control Operations).
 *
 *  Returns: VoltTable[] aPertinentRasEvents
 *
 *  Input parameter:
 *
 */

public class RasEventProcessNewControlOperations extends VoltProcedure {

    public final SQLStmt listPertinentRasEventsSql = new SQLStmt("SELECT * FROM RasEvent WHERE " +
                                                                 "(ControlOperation IS NOT NULL AND ControlOperationDone = 'N' AND " +
                                                                 " ((JobId IS NULL) OR (JobId != '?'))) " +
                                                                 "Order By DbUpdatedTimestamp ASC;");

    public VoltTable run() throws VoltAbortException {
        // Grab all the pertinent ras events ("new" events that have a specified control operation value that has not yet been executed).
        voltQueueSQL(listPertinentRasEventsSql);
        VoltTable[] aPertinentRasEvents = voltExecuteSQL();

        // Return the list of pertinent RAS events to the caller.
        return aPertinentRasEvents[0];
    }
}
