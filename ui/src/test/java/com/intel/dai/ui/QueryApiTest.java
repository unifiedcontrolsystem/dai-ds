// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;

import com.intel.dai.dsimpl.jdbc.DbConnectionFactory;
import com.intel.logging.LoggerFactory;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.sql.*;
import java.util.HashMap;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class QueryApiTest {
    HashMap<String,String> input_map = new HashMap<>();
    CallableStatement mockstmt = mock(CallableStatement.class);
    ResultSet mockrs = mock(ResultSet.class);
    Connection mockconn = mock(Connection.class);

    class MockQueryApi extends QueryAPI {
        MockQueryApi() {
            jsonConverter = new MockJsonConverter();
        }


        @Override
        public Connection get_connection() {
            return mockconn;
        }
    }

    class MockJsonConverter extends JsonConverterGUI {
        MockJsonConverter(){

        }

        @Override
        public PropertyArray convertToJsonResultSet(ResultSet resultsetinp) throws SQLException{
            PropertyArray resultJson = new PropertyArray();
            resultJson.add("QUERY API TESTING");
            return resultJson;
        }
    }

    @Before
    public void setUp() throws Exception {
        input_map.put("StartTime", "2017-10-01 10:00:00.310");
        input_map.put("EndTime","2017-10-01 10:00:00.310");
    }

    @Test
    public void filedata() throws Exception {
        MockQueryApi canned = new MockQueryApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("filedata", input_map);
        assertNotNull(result);
    }

    @Test
    public void diagsdata() throws Exception {
        MockQueryApi canned = new MockQueryApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("diagsact", input_map);
        assertNotNull(result);
        result = canned.getData("diagsnact", input_map);
        assertNotNull(result);
        result = canned.getData("jobsact", input_map);
        assertNotNull(result);
        result = canned.getData("jobsnonact", input_map);
        assertNotNull(result);
        result = canned.getData("computeinv", input_map);
        assertNotNull(result);
    }

    @Test
    public void envdata() throws Exception {
        input_map.put("StartTime", "null");
        input_map.put("EndTime","null");
        MockQueryApi canned = new MockQueryApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("nodestatehistory", input_map);
        assertNotNull(result);
        result = canned.getData("rasevent", input_map);
        assertNotNull(result);
        result = canned.getData("aggenv", input_map);
        assertNotNull(result);
        result = canned.getData("changets", input_map);
        assertNotNull(result);
        result = canned.getData("serviceinv", input_map);
        assertNotNull(result);
        result = canned.getData("computehistoldestts", input_map);
        assertNotNull(result);
        result = canned.getData("inventoryss", input_map);
        assertNotNull(result);
        result = canned.getData("reservationlist", input_map);
        assertNotNull(result);
    }

    @Test
    public void defaultoption() throws Exception {
        MockQueryApi canned = new MockQueryApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("default", input_map);
        assertNotNull(result);
    }
}
