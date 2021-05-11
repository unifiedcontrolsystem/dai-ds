// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.AdapterSingletonFactory;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EventSimEnvironmentalProviderForeignBusTest {
    static class MockEventSimEnvironmentalProviderForeignBus extends EventSimEnvironmentalProviderForeignBus {
        MockEventSimEnvironmentalProviderForeignBus(Logger logger) { super(logger); initialize(); }

        @Override
        protected InputStream getMetaDataStream() {
            if(!returnNull_)
                return new ByteArrayInputStream(jsonMetaData_.getBytes());
            else
                return null;
        }

        private static boolean returnNull_;
    }

    @BeforeClass
    public static void setUpClass() {
        AdapterSingletonFactory.initializeFactory("TEST_TYPE", "TEST_NAME", mock(Logger.class));
    }

    @Before
    public void setUp() {
        MockEventSimEnvironmentalProviderForeignBus.returnNull_ = false;
        EventSimEnvironmentalProviderForeignBus.Accumulator.useTime_ = false;
        EventSimEnvironmentalProviderForeignBus.Accumulator.count_ = 3;
        EventSimEnvironmentalProviderForeignBus.Accumulator.ns_ = 10L;
        EventSimEnvironmentalProviderForeignBus.Accumulator.moving_ = false;
        transformer_ = new MockEventSimEnvironmentalProviderForeignBus(mock(Logger.class));
        config_ = mock(NetworkListenerConfig.class);
        classConfig_.clear();
        classConfig_.put("useTimeWindow", false);
        classConfig_.put("windowSize", 25);
        classConfig_.put("useMovingAverage", false);
        classConfig_.put("timeWindowSeconds", 600);
        classConfig_.put("useAnalytics", true);
        when(config_.getProviderConfigurationFromClassName(anyString())).thenReturn(classConfig_);

        providerConfig_ = new PropertyMap();
        providerConfig_.put("publish", true);
        actionConfig_ = mock(NetworkListenerConfig.class);
        when(actionConfig_.getProviderConfigurationFromClassName(anyString())).
                thenReturn(providerConfig_);
    }

    @Test
    public void accumulatorCount() {
        EventSimEnvironmentalProviderForeignBus.Accumulator accum = new EventSimEnvironmentalProviderForeignBus.Accumulator(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(1000L, "Location", DataType.EnvironmentalData);
        data.setValueAndUnits(1.0, "W", "Power");
        accum.addValue(data);
        data.setValueAndUnits(2.0, "W", "Power");
        accum.addValue(data);
        data.setValueAndUnits(3.0, "W", "Power");
        accum.addValue(data);

        assertEquals(2.0, data.getAverage(), 0.0001);
        assertEquals(1.0, data.getMinimum(), 0.0001);
        assertEquals(3.0, data.getMaximum(), 0.0001);
    }

    @Test
    public void accumulatorMoving() {
        EventSimEnvironmentalProviderForeignBus.Accumulator.moving_ = true;
        EventSimEnvironmentalProviderForeignBus.Accumulator accum = new EventSimEnvironmentalProviderForeignBus.Accumulator(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(1000L, "Location", DataType.EnvironmentalData);
        data.setValueAndUnits(1.0, "W", "Power");
        accum.addValue(data);
        data.setValueAndUnits(2.0, "W", "Power");
        accum.addValue(data);
        data.setValueAndUnits(3.0, "W", "Power");
        accum.addValue(data);
        data.setValueAndUnits(4.0, "W", "Power");
        accum.addValue(data);

        assertEquals(3.0, data.getAverage(), 0.0001);
        assertEquals(2.0, data.getMinimum(), 0.0001);
        assertEquals(4.0, data.getMaximum(), 0.0001);
    }

    @Test
    public void accumulatorTime() {
        EventSimEnvironmentalProviderForeignBus.Accumulator.useTime_ = true;
        EventSimEnvironmentalProviderForeignBus.Accumulator accum = new EventSimEnvironmentalProviderForeignBus.Accumulator(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(1000L, "Location", DataType.EnvironmentalData);
        data.setValueAndUnits(1.0, "W", "Power");
        accum.addValue(data);
        data = new CommonDataFormat(1006L, "Location", DataType.EnvironmentalData);
        data.setValueAndUnits(2.0, "W", "Power");
        accum.addValue(data);
        data = new CommonDataFormat(1012L, "Location", DataType.EnvironmentalData);
        data.setValueAndUnits(3.0, "W", "Power");
        accum.addValue(data);

        assertEquals(2.0, data.getAverage(), 0.0001);
        assertEquals(1.0, data.getMinimum(), 0.0001);
        assertEquals(3.0, data.getMaximum(), 0.0001);
    }

    @Test
    public void processRawStringData() throws Exception {
        List<CommonDataFormat> dataList = transformer_.processRawStringData("topic", sample1, config_);
        CommonDataFormat data = dataList.get(0);
        assertEquals(2.0, data.getValue(), 0.0001);
        dataList = transformer_.processRawStringData("topic", sample1, config_);
        data =  dataList.get(0);
        assertEquals(2.0, data.getValue(), 0.0001);
        transformer_.configDone_ = false;
        classConfig_.put("useAnalytics", false);
        dataList = transformer_.processRawStringData("topic", sample2, config_);
        data =  dataList.get(0);
        assertEquals(2.1, data.getValue(), 0.0001);
        transformer_.configDone_ = false;
        when(config_.getProviderConfigurationFromClassName(anyString())).thenReturn(null);
        dataList = transformer_.processRawStringData("topic", sample3, config_);
        data = dataList.get(0);
        assertEquals(1.9, data.getValue(), 0.0001);
    }

    @Test(expected = RuntimeException.class)
    public void ctorNegative() {
        MockEventSimEnvironmentalProviderForeignBus.returnNull_ = true;
        new MockEventSimEnvironmentalProviderForeignBus(mock(Logger.class));
    }

    @Test(expected = NetworkListenerProviderException.class)
    public void processRawStringDataMissingSensor() throws Exception {
        transformer_.processRawStringData("topic", bad1, config_);
    }

    @Test
    public void processRawStringDataBadSensor() throws Exception {
        List<CommonDataFormat> result = transformer_.processRawStringData("topic", bad2, config_);
        assertEquals(0, result.size());
    }

    @Test(expected = NetworkListenerProviderException.class)
    public void processRawStringDataBadJson() throws Exception {
        transformer_.processRawStringData("topic", bad3, config_);
    }

    @Test(expected = NetworkListenerProviderException.class)
    public void processRawStringDataBadValue() throws Exception {
        transformer_.processRawStringData("topic", bad4, config_);
    }

    @Test
    public void actOnData1() {
        EventSimEnvironmentalProviderForeignBus actions = new EventSimEnvironmentalProviderForeignBus(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(9999999L, "here", DataType.EnvironmentalData);
        data.setValueAndUnits(100.0, "degC", "Temp");
        actions.actOnData(data, actionConfig_, mock(SystemActions.class));
        data.setMinMaxAvg(95.0, 105.0, 100.0);
        actions.actOnData(data, actionConfig_, mock(SystemActions.class));
    }

    @Test
    public void actOnData2() {
        providerConfig_.put("publish", false);
        EventSimEnvironmentalProviderForeignBus actions = new EventSimEnvironmentalProviderForeignBus(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(9999999L, "here", DataType.EnvironmentalData);
        data.setValueAndUnits(100.0, "degC", "Temp");
        actions.actOnData(data, actionConfig_, mock(SystemActions.class));
        data.setMinMaxAvg(95.0, 105.0, 100.0);
        actions.actOnData(data, actionConfig_, mock(SystemActions.class));
    }

    @Test
    public void actOnData3() {
        when(actionConfig_.getProviderConfigurationFromClassName(anyString())).
                thenReturn(null);
        EventSimEnvironmentalProviderForeignBus actions = new EventSimEnvironmentalProviderForeignBus(mock(Logger.class));
        CommonDataFormat data = new CommonDataFormat(9999999L, "here", DataType.EnvironmentalData);
        data.setValueAndUnits(100.0, "degC", "Temp");
        actions.actOnData(data, actionConfig_, mock(SystemActions.class));
        data.setMinMaxAvg(95.0, 105.0, 100.0);
        actions.actOnData(data, actionConfig_, mock(SystemActions.class));
    }

    private EventSimEnvironmentalProviderForeignBus transformer_;
    private NetworkListenerConfig config_;
    private PropertyMap classConfig_ = new PropertyMap();
    private NetworkListenerConfig actionConfig_;
    private PropertyMap providerConfig_;
    private static final String sample1 = "{\"sensor\":\"1001\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\",\"value\":2000.0}";
    private static final String sample2 = "{\"sensor\":\"1001\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:56:00.0000Z\",\"value\":\"2100.0\"}";
    private static final String sample3 = "{\"sensor\":\"1001\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:57:00.0000Z\",\"value\":1900.0}";
    private static final String bad1 = "{\"location\":\"all\",\"timestamp\":\"2019-05-28 15:57:00.0000Z\"," +
            "\"value\":1900.0}";
    private static final String bad2 = "{\"sensor\":\"2001\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:57:00.0000Z\",\"value\":1900.0}";
    private static final String bad3 = "{{\"sensor\":\"1001\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:57:00.0000Z\",\"value\":1900.0}";
    private static final String bad4 = "{\"sensor\":\"1001\",\"location\":\"all\",\"timestamp\":" +
            "\"2019-05-28 15:55:00.0000Z\",\"value\":true}";
    private static final String jsonMetaData_ = "{\n" +
            "\"988\": {\"description\": \"CC_F_RECT_FAN_SETPOINT_PEAK\", \"unit\": \"%\", \"type\": \"unknown\" },\n" +
            "\"989\": {\"description\": \"CC_H_CAB_HEALTH_MAIN\", \"unit\": \"status\", \"type\": \"unknown\" },\n" +
            "\"990\": {\"description\": \"CC_H_CAB_HEALTH_MAIN_LATCHED\", \"unit\": \"status\", \"type\": \"unknown\" },\n" +
            "\"991\": {\"description\": \"CC_T_MCU_TEMP\", \"unit\": \"degC\", \"type\": \"Temp\" },\n" +
            "\"992\": {\"description\": \"CC_T_PCB_TEMP\", \"unit\": \"degC\", \"type\": \"Temp\" },\n" +
            "\"993\": {\"description\": \"CC_V_VCC_5_0V\", \"unit\": \"V\", \"type\": \"VoltageIn\", \"specific\": \" \" },\n" +
            "\"994\": {\"description\": \"CC_V_VCC_5_0V_FAN1\", \"unit\": \"V\", \"type\": \"VoltageIn\", \"specific\": \"FAN1\" },\n" +
            "\"995\": {\"description\": \"CC_V_VCC_5_0V_SPI\", \"unit\": \"V\", \"type\": \"VoltageIn\" },\n" +
            "\"996\": {\"description\": \"CC_V_VDD_0_9V\", \"unit\": \"V\", \"type\": \"VoltageIn\" },\n" +
            "\"997\": {\"description\": \"CC_V_VDD_1_0V_OR_1_3V\", \"unit\": \"V\", \"type\": \"VoltageIn\" },\n" +
            "\"998\": {\"description\": \"CC_V_VDD_1_2V\", \"unit\": \"kV\", \"type\": \"VoltageIn\", \"factor\": 1000.0 },\n" +
            "\"999\": {\"description\": \"CC_V_VDD_1_2V_GTP\", \"unit\": \"V\", \"type\": \"VoltageIn\", \"factor\": true },\n" +
            "\"1000\": {\"description\": \"CC_V_VDD_1_8V\", \"unit\": \"mV\", \"type\": \"VoltageIn\", \"factor\": \"0.001\" },\n" +
            "\"1001\": {\"description\": \"CC_V_VDD_2_5V\", \"unit\": \"mV\", \"type\": \"VoltageIn\", \"factor\": 0.001 }\n" +
            "}\n";
}
