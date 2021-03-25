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
import com.intel.runtime_utils.TimeUtils;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specific provider for environmental data from foreign bus.
 */
public class EnvironmentalProviderHPCM implements NetworkListenerProvider, Initializer {
    static class EnvelopeData {
        EnvelopeData(String context, long nsTimestamp, String location, String prefixName) {
            this.context = context;
            this.nsTimestamp = nsTimestamp;
            this.location = location;
            this.prefixName = prefixName;
        }
        EnvelopeData(String context, long nsTimestamp, String location) {
            this(context, nsTimestamp, location, null);
        }
        final String context;
        final long nsTimestamp;
        final String location;
              String prefixName;
    }

    @FunctionalInterface
    private interface ContextHandler {
        void call(PropertyMap object, EnvelopeData data, List<CommonDataFormat> results);
    }

    public EnvironmentalProviderHPCM(Logger logger) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null) throw new RuntimeException("Failed to create a JSON parser instantiating class " +
                EnvironmentalProviderHPCM.class.getCanonicalName());
    }

    @Override
    public void initialize() {
        // Setup supported odata_context values and handler methods.
        dispatchMap_.put("/redfish/v1/$metadata#ProcessorMetrics.ProcessorMetrics", this::processProcessorMetrics);
        dispatchMap_.put("/redfish/v1/$metadata#Thermal.Thermal", this::processThermals);
        dispatchMap_.put("/redfish/v1/$metadata#Power.Power", this::processPower);
    }

    ////////////////////////
    // Parse Data Section //
    ////////////////////////
    @Override
    public List<CommonDataFormat> processRawStringData(String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        setUpConfig(config);
        List<CommonDataFormat> results = new ArrayList<>();
            log_.debug("*** Message: %s", data);
            List<String> sObjects = CommonFunctions.breakupStreamedJSONMessages(data);
            for(String streamObject: sObjects) {
                PropertyMap item = null;
                try {
                    item = parser_.fromString(streamObject).getAsMap();
                } catch (ConfigIOParseException e) {
                    log_.warn("Failed to parse telemetry string: '%s'", data);
                    throw new NetworkListenerProviderException("Failed to parse incoming data", e);
                }
                if(item != null)
                    dispatchProcessing(results, item);
            }
        return results;
    }

    private void dispatchProcessing(List<CommonDataFormat> results, PropertyMap item) {
        String itemContext = item.getStringOrDefault("odata_context", null);
        if(itemContext != null) {
            ContextHandler dispatch = dispatchMap_.getOrDefault(itemContext, null);
            if(dispatch != null) {
                try {
                    EnvelopeData envelopeData = getLocationAndTimestamp(item);
                    dispatch.call(item, envelopeData, results);
                } catch(PropertyNotExpectedType e) {
                    log_.exception(e, "One or more of the expected keys was not the right type or malformed!");
                } catch(ParseException e) {
                    log_.warn("The incoming 'timestamp' was not valid: %s",
                            item.getStringOrDefault("timestamp", "<MISSING>"));
                } catch(ConversionException e) {
                    log_.warn("The incoming 'location' was not valid: %s",
                            item.getStringOrDefault("location", "<MISSING>"));
                }
            } else
                log_.warn("Missing parsing dispatch handler for odata_context='%s'! Skipping data!", itemContext);
        } else
            log_.warn("Missing 'odata_context' key in incoming JSON! Skipping data!");
    }

    private EnvelopeData getLocationAndTimestamp(PropertyMap item)
            throws PropertyNotExpectedType, ParseException, ConversionException {
        String context = item.get("odata_context").toString(); // Cannot be null returned here, see dispatchProcessing()
        // TODO: Unclear how the timestamp will be delivered, long ms from epoch or ISO8601 format...
        long ts = TimeUtils.nSFromIso8601(item.getString("timestamp"));
//        long ts = TimeUtils.millisecondsToNanoseconds(item.getLong("timestamp"));
        String location = CommonFunctions.convertForeignToLocation(item.getString("location"));
        return new EnvelopeData(context, ts, location);
    }

    private void addToResults(String name, double value, String units, String type, EnvelopeData data,
                              List<CommonDataFormat> results) {
        CommonDataFormat common = new CommonDataFormat(data.nsTimestamp, data.location, DataType.EnvironmentalData);
        common.setValueAndUnits(value, units, type);
        common.setDescription(name);
        results.add(aggregateData(common));
    }

    private void processProcessorMetrics(PropertyMap item, EnvelopeData data, List<CommonDataFormat> results) {
        String name = item.getStringOrDefault("Name", null);
        if(name == null) {
            log_.warn("JSON for 'odata_context'='%s' is missing a 'Name' entry!",
                    item.getStringOrDefault("odata_context", "Unknown Context"));
            name = "<MISSING>";
        }
        if(item.containsKey("TemperatureCelsius") && item.get("TemperatureCelsius") != null) {
            double value = item.getDoubleOrDefault("TemperatureCelsius", -9999.99);
            addToResults(name, value, "C", "Thermal", data, results);
        } else {
            log_.warn("JSON for 'odata_context'='%s' has a missing or 'null' key for 'TemperatureCelsius'! " +
                            "Skipping data!", item.getStringOrDefault("odata_context", "Unknown Context"));
        }
    }

    private void processThermals(PropertyMap item, EnvelopeData data, List<CommonDataFormat> results) {
        PropertyArray fans = item.getArrayOrDefault("Fans", new PropertyArray());
        PropertyArray temperatures = item.getArrayOrDefault("Temperatures", new PropertyArray());
        data.prefixName = item.getStringOrDefault("Name", null);
        for(Object o: fans) {
            if(o instanceof PropertyMap) {
                PropertyMap fanObject = (PropertyMap) o;
                processThermalFan(fanObject, data, results);
            } else {
                log_.warn("Unknown array or primitive type in 'Fans' array JSON!");
            }
        }
        for(Object o: temperatures) {
            if(o instanceof PropertyMap) {
                PropertyMap tempObject = (PropertyMap) o;
                processThermalTemperature(tempObject, data, results);
            } else {
                log_.warn("Unknown array or primitive type in 'Fans' array JSON!");
            }
        }
    }

    private void processThermalFan(PropertyMap fan, EnvelopeData data, List<CommonDataFormat> results) {
        String name = checkNameInJson(data, fan.getStringOrDefault("Name", null));
        if(fan.containsKey("Reading") && fan.get("Reading") != null) {
            double value = fan.getDoubleOrDefault("Reading", -9999.99);
            addToResults(name, value, fan.getStringOrDefault("ReadingUnits", "RPM"), "Thermal",
                    data, results);
        } else {
            log_.warn("JSON for 'odata_context'='%s' has a missing or 'null' key for 'Reading'! " +
                    "Skipping data!", data.context);
        }
    }

    private void processThermalTemperature(PropertyMap temperature, EnvelopeData data, List<CommonDataFormat> results) {
        String name = checkNameInJson(data, temperature.getStringOrDefault("Name", null));
        if(temperature.containsKey("ReadingCelsius") && temperature.get("ReadingCelsius") != null) {
            double value = temperature.getDoubleOrDefault("ReadingCelsius", -9999.99);
            addToResults(name, value, "C", "Thermal", data, results);
        } else {
            log_.warn("JSON for 'odata_context'='%s' has a missing or 'null' key for 'ReadingCelsius'! " +
                    "Skipping data!", data.context);
        }
    }

    private void processPower(PropertyMap item, EnvelopeData data, List<CommonDataFormat> results) {
        PropertyArray powerControl = item.getArrayOrDefault("PowerControl", new PropertyArray());
        PropertyArray voltages = item.getArrayOrDefault("Voltages", new PropertyArray());
        data.prefixName = item.getStringOrDefault("Name", null);
        for(Object o: powerControl) {
            if(o instanceof PropertyMap) {
                PropertyMap powerControlObject = (PropertyMap) o;
                processPowerControl(powerControlObject, data, results);
            } else {
                log_.warn("Unknown array or primitive type in 'PowerControl' array JSON!");
            }
        }
        for(Object o: voltages) {
            if(o instanceof PropertyMap) {
                PropertyMap voltagesObject = (PropertyMap) o;
                processVoltages(voltagesObject, data, results);
            } else {
                log_.warn("Unknown array or primitive type in 'Voltages' array JSON!");
            }
        }
    }

    private void processPowerControl(PropertyMap item, EnvelopeData data, List<CommonDataFormat> results) {
        String name = checkNameInJson(data, item.getStringOrDefault("Name", null));
        if(item.containsKey("PowerConsumedWatts") && item.get("PowerConsumedWatts") != null) {
            double value = item.getDoubleOrDefault("PowerConsumedWatts", -9999.99);
            addToResults(name, value, "W", "Power", data, results);
        } else {
            log_.warn("JSON for 'odata_context'='%s' has a missing or 'null' key for 'PowerConsumedWatts'! " +
                    "Skipping data!", data.context);
        }
    }

    private void processVoltages(PropertyMap item, EnvelopeData data, List<CommonDataFormat> results) {
        String name = checkNameInJson(data, item.getStringOrDefault("Name", null));
        if(item.containsKey("ReadingVolts") && item.get("ReadingVolts") != null) {
            double value = item.getDoubleOrDefault("ReadingVolts", -9999.99);
            addToResults(name, value, "V", "Voltage", data, results);
        } else {
            log_.warn("JSON for 'odata_context'='%s' has a missing or 'null' key for 'ReadingVolts'! " +
                    "Skipping data!", data.context);
        }
    }

    private String checkNameInJson(EnvelopeData data, String name) {
        if (name == null) {
            log_.warn("JSON for 'odata_context'='%s' is missing a 'Name' entry!", data.context);
            name = "<MISSING>";
        }
        return name;
    }

    /////////////////////////
    // Act on Data Section //
    /////////////////////////
    @Override
    public void actOnData(CommonDataFormat data, NetworkListenerConfig config, SystemActions systemActions) {
        setUpConfig(config);
        String type = data.getDescription();
        if(data.getTelemetryDataType() != null && !data.getTelemetryDataType().trim().isEmpty())
            type = type + " (" + data.getTelemetryDataType() + ")";
        // Store raw but normalized data...
        log_.debug("Storing raw telemetry for node %s.", data.getLocation());
        systemActions.storeNormalizedData(type, data.getLocation(),
                data.getNanoSecondTimestamp(), data.getValue());
        // Publish raw but normalized data...
        if(publish_) {
            log_.debug("Publishing raw telemetry for node %s.", data.getLocation());
            systemActions.publishNormalizedData(rawTopic_, type, data.getLocation(),
                    data.getNanoSecondTimestamp(), data.getValue());
        }
        if(data.haveSummary()) {
            // Store and publish aggregate data it available...
            log_.debug("Storing aggregate data: type=%s,location=%s,ts=%d,min=%f,max=%f,agv=%f",
                    type, data.getLocation(), data.getNanoSecondTimestamp(),
                    data.getMinimum(), data.getMaximum(), data.getAverage());
            systemActions.storeAggregatedData(type, data.getLocation(),
                    data.getNanoSecondTimestamp(), data.getMinimum(), data.getMaximum(), data.getAverage());
            if(publish_) {
                log_.debug("Publishing aggregate data: type=%s,location=%s,ts=%d,min=%f,max=%f,agv=%f",
                        type, data.getLocation(), data.getNanoSecondTimestamp(),
                        data.getMinimum(), data.getMaximum(), data.getAverage());
                systemActions.publishAggregatedData(aggregatedTopic_, type, data.getLocation(),
                        data.getNanoSecondTimestamp(), data.getMinimum(), data.getMaximum(), data.getAverage());
            }
        }
    }

    private void setUpConfig(NetworkListenerConfig config) {
        if(!configDone_) {
            configDone_ = true;
            PropertyMap myConfig = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
            if (myConfig != null) {
                Accumulator.useTime_ = myConfig.getBooleanOrDefault("useTimeWindow", false);
                Accumulator.count_ = myConfig.getIntOrDefault("windowSize", 25);
                Accumulator.moving_ = myConfig.getBooleanOrDefault("useMovingAverage", false);
                Accumulator.ns_ = myConfig.getLongOrDefault("timeWindowSeconds", 600) * 1_000_000_000;
                doAggregation_ = myConfig.getBooleanOrDefault("useAggregation", true);
                publish_ = myConfig.getBooleanOrDefault("publish", publish_);
                rawTopic_ = myConfig.getStringOrDefault("publishRawTopic", rawTopic_);
                aggregatedTopic_ = myConfig.getStringOrDefault("publishAggregatedTopic", aggregatedTopic_);
            }
        }
    }

    private synchronized CommonDataFormat aggregateData(CommonDataFormat raw) {
        if(doAggregation_) {
            log_.debug("Aggregating data...");
            String type = raw.getTelemetryDataType();
            String name = raw.getDescription();
            if(type != null && !type.trim().isEmpty())
                name = name + " (" + type + ")";
            String key = raw.getLocation() + ":" + name;
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
    private final Map<String, ContextHandler> dispatchMap_ = new HashMap<>();

    private final Logger log_;
    private boolean publish_ = false;
    private String rawTopic_ = "ucs_raw_data";
    private String aggregatedTopic_ = "ucs_aggregate_data";
    private final ConfigIO parser_;
    private final Map<String, Accumulator> accumulators_ = new HashMap<>();
    private boolean configDone_ = false;
    private boolean doAggregation_ = true;

    static class Accumulator {
        Accumulator(Logger logger) { log_ = logger; }
        void addValue(CommonDataFormat data) {
            values_.add(data.getValue());
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
            double max = -Double.MAX_VALUE;
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

        static boolean useTime_ = false;    // time window if true; count window if false.
        static int count_ = 25;             // 25 sample count window.
        static long ns_ = 600_000_000_000L; // 10 minutes default in nanoseconds.
        static boolean moving_ = false;     // true will give a moving average with the count_ window;
                                            // false is a simple window.
    }
}
