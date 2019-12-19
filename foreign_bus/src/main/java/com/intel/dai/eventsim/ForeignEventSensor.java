// Copyright (C) 2018 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0

package com.intel.dai.eventsim;

public class ForeignEventSensor extends ForeignEvent {

    private int eventID_;
    private String sensorName_;

    @Override
    public String getEventCategory() {
        return "telemetry";
    }

    public void setEventID(int id) {
        props_.put("event_id", id);
    }

    public int getEventID() {
        return props_.getIntOrDefault("event_id", 0);
    }

    public void setSensorName(String sensorName) {
        props_.put("sensor", sensorName);
    }

    public String getSensorName() {
        return props_.getStringOrDefault("sensor", null);
    }

    public void setSensorValue(String sensorValue) {
        props_.put("value", sensorValue);
    }

    public String getSensorValue() {
        return props_.getStringOrDefault("value", null);
    }

    public void setSensorUnits(String sensorUnits) {
        props_.put("unit", sensorUnits);
    }

    public String getSensorUnits() {
        return props_.getStringOrDefault("unit", null);
    }

    public void setSensorID(String sensorID) {
        props_.put("id", sensorID);
    }

    public String getSensorID() {
        return props_.getStringOrDefault("id", null);
    }
}
