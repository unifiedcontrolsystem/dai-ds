// Copyright (C) 2020-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;

/*
 * Handle the database processing necessary to ensure that all of the specified node's components (e.g., Dimms, Processors, Accelerators, Hfi Nics) have the specified state.
 * It will change the state of any component not currently in that state, but does not change the component is already in that state.
 *
 *  Input parameter:
 *      String  sNodeLctn       = string containing the node's lctn (NOT the component's lctn)
 *      String  sNewState       = string containing the new state value for the above location's components
 *      long    lTsInMicroSecs  = Time that the event causing this state change occurred
 *      String  sReqAdapterType = Type of adapter that requested this stored procedure (PROVISIONER)
 *      long    lReqWorkItemId  = Work Item Id that the requesting adapter was performing when it requested this stored procedure (-1 is used when there is no work item yet associated with this change)
 *
 *  Return value:
 *      Number of node components who's state was changed
 */

public class NodeComponentSetStateUnlessInThatState extends VoltProcedure {
    public final SQLStmt selectProcessorSql   = new SQLStmt("SELECT * FROM Processor   WHERE NodeLctn=? AND State != ?;");
    public final SQLStmt selectDimmSql        = new SQLStmt("SELECT * FROM Dimm        WHERE NodeLctn=? AND State != ?;");
    public final SQLStmt selectAcceleratorSql = new SQLStmt("SELECT * FROM Accelerator WHERE NodeLctn=? AND State != ?;");
    public final SQLStmt selectHfiSql         = new SQLStmt("SELECT * FROM Hfi         WHERE NodeLctn=? AND State != ?;");

    public final SQLStmt updateProcessorSql   = new SQLStmt("UPDATE Processor   SET State=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE NodeLctn=? AND Lctn=?;");
    public final SQLStmt updateDimmSql        = new SQLStmt("UPDATE Dimm        SET State=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE NodeLctn=? AND Lctn=?;");
    public final SQLStmt updateAcceleratorSql = new SQLStmt("UPDATE Accelerator SET State=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE NodeLctn=? AND Lctn=?;");
    public final SQLStmt updateHfiSql         = new SQLStmt("UPDATE Hfi         SET State=?, DbUpdatedTimestamp=?, LastChgTimestamp=?, LastChgAdapterType=?, LastChgWorkItemId=? WHERE NodeLctn=? AND Lctn=?;");

    public final SQLStmt insertProcessorHistorySql = new SQLStmt(
                 "INSERT INTO Processor_History " +
                 "(NodeLctn, Lctn, State, SocketDesignation, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?);"
    );
    public final SQLStmt insertDimmHistorySql = new SQLStmt(
                 "INSERT INTO Dimm_History " +
                 "(NodeLctn, Lctn, State, SizeMB, ModuleLocator, BankLocator, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );
    public final SQLStmt insertAcceleratorHistorySql = new SQLStmt(
                 "INSERT INTO Accelerator_History " +
                 "(NodeLctn, Lctn, State, BusAddr, Slot, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );
    public final SQLStmt insertHfiHistorySql = new SQLStmt(
                 "INSERT INTO Hfi_History " +
                 "(NodeLctn, Lctn, State, BusAddr, Slot, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                 "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?);"
    );


    public long run(String sNodeLctn, String sNewState, long lTsInMicroSecs, String sReqAdapterType, long lReqWorkItemId) throws VoltAbortException
    {
        // Get the list of node components that need to be updated.
        voltQueueSQL(selectProcessorSql,   sNodeLctn, sNewState);
        voltQueueSQL(selectDimmSql,        sNodeLctn, sNewState);
        voltQueueSQL(selectAcceleratorSql, sNodeLctn, sNewState);
        voltQueueSQL(selectHfiSql,         sNodeLctn, sNewState);
        VoltTable[] aComponentRows = voltExecuteSQL();

        // Handle updating each of these component's states.
        long lNumComponentsChgd = 0L;
        for (int iComponentCntr=0; iComponentCntr < 4; ++iComponentCntr) {
            for (int iRowCntr = 0; iRowCntr < aComponentRows[iComponentCntr].getRowCount(); ++iRowCntr) {
                aComponentRows[iComponentCntr].advanceRow();
                //----------------------------------------------------------------------
                // Update the record for this component in its "active" table.
                //----------------------------------------------------------------------
                String sComponentLctn = aComponentRows[iComponentCntr].getString("Lctn");
                switch (iComponentCntr) {
                    case 0:
                        voltQueueSQL(updateProcessorSql,   sNewState, this.getTransactionTime(), lTsInMicroSecs, sReqAdapterType, lReqWorkItemId, sNodeLctn, sComponentLctn);
                        break;
                    case 1:
                        voltQueueSQL(updateDimmSql,        sNewState, this.getTransactionTime(), lTsInMicroSecs, sReqAdapterType, lReqWorkItemId, sNodeLctn, sComponentLctn);
                        break;
                    case 2:
                        voltQueueSQL(updateAcceleratorSql, sNewState, this.getTransactionTime(), lTsInMicroSecs, sReqAdapterType, lReqWorkItemId, sNodeLctn, sComponentLctn);
                        break;
                    case 3:
                        voltQueueSQL(updateHfiSql,         sNewState, this.getTransactionTime(), lTsInMicroSecs, sReqAdapterType, lReqWorkItemId, sNodeLctn, sComponentLctn);
                        break;
                }
                //----------------------------------------------------------------------
                // Insert a "history" record for this update into the history table
                // (this starts with pre-change values and then overlays them with the changes from this invocation).
                //----------------------------------------------------------------------
                switch (iComponentCntr) {
                    case 0:
                        voltQueueSQL(insertProcessorHistorySql
                                    ,sNodeLctn                              // NodeLctn
                                    ,sComponentLctn                         // Lctn
                                    ,sNewState                              // State
                                    ,aComponentRows[iComponentCntr].getString("SocketDesignation")
                                    ,this.getTransactionTime()              // DbUpdatedTimestamp
                                    ,lTsInMicroSecs                         // LastChgTimestamp
                                    ,sReqAdapterType                        // LastChgAdapterType
                                    ,lReqWorkItemId                         // LastChgWorkItemId
                                    );
                        ++lNumComponentsChgd;
                        break;
                    case 1:
                        voltQueueSQL(insertDimmHistorySql
                                    ,sNodeLctn                              // NodeLctn
                                    ,sComponentLctn                         // Lctn
                                    ,sNewState                              // State
                                    ,aComponentRows[iComponentCntr].getLong("SizeMB")
                                    ,aComponentRows[iComponentCntr].getString("ModuleLocator")
                                    ,aComponentRows[iComponentCntr].getString("BankLocator")
                                    ,this.getTransactionTime()              // DbUpdatedTimestamp
                                    ,lTsInMicroSecs                         // LastChgTimestamp
                                    ,sReqAdapterType                        // LastChgAdapterType
                                    ,lReqWorkItemId                         // LastChgWorkItemId
                                    );
                        ++lNumComponentsChgd;
                        break;
                    case 2:
                        voltQueueSQL(insertAcceleratorHistorySql
                                    ,sNodeLctn                              // NodeLctn
                                    ,sComponentLctn                         // Lctn
                                    ,sNewState                              // State
                                    ,aComponentRows[iComponentCntr].getString("BusAddr")
                                    ,aComponentRows[iComponentCntr].getString("Slot")
                                    ,this.getTransactionTime()              // DbUpdatedTimestamp
                                    ,lTsInMicroSecs                         // LastChgTimestamp
                                    ,sReqAdapterType                        // LastChgAdapterType
                                    ,lReqWorkItemId                         // LastChgWorkItemId
                                    );
                        ++lNumComponentsChgd;
                        break;
                    case 3:
                        voltQueueSQL(insertHfiHistorySql
                                    ,sNodeLctn                              // NodeLctn
                                    ,sComponentLctn                         // Lctn
                                    ,sNewState                              // State
                                    ,aComponentRows[iComponentCntr].getString("BusAddr")
                                    ,aComponentRows[iComponentCntr].getString("Slot")
                                    ,this.getTransactionTime()              // DbUpdatedTimestamp
                                    ,lTsInMicroSecs                         // LastChgTimestamp
                                    ,sReqAdapterType                        // LastChgAdapterType
                                    ,lReqWorkItemId                         // LastChgWorkItemId
                                    );
                        ++lNumComponentsChgd;
                        break;
                }
                ++lNumComponentsChgd;
            }
        }

        voltExecuteSQL(true);

        return lNumComponentsChgd;
    }
}
