// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

import com.intel.dai.network_listener.CommonDataFormat;
import com.intel.logging.Logger;
import com.intel.properties.PropertyMap;

import java.util.List;

public class TopicEventLogSel extends TopicBaseProcessor {
    TopicEventLogSel(Logger log, boolean doAggregation) { super(log, doAggregation); }

    @Override
    void processTopic(EnvelopeData data, PropertyMap map, List<CommonDataFormat> results) {
        addToResults(data.topic, "RasHPCMSELEvent", data.originalJsonText, data.location, data.nsTimestamp, results);
    }
}
