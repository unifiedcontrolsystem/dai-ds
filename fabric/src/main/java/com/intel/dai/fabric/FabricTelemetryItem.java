// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.fabric;

import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

/**
 * Object to store fabric telemetry name/value with timestamp.
 */
public class FabricTelemetryItem extends FabricItemBase {
    /**
     * Construct a new fabric telemetry item.
     *
     * @param timestamp The long microseconds since epoch (UTC).
     * @param name The name of the value.
     * @param location The location of the sensor value.
     * @param value The value associated with the name.
     */
    public FabricTelemetryItem(long timestamp, String name, String location, double value) {
        this(timestamp, name, location, value, false);
    }

    /**
     * Construct a new fabric telemetry item.
     *
     * @param timestamp The long microseconds since epoch (UTC).
     * @param name The name of the value.
     * @param location The location of the sensor value.
     * @param value The value associated with the name.
     * @param skipStore When true no aggregation and store are done, only a republish.
     */
    public FabricTelemetryItem(long timestamp, String name, String location, double value, boolean skipStore) {
        super(timestamp, location, name);
        value_ = value;
        skipAggregateAndStore_ = skipStore;
    }

    /**
     * Contructor to create a FabricTelemetryItem instance from a JSON string.
     *
     * @param json The JSON representation of the object.
     *
     * @throws ConfigIOParseException when the JSON is incorrect or malformed.
     */
    public FabricTelemetryItem(String json) throws ConfigIOParseException {
        PropertyMap map = getPropertyMapFromJSON(json);
        try {
            value_ = map.getDouble("value");
            if(map.containsKey("minimum") && map.containsKey("average") && map.containsKey("maximum")) {
                minimum_ = map.getDouble("minimum");
                average_ = map.getDouble("average");
                maximum_ = map.getDouble("maximum");
            }
            skipAggregateAndStore_ = map.getBooleanOrDefault("skipStore", false);
        } catch(PropertyNotExpectedType e) {
            throw new ConfigIOParseException("JSON content had unexpected value types", e);
        }
    }

    /**
     * Get the primary value for this data item.
     *
     * @return The primary Number value stored.
     */
    public double getValue() { return value_; }

    /**
     * Get the minimum value.
     *
     * @return Current minimum value or Double.NaN if undefined.
     */
    public double getMinimum() { return minimum_; }

    /**
     * Get the maximum value.
     *
     * @return Current maximum value or Double.NaN if undefined.
     */
    public double getMaximum() { return maximum_; }

    /**
     * Get the average value.
     *
     * @return Current average value or Double.NaN if undefined.
     */
    public double getAverage() { return average_; }

    /**
     * Get is the object was created with the skip aggreagtion and storing flag.
     *
     * @return True indicates that aggregation and storing is not required, False means normal telemetry operations.
     */
    public boolean skipAggregationAndStore() { return skipAggregateAndStore_; }

    /**
     * Set the statistics to use as aggregated data.
     *
     * @param minimum The minimum value, cannot be Double.MIN_VALUE.
     * @param average The average value, cannot be Double.MIN_VALUE.
     * @param maximum The maximum value, cannot be Double.MIN_VALUE.
     */
    public void setStatistics(double minimum, double average, double maximum) {
        if(minimum == Double.MIN_VALUE || maximum == Double.MIN_VALUE || average == Double.MIN_VALUE)
            throw new IllegalArgumentException("One or more of the parameters was Double.MIN_VALUE which is illegal");
        minimum_ = minimum;
        average_ = average;
        maximum_ = maximum;
        haveStats_ = true;
    }

    /**
     * Determine if the statistics have been set in this instance.
     *
     * @return true when setStatistics has been called successfully on this instance, false otherwise.
     */
    public boolean haveStatistics() {
        return haveStats_;
    }

    /**
     * Implemented by derived classes to add the new key values specific to the derived class.
     *
     * @param map The {@link PropertyMap} to add to.
     */
    @Override
    protected void addOtherValuesToJsonMap(PropertyMap map) {
        map.put("value", value_);
        if(haveStatistics()) {
            map.put("minimum", minimum_);
            map.put("average", average_);
            map.put("maximum", maximum_);
        }
        map.put("skipStore", skipAggregateAndStore_);
    }

    private double value_ = Double.MIN_VALUE;
    private double minimum_ = Double.MIN_VALUE;
    private double maximum_ = Double.MIN_VALUE;
    private double average_ = Double.MIN_VALUE;
    private boolean haveStats_ = false;
    private boolean skipAggregateAndStore_ = false;
}
