package com.intel.dai.procedures;

import java.lang.*;
import java.util.*;
import static java.lang.Math.toIntExact;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a service operation is Closed.
 * NOTE:
 *
 *  Input parameter:
 *      long    lServiceId          - id that uniquely identifies this specific instance of a service operation
 *      String  sLctn               - h/w location that is being serviced
 */

public class ServiceCloseOperation extends VoltProcedure {

    public final SQLStmt deleteServiceSql = new SQLStmt(
                    "DELETE from ServiceOperation WHERE ServiceOperationId = ? AND LCTN = ? ;" );

    public final SQLStmt selectServiceOperationSql = new SQLStmt(
                    "SELECT " +
                    "ServiceOperationId, Lctn, TypeOfServiceOperation, UserStartedService, UserStoppedService, State, Status, StartTimestamp, StopTimestamp, StartRemarks, StopRemarks, DbUpdatedTimeStamp,LogFile " +
                    " FROM ServiceOperation WHERE ServiceOperationId = ? AND LCTN = ? ;" );

    public final SQLStmt insertServiceToHistorySql = new SQLStmt(
                    "INSERT INTO ServiceOperation_History " +
                    "(ServiceOperationId, Lctn, TypeOfServiceOperation, UserStartedService, UserStoppedService, State, Status, StartTimestamp, StopTimestamp, StartRemarks, StopRemarks, DbUpdatedTimeStamp,LogFile) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);" );


    public long run(long lServiceId, String sLctn ) throws VoltAbortException {

        //--------------------------------------------------
        // Grab the ServiceOperation values and create the history record.
        //--------------------------------------------------
        voltQueueSQL(selectServiceOperationSql, EXPECT_ONE_ROW, lServiceId, sLctn);
        VoltTable[] aServiceData = voltExecuteSQL();
        aServiceData[0].advanceRow();

        //--------------------------------------------------------------------------------------
        // Insert a new row into the history table, indicating that this ServiceOperation has completed.
        //--------------------------------------------------------------------------------------
        voltQueueSQL(insertServiceToHistorySql
                    ,aServiceData[0].getLong("ServiceOperationId")
                    ,aServiceData[0].getString("Lctn")
                    ,aServiceData[0].getString("TypeOfServiceOperation")
                    ,aServiceData[0].getString("UserStartedService")
                    ,aServiceData[0].getString("UserStoppedService")
                    ,"C"
                    ,"C"
                    ,aServiceData[0].getTimestampAsTimestamp("StartTimestamp")
                    ,this.getTransactionTime()
                    ,aServiceData[0].getString("StartRemarks")
                    ,aServiceData[0].getString("StopRemarks")
                    ,this.getTransactionTime()
                    ,aServiceData[0].getString("LogFile")
                    );

        //---------------------------------------------------------------------
        // Remove the ServiceOperation entry from the ServiceOperation table
        //---------------------------------------------------------------------
        voltQueueSQL(deleteServiceSql, EXPECT_ONE_ROW, lServiceId, sLctn);

        voltExecuteSQL(true);
        return 0;
    }
}

