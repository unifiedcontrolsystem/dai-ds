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
 *      String  sUser               - Userid who initiated this service request
 *      String  sLctn               - hardware location string of the hardware that this service operation is running on, a compute node, a whole rack, a cdu, power distribution unit, a switch, etc.
 *      String  sOperation          - Type of Service Operation performed ( repair, FW update etc)
 *      String  sRemarks            - User Remarks
 *      String  sReqAdapterType     - Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId      - Work Item Id that the requesting adapter was performing when it requested this stored procedure
 */

public class ServiceStarted extends VoltProcedure {

    public final SQLStmt insertSql = new SQLStmt(
                    "INSERT INTO ServiceOperation " +
                    "(ServiceOperationId, Lctn, TypeOfServiceOperation, UserStartedService, State, Status, StartTimestamp, StopTimestamp, StartRemarks, DbUpdatedTimeStamp,LogFile) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?);"
    );
    public final SQLStmt insertHistorySql = new SQLStmt(
                    "INSERT INTO ServiceOperation_History " +
                    "(ServiceOperationId, Lctn, TypeOfServiceOperation, UserStartedService, State, Status, StartTimestamp, StopTimestamp, StartRemarks, DbUpdatedTimeStamp,LogFile) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?);"
    );


    public long run(String sLctn, long lServiceId, String sUser, String sOperation, String sRemarks, String sLogFile) throws VoltAbortException {
        //---------------------------------------------------------------------
        // Create a row in the table for this entry.
        //---------------------------------------------------------------------
        voltQueueSQL(insertSql
                        ,lServiceId
                        ,sLctn
                        ,sOperation
                        ,sUser                      // UserStartedService
                        ,"P"                        // State - P = Prepare
                        ,"A"                        // Status - A = Active
                        ,this.getTransactionTime()  // StartTimestamp
                        ,sRemarks
                        ,this.getTransactionTime()  // DbUpdatedTimeStamp
                        ,sLogFile
                        );
        voltQueueSQL(insertHistorySql
                        ,lServiceId
                        ,sLctn
                        ,sOperation
                        ,sUser                      // UserStartedService
                        ,"P"                        // State - P = Prepare
                        ,"A"                        // Status - A = Active
                        ,this.getTransactionTime()  // StartTimestamp
                        ,sRemarks
                        ,this.getTransactionTime()  // DbUpdatedTimeStamp
                        ,sLogFile
                        );
        voltExecuteSQL(true);
        return 0;
    }
}
