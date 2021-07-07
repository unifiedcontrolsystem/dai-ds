// Copyright (C) 2020-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;


/**
 * Handle the database processing that is necessary when setting a node's Fru's state.
 *
 *  Input parameter:
 *      String  sNodeLctn       = node's lctn
 *      long    lTsInMicroSecs  = Time that the event causing this state change occurred
 *      String  sInventoryInfo  = string containing the inventory in json format
 *      String  sSernum         = string containing the serial number
 *      String  sBiosInfo       = string containing the bios info in json format
 *
 *  Return value:
 *      0L = Everything completed fine, and this record did occur in timestamp order.
*/

public class FruAddToHistory extends VoltProcedure {

    public final SQLStmt insertHistory = new SQLStmt(
                 "INSERT INTO NodeInventory_History " +
                 "(Lctn, DbUpdatedTimestamp, InventoryTimestamp, InventoryInfo, Sernum, BiosInfo) " +
                 "VALUES (?, ?, ?, ?, ?, ?);"
    );

    public long run(String sNodeLctn, long lTsInMicroSecs, String sInventoryInfo, String sSernum, String sBiosInfo) throws VoltAbortException
    {

        //----------------------------------------------------------------------
        // Insert a "history" record for these updates into the history table
        // (this starts with pre-change values and then overlays them with the changes from this invocation).
        //----------------------------------------------------------------------
        voltQueueSQL(insertHistory
                    ,sNodeLctn                          // Lctn
                    ,this.getTransactionTime()          // DbUpdatedTimestamp
                    ,lTsInMicroSecs                     // InventoryTimestamp
                    ,sInventoryInfo                     // InventoryInfo
                    ,sSernum                           // Sernum
                    ,sBiosInfo                         // BiosInfo
                    );

        voltExecuteSQL(true);

        return 0L;
    }
}