// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * This procedure handles the setting of the flag indicating whether or not this machine is using synthesized data, as opposed to "real" data.
 *     Synthesized data is used for testing purposes.  On a production machine the expectation is that this method would never be used, it would always be real data.
 *
 *  Input parameter:
 *      String  sMachineSernum              = Machine serial number, if null this field defaults to "1"
 *      String  sIsSynthesizedDataBeingUsed = flag indicating whether or not synthesized data is now being used, 'Y' means synthesized data is being used, 'N' means synthesized data is no longer being used
 *
 *  Sample invocation:
 *      echo "exec MachineUpdateSynthesizedDataFlag '1', 'Y';" | sqlcmd
 */

public class MachineUpdateSynthesizedDataFlag extends VoltProcedure {

    public final SQLStmt selectMachine = new SQLStmt("SELECT * FROM Machine WHERE Sernum=?;");
    public final SQLStmt updateMachine = new SQLStmt("UPDATE Machine SET UsingSynthesizedData=?, DbUpdatedTimestamp=? WHERE Sernum=?;");
    public final SQLStmt insertMachineHistory = new SQLStmt(
            "INSERT INTO Machine_History (Sernum, Description, Type, NumRows, NumColsInRow, NumChassisInRack, State, ClockFreq, ManifestLctn, ManifestContent, DbUpdatedTimestamp, UsingSynthesizedData) " +
            "VALUES                      (?,      ?,           ?,    ?,       ?,            ?,                ?,     ?,         ?,            ?,               ?,                  ?);"
    );



    public long run(String sMachineSernum, String sIsSynthesizedDataBeingUsed) throws VoltAbortException {

        if (sMachineSernum == null)
            sMachineSernum = "1";  // fill in the default value.

        //----------------------------------------------------------------------
        // Grab the current record for this machine serial number out of the "active" table (Machine table).
        //----------------------------------------------------------------------
        voltQueueSQL(selectMachine, EXPECT_ZERO_OR_ONE_ROW, sMachineSernum);
        VoltTable[] aMachineData = voltExecuteSQL();
        if (aMachineData[0].getRowCount() == 0) {
            throw new VoltAbortException("MachineUpdateSynthesizedDataFlag - there is no entry in the Machine table for the specified " +
                                         "Machine serial number (" + sMachineSernum + ")!");
        }
        aMachineData[0].advanceRow();

        //----------------------------------------------------------------------
        // Update the UsingSynthesizedData flag for this machine in the "active" table.
        //----------------------------------------------------------------------
        voltQueueSQL(updateMachine, sIsSynthesizedDataBeingUsed, this.getTransactionTime(), sMachineSernum);

        //----------------------------------------------------------------------
        // Insert a "history" record for these updates into the history table
        // (this starts with pre-change values and then overlays them with the changes from this invocation).
        //----------------------------------------------------------------------
        voltQueueSQL(insertMachineHistory
                    ,aMachineData[0].getString("Sernum")
                    ,aMachineData[0].getString("Description")
                    ,aMachineData[0].getString("Type")
                    ,aMachineData[0].getLong("NumRows")
                    ,aMachineData[0].getLong("NumColsInRow")
                    ,aMachineData[0].getLong("NumChassisInRack")
                    ,aMachineData[0].getString("State")
                    ,aMachineData[0].getLong("ClockFreq")
                    ,aMachineData[0].getString("ManifestLctn")
                    ,aMachineData[0].getString("ManifestContent")
                    ,this.getTransactionTime()                      // DbUpdatedTimestamp
                    ,sIsSynthesizedDataBeingUsed                    // UsingSynthesizedData
                    );

        voltExecuteSQL(true);

        return 0L;
    }
}