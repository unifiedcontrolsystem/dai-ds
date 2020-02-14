package com.intel.dai.foreign_bus;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.network_listener.NetworkListenerProviderException;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.*;

public class CommonFunctionsTest {
    @BeforeClass
    public static void setUpClass() {
        CommonFunctions.loadCall_ = CommonFunctionsTest::testLoadCall;
    }

    @AfterClass
    public static void tearDownClass() {
        CommonFunctions.loadCall_ = CommonFunctions::loadConversionsMaps;
    }

    @Before
    public void setUp() {
        CommonFunctions.clearMaps();
    }

    private static void testLoadCall() {
        CommonFunctions.loadConversionsMaps();
        InputStream stream = CommonFunctions.class.getResourceAsStream("/resources/LocationTranslationMap.json");
        if(stream == null)
            throw new RuntimeException("Failed to load the location name transformer resource " +
                    "'/resources/LocationTranslationMap.json'");
        try {
            ConfigIO parser = ConfigIOFactory.getInstance("json");
            assert parser != null: "Failed to get the JSON parser!";
            PropertyMap root = parser.readConfig(stream).getAsMap();
            CommonFunctions.nodeMap_ = root.getMap("conversion_node_map");
            CommonFunctions.sensorCpuPattern_ = root.getString("sensor_embedded_cpu_pattern");
            CommonFunctions.sensorDimmPattern_ = root.getString("sensor_embedded_dimm_pattern");
            CommonFunctions.sensorChannelPattern_ = root.getString("sensor_embedded_channel_pattern");
            CommonFunctions.reverseNodeMap_ = new PropertyMap();
            for(String key: CommonFunctions.nodeMap_.keySet())
                CommonFunctions.reverseNodeMap_.put(CommonFunctions.nodeMap_.getString(key), key);
        } catch(IOException | ConfigIOParseException | PropertyNotExpectedType e) {
            throw new RuntimeException("Something went wrong reading or parsing the location transformer resource " +
                    "'/resources/LocationTranslationMap.json'", e);
        } finally {
            try { stream.close(); } catch(IOException ec) { /* Nothing to do if close fails. */ }
        }
    }

    @Test
    public void positiveConvertXNameLocationTests() throws Exception {
        assertEquals("R0-SMS", CommonFunctions.convertXNameToLocation("sms"));
        assertEquals("R0", CommonFunctions.convertXNameToLocation("x0"));
        assertEquals("R0-CH0", CommonFunctions.convertXNameToLocation("x0c0"));
        assertEquals("R0-CH0-CN3", CommonFunctions.convertXNameToLocation("x0c0s3b0n0"));
        assertEquals("R0-BC_C_I_YY", CommonFunctions.convertXNameToLocation("x0", "BC_C_I_YY"));
        assertEquals("R0-CH0-CN3-BC_I_NODE4_YY-EXTRA", CommonFunctions.convertXNameToLocation("x0c0s3b0n0",
                "BC_I_NODE4_YY", "EXTRA"));
        assertEquals("R0-CH0-CN2-CPU2-BC_I_NODE2_CPU2_DIMM3_YY",
                CommonFunctions.convertXNameToLocation("x0c0s2b0n0", "BC_I_NODE2_CPU2_DIMM3_YY"));
        assertEquals("R0-CH0-CN1-CPU2-CH1-DIMM3-BC_I_NODE1_CPU2_CH1_DIMM3_YY",
                CommonFunctions.convertXNameToLocation("x0c0s1b0n0", "BC_I_NODE1_CPU2_CH1_DIMM3_YY"));
        assertEquals("R0-CH0-CN3-BC_I_NODE3_YY-EXTRA", CommonFunctions.convertXNameToLocation("x0c0s3b0n0",
                "BC_I_NODE3_YY", "EXTRA"));
    }

    @Test(expected = NetworkListenerProviderException.class)
    public void negativeconvertXNameLocationTest1() throws Exception {
        CommonFunctions.convertXNameToLocation("x0c1s0n0");
    }

    @Test
    public void positiveConvertLocationToXNameTests() throws Exception {
        assertEquals("all", CommonFunctions.convertLocationToXName("all"));
        assertEquals("sms", CommonFunctions.convertLocationToXName("R0-SMS"));
        assertEquals("x0", CommonFunctions.convertLocationToXName("R0"));
        assertEquals("x0c0", CommonFunctions.convertLocationToXName("R0-CH0"));
        assertEquals("x0c0s2b0n0", CommonFunctions.convertLocationToXName("R0-CH0-CN2"));
        assertEquals("x0c0s1b0n0", CommonFunctions.convertLocationToXName("R0-CH0-CN1"));
        assertEquals("x0c0s3b0n0", CommonFunctions.convertLocationToXName("R0-CH0-CN3-BC_I_NODE3_YY-EXTRA"));
    }

    @Test(expected = NetworkListenerProviderException.class)
    public void negativeConvertLocationToXNameTests() throws Exception {
        CommonFunctions.convertLocationToXName("R3-CH0-CN0");
    }

    @Test
    public void testAllXnames() {
       assertTrue(CommonFunctions.getLocations().containsAll(CommonFunctions.nodeMap_ .values()));
    }

    @Test
    public void testAllLocations() {
        assertTrue(CommonFunctions.getXNames().containsAll(CommonFunctions.nodeMap_ .keySet()));
    }
}
