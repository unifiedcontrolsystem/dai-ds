// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;

import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.*;
import java.sql.ResultSet;
import java.sql.Types;
import java.lang.Integer;

import com.intel.dai.dsimpl.jdbc.DbConnectionFactory;
import com.intel.config_io.*;
import com.intel.dai.exceptions.ProviderException;
import com.intel.logging.LoggerFactory;
import com.intel.logging.Logger;
import com.intel.properties.*;
import com.intel.dai.exceptions.DataStoreException;

@SuppressWarnings("serial")
public class CannedAPI {
    private Connection conn = null;
    JsonConverter jsonConverter = new JsonConverter();
    private static ConfigIO jsonParser = ConfigIOFactory.getInstance("json");

    private static final Map<String, String> owner_map = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put("W", "WLM");
                put("S", "Service");
                put("G", "General");
                put("F", "Free Pool");
            }});

    private static final Map<String, String> state_map = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put("B", "Bios Starting");
                put("D", "Discovered (dhcp discover)");
                put("I", "IP address assigned (dhcp request)");
                put("L", "Starting load of Boot images");
                put("K", "Kernel boot started");
                put("A", "Active");
                put("M", "Missing");
                put("E", "Error");
                put("U", "Unknown");
            }});

    private static final Map<String, String> wlmstate_map = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put("A", "Available");
                put("U", "Unavailable");
                put("G", "General");
                put("F", "Free Pool");
            }});

    private static final Map<String, String> jobstate_map = Collections.unmodifiableMap(
            new HashMap<String, String>() {{
                put("T", "Terminated");
                put("S", "Started");
            }});

    CannedAPI() {
        assert jsonParser != null: "Failed to get a JSON parser!";
    }

    public Connection get_connection() throws DataStoreException {
        return DbConnectionFactory.createDefaultConnection();
    }

    public synchronized String getData(String requestKey, Map<String, String> params_map)
            throws SQLException, DataStoreException, ProviderException {
        assert params_map != null : "Input parameters should be provided";
        Logger log_ = LoggerFactory.getInstance("UI", AdapterUIRest.class.getName(), "log4j2");
        log_.info("Establishing DB connection");
        conn = get_connection();
        try {
            Timestamp endtime = getTimestamp(getStartEndTime(params_map, "EndTime"));
            Timestamp starttime = getTimestamp(getStartEndTime(params_map, "StartTime"));
            String limit = params_map.getOrDefault("Limit", null);

            PropertyMap jsonResult;
            switch (requestKey) {
                case "getraswithfilters": {
                    String lctn = params_map.getOrDefault("Lctn", null);
                    String event_type = params_map.getOrDefault("EventType", "%");
                    String severity = params_map.getOrDefault("Severity", "%");
                    String jobIdValue = params_map.getOrDefault("JobId", null);
                    String exclude = params_map.getOrDefault("Exclude", "%");
                    jsonResult = executeProcedureFiveVariableFilter("{call GetRasEventsWithFilters(?, ?, ?, ?, ?, ?, ?, ?)}", starttime, endtime, lctn, event_type, severity, jobIdValue, limit, exclude);
                    break;
                }
                case "getenvwithfilters": {
                    String lctn = params_map.getOrDefault("Lctn", "%");
                    jsonResult = executeProcedureOneVariableFilter("{call GetAggregatedEvnDataWithFilters(?, ?, ?, ?)}", starttime, endtime, lctn, limit);
                    break;
                }
                case "getinvspecificlctn": {
                    String lctn = params_map.getOrDefault("Lctn", "%");
                    jsonResult = executeProcedureOneVariableFilter("{call GetInventoryDataForLctn(?, ?, ?, ?)}", starttime, endtime, lctn, limit);
                    jsonResult = map_state_values(jsonResult);
                    break;
                }
                case "getjobinfo": {
                    String username = params_map.getOrDefault("Username", "%");
                    String jobid = params_map.getOrDefault("Jobid", "%");
                    String state = params_map.getOrDefault("State", "%");
                    log_.info("GetJobInfo procedure called with Jobid = %s and Username = %s", jobid, username);
                    jsonResult = executeProcedureThreeVariableFilter("{call GetJobInfo(?, ?, ?, ?,?, ?)}", starttime, endtime, jobid, username, state, limit);
                    jsonResult = map_job_values(jsonResult);
                    break;
                }
                case "getreservationinfo": {
                    String username = params_map.getOrDefault("Username", null);
                    String reservation = params_map.getOrDefault("Name", null);
                    log_.info("GetReservationInfo procedure called with Reservation Name = %s and Username = %s", reservation, username);
                    jsonResult = executeProcedureTwoVariableFilter("{call GetReservationInfo(?, ?, ?, ?, ?)}", starttime, endtime, reservation, username, limit);
                    break;
                }
                case "system_summary": {
                    jsonResult = new PropertyMap();
                    log_.info("GetComputeNodeSummary procedure called");
                    jsonResult.put("compute", map_state_values(executeProcedure("{call GetComputeNodeSummary()}")));

                    log_.info("GetServiceNodeSummary procedure called");
                    jsonResult.put("service", map_state_values(executeProcedure("{call GetServiceNodeSummary()}")));
                    break;
                }
                default:
                    throw new ProviderException("Invalid request, request key: '" + requestKey + "' : Not Found");
            }
            return jsonParser.toString(jsonResult);
        } finally {
            conn.close();
        }
    }

    private PropertyMap map_state_values(PropertyMap jsonResult)
    {
        try {
            Logger log_ = LoggerFactory.getInstance("UI", AdapterUIRest.class.getName(), "log4j2");
            Integer owner_pos = null;
            Integer state_pos = null;
            Integer wlmnodestate_pos = null;

            PropertyArray schema = jsonResult.getArray("schema");

            if(schema != null){
                for(int i = 0; i < schema.size(); i++){
                    PropertyMap m = schema.getMap(i);
                    if(m.getString("data").equals("owner"))
                        owner_pos = Integer.valueOf(i);
                    else if (m.getString("data").equals("state"))
                        state_pos = Integer.valueOf(i);
                    else if (m.getString("data").equals("wlmnodestate"))
                        wlmnodestate_pos = Integer.valueOf(i);
                }

                PropertyArray data = jsonResult.getArray("data");

                for (int i = 0; i < data.size(); i++){
                    PropertyArray items = data.getArray(i);
                    if (owner_pos != null)
                        items.set(owner_pos.intValue(), owner_map.get(items.getString(owner_pos.intValue())));
                    if (state_pos != null)
                        items.set(state_pos.intValue(), state_map.get(items.getString(state_pos.intValue())));
                    if (wlmnodestate_pos != null)
                        items.set(wlmnodestate_pos.intValue(), wlmstate_map.get(items.getString(wlmnodestate_pos.intValue())));
                }

                jsonResult.put("data", data);
            }

        }
        catch(PropertyNotExpectedType e){
            return jsonResult;
        }

        return jsonResult;
    }

    private PropertyMap map_job_values(PropertyMap jsonResult)
    {
        try {
            Logger log_ = LoggerFactory.getInstance("UI", AdapterUIRest.class.getName(), "log4j2");
            Integer state_pos = null;

            PropertyArray schema = jsonResult.getArray("schema");

            if(schema != null){
                for(int i = 0; i < schema.size(); i++){
                    PropertyMap m = schema.getMap(i);
                    if (m.getString("data").equals("state"))
                        state_pos = Integer.valueOf(i);
                }

                PropertyArray data = jsonResult.getArray("data");

                for (int i = 0; i < data.size(); i++){
                    PropertyArray items = data.getArray(i);
                    if (state_pos != null)
                        items.set(state_pos.intValue(), jobstate_map.get(items.getString(state_pos.intValue())));
                }

                jsonResult.put("data", data);
            }

        }
        catch(Exception e){
            return jsonResult;
        }

        return jsonResult;
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

    private PropertyMap executeProcedure(String prepProcedure)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prepProcedure)) {

            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }

    private PropertyMap executeProcedureOneVariableFilter(String prepProcedure, Timestamp StartTime,
                                                          Timestamp EndTime, String FilterVariableOne, String Limit)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prepProcedure)) {
            stmt.setTimestamp(1, StartTime);
            stmt.setTimestamp(2, EndTime);
            stmt.setString(3, FilterVariableOne);
            handleLimit(Limit, stmt, 4);

            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }

    private PropertyMap executeProcedureTwoVariableFilter(String prepProcedure, Timestamp StartTime,
                                                          Timestamp EndTime, String FilterVariableOne,
                                                          String FilterVariableTwo ,String Limit)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prepProcedure)) {
            stmt.setTimestamp(1, StartTime);
            stmt.setTimestamp(2, EndTime);
            stmt.setString(3, FilterVariableOne);
            stmt.setString(4, FilterVariableTwo);
            handleLimit(Limit, stmt, 5);

            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }

    private PropertyMap executeProcedureThreeVariableFilter(String prepProcedure, Timestamp StartTime,
                                                            Timestamp EndTime, String FilterVariableOne,
                                                            String FilterVariableTwo, String FilterVariableThree, String Limit)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prepProcedure)) {
            stmt.setTimestamp(1, StartTime);
            stmt.setTimestamp(2, EndTime);
            stmt.setString(3, FilterVariableOne);
            stmt.setString(4, FilterVariableTwo);
            stmt.setString(5, FilterVariableThree);
            handleLimit(Limit, stmt, 6);

            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }

    private PropertyMap executeProcedureFiveVariableFilter(String prepProcedure, Timestamp StartTime,
                                                           Timestamp EndTime, String FilterVariableOne, String FilterVariableTwo, String FilterVariableThree, String  FilterVariableFour, String Limit, String FilterVariableFive)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prepProcedure)) {
            stmt.setTimestamp(1, StartTime);
            stmt.setTimestamp(2, EndTime);
            stmt.setString(3, FilterVariableOne);
            stmt.setString(4, FilterVariableTwo);
            stmt.setString(5, FilterVariableThree);
            stmt.setString(6, FilterVariableFour);
            handleLimit(Limit, stmt, 7);
            stmt.setString(8, FilterVariableFive);

            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }

    private void handleLimit(String Limit, CallableStatement stmt, int value) throws SQLException {
        if(Limit != null)
            stmt.setInt(value, Integer.parseInt(Limit));
        else
            stmt.setNull(value, Types.INTEGER);
    }

}
