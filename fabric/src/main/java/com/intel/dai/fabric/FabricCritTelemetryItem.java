// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.fabric;

import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyMap;

/**
 * Object to store fabric telemetry name/value with timestamp.
 */
public class FabricCritTelemetryItem extends FabricItemBase {
    /**
     * Construct a new fabric telemetry item.
     *
     * @param timestamp The long microseconds since epoch (UTC).
     * @param name The name of the value.
     * @param location The location of the sensor value.
     * @param serialNum The serial number of the hardware the event occurred on. May be null.
     * @param jobId The job ID associated with the event at this location. May be null.
     * @param data The instance data associated with the event. May be null.
     */
    public FabricCritTelemetryItem(long timestamp, String name, String location, String serialNum, String jobId, String data) {
        super(timestamp, location, name);
        serialNumber_ = serialNum;
        jobId_ = jobId;
        data_ = data;
    }

    /**
     * Contructor to create a FabricTelemetryItem instance from a JSON string.
     *
     * @param json The JSON representation of the object.
     *
     * @throws ConfigIOParseException when the JSON is incorrect or malformed.
     */
    public FabricCritTelemetryItem(String json) throws ConfigIOParseException {
        PropertyMap map = getPropertyMapFromJSON(json);
        serialNumber_ = map.getStringOrDefault("serialNumber", null);
        jobId_ = map.getStringOrDefault("jobId", null);
        data_ = map.getStringOrDefault("data", null);
    }

    /**
     * Get the serial number of associated hardware for this event.
     *
     * @return The serial number or null.
     */
    public String getSerialNumber() { return serialNumber_; }

    /**
     * Get the job id associated with this event.
     *
     * @return The job id or null.
     */
    public String getJobId() { return jobId_; }

    /**
     * Get the instance data associated with this event.
     *
     * @return The instance data or null.
     */
    public String getInstanceData() { return data_; }

    /**
     * Implemented by derived classes to add the new key values specific to the derived class.
     *
     * @param map The {@link PropertyMap} to add to.
     */
    @Override
    protected void addOtherValuesToJsonMap(PropertyMap map) {
        map.put("serialNumber", serialNumber_);
        map.put("jobId", jobId_);
        map.put("data", data_);
    }

    private String serialNumber_;
    private String jobId_;
    private String data_;
}
