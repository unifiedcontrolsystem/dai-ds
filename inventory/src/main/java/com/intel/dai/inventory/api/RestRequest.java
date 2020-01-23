// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api;

import lombok.ToString;

@ToString
public class RestRequest {
    String endpoint;
    String verb;
    String resource;

    public RestRequest() {
        endpoint = "";
        verb = "";
        resource = "";
    }
}
