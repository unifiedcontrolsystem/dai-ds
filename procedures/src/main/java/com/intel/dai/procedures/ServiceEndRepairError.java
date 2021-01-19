package com.intel.dai.procedures;

import java.lang.*;
import java.util.*;
import static java.lang.Math.toIntExact;
import org.voltdb.*;

/**
 * Handle the database processing that is necessary when a service operation endRepair encounters an error.
 * NOTE:
 *
 *  Input parameter:
 *      long    lServiceId          - id that uniquely identifies this specific instance of a service operation
 *      String  sLctn               - h/w location being serviced
 *      String  sRemarks            - User's remarks
 */

public class ServiceEndRepairError extends VoltProcedure {

    public final SQLStmt updateServiceSql = new SQLStmt(
                    "UPDATE ServiceOperation SET State = ?, Status = ?, StopRemarks = ?, DbUpdatedTimeStamp = CURRENT_TIMESTAMP WHERE ServiceOperationId = ? AND LCTN = ?;" );


    public final SQLStmt selectServiceOperationSql = new SQLStmt(
                    "SELECT " +
                    "ServiceOperationId, Lctn, TypeOfServiceOperation, UserStartedService, UserStoppedService, State, Status, StartTimestamp, StopTimestamp, StartRemarks, StopRemarks, DbUpdatedTimeStamp,LogFile " +
                    " FROM ServiceOperation WHERE ServiceOperationId = ? AND LCTN = ? ;" );

    public final SQLStmt insertServiceToHistorySql = new SQLStmt(
                    "INSERT INTO ServiceOperation_History " +
                    "(ServiceOperationId, Lctn, TypeOfServiceOperation, UserStartedService, UserStoppedService, State, Status, StartTimestamp, StopTimestamp, StartRemarks, StopRemarks, DbUpdatedTimeStamp,LogFile) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, ?, ?);" );


    public long run(long lServiceId, String sLctn, String sRemarks) throws VoltAbortException {

        //--------------------------------------------------
        // Grab the ServiceOperation values and create the history record.
        //--------------------------------------------------
        voltQueueSQL(selectServiceOperationSql, EXPECT_ONE_ROW, lServiceId, sLctn);
        VoltTable[] aServiceData = voltExecuteSQL();
        aServiceData[0].advanceRow();
        //-----------------------------------------------------------------------------
        // Update the ServiceOperation to indicate EndRepair failed with error status
        //-----------------------------------------------------------------------------
        voltQueueSQL(updateServiceSql
                        ,"E"                        // State - E = End
                        ,"E"                        // Status - E = Error
                        ,sRemarks
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
                        ,"E"                        // State - E = End
                        ,"E"                        // Status - E = Error
                        ,aServiceData[0].getTimestampAsTimestamp("StartTimestamp")
                        ,aServiceData[0].getString("StartRemarks")
                        ,sRemarks
                        ,this.getTransactionTime()
                        ,aServiceData[0].getString("LogFile")
                        );
        voltExecuteSQL(true);
        return 0;
    }
}
