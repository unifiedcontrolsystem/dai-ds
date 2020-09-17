// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.procedures;

import java.lang.*;
import org.voltdb.*;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.TimeZone;
import java.text.SimpleDateFormat;


/**
 * The tables need to be returned in the following order in order for the DataMover and DataReceiver to correctly interpret these results.
 * Note: when changing, also change in the constructor for Adapter in Adapter.java and ALSO the constructor in AdapterOnlineTier (list of tables that should be purged from)!!!
 *      mDataMoverResultTblIndxToTableNameMap.put( 0, "Machine");
 *      mDataMoverResultTblIndxToTableNameMap.put( 1, "Job");
 *      mDataMoverResultTblIndxToTableNameMap.put( 2, "JobStep");
 *      mDataMoverResultTblIndxToTableNameMap.put( 3, "Rack");
 *      mDataMoverResultTblIndxToTableNameMap.put( 4, "Chassis");
 *      mDataMoverResultTblIndxToTableNameMap.put( 5, "ComputeNode");
 *      mDataMoverResultTblIndxToTableNameMap.put( 6, "ServiceNode");
 *      mDataMoverResultTblIndxToTableNameMap.put( 7, "ServiceOperation");
 *      mDataMoverResultTblIndxToTableNameMap.put( 8, "Replacement_History");
 *      mDataMoverResultTblIndxToTableNameMap.put( 9, "NonNodeHw_History");
 *      mDataMoverResultTblIndxToTableNameMap.put(10, "RasEvent");
 *      mDataMoverResultTblIndxToTableNameMap.put(11, "WorkItem");
 *      mDataMoverResultTblIndxToTableNameMap.put(12, "Adapter");
 *      mDataMoverResultTblIndxToTableNameMap.put(13, "BootImage");
 *      mDataMoverResultTblIndxToTableNameMap.put(14, "Switch");
 *      mDataMoverResultTblIndxToTableNameMap.put(15, "FabricTopology");
 *      mDataMoverResultTblIndxToTableNameMap.put(16, "Lustre");
 *      mDataMoverResultTblIndxToTableNameMap.put(17, "RasMetaData");
 *      mDataMoverResultTblIndxToTableNameMap.put(18, "WlmReservation_History");
 *      mDataMoverResultTblIndxToTableNameMap.put(19, "Diag");
 *      mDataMoverResultTblIndxToTableNameMap.put(20, "MachineAdapterInstance");
 *      mDataMoverResultTblIndxToTableNameMap.put(21, "UcsConfigValue");
 *      mDataMoverResultTblIndxToTableNameMap.put(22, "UniqueValues");
 *      mDataMoverResultTblIndxToTableNameMap.put(23, "Diag_Tools");
 *      mDataMoverResultTblIndxToTableNameMap.put(24, "Diag_List");
 *      mDataMoverResultTblIndxToTableNameMap.put(25, "DiagResults");
 *      mDataMoverResultTblIndxToTableNameMap.put(26, "NodeInventory_History");
 *      mDataMoverResultTblIndxToTableNameMap.put(27, "NonNodeHwInventory_History");
 *      mDataMoverResultTblIndxToTableNameMap.put(28, "RawHWInventory_History");
 */

public class DataMoverGetListOfRecsToMove extends VoltProcedure {

    public final SQLStmt selectMachinesToBeMovedSql                     = new SQLStmt("SELECT * FROM Machine_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, Sernum;");
    public final SQLStmt selectJobsToBeMovedSql                         = new SQLStmt("SELECT * FROM Job_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, JobId;");
    public final SQLStmt selectJobStepsToBeMovedSql                     = new SQLStmt("SELECT * FROM JobStep_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, JobId, JobStepId;");
    public final SQLStmt selectRacksToBeMovedSql                        = new SQLStmt("SELECT * FROM Rack_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, Lctn;");
    public final SQLStmt selectChassisToBeMovedSql                      = new SQLStmt("SELECT * FROM Chassis_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, Lctn;");
    public final SQLStmt selectComputeNodesToBeMovedSql                 = new SQLStmt("SELECT * FROM ComputeNode_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, Lctn;");
    public final SQLStmt selectServiceNodesToBeMovedSql                 = new SQLStmt("SELECT * FROM ServiceNode_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, Lctn;");
    public final SQLStmt selectServiceOperationsToBeMovedSql            = new SQLStmt("SELECT * FROM ServiceOperation_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, Lctn;");
    public final SQLStmt selectReplacement_HistoryToBeMovedSql          = new SQLStmt("SELECT * FROM Replacement_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, Lctn;");
    public final SQLStmt selectNonNodeHw_HistoryToBeMovedSql            = new SQLStmt("SELECT * FROM NonNodeHw_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, Lctn;");

    // Note: we are intentionally "ignoring" RAS events that have a JobId = "?", as that ras event will be updated with the appropriate JobId value and we will move it to Tier2 at that time.
    public final SQLStmt selectRasEventsToBeMovedSql                    = new SQLStmt("SELECT * FROM RasEvent WHERE (DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?)) AND (JobId IS DISTINCT FROM '?') ORDER BY DbUpdatedTimestamp ASC, EventType, Id;");

    public final SQLStmt selectWorkItemsToBeMovedSql                    = new SQLStmt("SELECT * FROM WorkItem_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, WorkingAdapterType, Id;");
    public final SQLStmt selectAdaptersToBeMovedSql                     = new SQLStmt("SELECT * FROM Adapter_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, AdapterType, Id;");
    public final SQLStmt selectBootImagesToBeMovedSql                   = new SQLStmt("SELECT * FROM BootImage_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, Id;");
    public final SQLStmt selectSwitchesToBeMovedSql                     = new SQLStmt("SELECT * FROM Switch_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, Lctn;");
    public final SQLStmt selectFabricTopologyToBeMovedSql               = new SQLStmt("SELECT * FROM FabricTopology_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC;");
    public final SQLStmt selectLustreToBeMovedSql                       = new SQLStmt("SELECT * FROM Lustre_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC;");
    public final SQLStmt selectRasMetaDataToBeMovedSql                  = new SQLStmt("SELECT * FROM RasMetaData WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC;");
    public final SQLStmt selectWlmReservationsToBeMovedSql              = new SQLStmt("SELECT * FROM WlmReservation_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC;");
    public final SQLStmt selectDiagsToBeMovedSql                        = new SQLStmt("SELECT * FROM Diag_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC;");
    public final SQLStmt selectMachineAdapterInstancesToBeMovedSql      = new SQLStmt("SELECT * FROM MachineAdapterInstance_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC;");
    public final SQLStmt selectUcsConfigValuesToBeMovedSql              = new SQLStmt("SELECT * FROM UcsConfigValue WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC;");
    public final SQLStmt selectUniqueValuesToBeMovedSql                 = new SQLStmt("SELECT * FROM UniqueValues WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC;");
    public final SQLStmt selectDiagToolsToBeMovedSql                    = new SQLStmt("SELECT * FROM Diag_Tools WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC;");
    public final SQLStmt selectDiagListsToBeMovedSql                    = new SQLStmt("SELECT * FROM Diag_List WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC;");
    public final SQLStmt selectDiagResultsToBeMovedSql                  = new SQLStmt("SELECT * FROM DiagResults WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC;");
    public final SQLStmt selectNodeInventory_HistoryToBeMovedSql        = new SQLStmt("SELECT * FROM NodeInventory_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, Lctn;");
    public final SQLStmt selectNonNodeHwInventory_HistoryToBeMovedSql   = new SQLStmt("SELECT * FROM NonNodeHwInventory_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC, Lctn;");
    public final SQLStmt selectRawHWInventory_HistoryToBeMovedSql       = new SQLStmt("SELECT * FROM RawHWInventory_History WHERE DbUpdatedTimestamp BETWEEN TO_TIMESTAMP(MICROSECOND, ?) AND TO_TIMESTAMP(MICROSECOND, ?) ORDER BY DbUpdatedTimestamp ASC;");

    public VoltTable[] run(long lEndTsInMicroSecs, long lStartTsInMicroSecs) throws VoltAbortException {
        // Get the appropriate data out of the history tables that need to be moved to Tier2.
        // Note: when changing, also change in the constructor for Adapter in Adapter.java!!!
        voltQueueSQL(selectMachinesToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectJobsToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectJobStepsToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectRacksToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectChassisToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectComputeNodesToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectServiceNodesToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectServiceOperationsToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectReplacement_HistoryToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectNonNodeHw_HistoryToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectRasEventsToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectWorkItemsToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectAdaptersToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectBootImagesToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectSwitchesToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectFabricTopologyToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectLustreToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectRasMetaDataToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectWlmReservationsToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectDiagsToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectMachineAdapterInstancesToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectUcsConfigValuesToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectUniqueValuesToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectDiagToolsToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectDiagListsToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectDiagResultsToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectNodeInventory_HistoryToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectNonNodeHwInventory_HistoryToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);
        voltQueueSQL(selectRawHWInventory_HistoryToBeMovedSql, lStartTsInMicroSecs, lEndTsInMicroSecs);

        // Actually get the results for each of the tables.
        VoltTable[] aVt = voltExecuteSQL(true);

        // Returns the array of VotlTables to the caller.
        return aVt;
    }
}
