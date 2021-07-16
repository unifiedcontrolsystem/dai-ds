// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.config_io.ConfigIO;
import com.intel.config_io.ConfigIOFactory;
import com.intel.config_io.ConfigIOParseException;
import com.intel.dai.dsapi.DataStoreFactory;
import com.intel.dai.foreign_bus.CommonFunctions;
import com.intel.dai.foreign_bus.ConversionException;
import com.intel.dai.network_listener.*;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;
import com.intel.properties.PropertyNotExpectedType;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Specific provider for HPCM RAS Events.
 */
public class RasEventProviderHPCM implements NetworkListenerProviderEx, Initializer {
    public RasEventProviderHPCM(Logger logger) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null) throw new RuntimeException("Failed to create a JSON parser instantiating class " +
                RasEventProviderHPCM.class.getCanonicalName());
    }

    @Override
    public void initialize() {
    }

    @Override
    public void setFactory(DataStoreFactory factory) {
        envelopeProcessor_ = new HPCMEnvelopeProcessing(log_, factory);
    }

    @Override
    public List<CommonDataFormat> processRawStringData(String topic, String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        getConfig(config);
        log_.debug("*** Topic: '%s'; Message: %s", topic, data);
        List<CommonDataFormat> results = new ArrayList<>();
        List<String> sObjects = CommonFunctions.breakupStreamedJSONMessages(data);
        for(String streamObject: sObjects) {
            PropertyMap item;
            try {
                item = parser_.fromString(streamObject).getAsMap();
            } catch (ConfigIOParseException e) {
                log_.warn("Failed to parse event string: '%s'", data);
                throw new NetworkListenerProviderException("Failed to parse incoming event", e);
            }
            try {
                EnvelopeData envelope = envelopeProcessor_.getLocationAndTimestamp(topic, item);
                envelope.setOriginalJson(streamObject);
                dispatchProcessing(results, item, envelope);
            } catch(PropertyNotExpectedType | ParseException | ConversionException e) {
                log_.warn("Failed to parse JSON envelope data, skipping this item!");
                log_.debug("Trapped Exception: %s", e.toString());
            }
        }
        return results;
    }

    private void dispatchProcessing(List<CommonDataFormat> results, PropertyMap item, EnvelopeData envelopeData) {
        if(envelopeData.topic != null) {
            TopicBaseProcessor dispatch = processorMap_.getOrDefault(envelopeData.topic, null);
            if(dispatch != null) {
                dispatch.processTopic(envelopeData, item, results);
            } else
                log_.warn("Missing parsing topic handler for topic='%s'! Skipping data!", envelopeData.topic);
        } else
            log_.warn("Null 'topic' for incoming JSON! Skipping data!");
    }

    private void getConfig(NetworkListenerConfig config) {
        if(!configured_) {
            configured_ = true;
            PropertyMap map = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
            if (map != null) {
                publish_ = map.getBooleanOrDefault("publish", false);
                topic_ = map.getStringOrDefault("publishTopic", topic_);
            }
            processorMap_.put("log_sel", new TopicEventLogSel(log_, false));
            processorMap_.put("SYSLOG", new TopicEventSyslog(log_, false));
        }
    }

    /////////////////////////
    // Act on Data Section //
    /////////////////////////
    @Override
    public void actOnData(CommonDataFormat data, NetworkListenerConfig config, SystemActions systemActions) {
        systemActions.storeRasEvent(data.getRasEvent(), data.getRasEventPayload(), data.getLocation(),
                data.getNanoSecondTimestamp());
        if(publish_) {
            systemActions.publishRasEvent(topic_, data.getRasEvent(), data.getRasEventPayload(),
                    data.getLocation(), data.getNanoSecondTimestamp());
        }
    }

    private final Map<String, TopicBaseProcessor> processorMap_ = new HashMap<>();
    private boolean publish_ = false;
    private String topic_ = "ucs_ras_event";
    private boolean configured_ = false;
    private final Logger log_;
    private final ConfigIO parser_;
    private HPCMEnvelopeProcessing envelopeProcessor_ = null;
}
