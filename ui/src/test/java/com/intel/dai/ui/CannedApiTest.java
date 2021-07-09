// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;
import com.intel.dai.exceptions.ProviderException;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;

import java.sql.*;
import java.util.HashMap;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class CannedApiTest {
    HashMap<String,String> input_map = new HashMap<>();
    CallableStatement mockstmt = mock(CallableStatement.class);
    ResultSet mockrs = mock(ResultSet.class);
    Connection mockconn = mock(Connection.class);

    class MockCannedApi extends CannedAPI {
        MockCannedApi() {
            super(mock(Logger.class), mock(LocationApi.class));
            jsonConverter = new MockJsonConverter();
        }


        @Override
        public Connection get_connection() {
            return mockconn;
        }
    }

    static class MockJsonConverter extends JsonConverter {
        MockJsonConverter(){

        }

        @Override
        public PropertyMap convertToJsonResultSet(ResultSet resultsetinp) {
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
        PropertyMap result = canned.getData("getraswithfilters", input_map);
        assertNotNull(result);
    }

    @Test
    public void envfilters() throws Exception {
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        PropertyMap result = canned.getData("getenvwithfilters", input_map);
        assertNotNull(result);
    }

    @Test
    public void invspecific() throws Exception {
        input_map.put("StartTime", "null");
        input_map.put("EndTime","null");
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        PropertyMap result = canned.getData("getinvspecificlctn", input_map);
        assertNotNull(result);
    }

    @Test
    public void invspecificSubfru() throws Exception {
        input_map.put("StartTime", "null");
        input_map.put("EndTime","null");
        input_map.put("subfru","all");
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        PropertyMap result = canned.getData("getinvspecificlctn", input_map);
        assertNotNull(result);
    }

    @Test(expected = ProviderException.class)
    public void defaultoption() throws Exception {
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        PropertyMap result = canned.getData("default", input_map);
    }

    @Test
    public void getJobInformation() throws Exception {
        String jobid = "testjobid";
        input_map.put("Jobid", jobid);
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        PropertyMap result = canned.getData("getjobinfo", input_map);
        assertNotNull(result);
    }

    @Test
    public void getReservationInformation() throws Exception {
        String reservation = "testname";
        input_map.put("Name", reservation);
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        PropertyMap result = canned.getData("getreservationinfo", input_map);
        assertNotNull(result);
    }

    @Test
    public void getSystemSummary() throws Exception {
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        PropertyMap result = canned.getData("system_summary", input_map);
        assertNotNull(result);
    }

    @Test
    public void getReplacementHistory() throws Exception {
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        PropertyMap result = canned.getData("getinvchanges", input_map);
        assertNotNull(result);
    }

    @Test
    public void getInventoryInfoForLctn() throws Exception {
        MockCannedApi canned = new MockCannedApi();
        when(mockstmt.executeQuery()).thenReturn(mockrs);
        when(mockconn.prepareCall(ArgumentMatchers.anyString())).thenReturn(mockstmt);
        PropertyMap result = canned.getData("getnodeinvinfo", input_map);
        assertNotNull(result);
    }
}