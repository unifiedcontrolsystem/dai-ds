// Copyright (C) 2020-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;


/**
 * Handle the database processing that is necessary when setting a node's Dimm's state.
 *
 *  Input parameter:
 *      String  sNodeLctn       = node's lctn (NOT the component's lctn)
 *      String  sComponentLctn  = component's lctn
 *      String  sNewState       = string containing the new state value for the above location
 *      long    lSizeMB         = long containing the size in MB of the DIMM
 *      String  sModuleLocator  = string containing the new module locator value for the above locatio
 *      String  sBankLocator    = string containing the new bank locator value for the above locatio
 *      long    lTsInMicroSecs  = Time that the event causing this state change occurred
 *      String  sReqAdapterType = Type of adapter that requested this stored procedure
 *      long    lReqWorkItemId  = Work Item Id that the requesting adapter was performing when it requested this stored procedure (-1 is used when there is no work item yet associated with this change)
 *
 *  Return value:
 *      0L = Everything completed fine, and this record did occur in timestamp order.
 */

public class DimmAddToHistory extends VoltProcedure {

    public final SQLStmt insertHistory = new SQLStmt(
                 "INSERT INTO Dimm " +
                 "(NodeLctn, Lctn, State, SizeMB, ModuleLocator, BankLocator, Sernum, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );

    public long run(String sNodeLctn, String sComponentLctn, String sNewState, long lSizeMB, String sModuleLocator, String sBankLocator, String sSerial, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException
    {

        //----------------------------------------------------------------------
        // Insert a "history" record for these updates into the history table
        // (this starts with pre-change values and then overlays them with the changes from this invocation).
        //----------------------------------------------------------------------
        voltQueueSQL(insertHistory
                    ,sNodeLctn                          // NodeLctn
                    ,sComponentLctn                     // Lctn
                    ,sNewState                          // State
                    ,lSizeMB
                    ,sModuleLocator
                    ,sBankLocator
                    ,sSerial
                    ,this.getTransactionTime()          // DbUpdatedTimestamp
                    ,lTsInMicroSecs                     // LastChgTimestamp
                    ,sReqAdapterType                    // LastChgAdapterType
                    ,lReqWorkItemId                     // LastChgWorkItemId
                    );

        voltExecuteSQL(true);

        return 0L;
    }
}