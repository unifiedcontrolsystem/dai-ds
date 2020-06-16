// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//

package com.intel.dai.fabric;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

/**
 * Base class for all fabric data types.
 */
public abstract class FabricItemBase {
    /**
     * Construct the base object instance, can only be called by derived classes.
     *
     * @param timestamp The long microseconds since epoch (UTC).
     * @param name The name of the value.
     * @param location The location of the sensor value.
     */
    protected FabricItemBase(long timestamp, String location, String name) {
        if(name == null)
            throw new IllegalArgumentException("The name parameter was null");
        timestamp_ = timestamp;
        name_ = name;
        location_ = location;
    }

    /**
     * Get the timestamp for this data item.
     *
     * @return The long microseconds since epoch (UTC)
     */
    final public long getTimestamp() { return timestamp_; }

    /**
     * Get the location of the data associated with this instance.
     *
     * @return The location string for the data.
     */
    public String getLocation() { return location_; }

    /**
     * Get the name for the primary value.
     *
     * @return The name of the value.
     */
    final public String getName() { return name_; }

    /**
     * Override of the Object.toString(). Returns the JSON object for this data.
     *
     * @return The JSON for this object as a String.
     */
    @Override
    public String toString() {
        if(parser_ == null) throw new NullPointerException("Failed to create a JSON parser!");
        PropertyMap map = new PropertyMap();
        map.put("timestamp", timestamp_);
        map.put("name", name_);
        map.put("location", location_);
        addOtherValuesToJsonMap(map);
        return parser_.toString(map);
    }

    /**
     * Implemented by derived classes to add the new key values specific to the derived class.
     *
     * @param map The {@link PropertyMap} to add to.
     */
    abstract protected void addOtherValuesToJsonMap(PropertyMap map);

    /**
     * Parses the JSON to a {@link PropertyMap} then extracts the common values for the base object. This should
     * be called by an appropriate ctor in the derived class.
     *
     * @param json The String containing the JSON to parse.
     * @return The PropertyMap so that the derived class can pull out its specific values.
     * @throws ConfigIOParseException when the JSON is incorrect or malformed.
     */
    final protected PropertyMap getPropertyMapFromJSON(String json) throws ConfigIOParseException {
        if(parser_ == null) throw new NullPointerException("Failed to create a JSON parser!");
        PropertyMap map = parser_.fromString(json).getAsMap();
        long timestamp = -1L;
        String name = null;
        String location = null;
        try {
            for(String key: map.keySet()) {
                switch (key) {
                    case "timestamp":
                        timestamp = map.getLong(key);
                        break;
                    case "location":
                        location = map.getString(key);
                        break;
                    case "name":
                        name = map.getString(key);
                        break;
                    default:
                        break;
                }
            }
            if(name == null || timestamp == -1L)
                throw new ConfigIOParseException("JSON content had null or missing values");
            map.remove("timestamp");
            map.remove("name");
            map.remove("location");
        } catch(PropertyNotExpectedType e) {
            throw new ConfigIOParseException("JSON content had unexpected value types", e);
        }
        timestamp_ = timestamp;
        location_ = location;
        name_ = name;
        return map;
    }

    FabricItemBase() {} // Only for compiler constraints on JSON version of ctors in derived classes.

    private long timestamp_;
    private String name_;
    private String location_;

    private static ConfigIO parser_ = ConfigIOFactory.getInstance("json");
}
