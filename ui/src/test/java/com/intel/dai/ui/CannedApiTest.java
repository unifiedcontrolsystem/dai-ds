// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.sql.*;
import java.util.HashMap;

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
            super(mock(Logger.class));
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
    public void rasjobid() throws Exception {
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("getraswithjobid", input_map);
        assertNotNull(result);
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
    public void invchanges() throws Exception {
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("getinvchanges", input_map);
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

    @Test
    public void snapshotspecificlctn() throws Exception {
        input_map.put("StartTime", "null");
        input_map.put("EndTime","null");
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("getsnapshotspecificlctn", input_map);
        assertNotNull(result);
    }

    @Test
    public void getrefsnapshotspecificlctn() throws Exception {
        input_map.put("lctn", "null");
        input_map.put("limit","null");
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("getrefsnapshot", input_map);
        assertNotNull(result);
    }

    @Test
    public void getjobdata() throws Exception {
        HashMap<String, String> inputMap= new HashMap<>();
        String location = "testLocation";
        String jobId = "22";
        inputMap.put("Lctn", location);
        inputMap.put("JobId", jobId);
        inputMap.put("StartTime", "null");
        inputMap.put("EndTime","null");
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("getjobdata", inputMap);
        Mockito.verify(mockconn, Mockito.times(1))
                .prepareCall(Mockito.contains("call GetJobPowerData"));
        // Verify the right parameters are passed
        Mockito.verify(mockstmt, Mockito.times(1)).setString(Mockito.anyInt(),
                Mockito.eq(location));
        Mockito.verify(mockstmt, Mockito.times(1)).setString(Mockito.anyInt(),
                Mockito.eq(jobId));
        assertNotNull(result);
    }

    @Test
    public void defaultoption() throws Exception {
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        String result = canned.getData("default", input_map);
        assertNotNull(result);
    }
}
