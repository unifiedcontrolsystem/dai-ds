// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.voltdb.*;
import java.lang.*;
import java.util.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import org.voltdb.types.TimestampType;

/**
 * This stored procedure returns an array of VoltTables:
 * - the first VoltTable contains the Lctn, State, and BootImageId information for each of the nodes in the specified node list, that are NOT in one of the states that was specified in the state list.
 * - the subsequent VoltTables contain the same information for each of the nodes that ARE in one of the states that was specified in the state list.
 *
 * So this means that if there were 2 states in the specified state list ("A", "E") then there would be a total of 3 entries in the array of VoltTables that will be returned.
 *     1)  This volt table would have the information for each of the specified nodes that have a state that is not A and is not E.
 *     2)  This volt table would have the information for each of the specified nodes that have a state of "A"
 *     3)  This volt table would have the information for each of the specified nodes that have a state of "E"
 */

public class ComputeNodesFromListWithoutAndWithTheseStates extends VoltProcedure {

    public final SQLStmt selectForUnexpected    = new SQLStmt("SELECT Lctn, State, BootImageId FROM ComputeNode WHERE ((Lctn IN ?) AND (State NOT IN ?));");

    public final SQLStmt selectForExplicitState = new SQLStmt("SELECT Lctn, State, BootImageId FROM ComputeNode WHERE ((Lctn IN ?) AND (State = ?));");


    public VoltTable[] run(String[] aNodeLctns, String[] aStates) throws VoltAbortException {

        // Get list of the specified lctns that are NOT in one of the specified states.
        voltQueueSQL(selectForUnexpected, aNodeLctns, aStates);

        // Get list of the specified lctns that are in each of the separate specified states.
        for (String sState: aStates) {
            voltQueueSQL(selectForExplicitState, aNodeLctns, sState);
        }

        VoltTable[] aVt = voltExecuteSQL();

        return aVt;
    }
}
