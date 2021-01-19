// Copyright (C) 2020-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.voltdb.*;
import java.lang.*;
import java.util.*;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import org.voltdb.types.TimestampType;

// This procedure handles the processing needed when purging data from the WlmReservation_History table,
// we do not want to purge the newest reservation info for each reservation name, when the reservation
// is still "in effect" (i.e., has not yet ended and has not been deleted).
public class ReservationPurging extends VoltProcedure {

    public final SQLStmt selectViaLctnAndInvTime = new SQLStmt(
        "SELECT ReservationName, EndTimestamp, DeletedTimestamp, DbUpdatedTimestamp FROM WlmReservation_History WHERE DbUpdatedTimestamp<=? Order By ReservationName, DbUpdatedTimestamp DESC;"
    );

    public final SQLStmt deleteViaResvNameAndDbUpdatedTs = new SQLStmt(
        "DELETE FROM WlmReservation_History WHERE ReservationName=? AND DbUpdatedTimestamp=?;"
    );



    public long run(TimestampType sPurgeTimestamp) throws VoltAbortException {
        // Get the list of rows that qualify to be purged.
        voltQueueSQL(selectViaLctnAndInvTime, sPurgeTimestamp);
        VoltTable[] aListOfRows = voltExecuteSQL();

        //----------------------------------------------------------------------
        // Loop through the recs, they are sorted by ReservationName and within that by DbUpdatedTimestamp in DESCENDING order.
        // - For the first row returned for a given reservation name
        //      If the DeletedTimestamp is not null - then purge it (this indicates that the reservation has been deleted)
        //      If the EndTimestamp is < current timestamp - then purge it (this indicates that the reservation has expired)
        //      Otherwise keep this initial row (don't purge this initial row at this time)
        // - For any subsequent rows with the same reservation name, purge them (they are not current so we don't need them)
        //----------------------------------------------------------------------
        long lNumPurgedRows = 0L;
        String sPrevResvName = "";
        for (int iRowCntr = 0; iRowCntr < aListOfRows[0].getRowCount(); ++iRowCntr) {
            aListOfRows[0].advanceRow();
            String sRowsResvName = aListOfRows[0].getString("ReservationName");
            // Check & see if this is a new ReservationName.
            boolean bPurgeThisRow = false;
            if (sPrevResvName.equals(sRowsResvName) == false) {
                // this is the first row for a new ReservationName - so this is the row that we may want to keep (so we don't purge any pertinent reservation info).
                // Save this new ReservationName as the "previous ReservationName", so that we know we should go ahead and delete all the rest of the rows for this ReservationName.
                sPrevResvName = sRowsResvName;
                // Check & see if we want to delete this row.
                TimestampType tRowsDeletedTs = aListOfRows[0].getTimestampAsTimestamp("DeletedTimestamp");
                if (aListOfRows[0].wasNull() == false) {
                    // this row's DeletedTimestamp has been filled in (i.e., this reservation was deleted).
                    bPurgeThisRow = true;
                }
                else {
                    Date dRowsEndTs = aListOfRows[0].getTimestampAsTimestamp("EndTimestamp").asExactJavaDate();
                    if ((aListOfRows[0].wasNull() == false) && (dRowsEndTs.compareTo(this.getTransactionTime()) < 0)) {
                        // this row's EndTimestamp is before the current time (i.e., this reservation has expired).
                        bPurgeThisRow = true;
                    }
                }
            }   // this is the first row for a new ReservationName - so this is the row that we may want to keep (so we don't purge any pertinent reservation info).
            else {
                // this is not the first row for a new ReservationName - so we want to purge this row.
                bPurgeThisRow = true;
            }
            // Purge this row (if appropriate).
            if (bPurgeThisRow) {
                TimestampType tRowsDbUpdatedTs = aListOfRows[0].getTimestampAsTimestamp("DbUpdatedTimestamp");
                voltQueueSQL(deleteViaResvNameAndDbUpdatedTs, EXPECT_ONE_ROW, sRowsResvName, tRowsDbUpdatedTs);
                ++lNumPurgedRows;
            }
        }  // Loop through all the rows we got back from the query.

        // Actually delete all of the pending purged rows.
        voltExecuteSQL(true);

        //----------------------------------------------------------------------
        // Return the number of rows purged from this table to the caller.
        //----------------------------------------------------------------------
        return lNumPurgedRows;
    }
}
