// Copyright (C) 2021 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.monitoring;

class EnvelopeData {
    EnvelopeData(String topic, long nsTimestamp, String location) {
        this.topic = topic;
        this.nsTimestamp = nsTimestamp;
        this.location = location;
    }

    void adjustTimestamp(long multiplyFactor) {
        nsTimestamp *= multiplyFactor;
    }

    void appendNameToTopic(String name) {
        topic += "." + name;
    }

    void setOriginalJson(String json) { originalJsonText = json; }

          String topic;
          long nsTimestamp;
    final String location;
          String originalJsonText;
}
