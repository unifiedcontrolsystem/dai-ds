// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.dsimpl.jdbc.DbConnectionFactory;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.exceptions.ProviderException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.intel.dai.exceptions.BadInputException;

import java.sql.*;
import java.util.*;

@SuppressWarnings("serial")
public class CannedAPI {
    private Connection conn = null;
    public LocationApi locationApi_;
    JsonConverter jsonConverter = new JsonConverter();
    private static ConfigIO jsonParser = ConfigIOFactory.getInstance("json");
    private final Logger log_;

    private static final Map<String, String> owner_map = Collections.unmodifiableMap(
            new HashMap<String,String>() {{
                put("W", "WLM");
                put("S", "Service");
                put("G", "General");
                put("F", "Free Pool");
            }});

    private static final Map<String, String> state_map = Collections.unmodifiableMap(
            new HashMap<String,String>() {{
                put("B", "Bios Starting");
                put("D", "Discovered (dhcp discover)");
                put("I", "IP address assigned (dhcp request)");
                put("L", "Starting load of Boot images");
                put("K", "Kernel boot started");
                put("A", "Active");
                put("M", "Missing");
                put("E", "Error");
                put("U", "Unknown");
                put("H", "Halting/Shutting Down");
                put("R", "Bios Starting due to Reset");
                put("S", "Selecting Boot Device");
                put("P", "PXE Downloading NBP file");
            }});

    private static final Map<String, String> wlmstate_map = Collections.unmodifiableMap(
            new HashMap<String,String>() {{
                put("A", "Available");
                put("U", "Unavailable");
                put("G", "General");
                put("F", "Free Pool");
            }});

    private static final Map<String, String> jobstate_map = Collections.unmodifiableMap(
            new HashMap<String,String>() {{
                put("T", "Terminated");
                put("S", "Started");
            }});

    CannedAPI(Logger logger, LocationApi locationApi) {
        assert jsonParser != null: "Failed to get a JSON parser!";
        assert logger != null: "Passed a null logger to the ctor!";
        log_ = logger;
        locationApi_ = locationApi;
    }

    public Connection get_connection() throws DataStoreException {
        return DbConnectionFactory.createDefaultConnection();
    }

    public synchronized PropertyMap getData(String requestKey, Map<String, String> params_map)
            throws SQLException, DataStoreException, ProviderException {
        assert params_map != null : "Input parameters should be provided";
        conn = get_connection();
        try {
            Timestamp[] times = new Timestamp[2];
            times[0] = getTimestamp(getStartEndTime(params_map, "StartTime"));
            times[1] = getTimestamp(getStartEndTime(params_map, "EndTime"));
            String[] vars;
            String limit = params_map.getOrDefault("Limit", null);

            PropertyMap jsonResult;
            switch (requestKey) {
                case "getraswithfilters": {
                    vars = new String[5];
                    vars[0] = params_map.getOrDefault("Lctn", null);
                    vars[1] = params_map.getOrDefault("EventType", "%");
                    vars[2] = params_map.getOrDefault("Severity", "%");
                    vars[3] = params_map.getOrDefault("JobId", null);
                    vars[4] = params_map.getOrDefault("Exclude", "%");
                    jsonResult = executeProcedure("{call GetRasEventsWithFilters(?, ?, ?, ?, ?, ?, ?, ?)}", times, vars, limit);
                    break;
                }
                case "getenvwithfilters": {
                    vars = new String[1];
                    vars[0] = params_map.getOrDefault("Lctn", "%");
                    jsonResult = executeProcedure("{call GetAggregatedEvnDataWithFilters(?, ?, ?, ?)}", times, vars, limit);
                    break;
                }
                case "getinvspecificlctn": {
                    jsonResult = new PropertyMap();
                    vars = new String[1];
                    vars[0] = params_map.getOrDefault("Lctn", "%");
                    if (params_map.getOrDefault("subfru", null) == null) {
                        jsonResult = executeProcedure("{call GetInventoryDataForLctn(?, ?, ?, ?)}", times, vars, limit);
                        jsonResult = map_state_values(jsonResult);
                    }
                    else {
                        jsonResult.put("state", map_state_values(executeProcedure("{call GetInventoryDataForLctn(?, ?, ?, ?)}", times, vars, limit)));
                        times = new Timestamp[0];
                        vars = new String[2];
                        vars[0] = params_map.getOrDefault("Lctn", "%");
                        vars[1] = params_map.getOrDefault("subfru", null);
                        limit = "";
                        log_.info("GetSubfruState procedure called with Lctn = %s and subfru = %s", vars[0], vars[1]);
                        jsonResult.put("subfru_state", map_state_values(executeProcedure("{call GetSubfruState(?, ?)}", times, vars, limit)));
                    }
                    break;
                }
                case "getjobinfo": {
                    vars = new String[4];
                    vars[0] = params_map.getOrDefault("Jobid", "%");
                    vars[1] = params_map.getOrDefault("Username", "%");
                    vars[2] = params_map.getOrDefault("State", "%");
                    vars[3] = params_map.getOrDefault("Lctn", "%");
                    times = new Timestamp[3];
                    times[0] = getTimestamp(getStartEndTime(params_map, "StartTime"));
                    times[1] = getTimestamp(getStartEndTime(params_map, "EndTime"));
                    times[2] = getTimestamp(getStartEndTime(params_map, "AtTime"));
                    log_.info("GetJobInfo procedure called with Jobid = %s and Username = %s", vars[0], vars[1]);
                    jsonResult = executeProcedure("{call GetJobInfo(?, ?, ?, ?, ?, ?, ?, ?)}", times, vars, limit);
                    jsonResult = map_job_values(jsonResult);
                    break;
                }
                case "getreservationinfo": {
                    vars = new String[2];
                    vars[0] = params_map.getOrDefault("Name", null);
                    vars[1] = params_map.getOrDefault("Username", null);
                    log_.info("GetReservationInfo procedure called with Reservation Name = %s and Username = %s", vars[0], vars[1]);
                    jsonResult = executeProcedure("{call GetReservationInfo(?, ?, ?, ?, ?)}", times, vars, limit);
                    String lctn = params_map.getOrDefault("Lctn", null);
                    jsonResult = filterLocations(jsonResult, lctn);
                    break;
                }
                case "system_summary": {
                    jsonResult = new PropertyMap();
                    times = new Timestamp[0];
                    vars = new String[0];
                    limit = "";
                    log_.info("GetComputeNodeSummary procedure called");
                    jsonResult.put("compute", map_state_values(executeProcedure("{call GetComputeNodeSummary()}", times, vars, limit)));

                    log_.info("GetServiceNodeSummary procedure called");
                    jsonResult.put("service", map_state_values(executeProcedure("{call GetServiceNodeSummary()}", times, vars, limit)));
                    break;
                }
                case "getfrumigrationhistory": {
                    vars = new String[1];
                    vars[0] = params_map.getOrDefault("Lctn", "%");
                    jsonResult = executeProcedure("{call MigrationHistoryOfFru(?, ?, ?, ?)}", times, vars, limit);
                    break;
                }
                case "getinvchanges": {
                    vars = new String[1];
                    vars[0] = params_map.getOrDefault("Lctn", null);
                    jsonResult = executeProcedure("{call GetInventoryChange(?, ?, ?, ?)}", times, vars, limit);
                    break;
                }
                case "getinvhislctn": {
                    vars = new String[1];
                    vars[0] = params_map.getOrDefault("Lctn", "%");
                    jsonResult = executeProcedure("{call GetInventoryHistoryForLctn(?, ?, ?, ?)}", times, vars, limit);
                    jsonResult = map_state_values(jsonResult);
                    break;
                }
                case "getnodeinvinfo": {
                    vars = new String[1];
                    vars[0] = params_map.getOrDefault("Lctn", "%");
                    jsonResult = executeProcedure("{call GetInventoryInfoForLctn(?, ?)}", times, vars, limit);
                    break;
                }
                default:
                    throw new ProviderException("Invalid request, request key: '" + requestKey + "' : Not Found");
            }
            return jsonResult;
        } finally {
            conn.close();
        }
    }

    private PropertyMap map_state_values(PropertyMap jsonResult)
    {
        try {
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
            Integer state_pos = null;

            PropertyArray schema = jsonResult.getArray("schema");

            if(schema != null){
                for(int i = 0; i < schema.size(); i++){
                    PropertyMap m = schema.getMap(i);
                    if (m.getString("data").equals("state"))
                        state_pos = i;
                }

                PropertyArray data = jsonResult.getArray("data");

                for (int i = 0; i < data.size(); i++){
                    PropertyArray items = data.getArray(i);
                    if (state_pos != null)
                        items.set(state_pos, jobstate_map.get(items.getString(state_pos)));
                }

                jsonResult.put("data", data);
            }

        }
        catch(PropertyNotExpectedType e) {
            return jsonResult;
        }

        return jsonResult;
    }

private PropertyMap filterLocations(PropertyMap jsonResult, String lctn)
    {
        if (lctn != null) {
            try {
                Integer node_pos = null;

                PropertyArray schema = jsonResult.getArray("schema");

                if(schema != null){
                    for(int i = 0; i < schema.size(); i++){
                        PropertyMap m = schema.getMap(i);
                        if (m.getString("data").equals("nodes"))
                            node_pos = i;
                    }

                    PropertyArray data = jsonResult.getArray("data");
                    String nodes;
                    int filtered = 0;
                    ArrayList<Integer> removed = new ArrayList<Integer>();

                    for (int i = 0; i < data.size(); i++){
                        PropertyArray items = data.getArray(i);
                        if (node_pos != null) {
                            nodes = rangeToLocations(items.getString(node_pos));
                            try {
                                Set<String> locations = locationApi_.convertHostnamesToLocations(new HashSet<String>(Arrays.asList(nodes.split(" "))));
                                String[] location = locations.toArray(new String[0]);
                                boolean found = false;

                                for(int j=0; j < location.length; j++){
                                    found = found || lctn.indexOf(location[j]) != -1;
                                }

                                if (!found) {
                                    removed.add(i);
                                    filtered++;
                                }
                            }
                            catch (BadInputException e) {
                                log_.info("Skipping filter for nodes: " + nodes);
                            }
                        }
                    }

                    for(int i = removed.size() - 1; i >= 0; i--) {
                        data.remove(removed.get(i).intValue());
                    }

                    jsonResult.put("data", data);
                    jsonResult.put("result-data-lines",jsonResult.getInt("result-data-lines")-filtered);
                }
            }
            catch(PropertyNotExpectedType e){
                return jsonResult;
            }
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

    private PropertyMap executeProcedure(String prepProcedure, Timestamp[] times, String[] vars, String Limit) throws SQLException {

        try (CallableStatement stmt = conn.prepareCall(prepProcedure)) {
            int pos = 1;
            for(int i = 0; i < times.length; i++) {
                stmt.setTimestamp(pos, times[i]);
                pos += 1;
            }

            for(int i = 0; i < vars.length; i++) {
                stmt.setString(pos, vars[i]);
                pos += 1;
            }

            if (!"".equals(Limit)) {
                handleLimit(Limit, stmt, pos);
            }

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

    private String rangeToLocations(String range) {
        StringBuilder builder = new StringBuilder();
        StringBuilder prefix = new StringBuilder();
        StringBuilder postfix = new StringBuilder();
        boolean afterBracket = false;
        boolean afterDash = false;
        int pad= 0;
        int first = 0;
        int last = 0;
        String padded = "";

        for(int i = 0; i < range.length(); i++){
            char c = range.charAt(i);
            if(c == '[') {
                afterBracket = true;
            }
            else if(c == ']') {
                afterBracket = false;

                if (afterDash) {
                    last = Integer.parseInt(postfix.toString());
                    for(int j = first + 1; j <= last; j++) {
                        padded = String.format("%0"+pad+"d" , j);
                        builder.append(prefix.toString()+ padded + " ");
                    }
                    afterDash = false;
                    postfix = new StringBuilder();
                    prefix = new StringBuilder();
                }
                else {
                    builder.append(prefix.toString()+postfix.toString() + " ");
                    postfix = new StringBuilder();
                    prefix = new StringBuilder();
                }

            }
            else if(c == '-') {
                afterDash = true;
                builder.append(prefix.toString()+postfix.toString() + " ");
                pad = postfix.toString().length();
                first = Integer.parseInt(postfix.toString());
                postfix = new StringBuilder();
            }
            else if(c == ',') {
                if (afterBracket) {

                    if (afterDash) {
                        last = Integer.parseInt(postfix.toString());
                        for(int j = first + 1; j <= last; j++) {
                            padded = String.format("%0"+pad+"d" , j);
                            builder.append(prefix.toString()+ padded + " ");
                        }
                        afterDash = false;
                        postfix = new StringBuilder();
                    }
                    else {
                        builder.append(prefix.toString()+postfix.toString() + " ");
                        postfix = new StringBuilder();
                    }

                }
                else {
                    builder.append(prefix.toString() + " ");
                    prefix = new StringBuilder();
                }

            }
            else {
                if (afterBracket) {
                    postfix.append(c);
                }
                else {
                    prefix.append(c);
                }

            }
        }

        if(prefix.toString() != null){
            builder.append(prefix.toString() + " ");
        }

        return builder.toString();
    }
}