// Copyright (C) 2017-2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;

import java.sql.*;
import java.util.HashMap;
import com.intel.dai.dsimpl.jdbc.DbConnectionFactory;
import com.intel.config_io.*;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.properties.*;
import com.intel.dai.exceptions.DataStoreException;

public class QueryAPI {
    private Connection conn = null;
    JsonConverterGUI jsonConverter = new JsonConverterGUI();
    private static ConfigIO jsonParser = ConfigIOFactory.getInstance("json");

    QueryAPI() {
        assert jsonParser != null: "Failed to get a JSON parser!";
    }

    public Connection get_connection() throws DataStoreException {
        return DbConnectionFactory.createDefaultConnection();
    }

    public synchronized String getData(String requestKey, HashMap<String, String> params_map)
            throws SQLException, DataStoreException {
        conn = get_connection();
        try {
            Timestamp endtime = getTimestamp(getStartEndTime(params_map, "EndTime"));
            Timestamp starttime = getTimestamp(getStartEndTime(params_map, "StartTime"));
            int seq_num = Integer.parseInt(params_map.getOrDefault("SeqNum", "-1"));
            PropertyArray jsonResult;
            switch (requestKey) {
                case "filedata":
                    jsonResult = executeProcedureNoParams("{call GetManifestContent()}");
                    break;
                case "diagsact":
                    jsonResult = executeProcedureEndTime("{call DiagListOfActiveDiagsAtTime(?)}", endtime);
                    break;
                case "diagsnonact":
                    jsonResult = executeProcedureEndTime("{call DiagListOfNonActiveDiagsAtTime(?)}", endtime);
                    break;
                case "computenodestatehistory":
                    jsonResult = executeProcedureStartEndTime("{call ComputeNodeHistoryListOfStateAtTime(?, ?)}", starttime, endtime);
                    break;
                case "servicenodestatehistory":
                    jsonResult = executeProcedureStartEndTime("{call ServiceNodeHistoryListOfStateAtTime(?, ?)}", starttime, endtime);
                    break;
                case "rasevent":
                    jsonResult = executeProcedureStartEndTime("{call RasEventListAtTime(?, ?)}", starttime, endtime);
                    break;
                case "aggenv":
                    jsonResult = executeProcedureStartEndTime("{call AggregatedEnvDataListAtTime(?, ?)}", starttime, endtime);
                    break;
                case "jobsact":
                    jsonResult = executeProcedureEndTime("{call JobHistoryListOfActiveJobsatTime(?)}", endtime);
                    break;
                case "jobsnonact":
                    jsonResult = executeProcedureStartEndTime("{call JobHistoryListOfNonActiveJobsAtTime(?, ?)}", starttime, endtime);
                    break;
                case "changets":
                    jsonResult = executeProcedureNoParams("{call DbChgTimestamps()}");
                    break;
                case "serviceinv":
                    jsonResult = executeProcedureStartEndTime("{call ServiceNodeInventoryList(?, ?)}", starttime, endtime);
                    break;
                case "computeinv":
                    jsonResult = executeProcedureStartEndTime("{call ComputeNodeInventoryList(?, ?)}", starttime, endtime);
                    break;
                case "computehistoldestts":
                    jsonResult = executeProcedureNoParams("{call ComputeNodeHistoryOldestTimestamp()}");
                    break;
                case "inventoryss":
                    jsonResult = executeProcedureStartEndTime("{call InventorySnapshotList(?, ?)}", starttime, endtime);
                    break;
                case "inventoryinfo":
                    jsonResult = executeProcedureStartEndTime("{call InventoryInfoList(?, ?)}", starttime, endtime);
                    break;
                case "replacementhistory":
                    jsonResult = executeProcedureStartEndTime("{call ReplacementHistoryList(?, ?)}", starttime, endtime);
                    break;
                case "reservationlist":
                    jsonResult = executeProcedureStartEndTime("{call ReservationListAtTime(?, ?)}", starttime, endtime);
                    break;
                case "serviceadapterdata":
                    jsonResult = executeProcedureEndTime("{call ServiceOperationAtTime(?)}", endtime);
                    break;
                default:
                    return "Invalid request, request key: '" + requestKey + "' : Not Found";
            }
            return jsonParser.toString(jsonResult);
        } finally {
            conn.close();
        }
    }

    private String getStartEndTime(HashMap <String, String> params_map, String key)
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

    private PropertyArray executeProcedureStartEndTime (String prep_procedure, Timestamp StartTime, Timestamp EndTime)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prep_procedure)) {
            stmt.setTimestamp(1, StartTime);
            stmt.setTimestamp(2, EndTime);
            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }

    private PropertyArray executeProcedureNoParams (String prep_procedure)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prep_procedure)) {
            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }
    private synchronized PropertyArray executeProcedureOneParam (String prep_procedure, Integer num_inp)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prep_procedure)) {
            stmt.setInt(1, num_inp);
            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }

    private PropertyArray executeProcedureEndTime (String prep_procedure, Timestamp EndTime)
            throws SQLException
    {
        try (CallableStatement stmt = conn.prepareCall(prep_procedure)) {
            stmt.setTimestamp(1, EndTime);
            try (ResultSet rs = stmt.executeQuery()) {
                return jsonConverter.convertToJsonResultSet(rs);
            }
        }
    }
}
