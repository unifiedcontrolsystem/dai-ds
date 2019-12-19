// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 */

public class InternalJobInfoJobRemove extends VoltProcedure {

    public final SQLStmt deleteJobInfo = new SQLStmt("DELETE FROM InternalJobInfo WHERE JobId=? AND WlmJobStartTime=?;");


    public long run(String sJobId, long lStartTsInMicroSecs) throws VoltAbortException {
        // Delete the existing JobInfo entries for the specified job.
        voltQueueSQL(deleteJobInfo, EXPECT_ONE_ROW, sJobId, lStartTsInMicroSecs);
        voltExecuteSQL(true);
        return 0L;
    }
}