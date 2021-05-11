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

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Specific provider for foreign bus RAS Events.
 */
public class RasEventProviderForeignBus implements NetworkListenerProvider, Initializer {
    public RasEventProviderForeignBus(Logger logger) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null) throw new RuntimeException("Failed to create a JSON parser instantiating class " +
                RasEventProviderForeignBus.class.getCanonicalName());
        useBenchmarking_ = Boolean.parseBoolean(System.getenv("USE_BENCHMARKING"));
    }

    @Override
    public void initialize() {
        try {
            InputStream stream = getMetaDataStream();
            if(stream == null)
                throw new RuntimeException("Failed to find the resource file 'resources/ForeignEventMetaData.json'");
            eventMetaData_ = parser_.readConfig(stream).getAsMap();
        } catch(ConfigIOParseException | IOException e) {
            throw new RuntimeException("Failed to load the event metadata", e);
        }
    }

    @Override
    public List<CommonDataFormat> processRawStringData(String subject, String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
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
                        String value = item.getStringOrDefault("Value", "<EMPTY>");
                        String event = lookupForeignEvent(name);
                        CommonDataFormat common = new CommonDataFormat(ts, location, DataType.RasEvent);
                        common.setDescription(name);
                        common.setRasEvent(event, name + " ==>> " + value);
                        results.add(common);
                    } catch(PropertyNotExpectedType e) {
                        log_.warn("One or more of the expected keys was not the right type or malformed!");
                    } catch(ParseException e) {
                        log_.warn("The incoming Timestamp was not valid: %s",
                                item.getStringOrDefault("Timestamp", "<MISSING>"));
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
        systemActions.storeRasEvent(data.getRasEvent(), data.getRasEventPayload(), data.getLocation(),
                data.getNanoSecondTimestamp());
        if(publish_) {
            systemActions.publishRasEvent(topic_, data.getRasEvent(), data.getRasEventPayload(),
                    data.getLocation(), data.getNanoSecondTimestamp());
        }
    }

    private String lookupForeignEvent(String foreignName) {
        return "RasUnknownEvent"; // TODO: Need real lookup here!
    }

    private void getConfig(NetworkListenerConfig config) {
        configured_ = true;
        PropertyMap map = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if(map != null) {
            publish_ = map.getBooleanOrDefault("publish", false);
            topic_ = map.getStringOrDefault("publishTopic", topic_);
        }
    }

    protected InputStream getMetaDataStream() {
        return getClass().getClassLoader().getResourceAsStream("resources/ForeignEventMetaData.json");
    }

    private boolean publish_ = false;
    private String topic_ = "ucs_ras_event";
    private boolean configured_ = false;
    private Logger log_;
    private boolean useBenchmarking_;
    private ConfigIO parser_;
    private PropertyMap eventMetaData_;
    @SuppressWarnings("serial")
    private static final List<String> requiredInMessage_ = new ArrayList<String>() {{
        add("__FullName__");
        add("Value");
        add("Timestamp");
        add("Location");
    }};
}
