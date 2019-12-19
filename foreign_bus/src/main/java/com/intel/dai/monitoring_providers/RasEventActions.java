// Copyright (C) 2019 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring_providers;

import com.intel.logging.Logger;
import com.intel.partitioned_monitor.*;
import com.intel.properties.PropertyMap;

/**
 * Description of class RasEventActions.
 *
 *      published to publisher under topics:
 *          ucs_ras_event
 */
public class RasEventActions implements DataAction, Initializer {
    public RasEventActions(Logger logger) {
        log_ = logger;
    }

    @Override
    public void initialize() { /* Not used but is required. */ }

    @Override
    public void actOnData(CommonDataFormat data, PartitionedMonitorConfig config, SystemActions systemActions) {
        if(!configured_) getConfig(config);
        systemActions.storeRasEvent(data.getRasEvent(), data.getRasEventPayload(), data.getLocation(),
                data.getNanoSecondTimestamp());
        if(publish_) {
            systemActions.publishRasEvent(topic_, data.getRasEvent(), data.getRasEventPayload(),
                    data.getLocation(), data.getNanoSecondTimestamp());
        }
    }

    private void getConfig(PartitionedMonitorConfig config) {
        configured_ = true;
        PropertyMap map = config.getProviderConfigurationFromClassName(getClass().getCanonicalName());
        if(map != null) {
            publish_ = map.getBooleanOrDefault("publish", false);
            topic_ = map.getStringOrDefault("publishTopic", topic_);
        }
    }

    private boolean publish_ = false;
    private String topic_ = "ucs_ras_event";
    private boolean configured_ = false;
    private Logger log_;
}
