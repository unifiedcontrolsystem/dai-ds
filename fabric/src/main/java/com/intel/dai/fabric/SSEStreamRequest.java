// Copyright (C) 2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.fabric;

import com.intel.networking.restclient.SSERequestBuilder;

import java.util.Collection;
import java.util.Map;

public class SSEStreamRequest implements SSERequestBuilder {
    @Override
    public String buildRequest(Collection<String> eventTypes, Map<String, String> builderSpecific) {
        return "?stream_id=dai_ds&batchsize=1024";
    }
}
