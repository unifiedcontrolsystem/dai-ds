// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOParseException;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Provide lookup for foreign sensor metadata. Internal class.
 */
class SensorMetaDataForeignBus {
    SensorMetaDataForeignBus(ConfigIO parser) {
        parser_ = parser;
    }

    boolean loadFromStream(InputStream stream) {
        if(stream == null)
            return false;
        try {
            PropertyMap map = parser_.readConfig(stream).getAsMap();
            for(String key: map.keySet())
                entries_.put(key, new SensorMetaDataEntry(key, map.getMap(key)));
        } catch(IOException | ConfigIOParseException | PropertyNotExpectedType e) {
            return false;
        }
        return true;
    }

    String getUnits(String sensor) {
        return checkSensor(sensor)?entries_.get(sensor).units:"";
    }

    boolean checkSensor(String sensor) {
        return entries_.containsKey(sensor);
    }

    private Map<String,SensorMetaDataEntry> entries_ = new HashMap<>();

    double normalizeValue(String sensor, double value) {
        return entries_.get(sensor).normalizeValue(value);
    }

    String getTelemetryType(String sensor) {
        return entries_.get(sensor).ucsType;
    }

    String getDescription(String sensor) {
        return entries_.get(sensor).name;
    }

    String normalizeLocation(String sensor, String location) {
        if(entries_.get(sensor).extraLocation.isBlank())
            return location;
        return location + "-" + entries_.get(sensor).extraLocation;
    }

    ConfigIO parser_;

    static class SensorMetaDataEntry {
        SensorMetaDataEntry(String sensorKey, PropertyMap entry) throws PropertyNotExpectedType {
            id = sensorKey;
            name = entry.getString("description");
            units = entry.getStringOrDefault("unit", "");
            ucsType = entry.getStringOrDefault("type", "unknown");
            if(entry.containsKey("factor")) {
                if(entry.get("factor") instanceof String)
                    factor = Double.valueOf(entry.getStringOrDefault("factor", "1.0"));
                else if(entry.get("factor") instanceof Number)
                    factor = entry.getDoubleOrDefault("factor", 1.0);
            }
            extraLocation = entry.getStringOrDefault("specific", "");
        }

        double normalizeValue(double rawValue) {
            if(factor > 0.99 && factor < 1.01)
                return rawValue;
            else
                return rawValue * factor;
        }

        final String id;
        final String name;
        final String units;
        final String ucsType;
        final String extraLocation;
        double factor = 1.0;
    }
}
