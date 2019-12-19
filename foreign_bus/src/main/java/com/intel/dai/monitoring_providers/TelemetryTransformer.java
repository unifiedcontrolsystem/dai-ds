// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring_providers;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.logging.Logger;
import com.intel.partitioned_monitor.*;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.io.InputStream;
import java.text.ParseException;
import java.util.*;

/**
 * Description of class TelemetryTransformer.
 */
public class TelemetryTransformer implements DataTransformer, Initializer {
    public TelemetryTransformer(Logger logger) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null) throw new RuntimeException("Failed to create a JSON parser instantiating class " +
                TelemetryTransformer.class.getCanonicalName());
        sensorMetaData_ = new SensorMetaData(parser_);
    }

    @Override
    public void initialize() {
        if(!sensorMetaData_.loadFromStream(getMetaDataStream()))
            throw new RuntimeException("Failed to load the sensor metadata");
    }

    protected InputStream getMetaDataStream() {
        return getClass().getClassLoader().getResourceAsStream("resources/ForeignSensorMetaData.json");
    }

    @Override
    public List<CommonDataFormat> processRawStringData(String data, PartitionedMonitorConfig config)
            throws DataTransformerException {
        if(!configDone_)
            setUpConfig(config);
        try {
            PropertyMap message = parser_.fromString(data).getAsMap();
            checkMessage(message);
            log_.debug("*** Message: %s", data);
            String[] xnameLocations = message.getString("location").split(",");
            List<CommonDataFormat> commonList = new ArrayList<>();
            for(String xnameLocation: xnameLocations) {
                String sensor = message.getString("sensor");
                if (!sensorMetaData_.checkSensor(sensor))
                    throw new DataTransformerException("The 'sensor' field in JSON is not a string name");
                String location = sensorMetaData_.normalizeLocation(sensor,
                        CommonFunctions.convertXNameToLocation(xnameLocation, sensorMetaData_.getDescription(sensor)));
                CommonDataFormat common = new CommonDataFormat(
                        CommonFunctions.convertISOToLongTimestamp(message.getString("timestamp")), location,
                        DataType.EnvironmentalData);
                common.setValueAndUnits(sensorMetaData_.normalizeValue(sensor, convertValue(message.get("value"))),
                        sensorMetaData_.getUnits(sensor), sensorMetaData_.getTelemetryType(sensor));
                aggregateData(common);
                commonList.add(common);
            }
            return commonList;
        } catch(ConfigIOParseException | PropertyNotExpectedType | ParseException e) {
            log_.debug("Failed to parse telemetry string: '%s'", data);
            throw new DataTransformerException("Failed to parse incoming data", e);
        }
    }

    private void setUpConfig(PartitionedMonitorConfig config) {
        configDone_ = true;
        PropertyMap myConfig = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if(myConfig != null) {
            Accumulator.useTime_ = myConfig.getBooleanOrDefault("useTimeWindow", false);
            Accumulator.count_ = myConfig.getIntOrDefault("windowSize", 25);
            Accumulator.moving_ = myConfig.getBooleanOrDefault("useMovingAverage", false);
            Accumulator.ns_ = myConfig.getLongOrDefault("timeWindowSeconds", 600) * 1000000;
            doAggregation_ = myConfig.getBooleanOrDefault("useAggregation", true);
        }
    }

    private CommonDataFormat aggregateData(CommonDataFormat raw) {
        if(doAggregation_) {
            log_.debug("Aggregating data...");
            String key = raw.getLocation() + ":" + raw.getTelemetryDataType();
            Accumulator accum = accumulators_.getOrDefault(key, null);
            if (accum == null) {
                accum = new Accumulator(log_);
                accumulators_.put(key, accum);
            }
            accum.addValue(raw);
        }
        return raw;
    }

    private double convertValue(Object oValue) throws ConfigIOParseException {
        if(oValue instanceof Number)
            return ((Number)oValue).doubleValue();
        else if(oValue instanceof String)
            return Double.valueOf((String)oValue);
        else
            throw new ConfigIOParseException("The 'value' was not a Number or a String and so could not be converted");
    }

    private void checkMessage(PropertyMap message) throws DataTransformerException {
        for(String required: requiredInMessage_)
            if (!message.containsKey(required))
                throw new DataTransformerException(String.format("Incoming data was missing '%s' in the JSON",
                        required));
    }

    private Logger log_;
    private ConfigIO parser_;
    private SensorMetaData sensorMetaData_;
    private Map<String, Accumulator> accumulators_ = new HashMap<>();
            boolean configDone_ = false;
    private boolean doAggregation_ = true;
    private static final String[] requiredInMessage_ = new String[] {"sensor", "value", "timestamp", "location"};

    static class Accumulator {
        Accumulator(Logger logger) { log_ = logger; }
        void addValue(CommonDataFormat data) {
            values_.add(data.getValue());
            log_.debug("Accumulator: value count now %d compared to %d", values_.size(), count_);
            timestamps_.add(data.getNanoSecondTimestamp());
            if(useTime_ && timeExceeded())
                doAggregation(data);
            else if(!useTime_ && values_.size() == count_)
                doAggregation(data);
        }

        void doAggregation(CommonDataFormat data) {
            log_.debug("Generating aggregated data...");
            double sum = 0.0;
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for(double value: values_) {
                sum += value;
                max = Double.max(value, max);
                min = Double.min(value, min);
            }
            double avg = sum / (double)values_.size();
            data.setMinMaxAvg(min, max, avg);
            if(moving_) { // moving average algorithm with window size = count_;
                values_.remove(0);
                timestamps_.remove(0);
            } else { // window algorithm
                values_.clear();
                timestamps_.clear();
            }
        }

        boolean timeExceeded() {
            return (timestamps_.get(timestamps_.size()-1) - timestamps_.get(0)) >= ns_;
        }

        List<Double> values_ = new ArrayList<>();
        List<Long> timestamps_ = new ArrayList<>();
        Logger log_;

        static boolean useTime_ = false; // time window if true; count window if false.
        static int count_ = 25;          // 25 sample count window.
        static long ns_ = 600000000;     // 10 minutes default in nanoseconds.
        static boolean moving_ = false;  // true will give a moving average with the count_ window;
                                         // false is a simple window.
    }
}
