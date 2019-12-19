// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.partitioned_monitor;

import com.intel.dai.dsapi.BootState;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CommonDataFormatTest {

    @Before
    public void setUp() throws Exception {
        data_ = new CommonDataFormat(9999L, "here", DataType.EnvironmentalData);
    }

    @Test
    public void getNanoSecondTimestamp() {
        assertEquals(9999L, data_.getNanoSecondTimestamp());
    }

    @Test
    public void getLocation() {
        assertEquals("here", data_.getLocation());
    }

    @Test
    public void getDataType() {
        assertEquals(DataType.EnvironmentalData, data_.getDataType());
    }

    @Test
    public void getDescription() {
        assertNull(data_.getDescription());
    }

    @Test
    public void setDescription() {
        data_.setDescription("CPU Power");
        assertEquals("CPU Power", data_.getDescription());
    }

    @Test
    public void setValueAndUnits() {
        data_.setValueAndUnits(37.0, "C", "power");
        assertEquals("C", data_.getUnits());
        assertEquals(37.0, data_.getValue(), 0.001);
        assertEquals("power", data_.getTelemetryDataType());
    }

    @Test
    public void setValue() {
        data_.setValue(100.0);
        assertEquals(100.0, data_.getValue(), 0.001);
    }

    @Test
    public void setMinMaxAvg() {
        data_.setMinMaxAvg(1.0, 3.0, 2.0);
        assertEquals(1.0, data_.getMinimum(), 0.001);
        assertEquals(3.0, data_.getMaximum(), 0.001);
        assertEquals(2.0, data_.getAverage(), 0.001);
    }

    @Test
    public void setRasEvent() {
        data_.setRasEvent("RasEvent", "Payload");
        assertEquals("RasEvent", data_.getRasEvent());
        assertEquals("Payload", data_.getRasEventPayload());
    }

    @Test
    public void setStateChangeEvent() {
        data_.setStateChangeEvent(BootState.NODE_BOOTING);
        assertEquals(BootState.NODE_BOOTING, data_.getStateEvent());
    }

    @Test
    public void setInventoryChangeEvent() {
        data_.setInventoryChangeEvent("Replaced");
        assertEquals("Replaced", data_.getInventoryEvent());
    }

    @Test
    public void setLogLine() {
        data_.setLogLine("Log Line");
        assertEquals("Log Line", data_.getLogLine());
    }

    @Test
    public void storeAndRetrieveExtraData() {
        assertEquals("", data_.retrieveExtraData("key"));
        data_.storeExtraData("key", "data");
        assertEquals("data", data_.retrieveExtraData("key"));
    }

    private CommonDataFormat data_;
}
