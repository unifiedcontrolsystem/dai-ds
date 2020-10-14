// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.text.ParseException;
import java.util.*;

/**
 * Specific provider for environmental data from foreign bus.
 */
public class EnvironmentalProviderForeignBus implements NetworkListenerProvider, Initializer {
    public EnvironmentalProviderForeignBus(Logger logger) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null) throw new RuntimeException("Failed to create a JSON parser instantiating class " +
                EnvironmentalProviderForeignBus.class.getCanonicalName());
    }

    @Override
    public void initialize() {
    }

    @Override
    public List<CommonDataFormat> processRawStringData(String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        if(!configDone_)
            setUpConfig(config);
        List<CommonDataFormat> results = new ArrayList<>();
        try {
            log_.debug("*** Message: %s", data);
            PropertyArray items = CommonFunctions.parseForeignTelemetry(data);
            items.iterator().forEachRemaining((o) -> {
                PropertyMap item = (PropertyMap)o;
                if(!item.keySet().containsAll(requiredInMessage_))
                    log_.warn("Not all expected keys were found in one of the payload object!");
                else {
                    try {
                        long ts = CommonFunctions.convertISOToLongTimestamp(item.getString("Timestamp"));
                        String location = CommonFunctions.convertForeignToLocation(item.getString("Location"));
                        String name = item.getString("__FullName__");
                        CommonDataFormat common = new CommonDataFormat(ts, location, DataType.EnvironmentalData);
                        common.setDescription(name);
                        common.setValueAndUnits(Double.parseDouble(item.getString("Value")), "", name);
                        aggregateData(common);
                        results.add(common);
                    } catch(PropertyNotExpectedType e) {
                        log_.warn("One or more of the expected keys was not the right type or malformed!");
                    } catch(ParseException e) {
                        log_.warn("The incoming Timestamp was not valid: %s",
                                item.getStringOrDefault("Timestamp", "<MISSING>"));
                    } catch(NumberFormatException e) {
                        log_.warn("The incoming Value was not valid: '%s'",
                                item.getStringOrDefault("Value", "<MISSING>"));
                    } catch(ConversionException e) {
                        log_.warn("The incoming Location was not valid: %s",
                                item.getStringOrDefault("Location", "<MISSING>"));
                    }
                }
            });
        } catch (ConfigIOParseException e) {
            log_.warn("Failed to parse telemetry string: '%s'", data);
            throw new NetworkListenerProviderException("Failed to parse incoming data", e);
        }
        return results;
    }

    @Override
    public void actOnData(CommonDataFormat data, NetworkListenerConfig config, SystemActions systemActions) {
        if(!configured_) getConfig(config);
        // Store raw but normalized data...
        log_.debug("Storing raw telemetry for node %s.", data.getLocation());
        systemActions.storeNormalizedData(data.getDescription(), data.getLocation(),
                data.getNanoSecondTimestamp(), data.getValue());
        // Publish raw but normalized data...
        if(publish_) {
            log_.debug("Publishing raw telemetry for node %s.", data.getLocation());
            systemActions.publishNormalizedData(rawTopic_, data.getDescription(), data.getLocation(),
                    data.getNanoSecondTimestamp(), data.getValue());
        }
        if(data.haveSummary()) {
            // Store and publish aggregate data it available...
            log_.debug("Storing aggregate data: type=%s,location=%s,ts=%d,min=%f,max=%f,agv=%f",
                    data.getDescription(), data.getLocation(), data.getNanoSecondTimestamp(),
                    data.getMinimum(), data.getMaximum(), data.getAverage());
            systemActions.storeAggregatedData(data.getTelemetryDataType(), data.getLocation(),
                    data.getNanoSecondTimestamp(), data.getMinimum(), data.getMaximum(), data.getAverage());
            if(publish_) {
                log_.debug("Publishing aggregate data: type=%s,location=%s,ts=%d,min=%f,max=%f,agv=%f",
                        data.getDescription(), data.getLocation(), data.getNanoSecondTimestamp(),
                        data.getMinimum(), data.getMaximum(), data.getAverage());
                systemActions.publishAggregatedData(aggregatedTopic_, data.getDescription(), data.getLocation(),
                        data.getNanoSecondTimestamp(), data.getMinimum(), data.getMaximum(), data.getAverage());
            }
        }
    }

    private void getConfig(NetworkListenerConfig config) {
        configured_ = true;
        PropertyMap map = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if(map != null) {
            publish_ = map.getBooleanOrDefault("publish", false);
            aggregatedTopic_ = map.getStringOrDefault("publishAggregatedTopic", aggregatedTopic_);
            rawTopic_ = map.getStringOrDefault("publishRawTopic", rawTopic_);
        }
    }

    private void setUpConfig(NetworkListenerConfig config) {
        configDone_ = true;
        PropertyMap myConfig = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if(myConfig != null) {
            Accumulator.useTime_ = myConfig.getBooleanOrDefault("useTimeWindow", false);
            Accumulator.count_ = myConfig.getIntOrDefault("windowSize", 25);
            Accumulator.moving_ = myConfig.getBooleanOrDefault("useMovingAverage", false);
            Accumulator.ns_ = myConfig.getLongOrDefault("timeWindowSeconds", 600) * 1_000_000_000;
            doAggregation_ = myConfig.getBooleanOrDefault("useAggregation", true);
        }
    }

    private synchronized CommonDataFormat aggregateData(CommonDataFormat raw) {
        if(doAggregation_) {
            log_.debug("Aggregating data...");
            String key = raw.getLocation() + ":" + raw.getTelemetryDataType();
            Accumulator accum = accumulators_.getOrDefault(key, null);
            if (accum == null) {
                accum = new Accumulator(log_);
                accumulators_.put(key, accum);
            }
            //log_.debug("===RAW_DATA_VALUE: %s = %f", key, raw.getValue()); // Leave for debugging for developers.
            accum.addValue(raw);
        }
        return raw;
    }

    private Logger log_;
    private boolean configured_ = false;
    private boolean publish_ = false;
    private String rawTopic_ = "ucs_raw_data";
    private String aggregatedTopic_ = "ucs_aggregate_data";
    private ConfigIO parser_;
    private Map<String, Accumulator> accumulators_ = new HashMap<>();
            boolean configDone_ = false;
    private boolean doAggregation_ = true;
    @SuppressWarnings("serial")
    private static final List<String> requiredInMessage_ = new ArrayList<String>() {{
        add("__FullName__");
        add("Value");
        add("Timestamp");
        add("Location");
    }};

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
