// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.fabric;

import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.AdapterSingletonFactory;
import com.intel.dai.IAdapter;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.dsimpl.DataStoreFactoryImpl;
import com.intel.dai.exceptions.AdapterException;
import com.intel.dai.exceptions.DataStoreException;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.logging.Logger;
import com.intel.logging.LoggerFactory;
import com.intel.properties.PropertyArray;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;
import com.intel.xdg.XdgConfigFile;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.*;
import java.util.regex.Pattern;

public final class FabricPerfTelemetryProvider extends FabricAdapter {
    /**
     * Constructor for fabric telemetry class.
     *
     * @param servers The comma separated list of VoltDB servers from the command line arguments.
     * @param logger  The logger to use in this class and derived classes.
     * @param factory The DataStoreFactory object to use in this class and derived classes.
     * @param adapter The Adapter based interface to be used in this class and derived classes.
     */
    protected FabricPerfTelemetryProvider(String servers, Logger logger, DataStoreFactory factory, IAdapter adapter) {
        super(servers, logger, factory, adapter);
    }

    /**
     * Get the adapter type from the implementation class.
     *
     * @return The adapter type name.
     */
    @Override
    final protected String adapterType() {
        return ADAPTER_TYPE;
    }

    /**
     * Required method for {@link com.intel.networking.sink.NetworkDataSinkDelegate} that must be implemented by the
     * provider.
     *
     * @param subject The message subject from {@link com.intel.networking.sink.NetworkDataSink} instance
     * @param message The actual raw JSON message from the {@link com.intel.networking.sink.NetworkDataSink}
     */
    @Override
    final protected void processRawMessage(String subject, String message) {
        if(matchSubject(subject)) {
            PropertyArray items;
            try {
                items = CommonFunctions.parseForeignTelemetry(message);
            } catch(ConfigIOParseException e) {
                logException(e, "Failed to parse incoming message: %s",
                        message.substring(0, Math.min(INSTANCE_DATA_MAX, message.length())));
                return;
            }
            List<FabricTelemetryItem> sensors = new ArrayList<>();
            for(int i = 0; i < items.size(); i++) {
                PropertyMap sensor;
                try {
                    sensor = items.getMap(i);
                } catch(PropertyNotExpectedType e) {
                    logException(e, "Event was not a JSON object but some other type");
                    continue;
                }
                try {
                    convertSensor(sensor, sensors);
                } catch(PropertyNotExpectedType e) {
                    logException(e, "Failed to parse event: %s", parser_.toString(sensor));
                }
            }
            for(FabricTelemetryItem item: sensors)
                processItem(item);
        }
    }

    private void convertSensor(PropertyMap sensor, List<FabricTelemetryItem> results)
            throws PropertyNotExpectedType {
        String location = sensor.getString("Location");
        String name = sensor.getString("__FullName__");
        String timestamp = sensor.getString("Timestamp");
        String sValue = sensor.getString("Value"); // The data examples had values as String types....
        if (name == null || name.trim().isEmpty()) {
            logError("'__FullName__' key was 'null' or empty from the sensor, skipping this sensor");
            return;
        }
        if (timestamp == null || timestamp.trim().isEmpty()) {
            logError("'Timestamp' key was 'null' or empty from the sensor, skipping this sensor: %s", name);
            return;
        }
        if(sValue == null) {
            logError("The 'Value' key was missing or 'null': %s", name);
            return;
        }
        sValue = sValue.trim();
        if(sValue.isEmpty()) {
            logError("The 'Value' key was empty: %s", name);
            return;
        }
        double value;
        long timestampUs;
        try {
            timestampUs = CommonFunctions.convertISOToLongTimestamp(timestamp);
        } catch (ParseException e) {
            logError("'Timestamp' key was not a valid timestamp, skipping this sensor: %s", name);
            return;
        }
        String daiLocation;
        try {
            daiLocation = CommonFunctions.convertForeignToLocation(location);
        } catch (ConversionException e) {
            logException(e, "'Location' key was not a known location, skipping this sensor: %s", name);
            return;
        }
        try {
            value = Double.parseDouble(sValue);
        } catch(NumberFormatException e) {
            logError("The 'Value' key was not a valid number: %s", name);
            return;
        }
        FabricTelemetryItem item = new FabricTelemetryItem(timestampUs, name, daiLocation, value, inBlacklist(name));
        results.add(item);
    }

    private void processItem(FabricTelemetryItem item) {
        if(!item.skipAggregationAndStore()) {
            aggregateData(item);
            try {
                if (hasAggregatedData(item))
                    storeTelemetry_.logEnvDataAggregated(item.getName(), item.getLocation(), item.getTimestamp(),
                            item.getMaximum(), item.getMinimum(), item.getAverage(), ADAPTER_TYPE,
                            workQueue_.baseWorkItemId());
            } catch (DataStoreException e) {
                log_.exception(e, "Failed to store the environmental data item: ", item.toString());
                try {
                    logSoftwareRasEvent("Failed to store the environmental data item", item.toString());
                } catch (AdapterException e2) {
                    log_.exception(e, "Failed to log the environmental data store failure as a RAS event",
                            item.toString());
                }
            }
            if (hasAggregatedData(item))
                publishAggregatedData(item.toString());
        }
        publishRawData(item.toString());
    }

    @Override
    protected void processConfigItems(Map<String, String> config) {
        super.processConfigItems(config);
        Accumulator.useTime_ = Boolean.parseBoolean(config.getOrDefault("aggregateUseTime", "false"));
        Accumulator.count_ = Integer.parseInt(config.getOrDefault("aggregateCount", "25"));
        Accumulator.moving_ = Boolean.parseBoolean(config.getOrDefault("aggregateUseMovingAverage", "false"));
        Accumulator.us_ = Long.parseLong(config.getOrDefault("aggregateTimeWindowSeconds", "300")) * 1_000_000L;
        aggregationEnabled_ = Boolean.parseBoolean(config.getOrDefault("aggregateEnabled", "true"));
    }

    private FabricTelemetryItem aggregateData(FabricTelemetryItem raw) {
        if(aggregationEnabled_) {
            log_.debug("Aggregating data...");
            String key = raw.getLocation() + ":" + raw.getName();
            Accumulator accum = accumulators_.getOrDefault(key, null);
            if (accum == null) {
                accum = new Accumulator(log_);
                accumulators_.put(key, accum);
            }
            accum.addValue(raw);
        }
        return raw;
    }

    private boolean hasAggregatedData(FabricTelemetryItem data) {
        return data.haveStatistics();
    }

    public static void main(String[] args) {
        String name = FabricPerfTelemetryProvider.class.getSimpleName();
        if(args == null || args.length != 3)
            throw new RuntimeException(String.format("Wrong number of arguments for this provider (%s), must " +
                    "use 3 arguments: voltdb_servers, location, and hostname in that order", name));
        Logger logger = LoggerFactory.getInstance(ADAPTER_TYPE, name, "console");
        AdapterSingletonFactory.initializeFactory(ADAPTER_TYPE, name, logger);
        IAdapter adapter;
        DataStoreFactory factory = new DataStoreFactoryImpl(args[0], logger);
        try {
            logger.info("Creating base adapter...");
            adapter = AdapterSingletonFactory.getAdapter();
        } catch (IOException e) {
            logger.exception(e, "%s exiting abnormally after initialization failure.", name);
            return;
        }
        FabricPerfTelemetryProvider provider = new FabricPerfTelemetryProvider(args[0], logger, factory, adapter);
        Map<String,String> config;
        try {
            logger.info("Loading the configuration '%s.json'...", name);
            XdgConfigFile xdg = new XdgConfigFile("ucs");
            String filename = xdg.FindFile(name + ".json");
            if(filename != null && !filename.trim().isEmpty()) {
                config = provider.buildConfig(new File(filename));
                config.put("requestBuilderSelectors.stream_id","dai-fabric-perf");
                String url = config.get("urlPath");
                if(url == null || url.equals("/"))
                    config.put("urlPath", "/apis/sma-telemetry/v1/stream/cray-fabric-perf-telemetry");
            } else {
                logger.error("Failed to find the configuration");
                return;
            }
        } catch(IOException | ConfigIOParseException e) {
            logger.exception(e, "Failed to load the configuration");
            return;
        }
        logger.info("Starting the " + name + " provider...");
        provider.mainProcessingFlow(config, args[1]);
        logger.info("Exiting the " + name + " provider");
    }

    private boolean aggregationEnabled_ = true;
    private Map<String, Accumulator> accumulators_ = new HashMap<>();
    private static final String ADAPTER_TYPE = "FABRICPERF";
}
