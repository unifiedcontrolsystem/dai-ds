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
 * Specific provider for environmental data from HPCM.
 */
public class EnvironmentalProviderHPCM implements NetworkListenerProviderEx, Initializer {
    public EnvironmentalProviderHPCM(Logger logger) {
        log_ = logger;
        parser_ = ConfigIOFactory.getInstance("json");
        if(parser_ == null) throw new RuntimeException("Failed to create a JSON parser instantiating class " +
                EnvironmentalProviderHPCM.class.getCanonicalName());
    }

    @Override
    public void initialize() {}

    @Override
    public void setFactory(DataStoreFactory factory) {
        envelopeProcessor_ = new HPCMEnvelopeProcessing(log_, factory);
    }

    ////////////////////////
    // Parse Data Section //
    ////////////////////////
    @Override
    public List<CommonDataFormat> processRawStringData(String topic, String data, NetworkListenerConfig config)
            throws NetworkListenerProviderException {
        setUpConfig(config);
        List<CommonDataFormat> results = new ArrayList<>();
        log_.debug("*** Topic: '%s'; Message: %s", topic, data);
        List<String> sObjects = CommonFunctions.breakupStreamedJSONMessages(data);
        for(String streamObject: sObjects) {
            PropertyMap item;
            try {
                item = parser_.fromString(streamObject).getAsMap();
            } catch (ConfigIOParseException e) {
                log_.warn("Failed to parse telemetry string: '%s'", data);
                throw new NetworkListenerProviderException("Failed to parse incoming data", e);
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
        boolean doAggregation = true;
        if(!configDone_) {
            configDone_ = true;
            PropertyMap myConfig = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
            if (myConfig != null) {
                Accumulator.useTime_ = myConfig.getBooleanOrDefault("useTimeWindow", false);
                Accumulator.count_ = myConfig.getIntOrDefault("windowSize", 25);
                Accumulator.moving_ = myConfig.getBooleanOrDefault("useMovingAverage", false);
                Accumulator.ns_ = myConfig.getLongOrDefault("timeWindowSeconds", 600) * 1_000_000_000L;
                doAggregation = myConfig.getBooleanOrDefault("useAggregation", true);
                publish_ = myConfig.getBooleanOrDefault("publish", publish_);
                rawTopic_ = myConfig.getStringOrDefault("publishRawTopic", rawTopic_);
                aggregatedTopic_ = myConfig.getStringOrDefault("publishAggregatedTopic", aggregatedTopic_);
            }

            // Setup supported topic JSON handler instances.
            processorMap_.put("metric_network_fabric", new TopicMetricNetworkFabric(log_, doAggregation));
            processorMap_.put("metric_network_fabricport", new TopicMetricNetworkFabricPort(log_, doAggregation));
            processorMap_.put("pdu_energy", new TopicMetricPduEnergy(log_, doAggregation));
            processorMap_.put("pdu_power", new TopicMetricPduPower(log_, doAggregation));
            processorMap_.put("sensors_node", new TopicMetricSensorNode(log_, doAggregation));
            processorMap_.put("metric_cooldev", new TopicMetricCoolDev(log_, doAggregation));
            processorMap_.put("pcm-monitoring", new TopicMetricPcmMonitoring(log_, doAggregation));
        }
    }

    private final Map<String, TopicBaseProcessor> processorMap_ = new HashMap<>();
    private final Logger log_;
    private boolean publish_ = false;
    private String rawTopic_ = "ucs_raw_data";
    private String aggregatedTopic_ = "ucs_aggregate_data";
    private final ConfigIO parser_;
    private boolean configDone_ = false;
    private HPCMEnvelopeProcessing envelopeProcessor_ = null;
}
