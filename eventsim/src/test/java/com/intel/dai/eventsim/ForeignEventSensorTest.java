// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.eventsim;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ForeignEventSensorTest {

    ForeignEventSensor ev;

    @Before
    public void SetUp() {
        ev = new ForeignEventSensor();
    }

    @Test
    public void CanInitTimeStamp() {
        //ev.setTimestamp(6);
        System.out.println("DATE: " + ev.getTimestampString());
        //assertEquals(51532649931, ev.getTimestamp());
    }

    @Test
    public void CanSetGetEventID() {
        ev.setEventID(5);
        assertEquals(5,ev.getEventID());
    }

    @Test
    public void CanSetGetSensorName() {
        ev.setSensorName("sensorName123");
        assertEquals("sensorName123", ev.getSensorName());
    }

    @Test
    public void CanSetGetSensorValue() {
        ev.setSensorValue("23");
        assertEquals("23", ev.getSensorValue());
    }

    @Test
    public void CanSetGetSensorUnits() {
        ev.setSensorUnits("degC");
        assertEquals("degC", ev.getSensorUnits());
    }

    @Test
    public void CanSetGetSensorId() {
        ev.setSensorID("1221");
        assertEquals("1221", ev.getSensorID());
    }

    @Test
    public void CanSetGetLocation() {
        //ev.setLocation("c0-0c0s2");
        //assertEquals("c0-0c0s2", ev.getLocation);
    }
}
