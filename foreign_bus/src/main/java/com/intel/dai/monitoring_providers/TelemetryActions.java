// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring_providers;

import com.intel.logging.Logger;
import com.intel.partitioned_monitor.*;
import com.intel.properties.PropertyMap;

/**
 * Description of class TelemetryActions. Publishing occurs to "ucs_raw_data" and "ucs_aggregate_data".
 *
 *      published to publisher under topics:
 *          ucs_raw_data
 *          ucs_aggregate_data
 */
public class TelemetryActions implements DataAction, Initializer {
    public TelemetryActions(Logger logger) {
        log_ = logger;
    }

    @Override
    public void initialize() { /* Not used but is required. */ }

    @Override
    public void actOnData(CommonDataFormat data, PartitionedMonitorConfig config, SystemActions systemActions) {
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
        if(data.getAverage() != Double.MIN_VALUE) { // Store and publish aggregate data it available...
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
    }

    private void getConfig(PartitionedMonitorConfig config) {
        configured_ = true;
        PropertyMap map = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if(map != null) {
            publish_ = map.getBooleanOrDefault("publish", false);
            aggregatedTopic_ = map.getStringOrDefault("publishAggregatedTopic", aggregatedTopic_);
            rawTopic_ = map.getStringOrDefault("publishRawTopic", rawTopic_);
        }
    }

    private Logger log_;
    private boolean configured_ = false;
    private boolean publish_ = false;
    private String rawTopic_ = "ucs_raw_data";
    private String aggregatedTopic_ = "ucs_aggregate_data";
}
