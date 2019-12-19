// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.voltdb.*;

/**
 * Gets a unique identifier for the specified entity.
 *
 * When the specified entity does exist in the table,
 * it grabs the previously generated "next" unique value and saves it away
 * then bumps the value and saves it back as the new "next" value.
 *
 * If the specified entity does not yet exist in the table,
 * a row for the entity is created and the above processing occurs.
 *
 *  Input parameter:
 *      specifiedEntity = string containing the entity that a unique id is needed for
 *
 *  Returns: long lUniqueValue = The unique value for the specified entity.
 *
 *  Sample invocation:
 *      echo "Exec GetUniqueId RasGenAdapterAbend;" | sqlcmd
 *          Get the "next" unique ID value for the specified entity.
 *
 */

public class GetUniqueId extends VoltProcedure {

    public final SQLStmt select = new SQLStmt("SELECT NextValue FROM UniqueValues WHERE Entity = ?;");
    public final SQLStmt update = new SQLStmt("UPDATE UniqueValues SET NextValue = NextValue + 1, DbUpdatedTimestamp = ? WHERE Entity = ?;");
    public final SQLStmt insert = new SQLStmt("INSERT INTO UniqueValues (Entity, NextValue, DbUpdatedTimestamp) VALUES (?, ?, ?);");


    public long run(String specifiedEntity) throws VoltAbortException {
        // Get the current "next unique id" for the specified entity.
        voltQueueSQL(select, EXPECT_ZERO_OR_ONE_ROW, specifiedEntity);
        VoltTable[] uniqueId = voltExecuteSQL();
        // Check and see if there is a matching record for the specified entity
        if (uniqueId[0].getRowCount() == 0) {
            // No matching record - add a new row for the specified entity
            voltQueueSQL(insert, specifiedEntity, 1, this.getTransactionTime());
            voltExecuteSQL();
            // Now redo the above query (to get the current "next unique id" for the specified entity)
            voltQueueSQL(select, EXPECT_ONE_ROW, specifiedEntity);
            uniqueId = voltExecuteSQL();
        }

        // Bump the current "next unique id" to generate the next "next unique id" for the specified entity.
        voltQueueSQL(update, EXPECT_ONE_ROW, this.getTransactionTime(), specifiedEntity);
        voltExecuteSQL(true);   // Note: true indicates that this is the final batch of sql stmts for this procedure.

        return uniqueId[0].asScalarLong();
    }
}