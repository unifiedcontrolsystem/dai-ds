// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import static org.junit.Assert.*;

public class SensorMetaDataForeignBusTest {

    @Before
    public void setUp() throws Exception {
        ConfigIO parser = ConfigIOFactory.getInstance("json");
        if(parser == null) throw new RuntimeException("No parser");
        metaData_ = new SensorMetaDataForeignBus(parser);
        try (InputStream stream = new ByteArrayInputStream(json_.getBytes())) {
            assertTrue(metaData_.loadFromStream(stream));
        }
    }

    @Test
    public void loadFRomStreamNegative() {
        assertFalse(metaData_.loadFromStream(null));
    }

    @Test
    public void getUnits() {
        assertEquals("degC", metaData_.getUnits("991"));
    }

    @Test
    public void checkSensor() {
        assertTrue(metaData_.checkSensor("999"));
    }

    @Test
    public void normalizeValue() {
        assertEquals(12.5, metaData_.normalizeValue("1001", 12500.0), 0.001);
        assertEquals(12.5, metaData_.normalizeValue("1000", 12500.0), 0.001);
        assertEquals(30.0, metaData_.normalizeValue("999", 30.0), 0.001);
        assertEquals(30.0, metaData_.normalizeValue("998", 0.03), 0.001);
    }

    @Test
    public void getTelemetryType() {
        assertEquals("Temp", metaData_.getTelemetryType("991"));
    }

    @Test
    public void getDescription() {
        assertEquals("CC_T_MCU_TEMP", metaData_.getDescription("991"));
    }

    @Test
    public void normalizeLocation() {
        assertEquals("R0-CH02-N3-FAN1", metaData_.normalizeLocation("994", "R0-CH02-N3"));
        assertEquals("R0-CH02-N3", metaData_.normalizeLocation("993", "R0-CH02-N3"));
    }

    SensorMetaDataForeignBus metaData_;

    static final String json_ = "{\n" +
            "    \"988\": {\"description\": \"CC_F_RECT_FAN_SETPOINT_PEAK\", \"unit\": \"%\", \"type\": \"unknown\" },\n" +
            "    \"989\": {\"description\": \"CC_H_CAB_HEALTH_MAIN\", \"unit\": \"status\", \"type\": \"unknown\" },\n" +
            "    \"990\": {\"description\": \"CC_H_CAB_HEALTH_MAIN_LATCHED\", \"unit\": \"status\", \"type\": \"unknown\" },\n" +
            "    \"991\": {\"description\": \"CC_T_MCU_TEMP\", \"unit\": \"degC\", \"type\": \"Temp\" },\n" +
            "    \"992\": {\"description\": \"CC_T_PCB_TEMP\", \"unit\": \"degC\", \"type\": \"Temp\" },\n" +
            "    \"993\": {\"description\": \"CC_V_VCC_5_0V\", \"unit\": \"V\", \"type\": \"VoltageIn\", \"specific\": \" \" },\n" +
            "    \"994\": {\"description\": \"CC_V_VCC_5_0V_FAN1\", \"unit\": \"V\", \"type\": \"VoltageIn\", \"specific\": \"FAN1\" },\n" +
            "    \"995\": {\"description\": \"CC_V_VCC_5_0V_SPI\", \"unit\": \"V\", \"type\": \"VoltageIn\" },\n" +
            "    \"996\": {\"description\": \"CC_V_VDD_0_9V\", \"unit\": \"V\", \"type\": \"VoltageIn\" },\n" +
            "    \"997\": {\"description\": \"CC_V_VDD_1_0V_OR_1_3V\", \"unit\": \"V\", \"type\": \"VoltageIn\" },\n" +
            "    \"998\": {\"description\": \"CC_V_VDD_1_2V\", \"unit\": \"kV\", \"type\": \"VoltageIn\", \"factor\": 1000.0 },\n" +
            "    \"999\": {\"description\": \"CC_V_VDD_1_2V_GTP\", \"unit\": \"V\", \"type\": \"VoltageIn\", \"factor\": true },\n" +
            "    \"1000\": {\"description\": \"CC_V_VDD_1_8V\", \"unit\": \"mV\", \"type\": \"VoltageIn\", \"factor\": \"0.001\" },\n" +
            "    \"1001\": {\"description\": \"CC_V_VDD_2_5V\", \"unit\": \"mV\", \"type\": \"VoltageIn\", \"factor\": 0.001 }\n" +
            "}\n";
}
