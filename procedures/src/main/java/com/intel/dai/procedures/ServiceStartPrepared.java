
// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import java.util.*;
import static java.lang.Math.toIntExact;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a service operation is started.
 * NOTE:
 *
 *  Input parameter:
 *      long    lServiceId          - id that uniquely identifies this specific instance of a service operation
 *      String  sLctn               - h/w location being serviced
 *      String  sRemarks            - User's remarks
 */

public class ServiceStartPrepared extends VoltProcedure {

    public final SQLStmt updateServiceSql = new SQLStmt(
                    "UPDATE ServiceOperation SET State = ?, Status = ?, DbUpdatedTimeStamp = CURRENT_TIMESTAMP WHERE ServiceOperationId = ? AND LCTN = ?;" );


    public final SQLStmt selectServiceOperationSql = new SQLStmt(
                    "SELECT " +
                    "ServiceOperationId, Lctn, TypeOfServiceOperation, UserStartedService, UserStoppedService, State, Status, StartTimestamp, StopTimestamp, StartRemarks, StopRemarks, DbUpdatedTimeStamp,LogFile " +
                    " FROM ServiceOperation WHERE ServiceOperationId = ? AND LCTN = ? ;" );

    public final SQLStmt insertServiceToHistorySql = new SQLStmt(
                    "INSERT INTO ServiceOperation_History " +
                    "(ServiceOperationId, Lctn, TypeOfServiceOperation, UserStartedService, UserStoppedService, State, Status, StartTimestamp, StopTimestamp, StartRemarks, StopRemarks, DbUpdatedTimeStamp,LogFile) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);" );


    public long run(String sLctn, long lServiceId, String sRemarks) throws VoltAbortException {

        Date dStopTime = this.getTransactionTime();
        //--------------------------------------------------
        // Grab the ServiceOperation values and create the history record.
        //--------------------------------------------------
        voltQueueSQL(selectServiceOperationSql, EXPECT_ONE_ROW, lServiceId, sLctn);
        VoltTable[] aServiceData = voltExecuteSQL();
        aServiceData[0].advanceRow();
        //---------------------------------------------------------------------
        // Update the ServiceOperation to indicate EndRepair.
        //---------------------------------------------------------------------
        voltQueueSQL(updateServiceSql
                        ,"P"                        // State - P = Prepare
                        ,"P"                        // Status - P = Prepared
                        ,lServiceId
                        ,sLctn
                        );

        //---------------------------------------------------------------------
        // Update the ServiceOperation_History to indicate EndRepair.
        //---------------------------------------------------------------------
        voltQueueSQL(insertServiceToHistorySql
                        ,aServiceData[0].getLong("ServiceOperationId")
                        ,aServiceData[0].getString("Lctn")
                        ,aServiceData[0].getString("TypeOfServiceOperation")
                        ,aServiceData[0].getString("UserStartedService")
                        ,aServiceData[0].getString("UserStoppedService")
                        ,"P"
                        ,"P"
                        ,aServiceData[0].getTimestampAsTimestamp("StartTimestamp")
                        ,dStopTime
                        ,aServiceData[0].getString("StartRemarks")
                        ,sRemarks
                        ,this.getTransactionTime()
                        ,aServiceData[0].getString("LogFile")
                        );
        voltExecuteSQL(true);
        return 0;
    }
}
