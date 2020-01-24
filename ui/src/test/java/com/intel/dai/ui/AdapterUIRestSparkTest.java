// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.ui;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.despegar.http.client.*;
import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.dai.dsapi.Location;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import org.junit.*;

import com.despegar.sparkjava.test.SparkServer;

import spark.servlet.SparkApplication;


public class AdapterUIRestSparkTest {

    static AdapterUIRestMock obj;

    public static class TestContollerTestSparkApplication implements SparkApplication {
        @Override
        public void init() {
            try {
                obj = new AdapterUIRestMock(new String[] {"testhost"});
                obj.initialize("test", "test", new String[] {"testhost"});
                AdapterUIRest.execute_routes(obj);
            } catch(IOException e) {

            }
        }
    }

    @ClassRule
    public static SparkServer<TestContollerTestSparkApplication> testServer =
            new SparkServer<>(AdapterUIRestSparkTest.TestContollerTestSparkApplication.class, 1767);

    @BeforeClass
    public static void setUpClass() { // This is for developers and should have no affect in automation.
        File local = Paths.get(System.getProperty("user.home"), ".config/ucs",
                "RasEventMetaData.json").toFile();
        File saved = new File(local.toString() + ".TESTING");
        if(local.exists()) local.renameTo(saved);
        try {
            try (FileWriter writer = new FileWriter("/tmp/" + "RasEventMetaData.json")) {
                writer.write("{ \"Events\" : [\n" +
                        "\t{\n" +
                        "\t\t\"EventType\":\"0000000000\",\n" +
                        "\t\t\"Severity\":\"ERROR\",\n" +
                        "\t\t\"Category\":\"Adapter\",\n" +
                        "\t\t\"Component\":\"AdapterGeneric\",\n" +
                        "\t\t\"ControlOperation\":null,\n" +
                        "\t\t\"Msg\":\"An unknown system event occurred, please review the instance data:\"\n" +
                        "\t}]}");
            }
        } catch(IOException ie) {

        }
    }

    @AfterClass
    public static void tearDownClass() { // This is for developers and should have no affect in automation.
        File local = Paths.get(System.getProperty("user.home"), ".config/ucs",
                "RasEventMetaData.json").toFile();
        File saved = new File(local.toString() + ".TESTING");
        if(saved.exists() && !local.exists()) saved.renameTo(local);
    }

    @Before
    public void setup() {
        ConfigIO jsonParser_ = ConfigIOFactory.getInstance("json");
        obj.setParser(jsonParser_);
    }

    @Test
    public void testLambdaGui() throws Exception {
        GetMethod get = testServer.get("/", false);
        HttpResponse httpResponse = testServer.execute(get);
    }

    @Test
    public void testLambdaGuivolt() throws Exception {
        GetMethod get = testServer.get("/v", false);
        HttpResponse httpResponse = testServer.execute(get);
    }

    @Test
    public void testLambdaQuery() throws Exception {
        GetMethod get = testServer.get("/query/test?limit=1", false);
        HttpResponse httpResponse = testServer.execute(get);
        assertEquals(200, httpResponse.code());
    }

    @Test
    public void testLambdaGroupsList() throws Exception {
        GetMethod get = testServer.get("/groups", false);
        HttpResponse httpResponse = testServer.execute(get);
        assertEquals(200, httpResponse.code());
    }

    @Test
    public void testLambdaGroupsGet() throws Exception {
        GetMethod get = testServer.get("/groups/g01?devices=node00", false);
        HttpResponse httpResponse = testServer.execute(get);
        assertEquals(200, httpResponse.code());
    }

    @Test
    public void testLambdaGroupsDeleteDevices() throws Exception {
        when(obj.groupsMgr.deleteDevicesFromGroup(any(), any())).thenReturn("success");
        DeleteMethod delete = testServer.delete("/groups/g01?devices=node00", false);
        HttpResponse httpResponse = testServer.execute(delete);
        assertEquals(200, httpResponse.code());

    }

    @Test
    public void testLambdaGroupsPutDevices() throws Exception {
        when(obj.groupsMgr.addDevicesToGroup(any(), any())).thenReturn("success");
        PutMethod put = testServer.put("/groups/g01", "{'devices':node00}", false);
        HttpResponse httpResponse = testServer.execute(put);
    }

    @Test
    public void testLambdaGroupsPostDevices() throws Exception {
        PostMethod post = testServer.post("/groups/g01?devices=node00", "",false);
        HttpResponse httpResponse = testServer.execute(post);
        assertEquals(200, httpResponse.code());
    }

    @Test
    public void testLambdaSystem() throws Exception {
        PropertyArray nodes = new PropertyArray();
        nodes.add("node00");
        when(obj.configMgr.getComputeNodeConfiguration()).thenReturn(nodes);
        when(obj.configMgr.getServiceNodeConfiguration()).thenReturn(nodes);
        GetMethod get = testServer.get("/system", false);
        HttpResponse httpResponse = testServer.execute(get);
        assertEquals(200, httpResponse.code());
    }

    @Test
    public void testLambdaCliCannedCommands() throws Exception {
        GetMethod get  = testServer.get("/cli/test_cmd", false);
        HttpResponse httpResponse = testServer.execute(get);
    }

    @Test
    public void testLocationsApi() throws Exception {
        PropertyMap locations = new PropertyMap();
        locations.put("device", "node00");
        obj.locationMgr = mock(Location.class);
        when(obj.locationMgr.getSystemLocations()).thenReturn(locations);
        GetMethod get = testServer.get("/locations", false);
        HttpResponse httpResponse = testServer.execute(get);
        assertEquals(200, httpResponse.code());
    }

    @Test
    public void testLocations() throws Exception {
        GetMethod get = testServer.get("/locations", false);
        HttpResponse httpResponse = testServer.execute(get);
    }


}
