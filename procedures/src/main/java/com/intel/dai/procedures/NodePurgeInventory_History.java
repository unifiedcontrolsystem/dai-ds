// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.voltdb.*;
import java.lang.*;
import java.util.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import org.voltdb.types.TimestampType;


public class NodePurgeInventory_History extends VoltProcedure {

    public final SQLStmt selectViaLctnAndInvTime = new SQLStmt(
        "SELECT Lctn, InventoryTimestamp, DbUpdatedTimestamp FROM NodeInventory_History WHERE DbUpdatedTimestamp<=? Order By Lctn DESC, InventoryTimestamp DESC;"
    );

    public final SQLStmt deleteViaLctnAndInvTime = new SQLStmt(
        "DELETE FROM NodeInventory_History WHERE Lctn=? AND InventoryTimestamp=?;"
    );



    public long run(TimestampType sPurgeTimestamp) throws VoltAbortException {
        // Get the list of records that could be purged.
        voltQueueSQL(selectViaLctnAndInvTime, sPurgeTimestamp);
        VoltTable[] aListOfRecs = voltExecuteSQL();

        //----------------------------------------------------------------------
        // Loop through the recs, they are sorted by Lctn and within that by InventoryTimestamp in descending order.
        // - Keep the first returned record for each Lctn, delete all the rest
        // - We are keeping the first record (newest) as we don't want to purge the "active" inventory info for each lctn.
        //----------------------------------------------------------------------
        long lNumPurgedRows = 0L;
        String sPrevLctn = "";
        for (int iRowCntr = 0; iRowCntr < aListOfRecs[0].getRowCount(); ++iRowCntr) {
            aListOfRecs[0].advanceRow();
            String sLctn = aListOfRecs[0].getString("Lctn");
            // Check & see if this is a new node lctn.
            if (sPrevLctn.equals(sLctn) == false) {
                // this is the first record for a new lctn string - so this is the record that we need to keep (so we don't purge the active inv info for this lctn).
                // Save this lctn as the "previous lctn", so that we know we should go ahead and delete all the rest of the records for this node lctn.
                sPrevLctn = sLctn;
            }  // this is the first record for a new lctn string - so this is the record that we need to keep (so we don't purge the active inv info for this lctn).
            else {
                // this is not a new lctn - so we want to purge this row.
                TimestampType tInvTimestamp = aListOfRecs[0].getTimestampAsTimestamp("InventoryTimestamp");
                voltQueueSQL(deleteViaLctnAndInvTime, EXPECT_ONE_ROW, sLctn, tInvTimestamp);
                ++lNumPurgedRows;
            }   // this is not a new lctn - so we want to purge this row.
        }  // Loop through all the records we got back from the query.

        // Actually delete all of the pending purged rows.
        voltExecuteSQL(true);

        //----------------------------------------------------------------------
        // Returns the information to the caller.
        //----------------------------------------------------------------------
        return lNumPurgedRows;
    }
}
