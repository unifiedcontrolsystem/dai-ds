// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Go through the JobStepInfo table and mark any in-flight JobSteps as ended AND get the VoltTable for the JobStepInfo that were marked ended by this processing.
 *
 *  Input parameter:
 *      String  sJobId              = string containing the WLM job id
 *      long    lEndTsInMicroSecs   = time this JobStep ended in units of micro-seconds since the epoch
 *
 *  Return value:
 *      VoltTable aJobStepInfo = VoltTable containing all of the JobStepInfo for this JobStep (after updating it to reflect the fact that the JobStep has ended)
 */

public class InternalJobStepInfoSpecialJobCleanup extends VoltProcedure {

    public final SQLStmt selectJobStepInfo = new SQLStmt("SELECT * FROM InternalJobStepInfo WHERE JobId=?;");
    public final SQLStmt updateJobStepInfo = new SQLStmt("UPDATE InternalJobStepInfo SET WlmJobStepEndTime=?, WlmJobStepEnded=? WHERE (JobId=? AND JobStepId=?);");



    public VoltTable run(String sJobId, long lEndTsInMicroSecs) throws VoltAbortException {
        // Get the list of all the JobStep entries for the specified job in the JobStepInfo table.
        voltQueueSQL(selectJobStepInfo, sJobId);
        VoltTable[] aJobStepInfo = voltExecuteSQL();

        // Loop through the list of JobStepInfo entries for this Job - marking each of the JobSteps as ended.
        for (int iRowCntr = 0; iRowCntr < aJobStepInfo[0].getRowCount(); ++iRowCntr) {
            aJobStepInfo[0].advanceRow();
            // Update the existing JobStepInfo with the fact that the JobStep has now ended.
            voltQueueSQL(updateJobStepInfo
                        ,lEndTsInMicroSecs                      // WlmJobStepEndTime information
                        ,"T"                                    // WlmJobStepEnded is true
                        ,sJobId                                 // JobId
                        ,aJobStepInfo[0].getString("JobStepId") // JobStepId
                        );
        }
        voltExecuteSQL();

        // Reread the newly updated JobStepInfo (so we have a VoltTable with the information in it to return to the caller).
        voltQueueSQL(selectJobStepInfo, sJobId);
        aJobStepInfo = voltExecuteSQL(true);

        // Return the JobStepInfo to the caller in the form of a VoltTable.
        return aJobStepInfo[0];
    }
}