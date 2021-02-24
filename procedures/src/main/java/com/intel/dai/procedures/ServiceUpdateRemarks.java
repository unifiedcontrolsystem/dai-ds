
// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import org.voltdb.SQLStmt;
import org.voltdb.VoltProcedure;
import org.voltdb.VoltTable;

public class ServiceUpdateRemarks extends VoltProcedure {

    public final SQLStmt updateStartRemarks = new SQLStmt(
                    "UPDATE ServiceOperation SET StartRemarks = ?, DbUpdatedTimeStamp = CURRENT_TIMESTAMP " +
                    "WHERE ServiceOperationId = ? AND Lctn = ?;" );

    public final SQLStmt updateStopRemarks = new SQLStmt(
                    "UPDATE ServiceOperation SET StopRemarks = ?, DbUpdatedTimeStamp = CURRENT_TIMESTAMP " +
                    "WHERE ServiceOperationId = ? AND Lctn = ?;" );

    public final SQLStmt selectServiceOperationSql = new SQLStmt(
                    "SELECT " +
                    "ServiceOperationId, Lctn, TypeOfServiceOperation, UserStartedService, " +
                    "UserStoppedService, State, Status, StartTimestamp, StopTimestamp, " +
                    "StartRemarks, StopRemarks, DbUpdatedTimeStamp, LogFile " +
                    "FROM ServiceOperation WHERE ServiceOperationId = ? AND LCTN = ?;" );

    public final SQLStmt insertServiceToHistorySql = new SQLStmt(
                    "INSERT INTO ServiceOperation_History " +
                    "(ServiceOperationId, Lctn, TypeOfServiceOperation, UserStartedService, " +
                    "UserStoppedService, State, Status, StartTimestamp, StopTimestamp, " +
                    "StartRemarks, StopRemarks, DbUpdatedTimeStamp,LogFile) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?);" );


    public long run(long serviceID, String lctn, String startRemarks, String stopRemarks)
        throws VoltAbortException {

        voltQueueSQL(selectServiceOperationSql, EXPECT_ONE_ROW, serviceID, lctn);
        VoltTable[] serviceOps = voltExecuteSQL();
        if (serviceOps.length == 0) {
            throw new VoltAbortException(
                String.format("no operation with ID %d found for %s",
                    serviceID, lctn));
        }

        VoltTable serviceOp = serviceOps[0];
        serviceOp.advanceRow();

        String startRemarksForHistory = serviceOp.getString("StartRemarks");
        if (startRemarks != null && startRemarks.length() > 0) {
            startRemarksForHistory = startRemarks;
            voltQueueSQL(updateStartRemarks, startRemarks, serviceID, lctn);
        }

        String stopRemarksForHistory = serviceOp.getString("StopRemarks");
        if (stopRemarks != null && stopRemarks.length() > 0) {
            stopRemarksForHistory = stopRemarks;
            voltQueueSQL(updateStopRemarks, stopRemarks, serviceID, lctn);
        }

        voltQueueSQL(insertServiceToHistorySql,
            serviceID,
            lctn,
            serviceOp.getString("TypeOfServiceOperation"),
            serviceOp.getString("UserStartedService"),
            serviceOp.getString("UserStoppedService"),
            serviceOp.getString("State"),
            serviceOp.getString("Status"),
            this.getTransactionTime(),
            startRemarksForHistory,
            stopRemarksForHistory,
            this.getTransactionTime(),
            serviceOp.getString("LogFile")
        );
        voltExecuteSQL(true);

        return 0;
    }
}

