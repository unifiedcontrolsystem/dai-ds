// Copyright (C) 2019-2020 Intel Corporation
//
// SPDX-License-Identifier: Apache-2.0
//
package com.intel.dai.inventory.api.pojo.rqst;

import lombok.ToString;

@ToString
public class RestMethod {
    public String endpoint;
    public String verb;
    public String resource;

    public RestMethod() {
        endpoint = "";
        verb = "";
        resource = "";
    }
}
