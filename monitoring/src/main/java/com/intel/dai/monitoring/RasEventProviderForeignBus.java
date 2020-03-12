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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Description of class RasEventTransformer.
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
    public List<CommonDataFormat> processRawStringData(String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        if(!configDone_)
            setUpConfig(config);
        try {
            PropertyMap message = parser_.fromString(data).getAsMap();
            List<CommonDataFormat> commonList = new ArrayList<>();
            if(!checkMessage(message))
                return commonList;
            String[] foreignLocations = message.getStringOrDefault("location", "").split(",");
            String eventType = message.getStringOrDefault("event-type", null);
            String ucsEvent = eventMetaData_.getStringOrDefault(eventType, "RasMntrForeignUnknownEvent");
            for(String foreignLocation: foreignLocations) {
                try {
                    String location = CommonFunctions.convertForeignToLocation(foreignLocation);
                    CommonDataFormat common = new CommonDataFormat(
                            CommonFunctions.convertISOToLongTimestamp(message.getString("timestamp")),
                            location, DataType.RasEvent);
                    String payload = message.getStringOrDefault("payload", message.getStringOrDefault("message", ""));
                    common.setRasEvent(ucsEvent, payload);
                    common = suppressEvents(common);
                    if (common != null)
                        commonList.add(common);
                } catch(ConversionException e) {
                    log_.warn("Failed to convert a foreign location to a DAI location, skipping data element for " +
                            "location %s", foreignLocation);
                    log_.debug("Skipped location for data: %s", data);
                }
            }
            return commonList;
        } catch(ConfigIOParseException | PropertyNotExpectedType | ParseException e) {
            throw new NetworkListenerProviderException("Failed to load the event metadata");
        }
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

    private boolean checkMessage(PropertyMap message) throws NetworkListenerProviderException {
        for(String required: requiredInMessage_)
            if (!message.containsKey(required))
                return false;
        return true;
    }

    private CommonDataFormat suppressEvents(CommonDataFormat raw) {
        if(doSuppression_) {
            log_.debug("Suppressing events...");
            String key = raw.getLocation() + ":" + raw.getRasEvent();
            Accumulator accum = accumulators_.getOrDefault(key, null);
            if (accum == null) {
                accum = new Accumulator(parser_);
                accumulators_.put(key, accum);
            }
            return accum.addEvent(raw);
        }
        return raw;
    }

    private void setUpConfig(NetworkListenerConfig config) {
        configDone_ = true;
        PropertyMap myConfig = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if(myConfig != null) {
            Accumulator.suppressionCount_ = myConfig.getIntOrDefault("suppressionCount", Accumulator.suppressionCount_);
            Accumulator.suppressionWindowsSeconds_ = myConfig.getIntOrDefault("suppressionWindowSeconds",
                    Accumulator.suppressionWindowsSeconds_);
            doSuppression_ = myConfig.getBooleanOrDefault("useRepeatSuppression", doSuppression_);
        }
    }


    private boolean publish_ = false;
    private String topic_ = "ucs_ras_event";
    private boolean configured_ = false;
    private Logger log_;
    private boolean useBenchmarking_;
    private ConfigIO parser_;
    private PropertyMap eventMetaData_;
    private boolean configDone_ = false;
            boolean doSuppression_ = false;
    private Map<String, Accumulator> accumulators_ = new HashMap<>();
    private static final String[] requiredInMessage_ = new String[] {"event-type", "timestamp", "location"};
    private long accumulator_ = 0L;
    private long deltaRunningAvg_ = 0L;

    static class Accumulator {
        Accumulator(ConfigIO parser) { parser_ = parser; }

        CommonDataFormat addEvent(CommonDataFormat event) {
            synchronized (this) {
                if (suppressionWindowsNanoSeconds_ == Long.MIN_VALUE)
                    suppressionWindowsNanoSeconds_ = (long)suppressionWindowsSeconds_ * 1000000L; // to ns once...
                events_.add(event);
                if (timeExpired() || events_.size() >= suppressionCount_)
                    return doSuppressionExit(event);
                else if (events_.size() == 1)
                    return event;
                return null;
            }
        }

        CommonDataFormat doSuppressionExit(CommonDataFormat last) {
            PropertyArray payload = new PropertyArray();
            PropertyMap value = new PropertyMap();
            for(CommonDataFormat event: events_) {
                value.put("nanoSecondTimeStamp", event.getNanoSecondTimestamp());
                value.put("instanceData", event.getRasEventPayload());
                payload.add(value);
                value.clear();
            }
            CommonDataFormat aggregated = new CommonDataFormat(last.getNanoSecondTimestamp(), last.getLocation(),
                    last.getDataType());
            aggregated.setRasEvent(last.getRasEvent(), parser_.toString(payload));
            events_.clear();
            return aggregated;
        }

        boolean timeExpired() {
            long diff = events_.get(events_.size() - 1).getNanoSecondTimestamp() -
                    events_.get(0).getNanoSecondTimestamp();
            return diff >= suppressionWindowsNanoSeconds_;
        }

        static int suppressionCount_ = 100;
        static int suppressionWindowsSeconds_ = 60;
        private static long suppressionWindowsNanoSeconds_ = Long.MIN_VALUE;
        private List<CommonDataFormat> events_ = new ArrayList<>();
        private ConfigIO parser_;
    }
}
