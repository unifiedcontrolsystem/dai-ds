// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai;

import java.sql.SQLException;
import java.util.HashMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Types;
import java.sql.Timestamp;
import java.util.Map;
import java.util.Calendar;
import java.util.TimeZone;

import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;
import com.intel.perflogging.BenchmarkHelper;
import com.intel.dai.dsimpl.jdbc.DbConnectionFactory;
import org.voltdb.VoltTable;
import org.voltdb.VoltType;

public class NearlineTableUpdater {
    private static final boolean USE_SNAPSHOTS = false;
    static class DataUpdateStmt {
        DataUpdateStmt(String stmtTxt, boolean isProcedure) {
            this.stmtTxt = stmtTxt;
            this.isProcedure = isProcedure;
        }
        String stmtTxt;
        boolean isProcedure;
    }
    private Logger log;
    private BenchmarkHelper benchmarker;

    NearlineTableUpdater(Logger logger) throws DataStoreException {
        log = logger;
        mCachedStmts = new HashMap<>();
        mConn = get_connection();
    }

    public Connection get_connection() throws DataStoreException {
        return DbConnectionFactory.createDefaultConnection();
    }

    public void Update(String tableName, VoltTable tableData) throws DataStoreException, SQLException  {
        mConn = get_connection();
        PreparedStatement stmt = getStmt(tableName);
        PreparedStatement snapshotStmt = getStmt(tableName + "_SS"); //Is there a snapshot table entry?
        // Is this table supported?
        if (stmt == null) {
            //log.info("SNAPSHOT TABLE UPDATE TABLE NAME %s \t STATEMNET %s",tableName+"_SS", snapshotStmt); // KEEP
            throw new DataStoreException("Unsupported table in nearline tier: " + tableName);
        }
        if (snapshotStmt != null && USE_SNAPSHOTS) {
            try {

                // Store all the data for this table
                while (tableData.advanceRow()) {
                    dbUpdateHelper(snapshotStmt, tableName, tableData);
                    dbUpdateHelper(stmt, tableName, tableData);
                }
                mConn.commit();

            } catch (SQLException ex) {
                mConn.close();
                mConn = get_connection();
            }

        } else {
            try {
                if(tableName.equals("RasEvent"))	                // Store all the data for this table
                    benchmarker.addNamedValue("BeforeRasDataWrite", tableData.getRowCount());

                // Store all the data for this table
                while (tableData.advanceRow()) {
                    dbUpdateHelper(stmt, tableName, tableData);
                }
                mConn.commit();

                if(tableName.equals("RasEvent"))
                    benchmarker.addNamedValue("WroteRasData", tableData.getRowCount());
            } catch (SQLException ex) {
                mConn.close();
                mConn = get_connection();
        }
    }

    public void setBenchmarker(BenchmarkHelper benchmarker) {
        this.benchmarker = benchmarker;
    }

    private void dbUpdateHelper (PreparedStatement stmt, String tableName, VoltTable tableData) throws SQLException{
        for (int i = 0; i < tableData.getColumnCount(); ++i) {
            VoltType voltType = tableData.getColumnType(i);
            int sqlType = voltType.getJdbcSqlType(); // Get equivalent JDBC type
            Object value;

            if (sqlType == Types.TIMESTAMP) {
                // Special case: timestamps (translate from VoltDB timestamp format to JDBC
                // timestamp format
                // Convert to GMT time zone
                value = tableData.getTimestampAsSqlTimestamp(i);
                if (value != null){
                    Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
                    stmt.setTimestamp(i + 1, (Timestamp) value, cal);
                }
                else {
                    stmt.setObject(i + 1, value, sqlType);
                }
            } else {
                value = tableData.get(i, voltType);
                if (tableData.wasNull())
                    value = null;
                stmt.setObject(i + 1, value, sqlType);
            }
        }
        stmt.execute();
    }

    private PreparedStatement getStmt(String tableName) throws DataStoreException {
        // Is this table supported?
        DataUpdateStmt dus = SQL_STMTS.get(tableName);
        if (dus == null) {
            return null;
        }

        // Do we already have a PreparedStatement for this table?
        PreparedStatement stmt = mCachedStmts.get(tableName);
        try {
            if (stmt == null) {
                // No, so let's prepare it and store it in our cache
                if (dus.isProcedure) {
                    stmt = mConn.prepareCall(dus.stmtTxt);
                } else {
                    stmt = mConn.prepareStatement(dus.stmtTxt);
                }
                mCachedStmts.put(tableName, stmt);
            }
        } catch (SQLException ex) {
            throw new DataStoreException("Unable to prepare nearline tier data update statement for table: " +
                    tableName, ex);
        }

        return stmt;
    }

    private Connection mConn;
    private Map<String, PreparedStatement> mCachedStmts;

    // SQL statements for all the supported tables in tier 2
    private static final Map<String, DataUpdateStmt> SQL_STMTS;

    static {
        SQL_STMTS = new HashMap<>();

        SQL_STMTS.put("Adapter",
            new DataUpdateStmt(
                "insert into Tier2_Adapter_History(Id, AdapterType, SconRank, State, "
                + "DbUpdatedTimestamp, LastChgAdapterType, LastChgWorkItemId, Lctn, Pid) values(?,?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("BootImage",
            new DataUpdateStmt(
                "insert into Tier2_BootImage_History(Id, Description, BootImageFile, "
                + "BootImageChecksum, BootOptions, BootStrapImageFile, BootStrapImageChecksum, State, "
                + "DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, KernelArgs, Files) "
                + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("Chassis",
            new DataUpdateStmt(
                "insert into Tier2_Chassis_History(Lctn, State, Sernum, Type, Vpd, DbUpdatedTimestamp, "
                + "LastChgTimestamp, Owner) values(?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("ComputeNode",
            new DataUpdateStmt(
                "insert into Tier2_ComputeNode_History(Lctn, SequenceNumber, State, HostName, "
                + "BootImageId, Environment, IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, "
                + "DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, "
                + "Aggregator, InventoryTimestamp, WlmNodeState, ConstraintId, ProofOfLifeTimestamp) " +
                        "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("FabricTopology",
            new DataUpdateStmt(
                "insert into Tier2_FabricTopology_History(DbUpdatedTimestamp) values(?)",
                false));
        SQL_STMTS.put("Job",
            new DataUpdateStmt(
                "insert into Tier2_Job_History(JobId, JobName, State, Bsn, NumNodes, Nodes, PowerCap, "
                + "UserName, Executable, InitialWorkingDir, Arguments, EnvironmentVars, "
                + "StartTimestamp, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, "
                + "LastChgWorkItemId, EndTimestamp, ExitStatus, JobAcctInfo, PowerUsed, WlmJobState) "
                + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("JobStep",
            new DataUpdateStmt(
                "insert into Tier2_JobStep_History(JobId, JobStepId, State, NumNodes, Nodes, "
                + "NumProcessesPerNode, Executable, InitialWorkingDir, Arguments, EnvironmentVars, "
                + "MpiMapping, StartTimestamp, DbUpdatedTimestamp, LastChgTimestamp, "
                + "LastChgAdapterType, LastChgWorkItemId, EndTimestamp, ExitStatus, WlmJobStepState) "
                + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("Lustre",
            new DataUpdateStmt(
                "insert into Tier2_Lustre_History(DbUpdatedTimestamp) values(?)",
                false));
        SQL_STMTS.put("Machine",
            new DataUpdateStmt(
                "insert into Tier2_Machine_History(Sernum, Description, Type, NumRows, NumColsInRow, "
                + "NumChassisInRack, State, ClockFreq, ManifestLctn, ManifestContent, "
                + "DbUpdatedTimestamp, UsingSynthesizedData) values(?,?,?,?,?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("Rack",
            new DataUpdateStmt(
                "insert into Tier2_Rack_History(Lctn, State, Sernum, Type, Vpd, DbUpdatedTimestamp, "
                + "LastChgTimestamp, Owner) values(?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("RasEvent",
            new DataUpdateStmt(
                "{call InsertOrUpdateRasEvent(?,?,?,?,?,?,?,?,?,?,?,?,?)}",
                true));
        SQL_STMTS.put("Replacement_History",
            new DataUpdateStmt(
                "insert into Tier2_Replacement_History(Lctn, FruType, ServiceOperationId, OldSernum, NewSernum, OldState, "
                + "NewState, DbUpdatedTimestamp, LastChgTimestamp) values(?,?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("ServiceOperation",
            new DataUpdateStmt(
                "insert into Tier2_ServiceOperation_History(ServiceOperationId, Lctn, TypeOfServiceOperation, "
                + "UserStartedService, UserStoppedService, State, Status, StartTimestamp, StopTimestamp, StartRemarks, "
                + "StopRemarks, DbUpdatedTimestamp, LogFile) values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("ServiceNode",
            new DataUpdateStmt(
                "insert into Tier2_ServiceNode_History(Lctn, SequenceNumber, HostName, State, BootImageId, "
                + "IpAddr, MacAddr, BmcIpAddr, BmcMacAddr, BmcHostName, DbUpdatedTimestamp, "
                + "LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp, ConstraintId, ProofOfLifeTimestamp) "
                + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("NonNodeHw_History",
                new DataUpdateStmt(
                "insert into Tier2_NonNodeHw_History(Lctn, SequenceNumber, Type, State, HostName, "
                + "IpAddr, MacAddr, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType,"
                + "LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp, Tier2DbUpdatedTimestamp)"
                + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?, current_timestamp)",
                false));
        SQL_STMTS.put("NodeInventory_History",
                new DataUpdateStmt(
                        "{call insertorupdatenodeinventorydata(?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("NonNodeHwInventory",
                new DataUpdateStmt(
                "insert into Tier2_NonNodeHwInventory_History(Lctn, DbUpdatedTimestamp, InventoryTimestamp, "
                + "InventoryInfo, Sernum, Tier2DbUpdatedTimestamp, EntryNumber ) values(?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("Switch",
            new DataUpdateStmt(
                "insert into Tier2_Switch_History(Lctn, State, Sernum, Type, DbUpdatedTimestamp, "
                + "LastChgTimestamp, Owner) values(?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("WorkItem",
            new DataUpdateStmt(
                "insert into Tier2_WorkItem_History(Queue, WorkingAdapterType, Id, WorkToBeDone, "
                + "Parameters, NotifyWhenFinished, State, RequestingWorkItemId, "
                + "RequestingAdapterType, WorkingAdapterId, WorkingResults, Results, StartTimestamp, "
                + "DbUpdatedTimestamp, EndTimestamp, RowInsertedIntoHistory) "
                + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("RasMetaData",
            new DataUpdateStmt(
                "{call InsertOrUpdateRasMetaData(?,?,?,?,?,?,?,?)}",
                true));
        SQL_STMTS.put("WlmReservation_History",
            new DataUpdateStmt(
                "insert into Tier2_WlmReservation_History(ReservationName, Users, Nodes, StartTimestamp, "
                + "EndTimestamp, DeletedTimestamp, LastChgTimestamp, DbUpdatedTimestamp, LastChgAdapterType, "
                + "LastChgWorkItemId) values(?,?,?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("Diag",
            new DataUpdateStmt(
                "insert into Tier2_Diag_History(DiagId, Lctn, ServiceOperationId, Diag, DiagParameters, State, StartTimestamp, "
                + "EndTimestamp, Results, DbUpdatedTimestamp, LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) "
                + "values(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("MachineAdapterInstance",
            new DataUpdateStmt(
                "insert into Tier2_MachineAdapterInstance_History(SnLctn, AdapterType, NumInitialInstances, "
                + "NumStartedInstances, Invocation, LogFile, DbUpdatedTimestamp)"
                + "values(?,?,?,?,?,?,?)",
                false));
        SQL_STMTS.put("UcsConfigValue",
            new DataUpdateStmt(
                "{call InsertOrUpdateUcsConfigValue(?,?,?)}",
                true));
        SQL_STMTS.put("UniqueValues",
            new DataUpdateStmt(
                "{call InsertOrUpdateUniqueValues(?,?,?)}",
                true));
        SQL_STMTS.put("Diag_Tools",
            new DataUpdateStmt(
                "{call InsertOrUpdateDiagTools(?,?,?,?,?,?,?,?)}",
                true));
        SQL_STMTS.put("Diag_List",
            new DataUpdateStmt(
                "{call InsertOrUpdateDiagList(?,?,?,?,?)}",
                true));
        SQL_STMTS.put("DiagResults",
            new DataUpdateStmt(
                "insert into Tier2_DiagResults(DiagId, Lctn, State, "
                + "Results, DbUpdatedTimestamp)"
                + "values(?,?,?,?,?)",
                false));
        SQL_STMTS.put("Processor",
                new DataUpdateStmt(
                        "insert into Tier2_Processor_history(NodeLctn , Lctn,"  +
                                "State, SocketDesignation,DbUpdatedTimestamp, "+
                                "LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                                "values (?,?,?,?,?,?,?,?)",
                        false));
        SQL_STMTS.put("Accelerator",
                new DataUpdateStmt(
                        "insert into Tier2_accelerator_history(NodeLctn , Lctn, " +
                                "State, BusAddr, Slot ,DbUpdatedTimestamp," +
                                "LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                                "values (?,?,?,?,?,?,?,?,?)",
                        false));
        SQL_STMTS.put("Hfi",
                new DataUpdateStmt(
                        "insert into Tier2_hfi_history(NodeLctn , Lctn, " +
                                "State, BusAddr, Slot,DbUpdatedTimestamp," +
                                "LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                                "values (?,?,?,?,?,?,?,?,?)",
                        false));
        SQL_STMTS.put("Dimm",
                new DataUpdateStmt(
                        "insert into Tier2_dimm_history(NodeLctn , Lctn, " +
                                "State, SizeMB, ModuleLocator, BankLocator,DbUpdatedTimestamp," +
                                "LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId) " +
                                "values (?,?,?,?,?,?,?,?,?,?)",
                        false));
        SQL_STMTS.put("RawHWInventory_History",
                new DataUpdateStmt(
                        "{call insertorupdaterawhwinventorydata(?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("ComputeNode_SS",
                new DataUpdateStmt(
                        "{call InsertOrUpdateComputeNodeData(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("ServiceNode_SS",
                new DataUpdateStmt(
                        "{call InsertOrUpdateServiceNodeData(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("Machine_SS",
                new DataUpdateStmt(
                        "{call InsertOrUpdateMachineData(?,?,?,?,?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("Chassis_SS",
                new DataUpdateStmt(
                        "{call InsertOrUpdateChassisData(?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("Rack_SS",
                new DataUpdateStmt(
                        "{call InsertOrUpdateRackData(?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("WorkItem_SS",
                new DataUpdateStmt(
                        "{call InsertOrUpdateWorkItemData(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("UcsConfigValue_SS",
                new DataUpdateStmt(
                        "{call InsertOrUpdateUcsConfigValue_ss(?,?,?)}",
                        true));
        SQL_STMTS.put("UniqueValues_SS",
                new DataUpdateStmt(
                        "{call InsertOrUpdateUniqueValues_ss(?,?,?)}",
                        true));
        SQL_STMTS.put("Diag_Tools_SS",
                new DataUpdateStmt(
                        "{call InsertOrUpdateDiagTools_SS(?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("BootImage_SS",
                new DataUpdateStmt(
                        "{call insertorupdatebootimagedata(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("MachineAdapterInstance_SS",
                new DataUpdateStmt(
                        "{call insertorupdatemachineadapterinstancedata(?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("RasEvent_SS",
                new DataUpdateStmt(
                        "{call insertorupdateraseventdata_ss(?,?,?,?,?,?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("Switch_SS",
                new DataUpdateStmt(
                        "{call insertorupdateswitchdata_ss(?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("NonNodeHw_History_SS",
                new DataUpdateStmt(
                        "{call insertorupdatenonnodehwdata_ss(?,?,?,?,?,?,?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("Constraint",
                new DataUpdateStmt(
                        "{call insertorupdateconstraint(?,?,?)}",
                        true));
        SQL_STMTS.put("Processor_SS",
                new DataUpdateStmt(
                        "{call insertorupdateprocessordata_ss(?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("Accelerator_SS",
                new DataUpdateStmt(
                        "{call insertorupdateacceleratordata_ss(?,?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("Hfi_SS",
                new DataUpdateStmt(
                        "{call insertorupdatehfidata_ss(?,?,?,?,?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("Dimm_SS",
                new DataUpdateStmt(
                        "{call insertorupdatedimmdata_ss(?,?,?,?,?,?,?,?,?,?)}",
                        true));

    }
}
