// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when adding Aggregated Environmental data to the Tier2 data store.
 *
 *  Input parameter:
 *      String  sTypeOfEnvData          = type of environmental data
 *      String  sLctn                   = location that this environmental data is for
 *      long    lTsInMicroSecs          = timestamp (in microseconds since epoch) that this env data is for
 *      double  dMaxValue               = maximum value from all of the samples that occurred within this interval
 *      double  dMinValue               = minimum value from all of the samples that occurred within this interval
 *      double  dAvgValue               = average value from all of the samples that occurred within this interval
 *      String  sReqAdapterType         = Type of adapter that requested this stored procedure (e.g., PROVISIONER)
 *      long    lReqWorkItemId          = Work Item Id that the requesting adapter was performing when it requested this stored procedure (-1 is used when there is no work item yet associated with this change)
 */

public class AggregatedEnvDataStore extends VoltProcedure {
    public final SQLStmt insertAggregatedEnvData = new SQLStmt(
            "INSERT INTO Tier2_AggregatedEnvData " +
            "(Lctn, Timestamp, Type, MaximumValue, MinimumValue, AverageValue, AdapterType, WorkItemId) " +
            "VALUES (?, ?, ?, ?, ?, ?, ?, ?);"
    );

    public long run(String sTypeOfEnvData, String sLctn, long lTsInMicroSecs, double dMaxValue, double dMinValue, double dAvgValue, String sReqAdapterType, long lReqWorkItemId)
                    throws VoltAbortException
    {
        voltQueueSQL(insertAggregatedEnvData
                    ,sLctn
                    ,lTsInMicroSecs
                    ,sTypeOfEnvData
                    ,dMaxValue
                    ,dMinValue
                    ,dAvgValue
                    ,sReqAdapterType
                    ,lReqWorkItemId
                    );
        voltExecuteSQL(true);
        return 0L;
    }
}