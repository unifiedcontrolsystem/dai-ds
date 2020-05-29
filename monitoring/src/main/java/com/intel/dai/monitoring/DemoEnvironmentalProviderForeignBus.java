// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description of class TelemetryTransformer.
 */
public class DemoEnvironmentalProviderForeignBus implements NetworkListenerProvider, Initializer {
    public DemoEnvironmentalProviderForeignBus(Logger logger) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null) throw new RuntimeException("Failed to create a JSON parser instantiating class " +
                DemoEnvironmentalProviderForeignBus.class.getCanonicalName());
    }

    @Override
    public void initialize() {
    }

    @Override
    public List<CommonDataFormat> processRawStringData(String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        if(!configDone_)
            setUpConfig(config);
        List<String> individualMessages = breakupStreamedJSONMessages(data.trim());
        List<CommonDataFormat> allData = new ArrayList<>();
        for(String individual: individualMessages) {
            List<CommonDataFormat> someData = processIndividualRawStringData(individual, config);
            if(someData != null)
                allData.addAll(someData);
        }
        return allData;
    }

    @Override
    public void actOnData(CommonDataFormat data, NetworkListenerConfig config, SystemActions systemActions) {
        if(!configured_) getConfig(config);
        // Store raw but normalized data...
        log_.debug("Storing raw telemetry for node %s.", data.getLocation());
        systemActions.storeNormalizedData(data.getTelemetryDataType(), data.getLocation(),
                data.getNanoSecondTimestamp(), data.getValue());
        // Publish raw but normalized data...
        if(publish_) {
            log_.debug("Publishing raw telemetry for node %s.", data.getLocation());
            systemActions.publishNormalizedData(rawTopic_, data.getTelemetryDataType(), data.getLocation(),
                    data.getNanoSecondTimestamp(), data.getValue());
        }
        // Store and publish aggregate data it available...
        log_.debug("Storing aggregate data: type=%s,location=%s,ts=%d,min=%f,max=%f,agv=%f",
                data.getTelemetryDataType(), data.getLocation(), data.getNanoSecondTimestamp(),
                data.getMinimum(), data.getMaximum(), data.getAverage());
        systemActions.storeAggregatedData(data.getTelemetryDataType(), data.getLocation(),
                data.getNanoSecondTimestamp(), data.getMinimum(), data.getMaximum(), data.getAverage());
        if(publish_) {
            log_.debug("Publishing aggregate data: type=%s,location=%s,ts=%d,min=%f,max=%f,agv=%f",
                    data.getTelemetryDataType(), data.getLocation(), data.getNanoSecondTimestamp(),
                    data.getMinimum(), data.getMaximum(), data.getAverage());
            systemActions.publishAggregatedData(aggregatedTopic_, data.getTelemetryDataType(), data.getLocation(),
                    data.getNanoSecondTimestamp(), data.getMinimum(), data.getMaximum(), data.getAverage());
        }
    }

    private List<String> breakupStreamedJSONMessages(String data) {
        List<String> jsonMessages = new ArrayList<>();
        int braceDepth = 0;
        int startOfMessage = 0;
        boolean inQuote = false;
        char chr;
        for(int index = 0; index < data.length(); index++) {
            chr = data.charAt(index);
            if(chr == '"')
                inQuote = toggleQuote(data, index, inQuote);
            if(inQuote || Character.isWhitespace(chr))
                continue;
            if(chr == '{')
                braceDepth++;
            else if(chr == '}')
                braceDepth--;
            if(braceDepth == 0) { // new message found
                jsonMessages.add(data.substring(startOfMessage, index + 1).trim());
                startOfMessage = index + 1;
            }
        }
        return jsonMessages;
    }

    private boolean toggleQuote(String data, int index, boolean inQuote) {
        if(inQuote && data.charAt(index - 1) == '\\')
            return inQuote;
        return !inQuote;
    }

    private List<CommonDataFormat> processIndividualRawStringData(String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException {

        List<CommonDataFormat> commonList = new ArrayList<>();
        PropertyMap document;
        try {
            document = parser_.fromString(data).getAsMap();
            if(document == null) {
                log_.debug("FAILED JSON: '%s'", data);
                return commonList;
            }
        } catch(ConfigIOParseException e) {
            log_.exception(e);
            log_.debug("FAILED JSON: '%s'", data);
            return commonList;
        }
        if(!document.containsKey("metrics") || document.get("metrics") == null)
            throw new NetworkListenerProviderException("Missing the 'metric' top level key.");
        PropertyMap metrics;
        try {
            metrics = document.getMap("metrics");
        } catch(PropertyNotExpectedType e) {
            throw new NetworkListenerProviderException("The 'metric' top level key was not a map.");
        }
        if(!metrics.containsKey("messages") || metrics.get("messages") == null)
            throw new NetworkListenerProviderException("Missing the second level 'messages' key.");
        PropertyArray messages;
        try {
            messages = metrics.getArray("messages");
        } catch(PropertyNotExpectedType e) {
            throw new NetworkListenerProviderException("Second level 'messages' key is not an array.");
        }
        for(Object obj: messages) {
            if(obj instanceof PropertyMap) {
                PropertyMap map = (PropertyMap)obj;
                if(checkMessage(map)) {
                    log_.debug("*** Processing data item...");
                    try {
                        // The 3 values from the map cannot be null after the call to checkMessage.
                        long nsTimestamp = map.getLong("timestamp") * 1_000_000; // ms to ns
                        String name = map.getString("name");
                        long rawValue = map.getLong("value");
                        CommonDataFormat dataObj = new CommonDataFormat(nsTimestamp, "X0-CH4-CN0",
                                DataType.EnvironmentalData);
                        dataObj.setDescription(name);
                        dataObj.setValue((double)rawValue);
                        log_.debug("*** Aggregating data...");
                        aggregateData(dataObj);
                        commonList.add(dataObj);
                    } catch(PropertyNotExpectedType e) {
                        log_.exception(e);
                    }
                }
            }
        }
        return commonList;
    }

    private boolean checkMessage(PropertyMap message) {
        boolean result = true;
        for(String required: requiredInMessage_)
            if (!message.containsKey(required) || message.get(required) == null) {
                result = false;
                break;
            }
        return result;
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

    private Logger log_;
    private boolean configured_ = false;
    private boolean publish_ = false;
    private String rawTopic_ = "ucs_raw_data";
    private String aggregatedTopic_ = "ucs_aggregate_data";
    private ConfigIO parser_;
    private SensorMetaDataForeignBus sensorMetaData_ForeignBus_;
    private Map<String, Accumulator> accumulators_ = new HashMap<>();
            boolean configDone_ = false;
    private boolean doAggregation_ = true;
    private static final String[] requiredInMessage_ = new String[] {"name", "value", "timestamp"};

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
