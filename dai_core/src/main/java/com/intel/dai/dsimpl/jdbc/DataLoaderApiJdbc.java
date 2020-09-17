// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.dsimpl.jdbc;

import com.intel.dai.dsapi.DataLoaderApi;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.logging.Logger;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class DataLoaderApiJdbc implements DataLoaderApi {

    public DataLoaderApiJdbc(Connection onlineConn, Connection nearlineConn, Logger log) {
        assert onlineConn != null : "Online tier DB connection must be provided to DataLoaderApiJdbc";
        assert nearlineConn != null : "Nearline tier DB connection must be provided to DataLoaderApiJdbc";
        assert log != null : "Logger must be provided to DataLoaderApiJdbc";

        this.log = log;

        this.onlineTierConn = onlineConn;
        this.nearlineTierConn = nearlineConn;
    }

    @Override
    public boolean isNearlineTierValid() throws DataStoreException {
        PreparedStatement stmt = null;
        ResultSet rs = null;

        try {
            stmt = nearlineTierConn.prepareStatement(GET_TIER2_VALID_CONFIG_VALUE_SQL);
            rs = stmt.executeQuery();
            if (!rs.next()) {
                throw new DataStoreException("Missing configuration parameter in Nearline tier database: " +
                        "'tier2_valid'");
            }
            String value = rs.getString(VALUE_COL);
            return Boolean.parseBoolean(value);
        } catch (SQLException ex) {
            throw new DataStoreException("An error occurred while retrieving Nearline tier configuration parameter: " +
                    "'tier_valid'", ex);
        } finally {
            closeResultSet(rs);
            closeStmt(stmt);
        }
    }

    @Override
    public void setNearlineTierValid(boolean valid) throws DataStoreException {
        PreparedStatement stmt = null;

        try {
            stmt = nearlineTierConn.prepareStatement(SET_TIER2_VALID_CONFIG_VALUE_SQL);
            stmt.setString(1, Boolean.toString(valid));
            stmt.execute();
            nearlineTierConn.commit();
        } catch (SQLException ex) {
            throw new DataStoreException("An error occurred while setting Nearline tier configuration parameter:" +
                    "'tier_valid'", ex);
        } finally {
            closeStmt(stmt);
        }
    }

    @Override
    public void populateOnlineTierFromNearlineTier() throws DataStoreException {
        for (String tableName : tables) {
            populateOnlineTableFromNearline(tableName);
        }
    }

    @Override
    public void dropSnapshotTablesFromNearlineTier() {
        CallableStatement stmt = null;
        log.debug("Dropping all the data from the snapshot tables since DB is currently not coherent");
        try {
            stmt = nearlineTierConn.prepareCall("{ call truncatesnapshottablerecords() }");
            stmt.execute();
            nearlineTierConn.commit();
        } catch (SQLException ex) {
            log.warn("Unable to delete the old data from snapshot tables");
        } finally{closeStmt(stmt);}
    }

    @Override
    public void disconnectAll() {
        if(onlineTierConn != null) {
            try {
                onlineTierConn.close();
            } catch (SQLException e) {
                log.exception(e);
            }
        }
        if(nearlineTierConn != null) {
            try {
                nearlineTierConn.close();
            } catch (SQLException e) {
                log.exception(e);
            }
        }
    }

    private void populateOnlineTableFromNearline(String tableName) throws DataStoreException {
        PreparedStatement loadStmt = null;
        PreparedStatement activeStoreStmt = null;
        PreparedStatement historyStoreStmt = null;
        String historyTable = historyTables.get(tableName);
        boolean hasHistoryTable = historyTable != null;

        try {
            loadStmt = createNearlineTableLoadStmt(tableName);
            activeStoreStmt = createOnlineTierTableStoreStmt(tableName);
            if (hasHistoryTable) {
                historyStoreStmt = createOnlineTierHistoryTableStoreStmt(historyTable);
            }
        } catch (DataStoreException ex) {
            closeStmt(loadStmt);
            closeStmt(activeStoreStmt);
            closeStmt(historyStoreStmt);

            throw ex;
        }

        ResultSet data = null;
        ResultSetMetaData metaData;

        log.info("Loading data from Nearline tier table: %s", tableName);
        try {
            data = loadStmt.executeQuery();
            metaData = data.getMetaData();
        } catch (SQLException ex) {
            log.exception(ex, "Unable to load data from Nearline tier");

            closeResultSet(data);
            closeStmt(loadStmt);
            closeStmt(activeStoreStmt);
            closeStmt(historyStoreStmt);

            throw new DataStoreException("Unable to load data from Nearline tier", ex);
        }

        try {
            log.info("Storing data in Online tier table: %s", tableName);
            Map<String, Integer> columnTypes = getColumnTypesFromMetaData(metaData);
            while (data.next()) {
                String[] fields = tableFields.get(tableName);
                assert fields != null : "Missing Nearline database schema details in DataLoaderApi";

                setStmtData(data, tableName, fields, columnTypes, activeStoreStmt);
                activeStoreStmt.execute();

                if (hasHistoryTable) {
                    String[] historyFields = historyTableFields.get(historyTable);
                    assert historyFields != null : "Missing Nearline database schema details in DataLoaderApi";

                    setStmtData(data, historyTable, historyFields, columnTypes, historyStoreStmt);
                    historyStoreStmt.execute();
                }
            }
        } catch (SQLException ex) {
            log.exception(ex, "An error occurred while updating Online tier database");
            throw new DataStoreException("An error occurred while updating Online tier database", ex);
        } finally {
            closeResultSet(data);
            closeStmt(loadStmt);
            closeStmt(activeStoreStmt);
            closeStmt(historyStoreStmt);
        }
    }

    private PreparedStatement createNearlineTableLoadStmt(String tableName) throws DataStoreException {
        try {
            PreparedStatement stmt = nearlineTierConn.prepareCall(
                    generateProcCallText(tableToProcedure.get(tableName)));
            return stmt;
        } catch (SQLException ex) {
            throw new DataStoreException("Unable to prepare statement to load data from Nearline tier table: " +
                    tableName, ex);
        }
    }

    private PreparedStatement createOnlineTierTableStoreStmt(String tableName) throws DataStoreException {
        try {
            String[] fields = tableFields.get(tableName);
            // fields should never be null (if it is, it's a programmer error)
            assert fields != null : "Missing Nearline database schema details in DataLoaderApi";

            PreparedStatement stmt = onlineTierConn.prepareStatement(
                    generateInsertStatementText(tableName, fields));
            return stmt;
        } catch (SQLException ex) {
            throw new DataStoreException("Unable to prepare statement to store data in Online tier table: " +
                    tableName, ex);
        }
    }

    private PreparedStatement createOnlineTierHistoryTableStoreStmt(String historyTableName) throws DataStoreException {
        try {
            String[] fields = historyTableFields.get(historyTableName);
            // fields should never be null (if it is, it's a programmer error)
            assert fields != null : "Missing Nearline database schema details in DataLoaderApi";

            PreparedStatement stmt = onlineTierConn.prepareStatement(
                    generateInsertStatementText(historyTableName, fields));
            return stmt;
        } catch (SQLException ex) {
            throw new DataStoreException("Unable to prepare statement to store data in Online tier history table: " +
                    historyTableName, ex);
        }
    }

    private void setStmtData(ResultSet data, String tableName, String[] fields, Map<String, Integer> columnTypes,
                             PreparedStatement stmt) throws SQLException {
        for (int i = 0; i < fields.length; i++) {
            Integer sqlType = columnTypes.get(fields[i].toUpperCase());
            if (sqlType == null) {
                log.error("An unexpected error has occurred: column not found in table %s: %s", tableName, fields[i]);
                throw new RuntimeException(
                        String.format("An unexpected error has occurred: column not found in table %s: %s",
                                tableName, fields[i]));
            }
            if (sqlType == Types.BINARY || sqlType == Types.VARBINARY) {
                // Special case for binary field types, as not every DBMS supports these in the same way.  For more
                // compatibility, handle this as a byte array (explicit type instead of generic Object).
                byte[] bytes = data.getBytes(fields[i]);
                if (bytes == null) {
                    stmt.setNull(i + 1, sqlType);
                } else {
                    stmt.setBytes(i + 1, bytes);
                }
            } else if (sqlType == Types.TIMESTAMP) {
                Object value = data.getTimestamp(fields[i]);
                if (value == null) {
                    stmt.setNull(i + 1, sqlType);
                }
                else {
                    Timestamp fromGmt = new Timestamp(((Timestamp) value).getTime() + TimeZone.getDefault().getOffset(((Timestamp) value).getTime()));
                    stmt.setObject(i+1, fromGmt, sqlType);
                }
            } else {
                Object value = data.getObject(fields[i]);
                if (value == null) {
                    stmt.setNull(i + 1, sqlType);
                } else {
                    stmt.setObject(i + 1, value, sqlType);
                }
            }
        }
    }


    private String generateProcCallText(String procedure) {
        String call = String.format("{call %s}", procedure);
        return call;
    }

    private String generateInsertStatementText(String tableName, String[] fields) {
        String csFields = fields[0];
        String csValues = "?";
        for (int i = 1; i < fields.length; i++) {
            csFields += ", " + fields[i];
            csValues += ", ?";
        }

        String query = String.format("insert into %s (%s) values (%s);", tableName, csFields, csValues);
        return query;
    }

    private Map<String, Integer> getColumnTypesFromMetaData(ResultSetMetaData metaData) throws SQLException {
        Map<String, Integer> columnTypes = new HashMap<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            columnTypes.put(metaData.getColumnName(i).toUpperCase(), metaData.getColumnType(i));
        }
        return columnTypes;
    }

    private void closeResultSet(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        } catch (SQLException ex) {
            log.warn("An unexpected error occurred while closing a DB resources (result set): " + ex.getMessage());
        }
    }

    private void closeStmt(PreparedStatement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        } catch (SQLException ex) {
            log.warn("An unexpected error occurred while closing a DB resources (statement): " + ex.getMessage());
        }
    }

    private static final String[] TABLES = { "BootImage", "Chassis", "ComputeNode",
            "Machine", "Rack", "ServiceNode", "ServiceOperation", "Switch", "WorkItem", "Diag_Tools", "MachineAdapterInstance", "RasMetaData", "CacheIpAddrToLctn", "CacheMacAddrToLctn",
            "UcsConfigValue", "UniqueValues", "RawHWInventory_History"};
    private static final Map<String, String> HISTORY_TABLES;
    private static final Map<String, String> TABLE_TO_PROCEDURE;
    private static final Map<String, String[]> TABLE_FIELDS;
    private static final Map<String, String[]> HISTORY_TABLE_FIELDS;

    // Provide a convenience entry point for unit tests
    String[] tables = TABLES;
    Map<String, String> historyTables = HISTORY_TABLES;
    Map<String, String> tableToProcedure = TABLE_TO_PROCEDURE;
    Map<String, String[]> tableFields = TABLE_FIELDS;
    Map<String, String[]> historyTableFields = HISTORY_TABLE_FIELDS;

    private static final String VALUE_COL = "value";
    private static final String GET_TIER2_VALID_CONFIG_VALUE_SQL =
            "select " + VALUE_COL + " from tier2_config where key = 'tier2_valid'";
    private static final String SET_TIER2_VALID_CONFIG_VALUE_SQL =
            "update tier2_config set " + VALUE_COL + " = ? where key = 'tier2_valid'";

    static {
        String[] adapterFields = {"Id", "AdapterType", "SconRank", "State", "DbUpdatedTimestamp", "LastChgAdapterType",
                "LastChgWorkItemId", "Lctn", "Pid"};
        String[] bootImageFields = {"Id", "Description", "BootImageFile", "BootImageChecksum", "BootOptions",
                "BootstrapImageFile", "BootstrapImageChecksum", "State", "DbUpdatedTimestamp", "LastChgTimestamp",
                "LastChgAdapterType", "LastChgWorkItemId", "KernelArgs", "Files"};
        String[] chassisFields = {"Lctn", "State", "Sernum", "Type", "Vpd", "Owner", "DbUpdatedTimestamp",
                "LastChgTimestamp"};
        String[] computeNodeFields = {"Lctn", "SequenceNumber", "State", "Hostname",  "BootImageId",
                "Environment", "IpAddr", "MacAddr", "BmcIpAddr", "BmcMacAddr", "BmcHostname",
                "DbUpdatedTimestamp", "LastChgTimestamp", "LastChgAdapterType", "LastChgWorkItemId", "Owner",
                "Aggregator", "InventoryTimestamp", "WlmNodeState"};
        String[] jobFields = {"JobId", "JobName", "State", "Bsn", "NumNodes", "Nodes", "PowerCap", "Username",
                "Executable", "InitialWorkingDir", "Arguments", "EnvironmentVars", "StartTimestamp",
                "DbUpdatedTimestamp", "LastChgTimestamp", "LastChgAdapterType", "LastChgWorkItemId"/*, "EndTimestamp",
                "ExitStatus", "JobAcctInfo", "PowerUsed", "WlmJobState"*/};
        String[] jobHistoryFields = {"JobId", "JobName", "State", "Bsn", "NumNodes", "Nodes", "PowerCap", "Username",
                "Executable", "InitialWorkingDir", "Arguments", "EnvironmentVars", "StartTimestamp",
                "DbUpdatedTimestamp", "LastChgTimestamp", "LastChgAdapterType", "LastChgWorkItemId", "EndTimestamp",
                "ExitStatus", "JobAcctInfo", "PowerUsed", "WlmJobState"};
        String[] jobStepFields = {"JobId", "JobStepId", "State", "NumNodes", "Nodes", "NumProcessesPerNode",
                "Executable", "InitialWorkingDir", "Arguments", "EnvironmentVars", "MpiMapping", "StartTimestamp",
                "DbUpdatedTimestamp", "LastChgTimestamp", "LastChgAdapterType", "LastChgWorkItemId"/*, "EndTimestamp",
                "ExitStatus", "WlmJobStepState"*/};
        String[] jobStepHistoryFields = {"JobId", "JobStepId", "State", "NumNodes", "Nodes", "NumProcessesPerNode",
                "Executable", "InitialWorkingDir", "Arguments", "EnvironmentVars", "MpiMapping", "StartTimestamp",
                "DbUpdatedTimestamp", "LastChgTimestamp", "LastChgAdapterType", "LastChgWorkItemId", "EndTimestamp",
                "ExitStatus", "WlmJobStepState"};
        String[] machineFields = {"Sernum", "Description", "Type", "NumRows", "NumColsInRow", "NumChassisInRack",
                "State", "ClockFreq", "ManifestLctn", "ManifestContent", "DbUpdatedTimestamp", "UsingSynthesizedData"};
        String[] rackFields = {"Lctn", "State", "Sernum", "Type", "Vpd", "Owner", "DbUpdatedTimestamp",
                "LastChgTimestamp"};
        String[] serviceNodeFields = {"Lctn", "SequenceNumber", "Hostname", "State",  "BootImageId", "IpAddr",
                "MacAddr", "BmcIpAddr", "BmcMacAddr", "BmcHostname", "DbUpdatedTimestamp", "LastChgTimestamp",
                "LastChgAdapterType", "LastChgWorkItemId", "Owner", "Aggregator", "InventoryTimestamp"};
        String[] serviceOperationFields = {"ServiceOperationId", "Lctn", "TypeOfServiceOperation", "UserStartedService",
                "UserStoppedService", "State", "Status", "StartTimestamp", "StopTimestamp", "StartRemarks",
                "StopRemarks", "DbUpdatedTimestamp", "LogFile"};
        String[] switchFields = {"Lctn", "State", "Sernum", "Type", "Owner", "DbUpdatedTimestamp", "LastChgTimestamp"};
        String[] workItemFields = {"Queue", "WorkingAdapterType", "Id", "WorkToBeDone", "Parameters",
                "NotifyWhenFinished", "State", "RequestingWorkItemId", "RequestingAdapterType", "WorkingAdapterId",
                "WorkingResults", "Results", "StartTimestamp", "DbUpdatedTimestamp"/*, "EndTimestamp",
                "RowInsertedIntoHistory"*/};
        String[] workItemHistoryFields = {"Queue", "WorkingAdapterType", "Id", "WorkToBeDone", "Parameters",
                "NotifyWhenFinished", "State", "RequestingWorkItemId", "RequestingAdapterType", "WorkingAdapterId",
                "WorkingResults", "Results", "StartTimestamp", "DbUpdatedTimestamp", "EndTimestamp",
                "RowInsertedIntoHistory"};
        String[] diagFields = {"DiagId", "Lctn", "ServiceOperationId", "Diag", "DiagParameters", "State",
                "StartTimestamp", "EndTimestamp", "Results", "DbUpdatedTimestamp", "LastChgTimestamp",
                "LastChgAdapterType", "LastChgWorkItemId"};
        String[] diagListFields = {"DiagListId", "DiagToolId", "Description", "DefaultParameters",
                "DbUpdatedTimestamp"};
        String[] diagToolsFields = {"DiagToolId", "Description", "UnitType", "UnitSize", "ProvisionReqd",
                "RebootBeforeReqd", "RebootAfterReqd", "DbUpdatedTimestamp"};
        String[] machineAdapterInstanceFields = {"SnLctn", "AdapterType", "NumInitialInstances", "NumStartedInstances",
                "Invocation", "LogFile", "DbUpdatedTimestamp"};
        String[] rasMetaDataFields = {"EventType", "DescriptiveName", "Severity", "Category", "Component",
                "ControlOperation", "Msg", "DbUpdatedTimestamp"};
        String[] cacheIpAddrToLctnFields = {"IpAddr", "Lctn"};
        String[] cacheMacAddrToLctnFields = {"MacAddr", "Lctn"};
        String[] ucsConfigValueFields = {"Key", "Value", "DbUpdatedTimestamp"};
        String[] uniqueValuesFields = {"Entity", "NextValue", "DbUpdatedTimestamp"};
        String[] hwinventoryfruFields = {"FruId", "FruType", "FruSubType", "DbUpdatedTimestamp"};
        String[] hwinventorylocationFields = {"Id", "Type", "Ordinal", "FruId", "DbUpdatedTimestamp"};
        String[] rawinventoryhistoryFields = {"Action", "Id", "FruId", "ForeignTimestamp", "DbUpdatedTimestamp"};

        TABLE_FIELDS = new HashMap<>();
        TABLE_FIELDS.put("Adapter", adapterFields);
        TABLE_FIELDS.put("BootImage", bootImageFields);
        TABLE_FIELDS.put("Chassis", chassisFields);
        TABLE_FIELDS.put("ComputeNode", computeNodeFields);
        TABLE_FIELDS.put("Job", jobFields);
        TABLE_FIELDS.put("JobStep", jobStepFields);
        TABLE_FIELDS.put("Machine", machineFields);
        TABLE_FIELDS.put("Rack", rackFields);
        TABLE_FIELDS.put("ServiceNode", serviceNodeFields);
        TABLE_FIELDS.put("ServiceOperation", serviceOperationFields);
        TABLE_FIELDS.put("Switch", switchFields);
        TABLE_FIELDS.put("WorkItem", workItemFields);
        TABLE_FIELDS.put("Diag", diagFields);
        TABLE_FIELDS.put("Diag_List", diagListFields);
        TABLE_FIELDS.put("Diag_Tools", diagToolsFields);
        TABLE_FIELDS.put("MachineAdapterInstance", machineAdapterInstanceFields);
        TABLE_FIELDS.put("RasMetaData", rasMetaDataFields);
        TABLE_FIELDS.put("CacheIpAddrToLctn", cacheIpAddrToLctnFields);
        TABLE_FIELDS.put("CacheMacAddrToLctn", cacheMacAddrToLctnFields);
        TABLE_FIELDS.put("UcsConfigValue", ucsConfigValueFields);
        TABLE_FIELDS.put("UniqueValues", uniqueValuesFields);
        TABLE_FIELDS.put("RawHWInventory_History", rawinventoryhistoryFields);

        HISTORY_TABLES = new HashMap<>();
        HISTORY_TABLES.put("Adapter", "Adapter_History");
        HISTORY_TABLES.put("BootImage", "BootImage_History");
        HISTORY_TABLES.put("Chassis", "Chassis_History");
        HISTORY_TABLES.put("ComputeNode", "ComputeNode_History");
        HISTORY_TABLES.put("Job", "Job_History");
        HISTORY_TABLES.put("JobStep", "JobStep_History");
        HISTORY_TABLES.put("Machine", "Machine_History");
        HISTORY_TABLES.put("Rack", "Rack_History");
        HISTORY_TABLES.put("ServiceNode", "ServiceNode_History");
        HISTORY_TABLES.put("ServiceOperation", "ServiceOperation_History");
        HISTORY_TABLES.put("Switch", "Switch_History");
        HISTORY_TABLES.put("WorkItem", "WorkItem_History");
        HISTORY_TABLES.put("Diag", "Diag_History");
        HISTORY_TABLES.put("MachineAdapterInstance", "MachineAdapterInstance_History");

        HISTORY_TABLE_FIELDS = new HashMap<>();
        HISTORY_TABLE_FIELDS.put("Adapter_History", adapterFields);
        HISTORY_TABLE_FIELDS.put("BootImage_History", bootImageFields);
        HISTORY_TABLE_FIELDS.put("Chassis_History", chassisFields);
        HISTORY_TABLE_FIELDS.put("ComputeNode_History", computeNodeFields);
        HISTORY_TABLE_FIELDS.put("Job_History", jobHistoryFields);
        HISTORY_TABLE_FIELDS.put("JobStep_History", jobStepHistoryFields);
        HISTORY_TABLE_FIELDS.put("Machine_History", machineFields);
        HISTORY_TABLE_FIELDS.put("Rack_History", rackFields);
        HISTORY_TABLE_FIELDS.put("ServiceNode_History", serviceNodeFields);
        HISTORY_TABLE_FIELDS.put("ServiceOperation_History", serviceOperationFields);
        HISTORY_TABLE_FIELDS.put("Switch_History", switchFields);
        HISTORY_TABLE_FIELDS.put("WorkItem_History", workItemHistoryFields);
        HISTORY_TABLE_FIELDS.put("Diag_History", diagFields);
        HISTORY_TABLE_FIELDS.put("MachineAdapterInstance_History", machineAdapterInstanceFields);

        TABLE_TO_PROCEDURE = new HashMap<>();
        TABLE_TO_PROCEDURE.put("Adapter", "get_latest_adapter_records()");
        TABLE_TO_PROCEDURE.put("BootImage", "get_latest_bootimage_records()");
        TABLE_TO_PROCEDURE.put("Chassis", "get_latest_chassis_records()");
        TABLE_TO_PROCEDURE.put("ComputeNode", "get_latest_computenode_records()");
        TABLE_TO_PROCEDURE.put("Job", "get_latest_job_records()");
        TABLE_TO_PROCEDURE.put("JobStep", "get_latest_jobstep_records()");
        TABLE_TO_PROCEDURE.put("Machine", "get_latest_machine_records()");
        TABLE_TO_PROCEDURE.put("Rack", "get_latest_rack_records()");
        TABLE_TO_PROCEDURE.put("ServiceNode", "get_latest_servicenode_records()");
        TABLE_TO_PROCEDURE.put("ServiceOperation", "get_latest_serviceoperation_records()");
        TABLE_TO_PROCEDURE.put("Switch", "get_latest_switch_records()");
        TABLE_TO_PROCEDURE.put("WorkItem", "get_latest_workitem_records()");
        TABLE_TO_PROCEDURE.put("Diag", "get_latest_diag_records()");
        TABLE_TO_PROCEDURE.put("Diag_List", "get_diag_list_records()");
        TABLE_TO_PROCEDURE.put("Diag_Tools", "get_diag_tools_records()");
        TABLE_TO_PROCEDURE.put("MachineAdapterInstance", "get_latest_machineadapterinstance_records()");
        TABLE_TO_PROCEDURE.put("RasMetaData", "get_rasmetadata_records()");
        TABLE_TO_PROCEDURE.put("CacheIpAddrToLctn", "get_cacheipaddrtolctn_records()");
        TABLE_TO_PROCEDURE.put("CacheMacAddrToLctn", "get_cachemacaddrtolctn_records()");
        TABLE_TO_PROCEDURE.put("UcsConfigValue", "get_ucsconfigvalue_records()");
        TABLE_TO_PROCEDURE.put("UniqueValues", "get_uniquevalues_records()");
        TABLE_TO_PROCEDURE.put("RawHWInventory_History", "get_rawinventoryhistory_records()");
        TABLE_TO_PROCEDURE.put("NodeInventory_History", "get_nodeinventoryhistory_records()");
    }

    private Connection onlineTierConn;
    private Connection nearlineTierConn;
    private Logger log;

    private PreparedStatement tier2ValidGetStmt = null;
    private PreparedStatement tier2ValidSetStmt = null;
}