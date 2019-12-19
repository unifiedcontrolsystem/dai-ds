// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;
import java.sql.ResultSet;

import com.intel.dai.dsimpl.jdbc.DbConnectionFactory;
import com.intel.config_io.*;
import com.intel.logging.LoggerFactory;
import com.intel.logging.Logger;
import com.intel.properties.*;
import com.intel.dai.exceptions.DataStoreException;

public class CannedAPI {
    private Connection conn = null;
    private Logger log_;
    JsonConverter jsonConverter = new JsonConverter();
    private static ConfigIO jsonParser = ConfigIOFactory.getInstance("json");

    CannedAPI(Logger logger) {
        log_ = logger;
        assert jsonParser != null: "Failed to get a JSON parser!";
    }

    public Connection get_connection() throws DataStoreException {
        return DbConnectionFactory.createDefaultConnection();
    }

    public synchronized String getData(String requestKey, Map<String, String> params_map)
            throws SQLException, DataStoreException {
        assert params_map != null : "Input parameters should be provided";
        log_.info("Establishing DB connection");
        conn = get_connection();
        Timestamp endtime = getTimestamp(getStartEndTime(params_map, "EndTime"));
        Timestamp starttime = getTimestamp(getStartEndTime(params_map, "StartTime"));
        int limit = Integer.parseInt(params_map.getOrDefault("Limit", "100"));
        PropertyMap jsonResult;
        switch (requestKey) {
            case "getraswithjobid":
                String jobId = params_map.getOrDefault("JobId", "null");
                jsonResult = executeProcedureOneVariableFilter("{call GetRasEventForJob(?, ?, ?, ?)}", starttime, endtime, jobId, limit);
                break;

            case "getraswithfilters": {
                String lctn = params_map.getOrDefault("Lctn", null);
                String event_type = params_map.getOrDefault("EventType", "%");
                String severity = params_map.getOrDefault("Severity", "%");
                String jobIdValue = params_map.getOrDefault("JobId", null);
                jsonResult = executeProcedureFourVariableFilter("{call GetRasEventsWithFilters(?, ?, ?, ?, ?, ?, ?)}", starttime, endtime, lctn, event_type, severity, jobIdValue, limit);
                break;
            }
            case "getenvwithfilters": {
                String lctn = params_map.getOrDefault("Lctn", "%");
                jsonResult = executeProcedureOneVariableFilter("{call GetAggregatedEvnDataWithFilters(?, ?, ?, ?)}", starttime, endtime, lctn, limit);
                break;
            }
            case "getinvchanges": {
                String lctn = params_map.getOrDefault("Lctn", "%");
                jsonResult = executeProcedureOneVariableFilter("{call GetInventoryChange(?, ?, ?, ?)}", starttime, endtime, lctn, limit);
                break;
            }
            case "getinvspecificlctn": {
                String lctn = params_map.getOrDefault("Lctn", "%");
                jsonResult = executeProcedureOneVariableFilter("{call GetInventoryDataForLctn(?, ?, ?, ?)}", starttime, endtime, lctn, limit);
                break;
            }
            case "getsnapshotspecificlctn": {
                String lctn = params_map.getOrDefault("Lctn", "%");
                jsonResult = executeProcedureOneVariableFilter("{call GetSnapshotDataForLctn(?, ?, ?, ?)}", starttime, endtime, lctn, limit);
                break;
            }
            case "getrefsnapshot": {
                if(!params_map.containsKey("Lctn")){
                    jsonResult = new PropertyMap();
                    jsonResult.put("result-status-code", 1);
                    jsonResult.put("Error", "Get Reference Snapshot API parameter: Lctn");
                    return jsonParser.toString(jsonResult);
                }
                String lctn = params_map.getOrDefault("Lctn", "%");
                jsonResult = executeProcedure("{call GetRefSnapshotDataForLctn(?, ?)}", lctn, limit);
                break;
            }
            case "getjobdata": {
                String lctn = params_map.getOrDefault("Lctn", "%");
                String jobid = params_map.getOrDefault("JobId", "%");
                log_.info("GetJobPowerData procedure called with Lctn = %s and JobId = %s", lctn, jobid);
                jsonResult = executeProcedureTwoVariableFilter("{call GetJobPowerData(?, ?, ?, ?, ?)}", starttime, endtime, lctn, jobid, limit);
                break;
            }
            case "getdiagsdata": {
                String lctn = params_map.getOrDefault("Lctn", "%");
                String diagid = params_map.getOrDefault("DiagId", "%");
                log_.info("GetDiagData procedure called with Lctn = %s and DiagId = %s", lctn, diagid);
                jsonResult = executeProcedureTwoVariableFilter("{call GetDiagData(?, ?, ?, ?, ?)}", starttime, endtime, lctn, diagid, limit);
                break;
            }
            default:
                return "Invalid request, request key: '" + requestKey + "' : Not Found";
        }

        conn.close();
        return jsonParser.toString(jsonResult);
    }

    private String getStartEndTime(Map<String, String> params_map, String key)
    {
        String val_time;
        val_time = params_map.getOrDefault(key, "null");
        return val_time;
    }

    private Timestamp getTimestamp(String Time)
    {
        Timestamp new_time;
        if(Time == null || Time.equalsIgnoreCase("null"))
            return null;
        else
            new_time = Timestamp.valueOf(Time);
        return new_time;
    }

    private PropertyMap executeProcedure(String prepProcedure, String FilterVariableOne, int Limit)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prepProcedure)) {
            stmt.setString(1, FilterVariableOne);
            stmt.setInt(2, Limit);
            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }

    private PropertyMap executeProcedureOneVariableFilter(String prepProcedure, Timestamp StartTime,
                                                            Timestamp EndTime, String FilterVariableOne, int Limit)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prepProcedure)) {
            stmt.setTimestamp(1, StartTime);
            stmt.setTimestamp(2, EndTime);
            stmt.setString(3, FilterVariableOne);
            stmt.setInt(4, Limit);
            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }

    private PropertyMap executeProcedureTwoVariableFilter(String prepProcedure, Timestamp StartTime,
                                                            Timestamp EndTime, String FilterVariableOne,
                                                            String FilterVariableTwo ,int Limit)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prepProcedure)) {
            stmt.setTimestamp(1, StartTime);
            stmt.setTimestamp(2, EndTime);
            stmt.setString(3, FilterVariableOne);
            stmt.setString(4, FilterVariableTwo);
            stmt.setInt(5, Limit);
            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }

    private PropertyMap executeProcedureFourVariableFilter(String prepProcedure, Timestamp StartTime,
                                                              Timestamp EndTime, String FilterVariableOne, String FilterVariableTwo, String FilterVariableThree, String  FilterVariableFour, int Limit)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prepProcedure)) {
            stmt.setTimestamp(1, StartTime);
            stmt.setTimestamp(2, EndTime);
            stmt.setString(3, FilterVariableOne);
            stmt.setString(4, FilterVariableTwo);
            stmt.setString(5, FilterVariableThree);
            stmt.setString(6, FilterVariableFour);
            stmt.setInt(7, Limit);
            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }

}
