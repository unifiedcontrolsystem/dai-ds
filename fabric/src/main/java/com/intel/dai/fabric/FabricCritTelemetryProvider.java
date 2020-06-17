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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FabricCritTelemetryProvider extends FabricAdapter {
    /**
     * Constructor for fabric telemetry class.
     *
     * @param servers The comma separated list of VoltDB servers from the command line arguments.
     * @param logger  The logger to use in this class and derived classes.
     * @param factory The DataStoreFactory object to use in this class and derived classes.
     * @param adapter The Adapter based interface to be used in this class and derived classes.
     */
    protected FabricCritTelemetryProvider(String servers, Logger logger, DataStoreFactory factory, IAdapter adapter) {
        super(servers, logger, factory, adapter);
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
            List<FabricCritTelemetryItem> events = new ArrayList<>();
            for(int i = 0; i < items.size(); i++) {
                PropertyMap event;
                try {
                    event = items.getMap(i);
                } catch(PropertyNotExpectedType e) {
                    logException(e, "Event was not a JSON object but some other type");
                    continue;
                }
                try {
                    convertEvent(event, events);
                } catch(PropertyNotExpectedType e) {
                    logException(e, "Failed to parse event: %s", parser_.toString(event));
                }
            }
            for(FabricCritTelemetryItem item: events)
                processItem(item);
        }
    }

    private void convertEvent(PropertyMap sensor, List<FabricCritTelemetryItem> results)
            throws PropertyNotExpectedType {
        String location = sensor.getString("Location");
        String name = sensor.getString("__FullName__");
        String timestamp = sensor.getString("Timestamp");
        if (name == null || name.trim().isEmpty()) {
            logError("'__FullName__' key was 'null' or empty from the event, skipping this event");
            return;
        }
        if (timestamp == null || timestamp.trim().isEmpty()) {
            logError("'Timestamp' key was 'null' or empty from the event, skipping this event: %s", name);
            return;
        }
        long timestampUs;
        try {
            timestampUs = CommonFunctions.convertISOToLongTimestamp(timestamp);
        } catch (ParseException e) {
            logError("'Timestamp' key was not a valid timestamp, skipping this event: %s", name);
            return;
        }
        String daiLocation;
        try {
            daiLocation = CommonFunctions.convertForeignToLocation(location);
        } catch (ConversionException e) {
            logException(e, "'Location' key was not a known location, skipping this event: %s", name);
            return;
        }
        FabricCritTelemetryItem item = new FabricCritTelemetryItem(timestampUs, name, daiLocation,
                "Unknown Serial No.", "?", parser_.toString(sensor));
        results.add(item);
    }

    private void processItem(FabricCritTelemetryItem item) {
//        try {
            // TODO: Store event here when more information is available.
//        } catch(DataStoreException e) {
//            logException(e, "Failed to store the event: %d", item.toString());
//        }
        publishEventData(item.toString());
    }

    @Override
    protected void processConfigItems(Map<String, String> config) {
        super.processConfigItems(config);
    }

    public static void main(String[] args) {
        String name = FabricCritTelemetryProvider.class.getSimpleName();
        Logger logger = LoggerFactory.getInstance(FabricAdapter.ADAPTER_TYPE, name, "console");
        AdapterSingletonFactory.initializeFactory(FabricAdapter.ADAPTER_TYPE, name, logger);
        IAdapter adapter;
        DataStoreFactory factory = new DataStoreFactoryImpl(args[0], logger);
        try {
            logger.info("Creating base adapter...");
            adapter = AdapterSingletonFactory.getAdapter();
        } catch (IOException e) {
            logger.exception(e, "%s exiting abnormally after initialization failure.", name);
            return;
        }
        FabricCritTelemetryProvider provider = new FabricCritTelemetryProvider(args[0], logger, factory, adapter);
        Map<String,String> config;
        try {
            logger.info("Loading the configuration...");
            XdgConfigFile xdg = new XdgConfigFile("ucs");
            String filename = xdg.FindFile(name);
            if(filename != null && !filename.trim().isEmpty()) {
                config = provider.buildConfig(new File(filename));
                String url = config.get("urlPath");
                if(url == null || url.equals("/"))
                    config.put("urlPath", "/apis/sma-telemetry/v1/stream/cray-fabric-crit-telemetry");
            } else {
                logger.error("Failed to find the configuration");
                return;
            }
        } catch(IOException | ConfigIOParseException e) {
            logger.exception(e, "Failed to load the configuration");
            return;
        }
        logger.info("Starting the " + name + " provider...");
        provider.mainProcessingFlow(config);
        logger.info("Exiting the " + name + " provider");
    }
}
