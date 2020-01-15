// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;
import com.intel.dai.dsimpl.jdbc.DbConnectionFactory;
import com.intel.dai.exceptions.ProviderException;
import com.intel.logging.LoggerFactory;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.ExcludeCategories;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CannedApiTest {
    HashMap<String,String> input_map = new HashMap<>();
    CallableStatement mockstmt = mock(CallableStatement.class);
    ResultSet mockrs = mock(ResultSet.class);
    ResultSetMetaData mockmd = mock(ResultSetMetaData.class);
    Connection mockconn = mock(Connection.class);

    class MockCannedApi extends CannedAPI {
        MockCannedApi() {
            jsonConverter = new MockJsonConverter();
        }


        @Override
        public Connection get_connection() {
            return mockconn;
        }
    }

    class MockJsonConverter extends JsonConverter {
        MockJsonConverter(){

        }

        @Override
        public PropertyMap convertToJsonResultSet(ResultSet resultsetinp) throws SQLException{
            PropertyMap resultJson = new PropertyMap();
            resultJson.put("TEST", "CANNED API TESTING");
            return resultJson;
        }
    }

    @Before
    public void setUp() throws Exception {
        input_map.put("StartTime", "2017-10-01 10:00:00.310");
        input_map.put("EndTime","2017-10-01 10:00:00.310");
    }

    @Test
    public void rasfilters() throws Exception {
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("getraswithfilters", input_map);
        assertNotNull(result);
    }

    @Test
    public void envfilters() throws Exception {
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("getenvwithfilters", input_map);
        assertNotNull(result);
    }

    @Test
    public void invspecific() throws Exception {
        input_map.put("StartTime", "null");
        input_map.put("EndTime","null");
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("getinvspecificlctn", input_map);
        assertNotNull(result);
    }

    @Test(expected = ProviderException.class)
    public void defaultoption() throws Exception {
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("default", input_map);
    }

    @Test
    public void getJobInformation() throws Exception {
        String jobid = "testjobid";
        input_map.put("Jobid", jobid);
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("getjobinfo", input_map);
        assertNotNull(result);
    }

    @Test
    public void getReservationInformation() throws Exception {
        String reservation = "testname";
        input_map.put("Name", reservation);
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("getreservationinfo", input_map);
        assertNotNull(result);
    }

    @Test
    public void getSystemSummary() throws Exception {
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("system_summary", input_map);
        assertNotNull(result);
    }

}
