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
import org.voltdb.VoltTable;
import org.voltdb.VoltType;

public class NearlineTableUpdater {
    // Defaults to use coherency, set system property 'useCoherency' to 'false' to turn it off.
    private static final boolean USE_COHERENCY = Boolean.parseBoolean(System.getProperty("useCoherency", "true"));

    static class DataUpdateStmt {
        DataUpdateStmt(String stmtTxt, boolean isProcedure) {
            this.stmtTxt = stmtTxt;
            this.isProcedure = isProcedure;
        }
        String stmtTxt;
        boolean isProcedure;
    }

    NearlineTableUpdater(Connection tier2DbConn, Logger logger, BenchmarkHelper benchmarking) {
        log_ = logger;
        mCachedStmts = new HashMap<>();
        mConn = tier2DbConn;
        benchmarking_ = benchmarking;
    }

    public void update(String tableName, VoltTable tableData) throws DataStoreException {
        PreparedStatement stmt = getStmt(tableName);
        // Is this table supported?
        log_.info("*** Starting update process...");
        if (stmt == null) {
            throw new DataStoreException("Unsupported table in nearline tier: " + tableName);
        }
        try {
            // Store all the data for this table
            if(tableName.equals("RasEvent"))
                benchmarking_.addNamedValue("BeforeRasDataWrite", tableData.getRowCount());
            while (tableData.advanceRow()) {
                dbUpdateHelper(stmt, tableData);
            }
            mConn.commit();
            if(tableName.equals("RasEvent"))
                benchmarking_.addNamedValue("WroteRasData", tableData.getRowCount());
        } catch (SQLException ex) {
            try {
                mConn.rollback();
            } catch(SQLException e) { /* Do Nothing on failure */ }
            throw new DataStoreException("Unable to update nearline tier table: " + tableName, ex);
        }
    }

    private void dbUpdateHelper (PreparedStatement stmt, VoltTable tableData) throws SQLException {
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
                    stmt.setObject(i + 1, null, sqlType);
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
        addTables();
    }

    private static void addTables() {
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
                                + "Aggregator, InventoryTimestamp, WlmNodeState) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
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
                                + "LastChgTimestamp, LastChgAdapterType, LastChgWorkItemId, Owner, Aggregator, InventoryTimestamp) "
                                + "values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
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
                        "{call InsertOrUpdateRasMetaData(?,?,?,?,?,?,?,?,?)}",
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
        SQL_STMTS.put("HW_Inventory_Fru",
                new DataUpdateStmt(
                        "{call InsertOrUpdateHWInventoryFru(?,?,?,?)}",
                        true));
        SQL_STMTS.put("HW_Inventory_Location",
                new DataUpdateStmt(
                        "{call InsertOrUpdateHWInventoryLocation(?,?,?,?,?)}",
                        true));
        SQL_STMTS.put("RawHWInventory_History",
                new DataUpdateStmt(
                        "insert into tier2_RawHWInventory_History(Action, id, fruid, DbUpdatedTimestamp)"
                                + "values(?,?,?,?)",
                        false));

    }

    private Logger log_;
    private BenchmarkHelper benchmarking_;
}
