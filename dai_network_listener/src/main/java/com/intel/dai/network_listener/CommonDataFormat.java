// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.network_listener;

import com.intel.dai.dsapi.BootState;

import java.util.HashMap;
import java.util.Map;

/**
 * Description of class CommonDataFormat which is the common internal representation of incoming data. This is a data
 * storage object only.
 */
public final class CommonDataFormat {
    public CommonDataFormat(long nanoSecondTimestamp, String location, DataType type) {
        nsTimestamp_ = nanoSecondTimestamp;
        location_ = location;
        type_ = type;
    }

    public long getNanoSecondTimestamp() { return nsTimestamp_; }
    public String getLocation() { return location_; }
    public DataType getDataType() { return type_; }

    public String getDescription() { return description_; }
    public void setDescription(String description) { description_ = description; }

    public void setValueAndUnits(double value, String units, String dataType) {
        value_ = value; units_ = units; telemetryDataType_ = dataType;
    }
    public void setValue(double value) { value_ = value; units_ = null; }
    public double getValue() { return value_; }
    public String getUnits() { return units_; }
    public String getTelemetryDataType() { return telemetryDataType_; }

    public void setMinMaxAvg(double min, double max, double avg) { min_ = min; max_ = max; average_ = avg; }
    public double getMinimum() { return min_; }
    public double getMaximum() { return max_; }
    public double getAverage() { return average_; }

    public void setRasEvent(String event, String payload) { event_ = event; rasEventPayload_ = payload; }
    public void setStateChangeEvent(BootState event) { bootStateEvent_ = event; }
    public void setInventoryChangeEvent(String event) { inventoryEvent_ = event; }
    public String getRasEvent() { return event_; }
    public String getInventoryEvent() { return inventoryEvent_; }
    public BootState getStateEvent() { return bootStateEvent_; }
    public String getRasEventPayload() { return rasEventPayload_; }

    public String getLogLine() { return logLine_; }
    public void setLogLine(String logLine) { logLine_ = logLine; }

    public void storeExtraData(String key, String value) { extraData_.put(key, value); }
    public String retrieveExtraData(String key) { return extraData_.getOrDefault(key, ""); }

    private long nsTimestamp_;
    private String location_;
    private String description_;
    private DataType type_;
    private String telemetryDataType_;
    private String units_ = null;
    private double value_ = Double.MIN_VALUE;
    private double min_ = Double.MAX_VALUE;
    private double max_ = Double.MIN_VALUE;
    private double average_ = Double.MIN_VALUE;
    private BootState bootStateEvent_ = null;
    private String inventoryEvent_ = null;
    private String event_ = null;
    private String rasEventPayload_ = null;
    private String logLine_ = null;
    private Map<String,String> extraData_ = new HashMap<>();
}
